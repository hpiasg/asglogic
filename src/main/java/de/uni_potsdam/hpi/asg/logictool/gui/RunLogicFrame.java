package de.uni_potsdam.hpi.asg.logictool.gui;

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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.gui.PropertiesPanel;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractParameters;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractParameters.GeneralBooleanParam;
import de.uni_potsdam.hpi.asg.common.gui.runner.AbstractRunFrame;
import de.uni_potsdam.hpi.asg.logictool.gui.LogicParameters.BooleanParam;
import de.uni_potsdam.hpi.asg.logictool.gui.LogicParameters.EnumParam;
import de.uni_potsdam.hpi.asg.logictool.gui.LogicParameters.TextParam;

public class RunLogicFrame extends AbstractRunFrame {
    private static final long   serialVersionUID = 2663337555026127634L;
    private static final Logger logger           = LogManager.getLogger();

    private LogicParameters     params;

    public RunLogicFrame(final LogicParameters params, WindowAdapter adapt, boolean isDebug) {
        super("ASGlogic runner", params, adapt);
        this.params = params;

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        constructGeneralPanel(tabbedPane);
        constructAdvancedPanel(tabbedPane);
        constructDebugPanel(tabbedPane, isDebug);

        JButton runBtn = new JButton("Run");
        runBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LogicRunner run = new LogicRunner(params);
                run.run();
            }
        });
        getContentPane().add(runBtn, BorderLayout.PAGE_END);
    }

    private void constructGeneralPanel(JTabbedPane tabbedPane) {
        PropertiesPanel panel = new PropertiesPanel(this);
        tabbedPane.addTab("General", null, panel, null);
        GridBagLayout gbl_generalpanel = new GridBagLayout();
        gbl_generalpanel.columnWidths = new int[]{150, 300, 30, 80, 0};
        gbl_generalpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_generalpanel.rowHeights = new int[]{15, 15, 15, 15, 15, 15, 15, 15, 15, 0};
        gbl_generalpanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_generalpanel);

        panel.addTextEntry(0, TextParam.GFile, "G file", "", true, JFileChooser.FILES_ONLY, false);

        String[] techs = params.getAvailableTechs();
        String defTech = params.getDefTech();
        if(techs.length == 0) {
            logger.error("No technologies installed. Please run ASGtechmngr");
            errorOccured = true;
        }
        panel.addTechnologyChooserWithDefaultEntry(1, "Technology library", techs, defTech, EnumParam.TechLib, BooleanParam.TechLibDef, "Use default");
        addOutSection(panel, 2, "logic.v");
        // 4: blank
        addIOSection(panel, 5, AbstractParameters.basedirStr + "/config/logicconfig.xml");

        getDataFromPanel(panel);
    }

    private void constructAdvancedPanel(JTabbedPane tabbedPane) {
        PropertiesPanel panel = new PropertiesPanel(this);
        tabbedPane.addTab("Advanced", null, panel, null);
        GridBagLayout gbl_advpanel = new GridBagLayout();
        gbl_advpanel.columnWidths = new int[]{200, 300, 30, 80, 0};
        gbl_advpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_advpanel.rowHeights = new int[]{15, 15, 15, 0};
        gbl_advpanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_advpanel);

        panel.addSingleRadioButtonGroupEntry(0, "Solve CSC", new String[]{"Don't solve", "Petrify", "MPSAT"}, new BooleanParam[]{BooleanParam.cscN, BooleanParam.cscP, BooleanParam.cscM}, 1);
        panel.addSingleRadioButtonGroupEntry(1, "Architecture", new String[]{"generalised C", "stanadard C"}, new BooleanParam[]{BooleanParam.archGC, BooleanParam.archSC}, 0);
        panel.addSingleRadioButtonGroupEntry(2, "Reset insertion", new String[]{"On demand", "full"}, new BooleanParam[]{BooleanParam.rstOD, BooleanParam.rstF}, 0);

        getDataFromPanel(panel);
    }

    private void constructDebugPanel(JTabbedPane tabbedPane, boolean isDebug) {
        PropertiesPanel panel = new PropertiesPanel(this);
        if(isDebug) {
            tabbedPane.addTab("Debug", null, panel, null);
        }
        GridBagLayout gbl_advpanel = new GridBagLayout();
        gbl_advpanel.columnWidths = new int[]{200, 300, 30, 80, 0};
        gbl_advpanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_advpanel.rowHeights = new int[]{15, 0};
        gbl_advpanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_advpanel);

        panel.addCheckboxEntry(0, GeneralBooleanParam.debug, "Debug", isDebug);

        getDataFromPanel(panel);
    }
}