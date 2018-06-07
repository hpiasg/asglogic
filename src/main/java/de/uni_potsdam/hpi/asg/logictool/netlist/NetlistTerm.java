package de.uni_potsdam.hpi.asg.logictool.netlist;

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

import java.util.EnumSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.Mapping;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDPairing;

public class NetlistTerm {
    private static final Logger logger = LogManager.getLogger();

    public enum NetlistTermAnnotation {
        rstNew, rstPart
    }

    private BDD                            bdd;
    protected Netlist                      netlist;
    private NetlistVariable                drivee;
    private Mapping                        mapping;
    protected NetlistVariable              loopVar;
    private EnumSet<NetlistTermAnnotation> annotations;

    public NetlistTerm(BDD bdd, Netlist netlist) {
        this.bdd = bdd;
        this.netlist = netlist;
        this.loopVar = null;
        this.annotations = EnumSet.noneOf(NetlistTermAnnotation.class);
    }

    public BDD getBdd() {
//		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//		StackTraceElement e = stackTraceElements[2];
//		String caller = e.getClassName() + "." + e.getMethodName() + ":" + Integer.toString(e.getLineNumber());
//		System.out.println("Get BDD");
//		System.out.println("\t" + toString());
//		System.out.println("\t" + caller);
        return bdd;
    }

    public NetlistVariable getDrivee() {
        return drivee;
    }

    void setDrivee(NetlistVariable newdrivee) {
        if(this.drivee != null) {
            logger.warn("Drivee was already set");
        }
        this.drivee = newdrivee;
    }

    void changeDrivee(NetlistVariable newdrivee) {
        if(mapping != null) {
            mapping.changeDrivee(newdrivee);
        }
        this.drivee = newdrivee;
    }

    public void setMapping(Mapping mapping) {
        this.mapping = mapping;
    }

    public Mapping getMapping() {
        return mapping;
    }

    /**
     * 
     * @param replacement
     * @param obsolete
     * @return 0: okay, 1: newbdd already in index, -1 not okay
     */
    int replace(NetlistVariable replacement, NetlistVariable obsolete) {
//        if(mapping != null) {
//            logger.error("Term is already mapped");
//            return -1;
//        }
        if(drivee == obsolete) {
            drivee.setDriver(null);
            changeDrivee(replacement);
            replacement.setDriver(this);
        }

        if(mapping != null) {
            mapping.replaceVar(replacement, obsolete);
        }

        Set<NetlistVariable> vars = BDDHelper.getVars(bdd, netlist);
        if(vars.contains(obsolete)) {
            BDD oldbdd = bdd;
//			System.out.println("Term " + toString() + " replacing");
            obsolete.removeReader(this);
            replacement.addReader(this);
            BDDPairing pair = netlist.getFac().makePair();
            pair.set(obsolete.getId(), replacement.getId());
            bdd = bdd.replace(pair);
//			System.out.println("Now " + toString());
            return netlist.updateTermBDDIndex(oldbdd);
        }

        return 0;
    }

    /**
     * 
     * @param newbdd
     * @return 0: okay, 1: newbdd already in index, -1 not okay
     */
    int setBdd(BDD newbdd) {
//		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//		StackTraceElement e = stackTraceElements[2];
//		String caller = e.getClassName() + "." + e.getMethodName() + ":" + Integer.toString(e.getLineNumber());
//		System.out.println("Set BDD");
//		System.out.println("\t" + toString());
//		System.out.println("\t" + caller);
//		System.out.println("\t" + BDDHelper.getFunctionString(newbdd, netlist));

        if(mapping != null) {
            logger.error("Term is already mapped");
            return -1;
        }
        Set<NetlistVariable> oldvars = BDDHelper.getVars(bdd, netlist);
        for(NetlistVariable var : oldvars) {
            var.removeReader(this);
        }
        Set<NetlistVariable> newvars = BDDHelper.getVars(newbdd, netlist);
        for(NetlistVariable var : newvars) {
            var.addReader(this);
        }
        BDD oldbdd = bdd;
        bdd = newbdd;
        return netlist.updateTermBDDIndex(oldbdd);
    }

    @Override
    public String toString() {
        return ((drivee == null) ? "???" : drivee.toString()) + "=" + BDDHelper.getFunctionString(bdd, netlist);
    }

    public NetlistVariable getLoopVar() {
        return loopVar;
    }

    void addAnnotation(NetlistTermAnnotation annot) {
        annotations.add(annot);
    }

    public boolean containsAnnotation(NetlistTermAnnotation annot) {
        return annotations.contains(annot);
    }

    public boolean mergingAllowed() {
        return true;
    }

    public boolean buildingLoopbackAllowed() {
        return true;
    }

    public boolean removeReaderTransitive() {
        Set<NetlistVariable> vars = BDDHelper.getVars(bdd, netlist);
        for(NetlistVariable var : vars) {
            if(!var.removeReaderTransitive(this)) {
                return false;
            }
        }
        return true;
    }
}
