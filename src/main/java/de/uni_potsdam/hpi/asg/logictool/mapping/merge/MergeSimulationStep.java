package de.uni_potsdam.hpi.asg.logictool.mapping.merge;

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

import java.util.Set;

import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import net.sf.javabdd.BDD;

public class MergeSimulationStep {

    private Set<NetlistTerm> terms;
    private NetlistTerm      newTerm;
    private BDD              bdd;
    private NetlistVariable  loopvar;
    private NetlistVariable  replvar;
    private NetlistVariable  drivee;
    private boolean          buildLoopbackAllowed;

    public MergeSimulationStep(BDD bdd, Set<NetlistTerm> terms, NetlistVariable loopvar, NetlistVariable drivee, NetlistVariable replvar, NetlistTerm newTerm, boolean buildLoopbackAllowed) {
        this.terms = terms;
        this.newTerm = newTerm;
        this.bdd = bdd;
        this.replvar = replvar;
        this.loopvar = loopvar;
        this.drivee = drivee;
        this.buildLoopbackAllowed = buildLoopbackAllowed;
    }

    public NetlistTerm getNewTerm() {
        return newTerm;
    }

    public Set<NetlistTerm> getTerms() {
        return terms;
    }

    public BDD getBdd() {
        return bdd;
    }

    public NetlistVariable getReplvar() {
        return replvar;
    }

    public NetlistVariable getLoopvar() {
        return loopvar;
    }

    public NetlistVariable getDrivee() {
        return drivee;
    }

    public boolean isBuildLoopbackAllowed() {
        return buildLoopbackAllowed;
    }
}
