package de.uni_potsdam.hpi.asg.logictool.srgraph;

/*
 * Copyright (C) 2014 - 2016 Norman Kluge
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

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.stg.GFile;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stggraph.AbstractSTGGraphComputer;
import de.uni_potsdam.hpi.asg.common.stggraph.AbstractState.Value;
import de.uni_potsdam.hpi.asg.logictool.srgraph.csc.CSCSolver;

import gnu.trove.map.hash.THashMap;

public class StateGraphComputer extends AbstractSTGGraphComputer<State> {
    private static final Logger logger = LogManager.getLogger();

    private SortedSet<Signal>   sortedSignals;
    private CSCSolver           cscsolver;

    //tmp
    private Set<State>          checkedStates;
    private List<State>         alsoset;

    public StateGraphComputer(STG stg, SortedSet<Signal> sortedSignals, CSCSolver cscsolver) {
        super(State.class, stg);
        this.insertPlacesIntoEmptyPostset = true;
        this.sortedSignals = new TreeSet<Signal>();
        for(Signal sig : sortedSignals) {
            if(!sig.isDummy()) {
                this.sortedSignals.add(sig);
            }
        }
        this.cscsolver = cscsolver;
    }

    public StateGraph compute() {
        if(!internalCompute(true)) {
            return null;
        }
        checkedStates = new HashSet<>();
        alsoset = new ArrayList<>();

        Set<State> states2 = new HashSet<State>();
        states2.addAll(states.values());

        if(!fillStates(states2)) {
            return null;
        }

//        new GraphicalStateGraph(new StateGraph(stg, init, states2, sortedSignals), false, null);

        if(!checkCSC(states2)) {
            if(cscsolver != null) {
                String newfilename = stg.getFile().getName() + "_csc.g";
                File newFile = new File(WorkingdirGenerator.getInstance().getWorkingDir(), newfilename);
                if(cscsolver.solveCSC(stg, newFile)) {
                    STG newSTG = GFile.importFromFile(new File(WorkingdirGenerator.getInstance().getWorkingDir(), newfilename));
                    StateGraphComputer newcomp = new StateGraphComputer(newSTG, new TreeSet<Signal>(newSTG.getSignals()), null);
                    return newcomp.compute();
                } else {
                    logger.error("Failed to solve CSC");
                    return null;
                }
            } else {
                logger.error("CSC solving not activated");
                return null;
            }
        }
        logger.info("STG has CSC");

        if(!checkOutputDetermency(states2)) {
            return null;
        }
        //TODO: check contains dummy trans

        logger.info("Number of states: " + states2.size());

        clear();
        return new StateGraph(stg, init, states2, sortedSignals);
    }

    private boolean checkOutputDetermency(Set<State> states2) {
        boolean retVal = true;
        for(State s : states2) {
            for(Entry<Signal, Value> sv : s.getStateValues().entrySet()) {
                switch(sv.getValue()) {
                    case falling:
                    case rising:
                        if(sv.getKey().isInternalOrOutput()) {
                            for(Entry<Transition, State> arc : s.getNextStates().entrySet()) {
                                if(arc.getKey().getSignal() != sv.getKey()) {
                                    if(arc.getValue().getStateValues().get(sv.getKey()) != sv.getValue()) {
                                        retVal = false;
                                        logger.error("Output " + sv.getKey().getName() + " disabled by " + arc.getKey().getSignal().getName());
                                    }
                                }
                            }
                        }
                        break;
                    case low:
                    case high:
                        break;
                }
            }
        }
        if(!retVal) {
            logger.error("No output determency");
        }
        return retVal;
    }

    private boolean checkCSC(Set<State> states2) {
        Map<BitSet, State> checkList = new THashMap<BitSet, State>();
        HashMap<BitSet, Set<State>> equallyEncodedStates = new HashMap<BitSet, Set<State>>();
        BitSet binRep = null;
        State otherState = null;
        Value v1 = null, v2 = null;
        boolean csc;
        Signal problematicSignal = null;
        for(State state : states2) {
            binRep = state.getBinaryRepresentationNormalised(sortedSignals);
            if(checkList.containsKey(binRep)) {
                otherState = checkList.get(binRep);
                csc = true;
                for(Entry<Signal, Value> entry2 : otherState.getStateValues().entrySet()) {
                    if(entry2.getKey().isInternalOrOutput()) {
                        v1 = state.getStateValues().get(entry2.getKey());
                        v2 = entry2.getValue();
                        if(v1 != v2) {
                            csc = false;
                            problematicSignal = entry2.getKey();
                            break;
                        }
                    }
                }
                if(csc) {
                    if(!equallyEncodedStates.containsKey(binRep)) {
                        equallyEncodedStates.put(binRep, new HashSet<State>());
                    }
                    if(state.hashCode() == otherState.hashCode()) {
                        logger.fatal("Different state objects have same Hash");
                        System.exit(-1);
                    }
                    equallyEncodedStates.get(binRep).add(state);
                    equallyEncodedStates.get(binRep).add(otherState);
                } else {
                    logger.warn("STG has no CSC");
                    logger.debug("States: " + state.getId() + " and " + otherState.getId() + ", Signal: " + problematicSignal.getName());
                    return false;
                }
            } else {
                checkList.put(binRep, state);
            }
        }

//		System.out.println("Equally encoded states:");
//		for(Entry<BitSet, Set<State>> e : equallyEncodedStates.entrySet()) {
//			for(State s : e.getValue()) {
//				System.out.print(s.getId() + ", ");
//			}
//			System.out.println();
//		}

        //merge
        Transition t = null;
        State s1 = null, s2 = null;
        Iterator<State> it = null;
        boolean change = false;
        do {
            change = false;
            for(Entry<BitSet, Set<State>> entry : equallyEncodedStates.entrySet()) {
                for(Set<State> set : Sets.powerSet(entry.getValue())) {
                    if(set.size() == 2) {
                        it = set.iterator();
                        s1 = it.next();
                        s2 = it.next();
                        //only merge if nextstates are the same
                        boolean merge = true;
                        for(Entry<Transition, State> nextentry : s2.getNextStates().entrySet()) {
                            State repnext = null;
                            for(Entry<Transition, State> repnextentry : s1.getNextStates().entrySet()) {
                                if(repnextentry.getKey().getSignal() == nextentry.getKey().getSignal()) {
                                    repnext = repnextentry.getValue();
                                }
                            }
                            if(repnext == null || !repnext.equals(nextentry.getValue())) {
                                merge = false;
                                break;
                            }
                        }

                        if(merge) {
                            for(State prev : s2.getPrevStates()) {
                                t = null;
                                for(Entry<Transition, State> entry2 : prev.getNextStates().entrySet()) {
                                    if(entry2.getValue() == s2) {
                                        t = entry2.getKey();
                                        break;
                                    }
                                }
                                if(t == null) {
                                    logger.error("Transition in map not found (Missing edge?)");
                                    return false;
                                }
                                prev.addEdgeNextState(s1, t);
                            }
                            s2.getPrevStates().clear();
                            for(Entry<Transition, State> next : s2.getNextStates().entrySet()) {
                                if(!next.getValue().getPrevStates().contains(s1)) {
                                    if(s1.getNextStates().containsKey(next.getKey())) {
                                        next.getValue().getPrevStates().remove(s2);
                                    } else {
                                        s1.addEdgeNextState(next.getValue(), next.getKey());
                                    }
                                }
                                next.getValue().getPrevStates().remove(s2);
                            }
                            states2.remove(s2);
                            entry.getValue().remove(s2);
                            change = true;
                            break;
                        }
                    }
                }
            }
        } while(change);

        for(Entry<BitSet, Set<State>> e : equallyEncodedStates.entrySet()) {
            if(e.getValue().size() <= 1) {
                continue;
            }
            for(State s3 : e.getValue()) {
                for(State s4 : e.getValue()) {
                    if(s3 != s4) {
                        s3.addEquallyEncodedState(s4);
                    }
                }
            }
        }

        return true;
    }

    private boolean fillStates(Set<State> states) {

        int sigsize = 0;
        for(Signal sig : stg.getSignals()) {
            if(!sig.isDummy()) {
                sigsize++;
            }
        }

        for(State s : states) {
            if(sigsize > s.getStateValues().size()) {
                for(Signal sig : stg.getSignals()) {
                    if(!sig.isDummy()) {
                        if(!s.isSignalSet(sig)) {
                            checkstates.clear();
                            checkedStates.clear();
                            checkstates.addAll(s.getPrevStates());
                            alsoset.clear();
                            State s2 = null;
                            boolean isNowSet = false;
                            while(!checkstates.isEmpty()) {
                                s2 = checkstates.poll();
                                if(checkedStates.contains(s2)) {
                                    continue;
                                }
                                if(s2.isSignalSet(sig)) {
                                    Value val = s2.getStateValues().get(sig);
                                    switch(val) {
                                        case falling:
                                        case rising:
                                            logger.error("Not possible: State: " + s.toStringSimple() + ", Sig " + sig.getName() + ", OtherState: " + s2.toStringSimple() + ": " + val);
                                            //											new Graph(new StateGraph(stg, init, states, sortedSignals), true, null);
                                            return false;
                                        case high:
                                        case low:
                                            s.setSignalState(sig, val);
                                            for(State s3 : alsoset) {
                                                s3.setSignalState(sig, val);
                                            }
                                            isNowSet = true;
                                            break;
                                    }
                                    if(isNowSet) {
                                        break;
                                    }
                                } else {
                                    alsoset.add(s2);
                                    checkstates.addAll(s2.getPrevStates());
                                    checkedStates.add(s2);
                                    if(checkedStates.size() == states.size()) {
                                        logger.error("Aborting fill states");
                                        return false;
                                    }
                                }
                            }
                            if(!isNowSet) {
                                logger.error("Cannot fill states");
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
