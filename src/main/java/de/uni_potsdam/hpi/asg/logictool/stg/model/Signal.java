package de.uni_potsdam.hpi.asg.logictool.stg.model;

/*
 * Copyright (C) 2014 - 2015 Norman Kluge
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
import java.util.List;

public class Signal implements Comparable<Signal> {

    public enum SignalType {
        input, output, dummy, internal
    }

    private SignalType       type;
    private String           name;
    private List<Transition> transitions;

    public Signal(String name, SignalType type) {
        this.name = name;
        this.type = type;
        this.transitions = new ArrayList<Transition>();
    }

    public String getName() {
        return name;
    }

    public SignalType getType() {
        return type;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void addTransition(Transition trans) {
        this.transitions.add(trans);
    }

    public void dummify() {
        this.type = SignalType.dummy;
        int id = 0;
        for(Transition t : transitions) {
            t.setId(id++);
        }
    }

    public void makeInput() {
        if(this.type != SignalType.dummy) {
            this.type = SignalType.input;
        }
    }

    public void changeType(SignalType type) {
        this.type = type;
    }

    public void changeName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Signal arg0) {
        return this.getName().compareTo(arg0.getName());
    }

    public String outputForGFile() {
        return name;
    }

    public boolean isDummy() {
        return this.type == SignalType.dummy;
    }

    public boolean isInternalOrOutput() {
        return(this.type == SignalType.internal || this.type == SignalType.output);
    }
}
