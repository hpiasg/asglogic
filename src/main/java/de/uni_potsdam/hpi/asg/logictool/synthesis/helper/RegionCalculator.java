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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.CFRegion;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.Regions;

public class RegionCalculator {
    private static final Logger  logger = LogManager.getLogger();

    private Map<Signal, Regions> regions;
    private StateGraph           stategraph;

    public RegionCalculator(StateGraph stateGraph) {
        this.stategraph = stateGraph;
        this.regions = computeRegions(stateGraph.getStates());
    }

    public Map<Signal, Regions> getRegions() {
        return regions;
    }

    private Map<Signal, Regions> computeRegions(Set<State> states) {

        Map<Signal, Regions> retVal = new HashMap<Signal, Regions>();

        Set<State> statelow = new TreeSet<State>();
        Set<State> statehigh = new TreeSet<State>();
        Set<State> statefalling = new TreeSet<State>();
        Set<State> staterising = new TreeSet<State>();

        for(Signal sig : stategraph.getAllSignals()) {
//			System.out.println(sig.getName());
            if(sig.getType() == SignalType.output || sig.getType() == SignalType.internal) {
                statelow.clear();
                statehigh.clear();
                statefalling.clear();
                staterising.clear();
                for(State state : states) {
                    switch(state.getStateValues().get(sig)) {
                        case low:
                            statelow.add(state);
                            break;
                        case high:
                            statehigh.add(state);
                            break;
                        case falling:
                            statefalling.add(state);
                            break;
                        case rising:
                            staterising.add(state);
                            break;
                    }
                }
                retVal.put(sig, new Regions(findCFRegions(sig, staterising, statehigh), findCFRegions(sig, statefalling, statelow)));
            }
        }
        return retVal;
    }

    private List<CFRegion> findCFRegions(Signal sig, Set<State> eStates, Set<State> qStates) {
        boolean found = false;
        List<CFRegion> constantFunctionRegions = new ArrayList<>();
        List<Set<State>> qRegions = groupRegions(qStates);
        for(Set<State> eRegion : groupRegions(eStates)) {
            found = false;
            State eRegionState = eRegion.iterator().next();
            State nextInQRegionState = null;
            for(Entry<Transition, State> entry : eRegionState.getNextStates().entrySet()) {
                if(entry.getKey().getSignal() == sig) {
                    nextInQRegionState = entry.getValue();
                    break;
                }
            }
            if(nextInQRegionState == null) {
                logger.error("No next state with for signal " + sig.getName() + " in state " + eRegionState + "found");
                return null;
            }
            for(Set<State> qRegion : qRegions) {
                if(qRegion.contains(nextInQRegionState)) {
                    constantFunctionRegions.add(new CFRegion(eRegion, qRegion, null));//findEntry(risingRegion)));
                    found = true;
                    break;
                }
            }
            if(!found) {
                logger.error("No QR for ER found");
                return null;
            }
        }

        return constantFunctionRegions;
    }

    private List<Set<State>> groupRegions(Set<State> states) {
//		System.out.println("States:");
//		for(State x : states) {
//			System.out.println("\t" + x.toString());
//		}

        List<Set<State>> retVal = new ArrayList<>();
        Set<State> toCheck = new HashSet<>();
        Set<State> newToCheck = new HashSet<>();
        while(!states.isEmpty()) {
            Set<State> region = new HashSet<>();
            newToCheck.add(states.iterator().next());
            while(!newToCheck.isEmpty()) {
                toCheck.addAll(newToCheck);
                newToCheck.clear();
                for(State checkState : toCheck) {
                    if(states.contains(checkState) && !region.contains(checkState)) {
                        region.add(checkState);
                        newToCheck.addAll(checkState.getPrevStates());
                        newToCheck.addAll(checkState.getNextStates().values());
                        if(checkState.getEquallyEncodedStates() != null) {
                            for(State x : checkState.getEquallyEncodedStates()) {
                                region.add(x);
                                newToCheck.addAll(x.getPrevStates());
                                newToCheck.addAll(x.getNextStates().values());
                            }
                        }
                    }
                }
            }
            retVal.add(region);
            states.removeAll(region);
        }

//		System.out.println("Regions:");
//		for(Set<State> set : retVal) {
//			System.out.println("\tReg:");
//			for(State x : set) {
//				System.out.println("\t\t" + x.toString());
//			}
//		}

        return retVal;
    }

    public void apply(Map<CFRegion, Set<State>> update) {
        for(Regions regs : regions.values()) {
            for(CFRegion reg : regs.getFallingRegions()) {
                if(update.containsKey(reg)) {
                    for(State s : update.get(reg)) {
                        reg.getQuiescentRegion().remove(s);
                    }
                }
            }
            for(CFRegion reg : regs.getRisingRegions()) {
                if(update.containsKey(reg)) {
                    for(State s : update.get(reg)) {
                        reg.getQuiescentRegion().remove(s);
                    }
                }
            }
        }
    }
}
