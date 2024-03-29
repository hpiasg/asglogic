package de.uni_potsdam.hpi.asg.logictool.reset.decision;

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

import java.util.HashMap;
import java.util.Map;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;

public abstract class ResetDecider {

    public enum ResetDecision {
        NORST, SETRST, RESETRST, BOTHRST
    }

    protected Reset                      reset;
    protected Map<Signal, ResetDecision> decision;

    public ResetDecider(Reset reset) {
        this.reset = reset;
        this.decision = new HashMap<>();
    }

    public abstract boolean decide(Netlist netlist);

    public Map<Signal, ResetDecision> getDecision() {
        return decision;
    }
}
