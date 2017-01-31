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

import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.iohelper.Zipper;
import de.uni_potsdam.hpi.asg.logictool.io.Config;
import de.uni_potsdam.hpi.asg.logictool.io.ConfigFile;
import de.uni_potsdam.hpi.asg.logictool.io.LogicInvoker;

public class LogicMain {
    private static Logger                  logger;
    private static LogicCommandlineOptions options;
    public static Config                   config;

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
            logger = LoggerHelper.initLogger(options.getOutputlevel(), options.getLogfile(), options.isDebug(), "/logic_log4j2.xml");
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
        Flow flow = new Flow(options);
        return flow.execute();
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
