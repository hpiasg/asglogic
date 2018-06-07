package de.uni_potsdam.hpi.asg.logictool.mapping.model;

/*
 * Copyright (C) 2015 - 2018 Norman Kluge
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

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;

public class WireMapping extends Mapping {

    private NetlistVariable driver;

    public WireMapping(NetlistVariable driver, NetlistVariable drivee, Netlist netlist, Set<NetlistTerm> terms) {
        super(terms, drivee);
        this.driver = driver;
    }

    public WireMapping(NetlistVariable driver, NetlistVariable drivee, Netlist netlist) {
        super(netlist.getNetlistTermByBdd(driver.toBDD()));
        this.driver = driver;
        this.drivee = drivee;
    }

    @Override
    public boolean replaceVar(NetlistVariable replacement, NetlistVariable obsolete) {
        if(this.driver == obsolete) {
            this.driver = replacement;
        }
        return super.replaceVar(replacement, obsolete);
    }

    @Override
    public boolean isMapped() {
        return true;
    }

    public NetlistVariable getDriver() {
        return driver;
    }
}