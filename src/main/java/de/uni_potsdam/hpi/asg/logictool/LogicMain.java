package de.uni_potsdam.hpi.asg.logictool;

/*
 * Copyright (C) 2014 - 2016 Norman Kluge
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

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.common.io.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.io.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.io.Zipper;
import de.uni_potsdam.hpi.asg.logictool.io.Config;
import de.uni_potsdam.hpi.asg.logictool.io.LogicInvoker;
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
import de.uni_potsdam.hpi.asg.logictool.stg.GFile;
import de.uni_potsdam.hpi.asg.logictool.stg.csc.CSCSolver;
import de.uni_potsdam.hpi.asg.logictool.stg.csc.ExternalCSCSolver;
import de.uni_potsdam.hpi.asg.logictool.stg.csc.ExternalCSCSolver.ExternalCSCSolverConfig;
import de.uni_potsdam.hpi.asg.logictool.stg.model.STG;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.synthesis.CElementSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.EspressoOptimiser;
import de.uni_potsdam.hpi.asg.logictool.synthesis.Synthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.techfile.TechLibrary;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class LogicMain {
    private static Logger                  logger;
    private static LogicCommandlineOptions options;
    public static Config                   config;

    public static void main(String[] args) throws Exception {
        int status = main2(args);
        System.exit(status);
    }

    public static int main2(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        int status = -1;
        options = new LogicCommandlineOptions();
        if(options.parseCmdLine(args)) {
            logger = LoggerHelper.initLogger(options.getOutputlevel(), options.getLogfile(), options.isDebug());
            logger.debug("Args: " + Arrays.asList(args).toString());
            config = Config.readIn(options.getConfigfile());
            if(config == null) {
                logger.error("Could not read config");
                return 1;
            }
            WorkingdirGenerator.getInstance().create(options.getWorkingdir(), config.workdir, "logicwork", LogicInvoker.getInstance());
            status = execute();
            zipWorkfile();
            WorkingdirGenerator.getInstance().delete();
        }
        long end = System.currentTimeMillis();
        if(logger != null) {
            logger.info("Runtime: " + LoggerHelper.formatRuntime(end - start, false));
        }
        return status;
    }

    private static int execute() {

        /*
         * Switches
         */
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

        /*
         * STG & state graph
         * 
         */
        STG stg = GFile.importFromFile(options.getGfile());
        if(stg == null) {
            return 1;
        }
        SortedSet<Signal> sortedSignals = new TreeSet<Signal>(stg.getSignals());
        logger.info("Number of signals: " + sortedSignals.size());
        // StateGraph
        StateGraphComputer graphcomp = new StateGraphComputer(stg, sortedSignals, cscsolver);
        StateGraph stateGraph = graphcomp.compute();
        if(stateGraph == null) {
            logger.error("Failed to generate state graph");
            return 1;
        }

//		for(State s : stateGraph.getStates()) {
//			System.out.println(s.toString() + "\t" + s.getId());
//		}

        /*
         *  Data structures
         */

        // Reset
        String resetname = "_reset";
        Reset reset = new Reset(resetname, stateGraph);
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
        ResetInserter inserter = new SimpleCElementResetInserter(reset, arch);
        reset.setDecider(decider);
        reset.setInserter(inserter);

        // Netlist
        int nodesize = 10000;
        BDDFactory storage = JFactory.init(nodesize, nodesize / 4);
        storage.setCacheRatio(4f);
        Netlist netlist = new Netlist(storage, stateGraph, reset);

        // Synthesis
        Synthesis syn = new CElementSynthesis(stateGraph, netlist, resetname, arch);
        EspressoOptimiser optimiser = new EspressoOptimiser();

        // Techmapper
        TechLibrary techlib = TechLibrary.importFromFile(options.getTechnology(), storage);
        if(techlib == null) {
            return 1;
        }
        TechnologyMapper map = new TechnologyMapper(stateGraph, netlist, techlib, syn, options.isUnsafeanddeco());
        GateMerger merge = new GateMerger(netlist, map);

        /*
         * Flow
         */
        if(!syn.doTableSynthesis()) {
            logger.error("Table synthesis failed");
            return 1;
        }
//		if(!reset.insertPreOptimisation(syn.getTable())) {
//			logger.error("Reset insertion (pre-optimisation) failed");
//			return 1;
//		}
        EspressoTable opttable = optimiser.espressoMinimization(syn.getTable());
        if(opttable == null) {
            logger.error("Optimisation failed");
            return 1;
        }
        if(!syn.doTableCheck(opttable)) {
            logger.error("Table check failed");
            return 1;
        }
        if(!syn.doFunctionSynthesis()) {
            logger.error("Function synthesis failed");
            return 1;
        }
        if(!reset.decide(netlist)) {
            logger.error("Reset decision failed");
            return 1;
        }
        if(!syn.doComplementaryCheck(reset)) {
            logger.error("Complementary check failed");
            return 1;
        }
        if(!reset.insertPostSynthesis(netlist)) {
            logger.error("Reset insertion (pre-synthesis) failed");
            return 1;
        }
        if(!map.mapAll()) {
            logger.error("Technology mapping failed");
            return 1;
        }
        if(!syn.doPostMappingSynthesis(map)) {
            logger.error("Synthesis Post-Technologymapping failed");
            return 1;
        }

//		new NetlistGraph(netlist, null, true);

        if(!merge.merge()) {
            logger.warn("Technology merging failed");
        }

        logger.debug("Number of BDD nodes: " + storage.getNodeNum());
        // verilog output
        if(!FileHelper.getInstance().writeFile(options.getSynthesisOutfile(), VerilogOutput.toV(stg, netlist, resetname))) {
            logger.error("Writing out synthesis file failed");
            return 1;
        }

        logger.info("Synthesis successful: " + options.getSynthesisOutfile());
        return 0;
    }

    private static boolean zipWorkfile() {
        if(options.getWorkfile() != null) {
            if(!Zipper.getInstance().zip(options.getWorkfile())) {
                logger.warn("Could not zip temp files");
                return false;
            }
        } else {
            logger.warn("No zip outfile");
            return false;
        }
        return true;
    }
}
