package de.uni_potsdam.hpi.asg.logictool.synthesis.helper;

/*
 * Copyright (C) 2015 Norman Kluge
 * 
 * This file is part of ASGlogic.
 * 
 * ASGlogic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ASGlogic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ASGlogic.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.CFRegion;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.Cubes;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.Regions;

public class MonotonicCoverChecker {
    private static final Logger       logger = LogManager.getLogger();

    private Set<State>                states;
    private EspressoTable             table;
    private Map<Signal, Regions>      allregions;
    private SortedSet<Signal>         checkSignals;
    private SortedSet<Signal>         allSignals;
    private Map<Signal, Cubes>        cubemap;
    private String                    resetname;

    private Map<CFRegion, Set<State>> monotonicityViolatingStates;
    private SortedSet<Signal>         nonMonotonSignals;

    public MonotonicCoverChecker(StateGraph stateGraph, SortedSet<Signal> signals, Map<Signal, Regions> regions, EspressoTable table, String resetname) {
        this.states = stateGraph.getStates();
        this.table = table;
        this.allregions = regions;
        this.checkSignals = signals;
        this.allSignals = stateGraph.getAllSignals();
        this.cubemap = new HashMap<Signal, Cubes>();
        this.resetname = resetname;
    }

    public boolean check() {
        if(!checkPreCondition()) {
            logger.error("Monotonic Cover: Precondition: FAILURE");
            return false;
        }
        if(!createDataStructure()) {
            logger.error("Monotonic Cover: 1st & 2nd condition: FAILURE");
            return false;
        }
        if(!check3rd()) {
            logger.warn("Monotonic Cover: 3rd condition: FAILURE. Retrying..");
            return false;
        }

        logger.info("Monotonic Cover: OK");
        return true;
    }

    private boolean checkPreCondition() { // Only one cube for ER
        boolean retVal = true;
        for(Signal sig : checkSignals) {
            if(sig.getType() == SignalType.internal || sig.getType() == SignalType.output) {
                Regions regions = allregions.get(sig);
                int id = 1;
                while(table.getTable().columnMap().containsKey(sig.getName() + "__set_" + id)) {
                    int num = 0;
                    for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().columnMap().get(sig.getName() + "__set_" + id).entrySet()) {
                        if(entry.getValue() == EspressoValue.one) {
                            num++;
                        }
                    }
                    if(num != 1) {
                        retVal = false;
                        logger.error(sig.getName() + "__set_" + id + " not in one cube");
                    }
                    id++;
                }
                if((id - 1) != regions.getRisingRegions().size()) {
                    retVal = false;
                    logger.error(sig.getName() + "__set has " + (id - 1) + " cubes, but the model has " + regions.getRisingRegions().size() + " cubes");
                }
                id = 1;
                while(table.getTable().columnMap().containsKey(sig.getName() + "__reset_" + id)) {
                    int num = 0;
                    for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().columnMap().get(sig.getName() + "__reset_" + id).entrySet()) {
                        if(entry.getValue() == EspressoValue.one) {
                            num++;
                        }
                    }
                    if(num != 1) {
                        retVal = false;
                        logger.error(sig.getName() + "__reset_" + (id - 1) + " not in one cube");
                    }
                    id++;
                }
                if((id - 1) != regions.getFallingRegions().size()) {
                    retVal = false;
                    logger.error(sig.getName() + "__reset has " + (id - 1) + " cubes, but the model has " + regions.getFallingRegions().size() + " cubes");
                }
            }
        }
        return retVal;
    }

    private boolean createDataStructure() { // check 1st & 2nd condition
        boolean retVal = true;
        for(Signal sig : checkSignals) {
            if(sig.getType() == SignalType.internal || sig.getType() == SignalType.output) {
                Regions regions = allregions.get(sig);
                BiMap<CFRegion, EspressoTerm> risingCubes = HashBiMap.create();
                BiMap<CFRegion, EspressoTerm> fallingCubes = HashBiMap.create();
                int id = 1;
                for(CFRegion reg : regions.getRisingRegions()) {
                    int num = 0;
                    for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().columnMap().get(sig.getName() + "__set_" + id).entrySet()) {
                        if(entry.getValue() == EspressoValue.one) {
                            if(check1st(reg, entry.getKey())) {
                                if(check2nd(reg, entry.getKey())) {
                                    risingCubes.put(reg, entry.getKey());
                                    num++;
                                }
                            }
                        }
                    }
                    if(num == 0) {
                        retVal = false;
                        logger.error("No matching cube found");
                    } else if(num > 1) {
                        logger.warn("More than one matching cube found");
                    }
                    id++;
                }
                id = 1;
                for(CFRegion reg : regions.getFallingRegions()) {
                    int num = 0;
                    for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().columnMap().get(sig.getName() + "__reset_" + id).entrySet()) {
                        if(entry.getValue() == EspressoValue.one) {
                            if(check1st(reg, entry.getKey())) {
                                if(check2nd(reg, entry.getKey())) {
                                    fallingCubes.put(reg, entry.getKey());
                                    num++;
                                }
                            }
                        }
                    }
                    if(num == 0) {
                        retVal = false;
                        logger.error("No matching cube found");
                    } else if(num > 1) {
                        logger.warn("More than one matching cube found");
                    }
                    id++;
                }
                cubemap.put(sig, new Cubes(risingCubes, fallingCubes));
            }
        }
        return retVal;
    }

    private boolean check1st(CFRegion reg, EspressoTerm term) { // 1st: Cover: Cj(a*) covers all states ERj(a*)
        for(State state : reg.getExcitationRegion()) {
            BitSet stateBin = state.getBinaryRepresentationNormalised(allSignals);
            if(!term.evaluate(stateBin, allSignals, resetname)) {
                return false;
            }
        }
        return true;
    }

    private boolean check2nd(CFRegion region, EspressoTerm term) { // 2nd: Onehot: Cj(a*) not covers states outside ERj(a*) U QRj(a*)
        for(State state : states) {
            if(region.getExcitationRegion().contains(state)) {
                continue;
            }
            if(region.getQuiescentRegion().contains(state)) {
                continue;
            }
            BitSet stateBin = state.getBinaryRepresentationNormalised(allSignals);
            if(term.evaluate(stateBin, allSignals, resetname)) {
                return false;
            }
        }
        return true;
    }

    private boolean check3rd() { // 3rd: Monotonicity: Cj(a*) can fall at most once along any state sequence within QRj(a*)
        boolean retVal = true;
        monotonicityViolatingStates = new HashMap<CFRegion, Set<State>>();
        nonMonotonSignals = new TreeSet<Signal>();
        for(Signal sig : checkSignals) {
            if(sig.getType() == SignalType.output || sig.getType() == SignalType.internal) {
                boolean isMonotonous = true;
                Cubes cubes = cubemap.get(sig);
                for(Entry<CFRegion, EspressoTerm> entry : cubes.getRisingCubes().entrySet()) {
                    Set<State> notworking = checkMonotonicity(entry.getKey(), entry.getValue());
                    if(notworking.size() != 0) {
                        retVal = false;
                        isMonotonous = false;
                        monotonicityViolatingStates.put(entry.getKey(), notworking);
                        logger.warn("A cover cube for signal " + sig.getName() + "+ is not monotonous");
                    }
                }
                for(Entry<CFRegion, EspressoTerm> entry : cubes.getFallingCubes().entrySet()) {
                    Set<State> notworking = checkMonotonicity(entry.getKey(), entry.getValue());
                    if(notworking.size() != 0) {
                        retVal = false;
                        isMonotonous = false;
                        monotonicityViolatingStates.put(entry.getKey(), notworking);
                        logger.warn("A cover cube for signal " + sig.getName() + "- is not monotonous");
                    }
                }
                if(!isMonotonous) {
                    nonMonotonSignals.add(sig);
                }
            }
        }
        return retVal;
    }

    private Set<State> checkMonotonicity(CFRegion startRegion, EspressoTerm term) {
        Set<State> notworkingStates = new HashSet<State>();
        for(State state : startRegion.getQuiescentRegion()) {
            boolean cubeactive = term.evaluate(state.getBinaryRepresentationNormalised(allSignals), allSignals, resetname);
            for(State next : state.getNextStates().values()) {
                boolean nextCubeActive = term.evaluate(next.getBinaryRepresentationNormalised(allSignals), allSignals, resetname);
                if(!cubeactive && nextCubeActive) {
                    notworkingStates.add(next);
                }
            }
        }
        return notworkingStates;
    }

    public Map<CFRegion, Set<State>> getMonotonicityViolatingStates() {
        return monotonicityViolatingStates;
    }

    public SortedSet<Signal> getNonMonotonSignals() {
        return nonMonotonSignals;
    }
}
