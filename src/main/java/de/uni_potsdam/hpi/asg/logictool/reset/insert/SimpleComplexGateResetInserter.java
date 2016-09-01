package de.uni_potsdam.hpi.asg.logictool.reset.insert;

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

import java.util.Map;
import java.util.Map.Entry;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;

public class SimpleComplexGateResetInserter extends ResetInserter {

    public SimpleComplexGateResetInserter(Reset reset) {
        super(reset);
    }

    @Override
    public boolean insertPreOptimisation(EspressoTable table, Map<Signal, ResetDecision> decision) {
        EspressoTerm t = reset.getEspressoResetTerm(table);

        for(Entry<Signal, ResetDecision> entry : decision.entrySet()) {
            if(entry.getValue() != ResetDecision.NORST) {
                Signal sig = entry.getKey();
                switch(reset.getStategraph().getInitState().getStateValues().get(sig)) {
                    case rising:
                    case low:
                        table.getTable().put(t, sig.getName(), EspressoValue.zero);
                        break;
                    case falling:
                    case high:
                        table.getTable().put(t, sig.getName(), EspressoValue.one);
                        break;
                }
            }
        }

        return true;
    }

    @Override
    public boolean insertPostSynthesis(Netlist netlist, Map<Signal, ResetDecision> decision) {
        return true;
    }
}