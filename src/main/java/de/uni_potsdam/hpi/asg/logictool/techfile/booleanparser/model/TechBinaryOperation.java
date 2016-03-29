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

public class TechBinaryOperation implements TechTerm {

    public enum TechBinaryOperator {
        and, or;

        @Override
        public String toString() {
            switch(this) {
                case and:
                    return "&";
                case or:
                    return "|";
                default:
                    return null;
            }
        }
    }

    private TechTerm           t1, t2;
    private TechBinaryOperator op;

    public TechBinaryOperation(TechTerm t1, TechBinaryOperator op, TechTerm t2) {
        this.t1 = t1;
        this.t2 = t2;
        this.op = op;
    }

    @Override
    public String toString() {
        return "(" + t1.toString() + " " + op + " " + t2.toString() + ")";
    }

    @Override
    public BDD toBDD(BDDFactory fac, Map<TechVariable, Integer> map) {
        BDD t1b = t1.toBDD(fac, map);
        BDD t2b = t2.toBDD(fac, map);
        switch(op) {
            case and:
                return t1b.and(t2b);
            case or:
                return t1b.or(t2b);
            default:
                System.err.println("Fehler BDD Binary OP unknown");
                return null;
        }
    }

    public TechBinaryOperator getOp() {
        return op;
    }

    public TechTerm getT1() {
        return t1;
    }

    public TechTerm getT2() {
        return t2;
    }
}