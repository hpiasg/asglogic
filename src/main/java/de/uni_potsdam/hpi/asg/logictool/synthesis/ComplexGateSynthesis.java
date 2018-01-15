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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.mapping.TechnologyMapper;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;

public class ComplexGateSynthesis extends Synthesis {
    private static final Logger logger = LogManager.getLogger();

    public ComplexGateSynthesis(StateGraph stateGraph, Netlist netlist, String resetname) {
        super(stateGraph, netlist, resetname);
    }

    @Override
    public boolean doTableSynthesis() {
        logger.error("ComplexGate synthesis is not yet implemented");
        return false;
    }

    @Override
    public boolean doTableCheck(EspressoTable table) {
        logger.error("ComplexGate synthesis is not yet implemented");
        return false;
    }

    @Override
    public boolean doFunctionSynthesis() {
        logger.error("ComplexGate synthesis is not yet implemented");
        return false;
    }

    @Override
    public boolean doPostMappingSynthesis(TechnologyMapper mapper) {
        logger.error("ComplexGate synthesis is not yet implemented");
        return false;
    }

    @Override
    public boolean doComplementaryCheck(Reset reset) {
        logger.error("ComplexGate synthesis is not yet implemented");
        return false;
    }
}
