package de.uni_potsdam.hpi.asg.logictool.srgraph;

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;

public class GraphicalStateGraph extends JFrame {

    private static final Logger logger           = LogManager.getLogger();
    private static final long   serialVersionUID = 1312772109999223948L;

    public GraphicalStateGraph(StateGraph stategraph, boolean wait, File exp) {
        super("State graph");

        Set<State> states2 = stategraph.getStates();

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

        Map<State, Object> map = new HashMap<>();
        graph.getModel().beginUpdate();

        StringBuilder legendstr = new StringBuilder();
        for(Signal sig : stategraph.getAllSignals()) {
            legendstr.append(sig.getName() + " ");
        }
        Object legend = graph.insertVertex(parent, null, legendstr.toString(), 0, 0, 0, 0, "");
        graph.updateCellSize(legend);
        for(State s : states2) {
            String id = Integer.toString(s.getId()) + " (" + s.getBinaryRepresentationDbg(stategraph.getAllSignals()) + ")";
            String style = null;
            if(s == stategraph.getInitState()) {
                style = "shape=doubleEllipse";
            } else {
                style = "shape=ellipse";
            }

            Object v = graph.insertVertex(parent, null, id, 0, 0, 0, 0, style);
            graph.updateCellSize(v);
            mxGeometry geo = graph.getCellGeometry(v);
            mxGeometry geo2 = new mxGeometry(0, 0, geo.getWidth() * 1.5, geo.getHeight() * 1.5);
            model.setGeometry(v, geo2);

            map.put(s, v);
        }
        for(State s : states2) {
            for(Entry<Transition, State> entry : s.getNextStates().entrySet()) {
                String str = entry.getKey().toString();
                State s2 = null;
                for(State s3 : entry.getValue().getPrevStates()) {
                    if(s3 == s) {
                        s2 = s3;
                    }
                }
                if(s2 == null) {
                    logger.error("next yes, prev no");
                    str += " / XXX";
                }
                String style = "";
                graph.insertEdge(parent, null, str, map.get(s), map.get(entry.getValue()), style);
            }
            for(State s2 : s.getPrevStates()) {
                State s3 = null;
                for(Entry<Transition, State> entry : s2.getNextStates().entrySet()) {
                    if(entry.getValue() == s) {
                        s3 = entry.getValue();
                    }
                }
                if(s3 == null) {
                    logger.error("next no, prev yes");
                    graph.insertEdge(parent, null, "", map.get(s2), map.get(s), "strokeColor=red");
                }
            }
        }

        graph.getModel().endUpdate();
        mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
        layout.setForceConstant(150);
        layout.execute(graph.getDefaultParent());

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        getContentPane().add(graphComponent);

        if(exp != null) {
            ExportAction a = new ExportAction();
            a.exportPng(exp, graph, graphComponent);
        }

        setSize(1024, 768);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        graphComponent.getInputMap().put(KeyStroke.getKeyStroke('s'), "save");
        graphComponent.getActionMap().put("save", new ExportAction());

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
