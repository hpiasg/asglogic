package de.uni_potsdam.hpi.asg.logictool;

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

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import de.uni_potsdam.hpi.asg.common.gui.WatchForCloseWindowAdapter;
import de.uni_potsdam.hpi.asg.common.iohelper.BasedirHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper;
import de.uni_potsdam.hpi.asg.common.iohelper.LoggerHelper.Mode;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;
import de.uni_potsdam.hpi.asg.common.technology.TechnologyDirectory;
import de.uni_potsdam.hpi.asg.logictool.io.Config;
import de.uni_potsdam.hpi.asg.logictool.io.ConfigFile;
import de.uni_potsdam.hpi.asg.logictool.runner.LogicParameters;
import de.uni_potsdam.hpi.asg.logictool.runner.RunLogicPanel;

public class LogicRunMain {

    public static final File LOGIC_BIN_UNIX = new File(CommonConstants.DEF_BIN_DIR_FILE, "ASGlogic");
    public static final File LOGIC_BIN_WIN  = new File(CommonConstants.DEF_BIN_DIR_FILE, "ASGlogic.bat");

    public static void main(String[] args) {
        int status = main2(args);
        System.exit(status);
    }

    public static int main2(String[] args) {
        boolean isDebug = false;
        for(String str : args) {
            if(str.equals("-debug")) {
                isDebug = true;
            }
        }

        if(isDebug) {
            LoggerHelper.initLogger(3, null, true, Mode.cmdline);
        } else {
            LoggerHelper.initLogger(3, null, false, Mode.gui);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
            return 1;
        }

        Config cfg = ConfigFile.readIn(LogicMain.DEF_CONFIG_FILE);
        String defTechName = null;
        if(cfg.defaultTech != null) {
            File defTechFile = BasedirHelper.replaceBasedirAsFile(cfg.defaultTech);
            if(defTechFile != null && defTechFile.exists()) {
                defTechName = defTechFile.getName().replace(CommonConstants.GENLIB_FILE_EXTENSION, "");
            }
        }
        TechnologyDirectory techDir = TechnologyDirectory.createDefault();
        if(techDir == null) {
            return 1;
        }
        LogicParameters params = new LogicParameters(defTechName, techDir);

        JFrame runframe = new JFrame("ASGlogic runner");
        RunLogicPanel runpanel = new RunLogicPanel(runframe, params, isDebug);
        if(runpanel.hasErrorOccured()) {
            return 1;
        }
        runframe.getContentPane().add(runpanel);
        WatchForCloseWindowAdapter adapt = new WatchForCloseWindowAdapter();
        runframe.addWindowListener(adapt);
        runframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        runframe.pack();
        runframe.setLocationRelativeTo(null); //center
        runframe.setVisible(true);

        while(!adapt.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
            }
        }
        return 0;

    }
}
