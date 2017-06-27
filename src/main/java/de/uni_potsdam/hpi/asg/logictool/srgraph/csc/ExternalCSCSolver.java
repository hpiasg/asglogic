package de.uni_potsdam.hpi.asg.logictool.srgraph.csc;

/*
 * Copyright (C) 2016 Norman Kluge
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.logictool.io.LogicInvoker;

public class ExternalCSCSolver implements CSCSolver {
    private static final Logger logger = LogManager.getLogger();

    public enum ExternalCSCSolverConfig {
        petrify, mpsat
    }

    private ExternalCSCSolverConfig config;

    public ExternalCSCSolver(ExternalCSCSolverConfig config) {
        this.config = config;
    }

    @Override
    public boolean solveCSC(STG stgin, File stgoutfile) {
        File workingDir = WorkingdirGenerator.getInstance().getWorkingDir();
        File stginfile = new File(workingDir, stgin.getFile().getName().replace(".g", "") + "_orig.g");
        FileHelper.getInstance().copyfile(stgin.getFile(), stginfile);
        switch(config) {
            case mpsat:
                return solveCSCmspat(stginfile, stgoutfile);
            case petrify:
                return solveCSCpetrify(stginfile, stgoutfile);
            default:
                return false;
        }
    }

    private boolean solveCSCpetrify(File stginfile, File stgoutfile) {
        logger.info("Solving CSC with petrify");
        File workingDir = WorkingdirGenerator.getInstance().getWorkingDir();
        String logFileName = stgoutfile.getName().replace(".g", "") + ".log";
        return LogicInvoker.getInstance().invokePetrifyCSC(stginfile, new File(workingDir, logFileName), stgoutfile);
    }

    private boolean solveCSCmspat(File stginfile, File stgoutfile) {
        logger.info("Solving CSC with mpsat");
        return LogicInvoker.getInstance().invokePUNFandMPSAT(stginfile, stgoutfile);
    }
}
