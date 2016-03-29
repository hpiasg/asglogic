package de.uni_potsdam.hpi.asg.logictool.synthesis.function;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;

public abstract class FunctionSynthesis {
    private static final Logger logger = LogManager.getLogger();

    protected EspressoTable     table;
    protected String            resetname;
    protected Netlist           netlist;
    protected StateGraph        stategraph;

    public FunctionSynthesis(EspressoTable table, String resetname, StateGraph stategraph, Netlist netlist) {
        this.table = table;
        this.resetname = resetname;
        this.stategraph = stategraph;
        this.netlist = netlist;
    }

    public boolean fillNetlist() {
        if(!initNetlist(table)) {
            logger.error("Netlist could not be initialised");
            return false;
        }
        if(!postProcessing()) {
            logger.error("Netlist could not be postprocessed");
            return false;
        }
        if(!netlist.killBuffers()) {
            logger.error("Removing of buffers failed");
        }
        return true;
    }

    protected abstract boolean postProcessing();

    protected abstract boolean initNetlist(EspressoTable table);
}
