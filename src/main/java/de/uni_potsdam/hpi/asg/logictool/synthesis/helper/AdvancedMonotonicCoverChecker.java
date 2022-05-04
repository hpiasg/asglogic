package de.uni_potsdam.hpi.asg.logictool.synthesis.helper;

/*
 * Copyright (C) 2015 - 2018 Norman Kluge
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
import java.util.List;
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
import de.uni_potsdam.hpi.asg.common.stggraph.stategraph.State;
import de.uni_potsdam.hpi.asg.common.stggraph.stategraph.StateGraph;
import de.uni_potsdam.hpi.asg.common.stghelper.model.CFRegion;
import de.uni_potsdam.hpi.asg.common.stghelper.model.Regions;
import de.uni_potsdam.hpi.asg.logictool.synthesis.AdvancedSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.Cubes;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;

public class AdvancedMonotonicCoverChecker {
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
    private Map<Signal, Boolean>      celemCubesMonotonicCover;
    private Map<Signal, Boolean>      highCubesMonotonicCover;
    private Map<Signal, Boolean>      lowCubesMonotonicCover;

    public AdvancedMonotonicCoverChecker(StateGraph stateGraph, SortedSet<Signal> signals, Map<Signal, Regions> regions, EspressoTable table, String resetname) {
        this.states = stateGraph.getStates();
        this.table = table;
        this.allregions = regions;
        this.checkSignals = signals;
        this.allSignals = stateGraph.getAllSignals();
        this.cubemap = new HashMap<Signal, Cubes>();
        this.resetname = resetname;
        this.celemCubesMonotonicCover = new HashMap<>();
        this.highCubesMonotonicCover = new HashMap<>();
        this.lowCubesMonotonicCover = new HashMap<>();
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

    private boolean checkPreCondition() { // Only one cube per region & number of regions
        boolean retVal = true;
        for(Signal sig : checkSignals) {
            if(sig.isInternalOrOutput()) {
                Regions regions = allregions.get(sig);

                celemCubesMonotonicCover.put(sig, checkSingleCubes(sig, AdvancedSynthesis.setEnding, regions.getRisingRegions().size(), true));
                if(celemCubesMonotonicCover.get(sig)) {
                    celemCubesMonotonicCover.put(sig, checkSingleCubes(sig, AdvancedSynthesis.resetEnding, regions.getFallingRegions().size(), true));
                }

                highCubesMonotonicCover.put(sig, checkSingleCubes(sig, AdvancedSynthesis.highEnding, regions.getRisingRegions().size(), false));
                lowCubesMonotonicCover.put(sig, checkSingleCubes(sig, AdvancedSynthesis.lowEnding, regions.getFallingRegions().size(), false));

                if(!celemCubesMonotonicCover.get(sig) && !highCubesMonotonicCover.get(sig) && !lowCubesMonotonicCover.get(sig)) {
                    retVal = false;
                }
            }
        }
        return retVal;
    }

    private boolean checkSingleCubes(Signal sig, String suffix, int numberOfRegions, boolean printErrors) { // Only one cube per region & number of regions
        int id = 1;
        boolean retVal = true;
        while(table.getTable().columnMap().containsKey(sig.getName() + suffix + "_" + id)) {
            int num = 0;
            for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().columnMap().get(sig.getName() + suffix + "_" + id).entrySet()) {
                if(entry.getValue() == EspressoValue.one) {
                    num++;
                }
            }
            if(num != 1) {
                retVal = false;
                if(printErrors) {
                    logger.warn(sig.getName() + suffix + "_" + id + " not in one cube");
                }
            }
            id++;
        }
        if((id - 1) != numberOfRegions) {
            retVal = false;
            if(printErrors) {
                logger.warn(sig.getName() + suffix + " has " + (id - 1) + " cubes, but the model has " + numberOfRegions + " cubes");
            }
        }
        return retVal;
    }

    private boolean createDataStructure() { // check 1st & 2nd condition
        boolean retVal = true;
        for(Signal sig : checkSignals) {
            if(sig.isInternalOrOutput()) {
                Regions regions = allregions.get(sig);
                BiMap<CFRegion, EspressoTerm> risingCubes = HashBiMap.create();
                BiMap<CFRegion, EspressoTerm> fallingCubes = HashBiMap.create();
                BiMap<CFRegion, EspressoTerm> highCubes = HashBiMap.create();
                BiMap<CFRegion, EspressoTerm> lowCubes = HashBiMap.create();

                if(celemCubesMonotonicCover.get(sig)) {
                    celemCubesMonotonicCover.put(sig, check1stAnd2nd(sig, AdvancedSynthesis.setEnding, regions.getRisingRegions(), risingCubes, true, false));
                }
                if(celemCubesMonotonicCover.get(sig)) {
                    celemCubesMonotonicCover.put(sig, check1stAnd2nd(sig, AdvancedSynthesis.resetEnding, regions.getFallingRegions(), fallingCubes, true, false));
                }

                if(highCubesMonotonicCover.get(sig)) {
                    highCubesMonotonicCover.put(sig, check1stAnd2nd(sig, AdvancedSynthesis.highEnding, regions.getRisingRegions(), highCubes, false, true));
                }
                if(lowCubesMonotonicCover.get(sig)) {
                    lowCubesMonotonicCover.put(sig, check1stAnd2nd(sig, AdvancedSynthesis.lowEnding, regions.getFallingRegions(), lowCubes, false, true));
                }

                cubemap.put(sig, new Cubes(risingCubes, fallingCubes, highCubes, lowCubes));

                if(!celemCubesMonotonicCover.get(sig) && !highCubesMonotonicCover.get(sig) && !lowCubesMonotonicCover.get(sig)) {
                    retVal = false;
                }
            }
        }
        return retVal;
    }

    private boolean check1stAnd2nd(Signal sig, String suffix, List<CFRegion> regions, BiMap<CFRegion, EspressoTerm> cubes, boolean printErrors, boolean checkQuiesecent) {
        int id = 1;
        for(CFRegion reg : regions) {
            int num = 0;
            for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().columnMap().get(sig.getName() + suffix + "_" + id).entrySet()) {
                if(entry.getValue() == EspressoValue.one) {
                    if(checkQuiesecent) {
                        if(!check1stAndQuiescent(reg, entry.getKey())) {
                            continue;
                        }
                    } else {
                        if(!check1st(reg, entry.getKey())) {
                            continue;
                        }
                    }
                    if(!check2nd(reg, entry.getKey())) {
                        continue;
                    }
                    cubes.put(reg, entry.getKey());
                    num++;
                }
            }
            if(num == 0) {
                if(printErrors) {
                    logger.warn("No matching cube found for signal '" + sig + "'");
                }
                return false;
            } else if(num > 1) {
                if(printErrors) {
                    logger.warn("More than one matching cube found for signal '" + sig + "'");
                }
                return false;
            }
            id++;
        }
        return true;
    }

    // 1st: Cover: Cj(a*) covers all states ERj(a*)
    private boolean check1st(CFRegion reg, EspressoTerm term) {
        for(State state : reg.getExcitationRegion()) {
            BitSet stateBin = state.getBinaryRepresentationNormalised(allSignals);
            if(!term.evaluate(stateBin, allSignals, resetname)) {
                return false;
            }
        }
        return true;
    }

    // 1st and: U QRj(a*) iff high/low
    private boolean check1stAndQuiescent(CFRegion reg, EspressoTerm term) {
        if(!check1st(reg, term)) {
            return false;
        }
        for(State state : reg.getQuiescentRegion()) {
            BitSet stateBin = state.getBinaryRepresentationNormalised(allSignals);
            if(!term.evaluate(stateBin, allSignals, resetname)) {
                return false;
            }
        }
        return true;
    }

    // 2nd: Onehot: Cj(a*) not covers states outside ERj(a*) U QRj(a*)
    private boolean check2nd(CFRegion region, EspressoTerm term) {
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

    // 3rd: Monotonicity: Cj(a*) can fall at most once along any state sequence within QRj(a*)
    private boolean check3rd() {
        boolean retVal = true;
        monotonicityViolatingStates = new HashMap<CFRegion, Set<State>>();
        nonMonotonSignals = new TreeSet<Signal>();
        for(Signal sig : checkSignals) {
            if(sig.isInternalOrOutput()) {
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
                if(highCubesMonotonicCover.get(sig)) {
                    for(Entry<CFRegion, EspressoTerm> entry : cubes.getHighCubes().entrySet()) {
                        Set<State> notworking = checkMonotonicity(entry.getKey(), entry.getValue());
                        if(notworking.size() != 0) {
                            highCubesMonotonicCover.put(sig, false);
                        }
                    }
                }
                if(lowCubesMonotonicCover.get(sig)) {
                    for(Entry<CFRegion, EspressoTerm> entry : cubes.getLowCubes().entrySet()) {
                        Set<State> notworking = checkMonotonicity(entry.getKey(), entry.getValue());
                        if(notworking.size() != 0) {
                            lowCubesMonotonicCover.put(sig, false);
                        }
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

    public Map<Signal, Boolean> getCelemCubesMonotonicCover() {
        return celemCubesMonotonicCover;
    }

    public Map<Signal, Boolean> getHighCubesMonotonicCover() {
        return highCubesMonotonicCover;
    }

    public Map<Signal, Boolean> getLowCubesMonotonicCover() {
        return lowCubesMonotonicCover;
    }
}
