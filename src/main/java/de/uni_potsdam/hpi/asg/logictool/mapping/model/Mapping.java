package de.uni_potsdam.hpi.asg.logictool.mapping.model;

/*
 * Copyright (C) 2014 - 2018 Norman Kluge
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

import java.util.HashSet;
import java.util.Set;

import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;

public abstract class Mapping {

    protected Set<NetlistTerm> terms;
    protected NetlistVariable  drivee;

    protected Mapping(NetlistTerm term) {
        this.terms = new HashSet<>();
        this.terms.add(term);
        this.drivee = term.getDrivee();
    }

    protected Mapping(Set<NetlistTerm> terms, NetlistVariable drivee) {
        this.terms = terms;
        this.drivee = drivee;
    }

    public void setMappingInTerms() {
        if(terms != null) {
            for(NetlistTerm t : terms) {
                t.setMapping(this);
            }
        }
    }

    public boolean replaceVar(NetlistVariable replacement, NetlistVariable obsolete) {
        if(this.drivee == obsolete) {
            this.drivee = replacement;
        }
        return true;
    }

    public abstract boolean isMapped();

    public Set<NetlistTerm> getTerms() {
        return terms;
    }

    public NetlistVariable getDrivee() {
        return drivee;
    }

    public void changeDrivee(NetlistVariable drivee) {
        this.drivee = drivee;
    }
}
