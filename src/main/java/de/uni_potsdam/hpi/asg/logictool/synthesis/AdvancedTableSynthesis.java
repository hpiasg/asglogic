package de.uni_potsdam.hpi.asg.logictool.synthesis;

/*
 * Copyright (C) 2014 - 2018 Norman Kluge
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.helper.BitSetHelper;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.CFRegion;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.Regions;

public class AdvancedTableSynthesis {
    private static final Logger  logger = LogManager.getLogger();

    private Map<Signal, Regions> regions;

    protected String             resetname;
    protected SortedSet<Signal>  signals;
    protected StateGraph         stategraph;
    private EspressoTable        table;

    public AdvancedTableSynthesis(StateGraph stateGraph, SortedSet<Signal> signals, Map<Signal, Regions> regions, String resetname) {
        this.regions = regions;
        this.resetname = resetname;
        this.signals = signals;
        this.stategraph = stateGraph;
    }

    public EspressoTable synthesise() {
        logger.info("Synthesis of signals: " + signals.toString());
        table = createEspressoTable();
        return table;
    }

    protected EspressoTable createEspressoTable() {
        int num = stategraph.getAllSignals().size();
        if(resetname != null) {
            num++;
        }
        int i = 0;
        String[] inputs = new String[num];
        if(resetname != null) {
            inputs[i++] = resetname;
        }
        for(Signal sig : stategraph.getAllSignals()) {
            inputs[i++] = sig.getName();
        }
        Table<EspressoTerm, String, EspressoValue> table = HashBasedTable.create();

        fillTable(num, inputs, table);

        return new EspressoTable(inputs, table);
    }

    protected void fillTable(int num, String[] inputs, Table<EspressoTerm, String, EspressoValue> table) {
        for(State state : stategraph.getStates()) {
            BitSet stateBit = state.getBinaryRepresentationNormalised(stategraph.getAllSignals());
            EspressoTerm t = new EspressoTerm(BitSetHelper.formatBitset(stateBit, num), inputs);
            for(Signal sig : signals) {
                if(sig.isInternalOrOutput()) {
                    Regions reg = regions.get(sig);
                    int i = 1;
                    for(CFRegion highReg : reg.getRisingRegions()) {
                        String setName = sig.getName() + AdvancedSynthesis.setEnding + "_" + (i);
                        String highName = sig.getName() + AdvancedSynthesis.highEnding + "_" + (i);
                        i++;
                        if(highReg.getExcitationRegion().contains(state)) {
                            // State=ER => 1
                            table.put(t, setName, EspressoValue.one);
                            table.put(t, highName, EspressoValue.one);
                        } else if(highReg.getQuiescentRegion().contains(state)) {
                            // State=QR => -,1
                            table.put(t, setName, EspressoValue.dontcare);
                            table.put(t, highName, EspressoValue.one);
                        } else {
                            // State=Other ER,QR => 0
                            table.put(t, setName, EspressoValue.zero);
                            table.put(t, highName, EspressoValue.zero);
                        }
                    }

                    i = 1;
                    for(CFRegion lowReg : reg.getFallingRegions()) {
                        String resetName = sig.getName() + AdvancedSynthesis.resetEnding + "_" + (i);
                        String lowName = sig.getName() + AdvancedSynthesis.lowEnding + "_" + (i);
                        i++;
                        if(lowReg.getExcitationRegion().contains(state)) {
                            // State=ER => 1
                            table.put(t, resetName, EspressoValue.one);
                            table.put(t, lowName, EspressoValue.one);
                        } else if(lowReg.getQuiescentRegion().contains(state)) {
                            // State=QR => -
                            table.put(t, resetName, EspressoValue.dontcare);
                            table.put(t, lowName, EspressoValue.one);
                        } else {
                            // State=Other ER,QR => 0
                            table.put(t, resetName, EspressoValue.zero);
                            table.put(t, lowName, EspressoValue.zero);
                        }
                    }
                }
            }
        }
    }
}