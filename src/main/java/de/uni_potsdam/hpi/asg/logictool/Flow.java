package de.uni_potsdam.hpi.asg.logictool;

/*
 * Copyright (C) 2014 - 2017 Norman Kluge
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
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.stg.GFile;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.io.VerilogOutput;
import de.uni_potsdam.hpi.asg.logictool.mapping.GateMerger;
import de.uni_potsdam.hpi.asg.logictool.mapping.TechnologyMapper;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.AdvancedCElementResetDecider;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.FullReset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider;
import de.uni_potsdam.hpi.asg.logictool.reset.insert.ResetInserter;
import de.uni_potsdam.hpi.asg.logictool.reset.insert.SimpleCElementResetInserter;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraphComputer;
import de.uni_potsdam.hpi.asg.logictool.srgraph.csc.CSCSolver;
import de.uni_potsdam.hpi.asg.logictool.srgraph.csc.ExternalCSCSolver;
import de.uni_potsdam.hpi.asg.logictool.srgraph.csc.ExternalCSCSolver.ExternalCSCSolverConfig;
import de.uni_potsdam.hpi.asg.logictool.synthesis.CElementSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.EspressoOptimiser;
import de.uni_potsdam.hpi.asg.logictool.synthesis.Synthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.techfile.TechLibrary;
import net.sf.javabdd.BDDFactory;

public class Flow {
    private static final Logger     logger    = LogManager.getLogger();
    private static final String     resetname = "_reset";

    private LogicCommandlineOptions options;
    private TechLibrary             techlib;
    private BDDFactory              storage;
    private Netlist                 netlist;

    // Flow agents
    private Reset                   reset;
    private Synthesis               syn;
    private EspressoOptimiser       optimiser;
    private TechnologyMapper        map;
    private GateMerger              merge;
    private VerilogOutput           verilogout;

    public Flow(LogicCommandlineOptions options, TechLibrary techlib, BDDFactory storage) {
        this.options = options;
        this.techlib = techlib;
        this.storage = storage;
    }

    public int execute() {
        // STG & state graph generation
        StateGraph stateGraph = generateStateGraph();
        if(stateGraph == null) {
            return 1;
        }

        // Flow agents generation
        if(!generateFlowAgents(stateGraph)) {
            return 1;
        }

        // Execute flow
        int status = executeFlow();
        if(status != 0) {
            return status;
        }

        logger.debug("Number of BDD nodes: " + netlist.getFac().getNodeNum());
        // Verilog output
        if(!FileHelper.getInstance().writeFile(options.getSynthesisOutfile(), verilogout.toV())) {
            logger.error("Writing out synthesis file failed");
            return 1;
        }

        logger.info("Synthesis successful: " + options.getSynthesisOutfile());
        return 0;
    }

    private int executeFlow() {
        // Generate a function table from the state graph 
        if(!syn.doTableSynthesis()) {
            logger.error("Table synthesis failed");
            return 1;
        }
        // Minimise the function table with Espresso
        EspressoTable opttable = optimiser.espressoMinimization(syn.getTable());
        if(opttable == null) {
            logger.error("Optimisation failed");
            return 1;
        }
        // Check if implementation constraint met: Check for monotonic cover. If not satisfied: Try to fix.
        if(!syn.doTableCheck(opttable)) {
            logger.error("Table check failed");
            return 1;
        }
        // Derive logic functions from table
        if(!syn.doFunctionSynthesis()) {
            logger.error("Function synthesis failed");
            return 1;
        }
        // Check if reset needed
        if(!reset.decide(netlist)) {
            logger.error("Reset decision failed");
            return 1;
        }
        // Check if it is sufficient to use either of the set/reset networks
        if(!syn.doComplementaryCheck(reset)) {
            logger.error("Complementary check failed");
            return 1;
        }
        // Insert reset logic if needed
        if(!reset.insertPostSynthesis(netlist)) {
            logger.error("Reset insertion (pre-synthesis) failed");
            return 1;
        }
        // Try to map all abstract logic functions to gates
        if(!map.mapAll()) {
            logger.error("Technology mapping failed");
            return 1;
        }
        // Apply decision to either use only set/reset network
        if(!syn.doPostMappingSynthesis(map)) {
            logger.error("Synthesis Post-Technologymapping failed");
            return 1;
        }
        // Try to merge mapped gates
        if(!merge.merge()) {
            logger.warn("Technology merging failed");
        }

        return 0;
    }

    private boolean generateFlowAgents(StateGraph stateGraph) {
        // Choose architecture for implementation
        Arch arch = chooseArchitecture();

        // Reset generator
        reset = generateResetFlowAgents(arch, stateGraph);

        // Netlist - main structure for storing (abstract) gate netlists
        netlist = new Netlist(storage, stateGraph, reset);

        // Synthesis - generating a abstract gate netlist from the state graph
        syn = new CElementSynthesis(stateGraph, netlist, resetname, arch);
        // Espresso optimiser - optimises a function table with Espresso
        optimiser = new EspressoOptimiser();

        // Technology mapper - map abstract gates to actual gates of the library
        map = new TechnologyMapper(stateGraph, netlist, techlib, syn, options.isUnsafeanddeco());
        // Gate merger - combines mapped gates to larger ones if possible
        merge = new GateMerger(netlist, map);

        // Verilog generator
        verilogout = new VerilogOutput(stateGraph, netlist, resetname);

        return true;
    }

    private StateGraph generateStateGraph() {
        // STG import
        STG stg = GFile.importFromFile(options.getGfile());
        if(stg == null) {
            return null;
        }
        SortedSet<Signal> sortedSignals = new TreeSet<Signal>(stg.getSignals());
        logger.info("Number of signals: " + sortedSignals.size());

        CSCSolver cscsolver = chooseCSCsolver();

        // State graph generation
        StateGraphComputer graphcomp = new StateGraphComputer(stg, sortedSignals, cscsolver);
        StateGraph stateGraph = graphcomp.compute();
        if(stateGraph == null) {
            logger.error("Failed to generate state graph");
            return null;
        }
        return stateGraph;
    }

    private Reset generateResetFlowAgents(Arch arch, StateGraph stateGraph) {
        Reset reset = new Reset(resetname, stateGraph);
        // A reset decider decides whether a(n) (STG) signal needs reset logic or not
        ResetDecider decider = null;
        switch(options.getResettype()) {
            case "full":
                decider = new FullReset(reset);
                break;
            default:
                logger.warn("Reset mechanism '" + options.getResettype() + "' undefined. Using default 'ondemand'");
            case "ondemand":
                decider = new AdvancedCElementResetDecider(reset, arch);
                break;
        }
        // A reset inserter inserts reset logic, if the decider had chosen to
        ResetInserter inserter = new SimpleCElementResetInserter(reset, arch);
        reset.setDecider(decider);
        reset.setInserter(inserter);
        return reset;
    }

    private CSCSolver chooseCSCsolver() {
        CSCSolver cscsolver = null;
        switch(options.getCscSolving()) {
            case "M":
                cscsolver = new ExternalCSCSolver(ExternalCSCSolverConfig.mpsat);
                break;
            default:
                logger.warn("CSC solver '" + options.getCscSolving() + "' undefined. Using default 'none'");
            case "N":
                break;
            case "P":
                cscsolver = new ExternalCSCSolver(ExternalCSCSolverConfig.petrify);
                break;
        }
        return cscsolver;
    }

    private Arch chooseArchitecture() {
        Arch arch = null;
        switch(options.getArch()) {
            case "sC":
                arch = Arch.standardC;
                break;
            default:
                logger.warn("Architecture '" + options.getArch() + "' undefined. Using default 'gC'");
            case "gC":
                arch = Arch.generalisedC;
                break;
        }
        return arch;
    }
}
