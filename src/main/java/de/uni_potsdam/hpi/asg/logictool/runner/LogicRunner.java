package de.uni_potsdam.hpi.asg.logictool.runner;

/*
 * Copyright (C) 2017 Norman Kluge
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

import java.awt.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractParameters.GeneralBooleanParam;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractRunner;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.logictool.LogicRunMain;
import de.uni_potsdam.hpi.asg.logictool.runner.LogicParameters.BooleanParam;
import de.uni_potsdam.hpi.asg.logictool.runner.LogicParameters.EnumParam;
import de.uni_potsdam.hpi.asg.logictool.runner.LogicParameters.TextParam;

public class LogicRunner extends AbstractRunner {
    private static final Logger logger = LogManager.getLogger();

    private LogicParameters     params;

    public LogicRunner(LogicParameters params) {
        super(params);
        this.params = params;
    }

    public void run(TerminalMode mode) {
        run(mode, null);
    }

    public void run(TerminalMode mode, Window parent) {
        if(!checkParams()) {
            return;
        }
        List<String> cmd = buildCmd();
        exec(cmd, "ASGlogic terminal", mode, null, parent);
    }

    private boolean checkParams() {
        File breezefile = new File(params.getTextValue(TextParam.GFile));
        if(!breezefile.exists()) {
            logger.error("G-file not found");
            return false;
        }
        if(!params.getBooleanValue(BooleanParam.TechLibDef)) {
            File techfile = getTechFile();
            if(!techfile.exists()) {
                logger.error("Techfile not found");
                return false;
            }
        }

        return true;
    }

    private File getTechFile() {
        String techName = params.getEnumValue(EnumParam.TechLib);
        return new File(CommonConstants.DEF_TECH_DIR_FILE, techName + CommonConstants.GENLIB_FILE_EXTENSION);
    }

    private List<String> buildCmd() {
        List<String> cmd = new ArrayList<>();
        File logicbin = null;
        if(SystemUtils.IS_OS_WINDOWS) {
            logicbin = LogicRunMain.LOGIC_BIN_WIN;
        } else if(SystemUtils.IS_OS_UNIX) {
            logicbin = LogicRunMain.LOGIC_BIN_UNIX;
        }
        if(logicbin == null) {
            logger.error("Unsupported operating system");
            return null;
        }
        cmd.add(logicbin.getAbsolutePath());

        addGeneralParams(cmd);
        addAdvancedParams(cmd);
        addDebugParams(cmd);

        cmd.add(params.getTextValue(TextParam.GFile));

        return cmd;
    }

    private void addGeneralParams(List<String> cmd) {
        if(!params.getBooleanValue(BooleanParam.TechLibDef)) {
            cmd.add("-lib");
            File techfile = getTechFile();
            cmd.add(techfile.getAbsolutePath());
        }

        addStandardIOParams(cmd, "-out");
    }

    private void addAdvancedParams(List<String> cmd) {
        cmd.add("-csc");
        if(params.getBooleanValue(BooleanParam.cscN)) {
            cmd.add("N");
        } else if(params.getBooleanValue(BooleanParam.cscP)) {
            cmd.add("P");
        } else if(params.getBooleanValue(BooleanParam.cscM)) {
            cmd.add("M");
        }

        cmd.add("-arch");
        if(params.getBooleanValue(BooleanParam.archGC)) {
            cmd.add("gC");
        } else if(params.getBooleanValue(BooleanParam.archSC)) {
            cmd.add("sC");
        }

        cmd.add("-rst");
        if(params.getBooleanValue(BooleanParam.rstOD)) {
            cmd.add("ondemand");
        } else if(params.getBooleanValue(BooleanParam.rstF)) {
            cmd.add("full");
        }
    }

    private void addDebugParams(List<String> cmd) {
        if(params.getBooleanValue(GeneralBooleanParam.debug)) {
            cmd.add("-debug");
        }
        if(params.getBooleanValue(BooleanParam.unsafeanddeco)) {
            cmd.add("-unsafeanddeco");
        }
    }
}
