package de.uni_potsdam.hpi.asg.logictool.techfile;

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

import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model.TechVariable;
import net.sf.javabdd.BDD;

public class Gate {

    private String       name;
    private BDD          expression;
    private String       outputname;
    private float        size;
    private TechVariable loopinput;

    public Gate(String name, BDD expression, float size, String outputname, TechVariable loopinput) {
        this.name = name;
        this.expression = expression;
        this.outputname = outputname;
        this.size = size;
        this.loopinput = loopinput;
    }

    public BDD getExpression() {
        return expression;
    }

    public String getName() {
        return name;
    }

    public String getOutputname() {
        return outputname;
    }

    public float getSize() {
        return size;
    }

    public TechVariable getLoopInput() {
        return loopinput;
    }

    @Override
    public String toString() {
        return name + "(" + size + "): " + outputname + "=" + expression + "; L: " + loopinput;
    }
}
