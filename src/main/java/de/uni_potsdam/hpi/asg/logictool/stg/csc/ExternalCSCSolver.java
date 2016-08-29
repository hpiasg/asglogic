package de.uni_potsdam.hpi.asg.logictool.stg.csc;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.logictool.io.LogicInvoker;
import de.uni_potsdam.hpi.asg.logictool.stg.model.STG;

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
    public boolean solveCSC(STG stgin, String stgoutfile) {
        String stginfile = stgin.getFile().getName().replace(".g", "") + "_orig.g";
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

    private boolean solveCSCpetrify(String stginfile, String stgoutfile) {
        logger.info("Solving CSC with petrify");
        return LogicInvoker.getInstance().invokePetrifyCSC(stginfile, stgoutfile.replace(".g", "") + ".log", stgoutfile);
    }

    private boolean solveCSCmspat(String stginfile, String stgoutfile) {
        logger.info("Solving CSC with mpsat");
        return LogicInvoker.getInstance().invokePUNFandMPSAT(stginfile, stgoutfile);
    }
}
