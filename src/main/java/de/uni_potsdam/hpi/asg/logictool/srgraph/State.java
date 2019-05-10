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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stggraph.AbstractState;

public class State extends AbstractState<State> implements Comparable<State> {
    private static final Logger      logger = LogManager.getLogger();

    private SortedMap<Signal, Value> state;
    private BitSet                   binaryRepresentation;
    private Set<State>               equallyEncodedStates;

    public State() {
        this.state = new TreeMap<Signal, Value>();
        this.nextStates = new HashMap<Transition, State>();
        this.prevStates = new HashSet<State>();
        this.binaryRepresentation = null;
    }

    @Override
    public void setSignalState(Signal sig, Value val) {
        if(sig.getType() == SignalType.dummy) {
            logger.error("Dummy set");
            return;
        }
        if(!this.state.containsKey(sig)) {
            this.state.put(sig, val);
        }
        if(this.state.get(sig) != val) {
            logger.warn("State: " + id + ", Signal: " + sig + ": old: " + this.state.get(sig) + " new: " + val);
        }
    }

    @Override
    public boolean isSignalSet(Signal sig) {
        return this.state.containsKey(sig);
    }

    @Override
    public Map<Signal, Value> getStateValues() {
        return state;
    }

    public BitSet getBinaryRepresentationNormalised(SortedSet<Signal> sortedSignals) {
        if(this.binaryRepresentation == null) {
            binaryRepresentation = new BitSet();
            int i = sortedSignals.size() - 1;
            for(Signal s : sortedSignals) {
                Value v = state.get(s);
                if(v == Value.high || v == Value.falling) {
                    binaryRepresentation.set(i--);
                } else {
                    binaryRepresentation.clear(i--);
                }
            }
        }
        return binaryRepresentation;
    }

    public String getBinaryRepresentationDbg(SortedSet<Signal> sortedSignals) {
        StringBuilder str = new StringBuilder();
        for(Signal sig : sortedSignals) {
            Value v = state.get(sig);
            if(sig.getType() == SignalType.dummy) {
                if(v == null) {
                    str.append("_");
                } else {
                    str.append("!");
                }
            } else {
                if(v == null) {
                    str.append("?");
                } else {
                    str.append(v.toString());
                }
            }
        }
        return str.toString();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("S" + id);
        str.append("={");
        for(Entry<Signal, Value> entry : state.entrySet()) {
            str.append(entry.getKey().toString() + ":" + entry.getValue() + ", ");
        }
        str.replace(str.length() - 2, str.length(), "}");
        return str.toString();
    }

    public String toStringSimple() {
        return "S" + Integer.toString(id);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof State) {
            State otherState = (State)obj;
            return this.id == otherState.id;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(State otherState) {
        return Integer.compare(this.id, otherState.id);
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void addEquallyEncodedState(State state) {
        if(equallyEncodedStates == null) {
            equallyEncodedStates = new HashSet<State>();
        }
        equallyEncodedStates.add(state);
    }

    public Set<State> getEquallyEncodedStates() {
        return equallyEncodedStates;
    }
}
