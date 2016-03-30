package de.uni_potsdam.hpi.asg.logictool.io;

/*
 * Copyright (C) 2014 - 2015 Norman Kluge
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

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import de.uni_potsdam.hpi.asg.logictool.mapping.model.GateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.Mapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.WireMapping;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model.TechVariable;

public class VerilogOutput {
    private static final String linebreak = System.getProperty("line.separator");

    private StateGraph          stategraph;
    private Netlist             netlist;
    private String              resetname;

    public VerilogOutput(StateGraph stategraph, Netlist netlist, String resetname) {
        this.stategraph = stategraph;
        this.netlist = netlist;
        this.resetname = resetname;
    }

    public String toV() {
        SortedSet<String> inputs = new TreeSet<String>();
        SortedSet<String> outputs = new TreeSet<String>();
        SortedSet<String> wires = new TreeSet<String>();
        for(Signal sig : stategraph.getSTG().getSignals()) {
            switch(sig.getType()) {
                case dummy:
                    break;
                case input:
                    inputs.add(sig.getName());
                    break;
                case internal:
                    wires.add(sig.getName());
                    break;
                case output:
                    outputs.add(sig.getName());
                    break;
            }
        }
        for(Mapping map : netlist.getMappings()) {
            if(map instanceof GateMapping) {
                NetlistVariable var = map.getDrivee();
                if(!inputs.contains(var.getName()) && !outputs.contains(var.getName())) {
                    wires.add(var.getName());
                }
            }
        }

        // interface
        StringBuilder str = new StringBuilder();
        str.append("module test (" + linebreak);
        for(String str2 : inputs) {
            str.append("  " + str2 + "," + linebreak);
        }
        str.append("  " + resetname + "," + linebreak);
        for(String str2 : outputs) {
            str.append("  " + str2 + "," + linebreak);
        }
        str.replace(str.length() - 2, str.length(), linebreak + ");" + linebreak + linebreak);

        // signals
        for(String str2 : inputs) {
            str.append("input " + str2 + ";" + linebreak);
        }
        str.append("input " + resetname + ";" + linebreak);
        for(String str2 : outputs) {
            str.append("output " + str2 + ";" + linebreak);
        }
        for(String str2 : wires) {
            str.append("wire " + str2 + ";" + linebreak);
        }
        str.append(linebreak);

        for(Mapping m : netlist.getMappings()) {
            if(m instanceof WireMapping) {
                WireMapping m2 = (WireMapping)m;
                str.append("assign " + m2.getDrivee().getName() + " = " + m2.getDriver().getName() + ";" + linebreak);
            }
        }

        int id = 0;
        //float size = 0;
        for(Mapping m : netlist.getMappings()) {
            if(m instanceof GateMapping) {
                GateMapping m2 = (GateMapping)m;
                str.append(m2.getGate().getName() + " U" + (id++) + " (");
                for(Entry<TechVariable, NetlistVariable> entry : m2.getMapping().entrySet()) {
                    if(entry.getKey() == m2.getGate().getLoopInput()) {
                        continue;
                    }
                    str.append("." + entry.getKey() + "(" + entry.getValue() + "), ");
                }
                str.append("." + m2.getGate().getOutputname() + "(" + m.getDrivee().getName() + "));" + linebreak);
                //size += m.getGate().getSize();
            }
        }

        str.append(linebreak + "endmodule" + linebreak);

        return str.toString();
    }
}
