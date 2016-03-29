package de.uni_potsdam.hpi.asg.logictool.synthesis.model;

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

import java.util.BitSet;
import java.util.SortedSet;

import net.sf.javabdd.BDD;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;

public class EspressoTerm {
    private static final Logger logger = LogManager.getLogger();

    private String              line;
    private String[]            vars;

    public EspressoTerm(String line, String[] vars) {
        this.line = line;
        this.vars = vars;
    }

    //public NetlistTerm getAsTerm(Netlist netlist) {
    public BDD getAsBdd(Netlist netlist) {
        BDD bdd = netlist.getFac().one();
        for(int i = 0; i < line.length(); i++) {
            switch(line.charAt(i)) {
                case '0':
                    bdd = bdd.and(internalGetVar(netlist, i).toNotBDD());
                    break;
                case '1':
                    bdd = bdd.and(internalGetVar(netlist, i).toBDD());
                    break;
                case '-':
                    break;
                default:
                    System.err.println("Unknown char in table: " + line.charAt(i));
            }
        }
        return bdd;
    }

    private NetlistVariable internalGetVar(Netlist netlist, int index) {
        String name = vars[index];
        return netlist.getNetlistVariableByName(name);
    }

    public String getLine() {
        return line;
    }

    @Override
    public String toString() {
        return line + "\n";
    }

    public boolean evaluate(BitSet state, SortedSet<Signal> sortedSignals, String resetname) {
        boolean retVal = true;
        int lineid = -1;
        //System.out.println("State: " + BitSetHelper.formatBitset(state, 7) + " // Line: " + line);
        for(String linevarname : vars) {
            lineid++;
            //linevar = reset
            if(linevarname.equals(resetname)) {
                continue;
            }
            //line for this linevar irrelevant
            if(line.charAt(lineid) == '-') {
                continue;
            }
            int id = 0;
            for(Signal sig : sortedSignals) {
                if(sig.getName().equals(linevarname)) {
                    break;
                }
                id++;
            }
            if(sortedSignals.size() == id) {
                logger.error("Signal not found: " + linevarname + " in " + sortedSignals.toString());
                return false;
            }
            boolean stateValue = state.get(sortedSignals.size() - id - 1);
            char lineValue = line.charAt(lineid);
            if(lineValue == '0' && stateValue) {
                retVal = false;
            } else if(lineValue == '0' && !stateValue) {
                continue;
            } else if(lineValue == '1' && stateValue) {
                continue;
            } else if(lineValue == '1' && !stateValue) {
                retVal = false;
            } else {
                logger.error("Cant evaluate " + lineValue + " and " + stateValue);
                return false;
            }
        }
        return retVal;
    }
}
