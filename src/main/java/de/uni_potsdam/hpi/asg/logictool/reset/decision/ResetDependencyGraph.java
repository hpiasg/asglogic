package de.uni_potsdam.hpi.asg.logictool.reset.decision;

/*
 * Copyright (C) 2015 Norman Kluge
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;

public class ResetDependencyGraph extends JFrame {
    private static final long serialVersionUID = -3433426370569233566L;

    public ResetDependencyGraph(Map<Signal, Set<Signal>> dependencies, StateGraph sg, boolean wait) {
        super("Reset Dependency Graph");

        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        mxIGraphModel model = graph.getModel();
        graph.setAutoSizeCells(true);
        graph.setCellsResizable(false);

        graph.setCellsEditable(false);
        graph.setAllowDanglingEdges(false);
        graph.setAllowLoops(false);
        graph.setCellsDeletable(false);
        graph.setCellsCloneable(false);
        graph.setCellsDisconnectable(false);
        graph.setDropEnabled(false);
        graph.setSplitEnabled(false);
        graph.setCellsBendable(false);

        graph.getModel().beginUpdate();

        Map<Signal, Object> map = new HashMap<>();
        for(Signal sig : sg.getAllSignals()) {
            String id = sig.getName();
            String style = "shape=rectangle";

            Object v = graph.insertVertex(parent, null, id, 0, 0, 0, 0, style);
            graph.updateCellSize(v);
            mxGeometry geo = graph.getCellGeometry(v);
            mxGeometry geo2 = new mxGeometry(0, 0, geo.getWidth() * 1.5, geo.getHeight() * 1.5);
            model.setGeometry(v, geo2);

            map.put(sig, v);
        }

        for(Entry<Signal, Set<Signal>> entry : dependencies.entrySet()) {
            Object target = map.get(entry.getKey());
            String style = "";
            for(Signal depend : entry.getValue()) {
                Object source = map.get(depend);
                graph.insertEdge(parent, null, "", source, target, style);
            }
        }

        graph.getModel().endUpdate();
        mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
        layout.setForceConstant(150);
        layout.execute(graph.getDefaultParent());

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        getContentPane().add(graphComponent);

        setSize(1024, 768);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        //graphComponent.getInputMap().put(KeyStroke.getKeyStroke('s'), "save");
        //graphComponent.getActionMap().put("save", new ExportAction());

        if(wait) {
            while(isVisible()) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
