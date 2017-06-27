package de.uni_potsdam.hpi.asg.logictool.io;

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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.Invoker;
import de.uni_potsdam.hpi.asg.common.iohelper.ProcessReturn;
import de.uni_potsdam.hpi.asg.logictool.LogicMain;

public class LogicInvoker extends Invoker {
    private final static Logger logger = LogManager.getLogger();

    private static LogicInvoker instance;

    private LogicInvoker() {
    }

    public static LogicInvoker getInstance() {
        if(LogicInvoker.instance == null) {
            LogicInvoker.instance = new LogicInvoker();
            if(Invoker.instance == null) {
                Invoker.instance = LogicInvoker.instance;
            } else {
                logger.warn("Logger instance already set");
            }
        }
        return LogicInvoker.instance;
    }

    public String invokeEspresso(String infile, String outfile) {
        String[] command = convertCmd(LogicMain.config.toolconfig.espressocmd);
        if(command == null) {
            logger.error("Could not read espresso cmd String");
            return null;
        }
        String[] params = {"-of", "-eonset", "-Dso", infile/*, " > ", outfile*/};
        ProcessReturn ret = invoke(command, params);

        if(errorHandling(ret) == true) {
            if(FileHelper.getInstance().writeFile(new File(outfile), ret.getStream())) {
                return ret.getStream();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean invokeDesijKilldummies(File outfile, File infile) {
        String[] params = {"-Y", "-t", "operation=killdummiesrelaxed", "outfile=" + outfile.getAbsolutePath(), infile.getAbsolutePath()}; //"-t"
        return invokeDesij(Arrays.asList(params));
    }

    private boolean invokeDesij(List<String> params) {
        String[] cmd = convertCmd(LogicMain.config.toolconfig.desijcmd);
        if(cmd == null) {
            logger.error("Could not read desij cmd String");
            return false;
        }
        ProcessReturn ret = invoke(cmd, params);
        return errorHandling(ret);
    }

    public boolean invokePetrifyCSC(File infile, File logfile, File outfile) {
        String[] command = convertCmd(LogicMain.config.toolconfig.petrifycmd);
        if(command == null) {
            logger.error("Could not read petrify cmd String");
            return false;
        }
        String[] params = {"-csc", "-dead", "-o", outfile.getAbsolutePath(), "-log", logfile.getAbsolutePath(), infile.getAbsolutePath()};
        ProcessReturn ret = invoke(command, params);
        return errorHandling(ret);
    }

    public boolean invokePUNFandMPSAT(File infile, File outfile) {
        String mcifile = infile.getAbsolutePath().replace(".g", "") + ".mci";

        String[] cmd = convertCmd(LogicMain.config.toolconfig.punfcmd);
        if(cmd == null) {
            logger.error("Could not read punf cmd string");
            return false;
        }
        String[] params = {"-m=" + mcifile, "-f=" + infile};
        ProcessReturn ret = invoke(cmd, params);

        File mci = new File(workingDir, mcifile);
//		logger.debug("punf: " + ret.getCode());
        if(!mci.exists()) { // punf returns != 0 even if there are only warnings
            if(!errorHandling(ret)) {
                logger.error("PUNF Error with " + infile);
                return false;
            }
        }

        File tmpfolder = new File(workingDir, infile.getName() + "_tmp");
        if(!tmpfolder.mkdir()) {
            logger.error("Could not create tmp folder for mpsat: " + tmpfolder.getName());
            return false;
        }

        String[] cmd2 = convertCmd(LogicMain.config.toolconfig.mpsatcmd);
        if(cmd2 == null) {
            logger.error("Could not read mpsat cmd string");
            return false;
        }
        String[] params2 = {"-R", "-f", "-@", "-p0", "-cl", mcifile};
        ProcessReturn ret2 = invoke(cmd2, params2, tmpfolder);
        if(!errorHandling(ret2)) {
            logger.error("MPSAT Error with " + mcifile);
            return false;
        }

        String mpsatresult = tmpfolder.getAbsolutePath() + File.separator + "mpsat.g";
        if(!FileHelper.getInstance().copyfile(new File(mpsatresult), outfile)) {
            logger.error("Could not copy mpsat result file from " + mpsatresult);
            return false;
        }

        return true;
    }
}
