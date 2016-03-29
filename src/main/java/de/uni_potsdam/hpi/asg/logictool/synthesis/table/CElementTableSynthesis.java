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
import java.util.Map;
import java.util.SortedSet;

import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.logictool.helper.BitSetHelper;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.synthesis.CElementSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.CFRegion;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.Regions;

public class CElementTableSynthesis extends TableSynthesis {

    private Map<Signal, Regions> regions;

    public CElementTableSynthesis(StateGraph stateGraph, SortedSet<Signal> signals, Map<Signal, Regions> regions, String resetname) {
        super(stateGraph, signals, resetname);
        this.regions = regions;
    }

    @Override
    protected void fillTable(int num, String[] inputs, Table<EspressoTerm, String, EspressoValue> table) {
        for(State state : stategraph.getStates()) {
            BitSet stateBit = state.getBinaryRepresentationNormalised(stategraph.getAllSignals());
            EspressoTerm t = new EspressoTerm(BitSetHelper.formatBitset(stateBit, num), inputs);
            for(Signal sig : signals) {
                if(sig.getType() == SignalType.output || sig.getType() == SignalType.internal) {
                    Regions reg = regions.get(sig);
                    int i = 1;
                    for(CFRegion highReg : reg.getRisingRegions()) {
                        String name = sig.getName() + CElementSynthesis.setEnding + "_" + (i++);
                        if(highReg.getExcitationRegion().contains(state)) {
                            // State=ER => 1
                            table.put(t, name, EspressoValue.one);
                        } else if(highReg.getQuiescentRegion().contains(state)) {
                            // State=QR => -
                            table.put(t, name, EspressoValue.dontcare);
                            //table.put(t, name, EspressoValue.zero);
                        } else {
                            // State=Other ER,QR => 0
                            table.put(t, name, EspressoValue.zero);
                        }
                    }

                    i = 1;
                    for(CFRegion lowReg : reg.getFallingRegions()) {
                        String name = sig.getName() + CElementSynthesis.resetEnding + "_" + (i++);
                        if(lowReg.getExcitationRegion().contains(state)) {
                            // State=ER => 1
                            table.put(t, name, EspressoValue.one);
                        } else if(lowReg.getQuiescentRegion().contains(state)) {
                            // State=QR => -
                            table.put(t, name, EspressoValue.dontcare);
                            //table.put(t, name, EspressoValue.zero);
                        } else {
                            // State=Other ER,QR => 0
                            table.put(t, name, EspressoValue.zero);
                        }
                    }
                }
            }
        }
    }
}
