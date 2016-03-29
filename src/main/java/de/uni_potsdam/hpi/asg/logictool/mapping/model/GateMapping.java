package de.uni_potsdam.hpi.asg.logictool.mapping.model;

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

import java.util.Map;
import java.util.Set;

import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.techfile.Gate;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model.TechVariable;

public class GateMapping extends Mapping {

    private Gate                               gate;
    private Map<TechVariable, NetlistVariable> mapping;

    public GateMapping(NetlistTerm term, IntermediateGateMapping igmap) {
        super(term);
        this.gate = igmap.getGate();
        this.mapping = igmap.getMapping();
    }

    public GateMapping(NetlistTerm term, Gate gate, Map<TechVariable, NetlistVariable> mapping) {
        super(term);
        this.gate = gate;
        this.mapping = mapping;
    }

    public GateMapping(Set<NetlistTerm> terms, NetlistVariable drivee, IntermediateGateMapping igmap) {
        super(terms, drivee);
        this.gate = igmap.getGate();
        this.mapping = igmap.getMapping();
    }

    public GateMapping(Set<NetlistTerm> terms, NetlistVariable drivee, Gate gate, Map<TechVariable, NetlistVariable> mapping) {
        super(terms, drivee);
        this.gate = gate;
        this.mapping = mapping;
    }

    @Override
    public boolean isMapped() {
        return true;
    }

    @Override
    public String toString() {
        return terms.toString() + " ==> " + gate.getName() + "(" + mapping.toString() + ", " + gate.getOutputname() + "=" + drivee.getName() + ")";
    }

    public Gate getGate() {
        return gate;
    }

    public Map<TechVariable, NetlistVariable> getMapping() {
        return mapping;
    }
}
