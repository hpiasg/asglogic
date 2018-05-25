package de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco;

/*
 * Copyright (C) 2015 - 2017 Norman Kluge
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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.asgtoolswrapper.DesiJInvoker;
import de.uni_potsdam.hpi.asg.asynctoolswrapper.PetrifyInvoker;
import de.uni_potsdam.hpi.asg.common.invoker.InvokeReturn;
import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.WorkingdirGenerator;
import de.uni_potsdam.hpi.asg.common.stg.GFile;
import de.uni_potsdam.hpi.asg.common.stg.model.STG;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraphComputer;

public class AndDecoSGHelper {
    private static final Logger logger = LogManager.getLogger();

    private File                stgfile;

    public AndDecoSGHelper(File stgfile) {
        this.stgfile = stgfile;
    }

    public StateGraph getNewStateGraph(Set<Signal> signals, Signal outSig) {
        logger.debug("Creating new state graph with inputs " + signals.toString() + " for output " + outSig);

        StringBuilder str = new StringBuilder();
        str.append(outSig.getName() + "__");
        for(Signal sig : signals) {
            str.append(sig.getName());
        }

        File workingDir = WorkingdirGenerator.getInstance().getWorkingDir();
        String basename = stgfile.getName() + "_" + str.toString();
        String newfilename = basename + ".g";
        FileHelper.getInstance().copyfile(stgfile, newfilename);
        STG stg = GFile.importFromFile(new File(workingDir, newfilename));

        Set<String> signames = new HashSet<>();
        for(Signal sig : signals) {
            signames.add(sig.getName());
        }
        //signames.add(outSig.getName());

        for(Signal sig : stg.getSignals()) {
            if(signames.contains(sig.getName())) {
                sig.makeInput();
            } else if(outSig.getName().equals(sig.getName())) {
                continue;
            } else {
                sig.dummify();
            }
        }
        basename = basename.substring(0, Math.min(basename.length(), 10));

        File newfile_d = new File(workingDir, basename + "_d.g");
        File newfile_dk = new File(workingDir, basename + "_dk.g");
        File newfile_dkc = new File(workingDir, basename + "_dkc.g");
        File newfile_dkc_log = new File(workingDir, basename + "_dkc.log");

        GFile.writeGFile(stg, newfile_d);
        InvokeReturn killDummiesRet = DesiJInvoker.killDummies(newfile_dk, newfile_d, false);
        if(killDummiesRet == null || !killDummiesRet.getResult()) {
            return null;
        }
        InvokeReturn cscRet = PetrifyInvoker.solveCSC(newfile_dk, newfile_dkc_log, newfile_dkc);
        if(cscRet == null || !cscRet.getResult()) {
            return null;
        }

        STG stg2 = GFile.importFromFile(newfile_dkc);
        LoggerHelper.setLogLevel(LogManager.getLogger(StateGraphComputer.class), Level.OFF);
        StateGraphComputer comp = new StateGraphComputer(stg2, new TreeSet<Signal>(stg2.getSignals()), null);
        LoggerHelper.setLogLevel(LogManager.getLogger(StateGraphComputer.class), Level.ALL);

        return comp.compute();
    }
}
