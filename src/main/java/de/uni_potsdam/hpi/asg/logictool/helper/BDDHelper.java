package de.uni_potsdam.hpi.asg.logictool.helper;

import java.util.BitSet;

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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State.Value;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import net.sf.javabdd.BDD;

public class BDDHelper {
    private static final Logger logger = LogManager.getLogger();

    public static String getFunctionString(BDD bdd, Netlist netlist) {
        if(bdd.isZero()) {
            return "0";
        } else if(bdd.isOne()) {
            return "1";
        } else if(isOrGate(bdd)) {
            return formatOrGate(bdd, netlist);
        } else if(isAndGate(bdd)) {
            return formatArbitraryFunction(bdd, netlist); // And Gate looks normal in this format
        } else {
            return formatArbitraryFunction(bdd, netlist);
        }
    }

    private static String formatOrGate(BDD bdd, Netlist netlist) {
        StringBuilder str = new StringBuilder();
        str.append("(");
        for(NetlistVariable var : getVars(bdd, netlist)) {
            if(!isPos(bdd, var)) {
                str.append("!");
            }
            str.append(var.getName() + "+");
        }
        str.replace(str.length() - 1, str.length(), ")");
        return str.toString();
    }

    @SuppressWarnings("unchecked")
    private static String formatArbitraryFunction(BDD bdd, Netlist netlist) {
        StringBuilder str = new StringBuilder();
        Object x = bdd.allsat();
        if(x instanceof LinkedList<?>) {
            for(Object o : (LinkedList<Object>)x) {
                int id = 0;
                byte[] a = (byte[])o;
                str.append("(");
                for(byte b : a) {
                    switch(b) {
                        case -1:
                            break;
                        case 0:
                            str.append("!" + netlist.getNetlistVariableByBddId(id).getName() + "*");
                            break;
                        case 1:
                            str.append(netlist.getNetlistVariableByBddId(id).getName() + "*");
                            break;
                        default:
                            logger.error("BBD allsat entry unknown");
                    }
                    id++;
                }
                str.replace(str.length() - 1, str.length(), ")+");
            }
            str.replace(str.length() - 1, str.length(), "");
        }
        return str.toString();
    }

    public static boolean isNotGate(BDD bdd) {
        if(isAndGateInternal(bdd) && isOrGateInternal(bdd)) {
            int var = getVars(bdd).iterator().next();
            if(!isPosAnd(bdd, var)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuffer(BDD bdd) {
        if(isAndGateInternal(bdd) && isOrGateInternal(bdd)) {
            int var = getVars(bdd).iterator().next();
            if(isPosAnd(bdd, var)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAndGate(BDD bdd) {
//		if(isNotGate(bdd) || isBuffer(bdd)) {
//			return false;
//		}
        return isAndGateInternal(bdd);
    }

    private static boolean isAndGateInternal(BDD bdd) {
        // only one "1" in table
        double sats = bdd.satCount();
        int irr = numberOfIrrelevantVars(bdd);
        double onetableentries = sats / Math.pow(2, irr);
        if(onetableentries == 1) {
            return true;
        }
        return false;
    }

    public static boolean isOrGate(BDD bdd) {
//		if(isNotGate(bdd) || isBuffer(bdd)) {
//			return false;
//		}
        return isOrGateInternal(bdd);
    }

    private static boolean isOrGateInternal(BDD bdd) {
        // only one "0" in table
        double sats = bdd.satCount();
        int irr = numberOfIrrelevantVars(bdd);
        double onetableentries = sats / Math.pow(2, irr);
        if(onetableentries == (Math.pow(2, BDDHelper.numberOfVars(bdd)) - 1)) {
            return true;
        }
        return false;
    }

    private static int numberOfIrrelevantVars(BDD bdd) {
        return bdd.getFactory().varNum() - numberOfVars(bdd);
    }

    public static int numberOfVars(BDD bdd) {
        int num = 0;
        for(int x : bdd.varProfile()) {
            if(x > 0) {
                num++;
            }
        }
        return num;
    }

    public static Set<NetlistVariable> getVars(BDD bdd, Netlist netlist) {
        Set<NetlistVariable> retVal = new LinkedHashSet<>();
        for(int id : getVars(bdd)) {
            retVal.add(netlist.getNetlistVariableByBddId(id));
        }
        return retVal;
    }

    private static Set<Integer> getVars(BDD bdd) {
        Set<Integer> retVal = new LinkedHashSet<>();
        int id = 0;
        for(int x : bdd.varProfile()) {
            if(x > 0) {
                retVal.add(id);
            }
            id++;
        }
        return retVal;
    }

    public static Boolean isPos(BDD bdd, NetlistVariable var) {
        return isPos(bdd, var.getId());
    }

    private static Boolean isPos(BDD bdd, int id) {
        if(isAndGate(bdd)) {
            return isPosAnd(bdd, id);
        } else if(isOrGate(bdd)) {
            return isPosOr(bdd, id);
        } else {
            logger.warn("ispos only for 'and' and 'or' gates");
            return null;
        }
    }

    private static Boolean isPosOr(BDD bdd, int id) {
        Boolean retVal = isPosAnd(bdd.not(), id);
        if(retVal == null) {
            return null;
        }
        return !retVal;
    }

    @SuppressWarnings("unchecked")
    private static Boolean isPosAnd(BDD bdd, int id) {
        LinkedList<Object> list = (LinkedList<Object>)bdd.allsat();
        for(Object o : list) {
            byte[] a = (byte[])o;
            switch(a[id]) {
                case -1:
                    return null;
                case 0:
                    return false;
                case 1:
                    return true;
                default:
                    logger.error("BBD allsat entry unknown");
            }
        }
        return null;
    }

    public static BDD mergeBDDs(BDD bdd, NetlistVariable replaceVar, BDD replaceBdd, Netlist netlist) {

        Set<NetlistVariable> bddvars = BDDHelper.getVars(bdd, netlist);
        if(!bddvars.contains(replaceVar)) {
            logger.error("ReplaceVar not in Vars");
            return null;
        }

        if(bddvars.size() == 1) {
//			logger.debug("Shortcut");
//			logger.debug("BDD: " + getFunctionString(bdd, netlist));
//			logger.debug("ReplBDD: " + getFunctionString(replaceBdd, netlist));
//			logger.debug("ReplVar: " + replaceVar.getName());
            if(isPos(bdd, replaceVar)) {
                return replaceBdd;
            } else {
                return replaceBdd.not();
            }
//			return replaceBdd;//.and(netlist.getFac().one());
        }

        SortedSet<NetlistVariable> newinputs = new TreeSet<>();
        newinputs.addAll(bddvars);
        newinputs.addAll(BDDHelper.getVars(replaceBdd, netlist));
        newinputs.remove(replaceVar);
//		System.out.println("New Inp: " + newinputs.toString());

        BDD retVal = netlist.getFac().zero();
        BitSet b = new BitSet(newinputs.size());
        for(int i = 0; i < Math.pow(2, newinputs.size()); i++) {
//			System.out.println(i + ": " + BitSetHelper.formatBitset(b, newinputs.size()));
            int index = 0;
            BDD bdd_new = bdd;
            BDD replacBdd_new = replaceBdd;
            BDD minterm = netlist.getFac().one();
            //TODO: xWITH
            for(NetlistVariable var : newinputs) {
                if(b.get(index)) {
                    bdd_new = bdd_new.restrict(var.toBDD());
                    replacBdd_new = replacBdd_new.restrict(var.toBDD());
                    minterm = minterm.and(var.toBDD());
                } else {
                    bdd_new = bdd_new.restrict(var.toNotBDD());
                    replacBdd_new = replacBdd_new.restrict(var.toNotBDD());
                    minterm = minterm.and(var.toNotBDD());
                }
                index++;
            }
            if(replacBdd_new.isZero()) {
                bdd_new = bdd_new.restrict(replaceVar.toNotBDD());
            } else if(replacBdd_new.isOne()) {
                bdd_new = bdd_new.restrict(replaceVar.toBDD());
            } else {
                logger.error("Repl BDD should be one or zero");
            }

            if(bdd_new.isZero()) {

            } else if(bdd_new.isOne()) {
                retVal.orWith(minterm);
            } else {
                logger.error("BDD should be one or zero");
            }

            BitSetHelper.dualNext(b);
        }

//		if(bddvars.size() == 1) {
//			logger.debug("RetVal: " + getFunctionString(retVal, netlist));
//		}

        return retVal;
    }

    public static boolean evaluateBDD(MutableBoolean result, BDD bdd, State state, Netlist netlist) {
        if(bdd == null) {
            return false;
        }
        BDD bdd2 = bdd.and(bdd.getFactory().one());
        for(Entry<Signal, Value> entry : state.getStateValues().entrySet()) {
            BDD sigbdd = null;
            switch(entry.getValue()) {
                case falling:
                case high:
                    sigbdd = netlist.getNetlistVariableBySignal(entry.getKey()).toBDD();
                    break;
                case low:
                case rising:
                    sigbdd = netlist.getNetlistVariableBySignal(entry.getKey()).toNotBDD();
                    break;
            }
            bdd2 = bdd2.restrictWith(sigbdd);
        }
        for(Entry<NetlistVariable, Boolean> entry : netlist.getQuasiSignals().entrySet()) {
            BDD sigbdd = null;
            if(entry.getValue()) {
                //true => Normally 1
                sigbdd = entry.getKey().toBDD();
            } else {
                sigbdd = entry.getKey().toNotBDD();
            }
            bdd2 = bdd2.restrictWith(sigbdd);
        }

        if(bdd2.isOne()) {
            result.setTrue();
        } else if(bdd2.isZero()) {
            result.setFalse();
        } else {
            System.out.println(BDDHelper.getFunctionString(bdd2, netlist));
            logger.error("BDD not restricted enough?!");

            return false;
        }
        return true;
    }
}
