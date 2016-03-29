package de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model;

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

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class TechVariable implements TechTerm {

    private String name;

    public TechVariable(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "" + name + "";
    }

    @Override
    public BDD toBDD(BDDFactory fac, Map<TechVariable, Integer> map) {
        if(name.equals("0") || name.equals("CONST0")) {
            return fac.zero();
        } else if(name.equals("1") || name.equals("CONST1")) {
            return fac.one();
        } else if(!map.containsKey(this)) {
            int id = fac.extVarNum(1);
            map.put(this, id);
        }
        return fac.ithVar(map.get(this));
    }

    public String getName() {
        return name;
    }
}