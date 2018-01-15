package de.uni_potsdam.hpi.asg.logictool.synthesis;

/*
 * Copyright (C) 2015 - 2018 Norman Kluge
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

import de.uni_potsdam.hpi.asg.logictool.mapping.TechnologyMapper;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.helper.RegionCalculator;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;

public abstract class Synthesis {

    protected StateGraph       stateGraph;
    protected Netlist          netlist;
    protected String           resetname;
    protected EspressoTable    table;
    protected RegionCalculator regCalc;

    public Synthesis(StateGraph stateGraph, Netlist netlist, String resetname) {
        this.stateGraph = stateGraph;
        this.netlist = netlist;
        this.resetname = resetname;
    }

    public boolean doRegionCalculation() {
        regCalc = RegionCalculator.create(stateGraph);
        if(regCalc == null) {
            return false;
        }
        return true;
    }

    public abstract boolean doTableSynthesis();

    public abstract boolean doTableCheck(EspressoTable table);

    public abstract boolean doComplementaryCheck(Reset reset);

    public abstract boolean doFunctionSynthesis();

    public abstract boolean doPostMappingSynthesis(TechnologyMapper mapper);

    public Netlist getNetlist() {
        return netlist;
    }

    public EspressoTable getTable() {
        return table;
    }
}
