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

import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;

public abstract class TableSynthesis {
    private static final Logger logger = LogManager.getLogger();

    protected String            resetname;
    protected SortedSet<Signal> signals;
    protected StateGraph        stategraph;
    private EspressoTable       table;

    public TableSynthesis(StateGraph stateGraph, SortedSet<Signal> signals, String resetname) {
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
        int num = stategraph.getSTG().getSignals().size();
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

    protected abstract void fillTable(int num, String[] inputs, Table<EspressoTerm, String, EspressoValue> table);

}
