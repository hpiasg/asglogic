package de.uni_potsdam.hpi.asg.logictool.synthesis.model;

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

import java.util.Set;

import de.uni_potsdam.hpi.asg.logictool.srgraph.State;

public class CFRegion {

    private State      entryState;
    private Set<State> excitationRegion;
    private Set<State> quiescentRegion;

    public CFRegion(Set<State> excitationRegion, Set<State> quiescentRegion, State entryState) {
        this.excitationRegion = excitationRegion;
        this.quiescentRegion = quiescentRegion;
        this.entryState = entryState;
    }

    public Set<State> getExcitationRegion() {
        return excitationRegion;
    }

    public Set<State> getQuiescentRegion() {
        return quiescentRegion;
    }

    public State getEntryState() {
        return entryState;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("ER: ");
        for(State s : excitationRegion) {
            str.append(s.toStringSimple() + ",");
        }
        str.replace(str.length() - 1, str.length(), "\nQR: ");
        for(State s : quiescentRegion) {
            str.append(s.toStringSimple() + ",");
        }
        str.replace(str.length() - 1, str.length(), "");
        return str.toString();
    }
}
