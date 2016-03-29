package de.uni_potsdam.hpi.asg.logictool.synthesis.table;

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

import java.util.BitSet;
import java.util.SortedSet;

import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.logictool.helper.BitSetHelper;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;

public class ComplexGateTableSynthesis extends TableSynthesis {

    public ComplexGateTableSynthesis(StateGraph stateGraph, SortedSet<Signal> signals, String resetname) {
        super(stateGraph, signals, resetname);
    }

    @Override
    protected void fillTable(int num, String[] inputs, Table<EspressoTerm, String, EspressoValue> table) {
        for(State state : stategraph.getStates()) {
            BitSet x = state.getBinaryRepresentationNormalised(stategraph.getAllSignals());
            EspressoTerm t = new EspressoTerm(BitSetHelper.formatBitset(x, num), inputs);
            for(Signal sig : stategraph.getAllSignals()) {
                if(sig.getType() == SignalType.output || sig.getType() == SignalType.internal) {
                    switch(state.getStateValues().get(sig)) {
                        case high:
                        case rising:
                            table.put(t, sig.getName(), EspressoValue.one);
                            break;
                        case falling:
                        case low:
                            table.put(t, sig.getName(), EspressoValue.zero);
                            break;
                    }
                }
            }
        }
    }
}