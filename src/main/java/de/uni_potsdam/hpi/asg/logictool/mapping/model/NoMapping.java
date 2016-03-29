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

import java.util.Set;

import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;

public class NoMapping extends Mapping {

    public NoMapping(NetlistTerm term) {
        super(term);
    }

    public NoMapping(Set<NetlistTerm> terms, NetlistVariable drivee) {
        super(terms, drivee);
    }

    @Override
    public boolean isMapped() {
        return false;
    }

    @Override
    public String toString() {
        return "[" + terms.toString() + "] ==> NO\n";
    }
}
