package de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco;

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

import java.util.List;

import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;

public class IOBehaviour {

    private List<Transition> sequence;
    private State            start;
    private State            end;

    public IOBehaviour(List<Transition> sequence, State start, State end) {
        this.sequence = sequence;
        this.start = start;
        this.end = end;
    }

    public List<Transition> getSequence() {
        return sequence;
    }

    public State getStart() {
        return start;
    }

    public State getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return sequence.toString() + "; S:" + start + "; E:" + end;
    }
}
