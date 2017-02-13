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

import java.io.File;
import java.util.Arrays;

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.iohelper.Zipper;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.logictool.io.Config;
import de.uni_potsdam.hpi.asg.logictool.io.ConfigFile;
import de.uni_potsdam.hpi.asg.logictool.io.LogicInvoker;
import de.uni_potsdam.hpi.asg.logictool.techfile.TechLibrary;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class LogicMain {

    public static final String             CONFIG_FILE_NAME = "logicconfig.xml";
    public static final File               CONFIG_FILE      = new File(CommonConstants.DEF_CONFIG_DIR_FILE, CONFIG_FILE_NAME);

    private static Logger                  logger;
    private static LogicCommandlineOptions options;
    public static Config                   config;

    // Magic number: Initial node size of the BDD factory for the Netlist data structure.
    private static final int               netlistNodesize  = 10000;

    /**
     * Main entrance of program.
     * 
     * @param args
     *            see {@link LogicCommandlineOptions}
     */
    public static void main(String[] args) {
        int status = -1;
        try {
            status = main2(args);
        } catch(Exception e) {
            if(options != null && options.isDebug()) {
                e.printStackTrace();
            } else {
                System.err.println("Something really bad happend");
                status = -2;
            }
        }
        System.exit(status);
    }

    /**
     * Sets up all helpers (command line parser, configuration, logger, working
     * directory). Calls execute.
     * 
     * @param args
     *            see {@link LogicCommandlineOptions}
     * @return Status code:
     *         -1: Command line problem
     *         0: Everything okay
     *         1: Something failed
     * @throws Exception
     */
    public static int main2(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        int status = -1;
        options = new LogicCommandlineOptions();
        if(options.parseCmdLine(args)) {
            logger = LoggerHelper.initLogger(options.getOutputlevel(), options.getLogfile(), options.isDebug());
            logger.debug("Args: " + Arrays.asList(args).toString());
            config = ConfigFile.readIn(options.getConfigfile());
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

    /**
     * Calls the logic synthesis flow
     * 
     * @return Status code:
     *         0: Everything okay
     *         1: Something failed
     */
    private static int execute() {
        BDDFactory storage = JFactory.init(netlistNodesize, netlistNodesize / 4);
        storage.setCacheRatio(4f);

        TechLibrary tech = readTechnology(options.getTechnology(), config.defaultTech, storage);
        if(tech == null) {
            logger.error("No technology found");
            return 1;
        }

        Flow flow = new Flow(options, tech, storage);
        return flow.execute();
    }

    private static TechLibrary readTechnology(File optTech, String cfgTech, BDDFactory storage) {
        if(optTech != null) {
            if(optTech.exists()) {
                logger.debug("Using options technology file: " + optTech.getAbsolutePath());
                return TechLibrary.importFromFile(optTech, storage);
            } else {
                logger.warn("Options technology file " + optTech.getAbsolutePath() + " not found. Trying default from config");
            }
        }

        if(cfgTech != null) {
            File cfgTechFile = FileHelper.getInstance().replaceBasedir(cfgTech);
            if(cfgTechFile.exists()) {
                logger.debug("Using config technology file: " + cfgTechFile.getAbsolutePath());
                return TechLibrary.importFromFile(cfgTechFile, storage);
            } else {
                logger.warn("Config technology file " + cfgTechFile.getAbsolutePath() + " not found.");
            }
        } else {
            logger.warn("No default technology in config file defined");
        }

        return null;
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
