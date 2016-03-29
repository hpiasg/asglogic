package de.uni_potsdam.hpi.asg.logictool.netlist;

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

import java.util.HashSet;
import java.util.Set;

import net.sf.javabdd.BDD;

public class NetlistVariable implements Comparable<NetlistVariable> {

    private String           name;
    private int              id;
    private Netlist          netlist;
    private NetlistTerm      driver;
    private Set<NetlistTerm> reader;

    public NetlistVariable(String name, int id, Netlist netlist) {
        this.name = name;
        this.id = id;
        this.netlist = netlist;
        this.reader = new HashSet<>();
    }

    @Override
    public String toString() {
        return "" + name + "";
    }

    public BDD toBDD() {
        return netlist.getFac().ithVar(id);
    }

    public BDD toNotBDD() {
        return netlist.getFac().nithVar(id);
    }

    public NetlistTerm getDriver() {
        return driver;
    }

    public int getId() {
        return id;
    }

    public Set<NetlistTerm> getReader() {
        return reader;
    }

    public String getName() {
        return name;
    }

    public void setDriver(NetlistTerm driver) {
        this.driver = driver;
    }

    public void addReader(NetlistTerm term) {
        this.reader.add(term);
    }

    public void removeReader(NetlistTerm term) {
        this.reader.remove(term);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NetlistVariable) {
            NetlistVariable other = (NetlistVariable)obj;
            return this.name.equals(other.name);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(NetlistVariable o) {
        return this.name.compareTo(o.name);
    }

    public boolean removeReaderTransitive(NetlistTerm term) {
        removeReader(term);
        if(this.reader.size() == 0) {
            if(this.driver != null) {
                netlist.removeTerm(this.driver);
                return this.driver.removeReaderTransitive();
            }
        }
        return true;
    }
}