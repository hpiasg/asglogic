package de.uni_potsdam.hpi.asg.logictool.reset.insert;

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

import java.util.Map;

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;

public abstract class ResetInserter {

    protected Reset reset;

    public ResetInserter(Reset reset) {
        this.reset = reset;
    }

    public abstract boolean insertPreOptimisation(EspressoTable table, Map<Signal, ResetDecision> decision);

    public abstract boolean insertPostSynthesis(Netlist netlist, Map<Signal, ResetDecision> decision);

}
