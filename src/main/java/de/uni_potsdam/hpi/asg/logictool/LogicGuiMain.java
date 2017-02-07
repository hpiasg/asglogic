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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import de.uni_potsdam.hpi.asg.common.gui.WatchForCloseWindowAdapter;
import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.technology.TechnologyDirectory;
import de.uni_potsdam.hpi.asg.logictool.gui.LogicParameters;
import de.uni_potsdam.hpi.asg.logictool.gui.RunLogicFrame;
import de.uni_potsdam.hpi.asg.logictool.io.Config;
import de.uni_potsdam.hpi.asg.logictool.io.ConfigFile;

public class LogicGuiMain {

    public static final String techdir       = "$BASEDIR/tech";
    public static final String logicconfig   = "$BASEDIR/config/logicconfig.xml";
    public static final String logicbin_unix = "$BASEDIR/bin/ASGlogic";
    public static final String logicbin_win  = "$BASEDIR/bin/ASGlogic.bat";

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
    }

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

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
            return 1;
        }

        File cfgFile = FileHelper.getInstance().replaceBasedir(logicconfig);
        Config cfg = ConfigFile.readIn(cfgFile);
        String defTechName = null;
        if(cfg.defaultTech != null) {
            File defTechFile = FileHelper.getInstance().replaceBasedir(cfg.defaultTech);
            if(defTechFile != null && defTechFile.exists()) {
                defTechName = defTechFile.getName().replace(TechnologyDirectory.genlibfileExtension, "");
            }
        }
        TechnologyDirectory techDir = TechnologyDirectory.create(techdir, null);
        if(techDir == null) {
            return 1;
        }
        LogicParameters params = new LogicParameters(defTechName, techDir);

        WatchForCloseWindowAdapter adapt = new WatchForCloseWindowAdapter();
        RunLogicFrame rframe = new RunLogicFrame(params, adapt, isDebug);
        if(rframe.hasErrorOccured()) {
            return 1;
        }

        rframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rframe.pack();
        rframe.setLocationRelativeTo(null); //center
        rframe.setVisible(true);

        while(!adapt.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
            }
        }
        return 0;

    }
}
