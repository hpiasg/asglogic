package de.uni_potsdam.hpi.asg.logictool.netlist;

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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.helper.BDDComparator;
import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.Mapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.NoMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.WireMapping;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.InternalArch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm.NetlistTermAnnotation;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class Netlist {
    private static final Logger            logger = LogManager.getLogger();

    private BDDFactory                     fac;
    private Reset                          reset;

    private Map<String, NetlistVariable>   nameVarMap;
    private Map<Integer, NetlistVariable>  idVarMap;
    private BiMap<Signal, NetlistVariable> sigVarMap;
    private Map<NetlistVariable, Boolean>  quasiSignals;

    private Map<BDD, NetlistTerm>          terms;
    private Set<NetlistTerm>               unmappedTerms;
    private Map<NetlistTerm, Mapping>      mappedTerms;
    private int                            tmpid;

    public Netlist(BDDFactory fac, StateGraph sg, Reset reset) {
        this.fac = fac;
        this.reset = reset;
        this.nameVarMap = new HashMap<>();
        this.idVarMap = new HashMap<>();
        this.terms = new TreeMap<>(new BDDComparator());
        this.unmappedTerms = new HashSet<>();
        this.mappedTerms = new HashMap<>();
        this.quasiSignals = new HashMap<>();
        this.tmpid = 0;

        this.sigVarMap = HashBiMap.create();
        for(Signal sig : sg.getAllSignals()) {
            sigVarMap.put(sig, getNetlistVariableByName(sig.getName()));
        }

        initReset();
    }

    private void initReset() {
        if(reset != null) {
            NetlistVariable resetVar = getNetlistVariableByName(reset.getName());
            NetlistVariable notResetVar = getNetlistVariableByName(reset.getNameNot());

            if(!sigVarMap.containsValue(resetVar)) {
                sigVarMap.put(new Signal(reset.getName(), SignalType.input), resetVar);
            }
            //sigVarMap.put(new Signal(reset.getNameNot(), SignalType.input), getNetlistVariableByName(reset.getNameNot()));
            NetlistTerm notResetTerm = getNetlistTermByBdd(resetVar.toBDD().not());
            addConnection(notResetVar, notResetTerm);
            quasiSignals.put(notResetVar, true);
            quasiSignals.put(resetVar, false);
        }
    }

    public boolean checkNotResetNeeded() {
        if(reset != null) {
            NetlistVariable notResetVar = getNetlistVariableByName(reset.getNameNot());
            if(notResetVar.getReader().isEmpty()) {
                replaceMapping(new NoMapping(notResetVar.getDriver()));
            }
        }
        return true;
    }

    public NetlistVariable getNetlistVariableByName(String name) {
        if(!nameVarMap.containsKey(name)) {
            int id = fac.extVarNum(1);
            NetlistVariable var = new NetlistVariable(name, id, this);
            nameVarMap.put(name, var);
            idVarMap.put(id, var);
        }
        return nameVarMap.get(name);
    }

    public NetlistTerm getNetlistTermByBdd(BDD bdd) {
        return getNetlistTermByBdd(bdd, null);
    }

    public NetlistTerm getNetlistTermByBdd(BDD bdd, EnumSet<NetlistTermAnnotation> annotations) {
        if(!terms.containsKey(bdd)) {
            Set<NetlistVariable> vars = BDDHelper.getVars(bdd, this);
            //Buffer?
            NetlistTerm term = null;
            if(vars.size() == 1) {
                NetlistVariable var = vars.iterator().next();
                if(bdd.restrict(var.toBDD()).isOne()) {
                    //isbuffer
                    term = new NetlistBuffer(bdd, this, var);
                }
            }
            if(term == null) {
                term = new NetlistTerm(bdd, this);
            }
            terms.put(bdd, term);
            unmappedTerms.add(term);
            for(NetlistVariable var : vars) {
                var.addReader(term);
            }
        }
        NetlistTerm term = terms.get(bdd);
        if(annotations != null) {
            for(NetlistTermAnnotation annot : annotations) {
                term.addAnnotation(annot);
            }
        }
        return term;
    }

    public NetlistCelem getNetlistCelem(NetlistVariable setInput, NetlistVariable resetInput, NetlistVariable loopback, InternalArch arch) {
        BDD bdd = NetlistCelem.getCelemBDD(setInput, resetInput, loopback, arch, this);
        if(!terms.containsKey(bdd)) {
            NetlistCelem celem = new NetlistCelem(bdd, setInput, resetInput, loopback, this, arch);
            terms.put(bdd, celem);
            unmappedTerms.add(celem);
            Set<NetlistVariable> vars = BDDHelper.getVars(bdd, this);
            for(NetlistVariable var : vars) {
                var.addReader(celem);
            }
        }
        return (NetlistCelem)terms.get(bdd);
    }

    public boolean mergeWires() {
        Queue<NetlistTerm> check = new LinkedList<>(mappedTerms.keySet());
        while(!check.isEmpty()) {
            NetlistTerm term = check.poll();
            Mapping m = mappedTerms.get(term);
            if(m instanceof WireMapping) {
                WireMapping wire = (WireMapping)m;
                Signal inSig = getSignalByNetlistVariable(wire.getDriver());
                Signal outSig = getSignalByNetlistVariable(wire.getDrivee());
                // in -> [wire] -> out
                if(outSig == null) {
                    // "out" is not a signal, so replace all occurrences of "out" with "in"
                    terms.remove(term.getBdd());
                    mappedTerms.remove(term);
                    replaceVar(wire.getDriver(), wire.getDrivee());
                } else if(inSig == null) {
                    // "out" is a signal, but "in" is not, so replace all occurrences of "in" with "out"
                    terms.remove(term.getBdd());
                    mappedTerms.remove(term);
                    replaceVar(wire.getDrivee(), wire.getDriver());
                } else {
                    // "in" and "out" are signals, so keep wire
                }
            }
        }
        return true;
    }

    public NetlistVariable getNetlistVariableByBddId(int id) {
        return idVarMap.get(id);
    }

    public Signal getSignalByNetlistVariable(NetlistVariable var) {
        return sigVarMap.inverse().get(var);
    }

    public Signal getSignalByBddId(int id) {
        NetlistVariable var = getNetlistVariableByBddId(id);
        return sigVarMap.inverse().get(var);
    }

    public NetlistVariable getNetlistVariableBySignal(Signal sig) {
        return sigVarMap.get(sig);
    }

    public void addConnection(NetlistVariable drivee, NetlistTerm driver) {
        if(driver.getDrivee() == null) {
            drivee.setDriver(driver);
            driver.setDrivee(drivee);
        } else {
            replaceVar(driver.getDrivee(), drivee);
        }
    }

    public boolean replaceVar(NetlistVariable replacement, NetlistVariable obsolete) {
//		System.out.println("Replacing " + obsolete.getName() + " with " + replacement.getName());
//		System.out.println("-- Before --");
//		for(NetlistTerm t : unmappedTerms) {
//			System.out.println(t.toString());
//		}
        Set<NetlistTerm> rmTerms = new HashSet<>();
        for(NetlistTerm term : new HashSet<>(terms.values())) {
            switch(term.replace(replacement, obsolete)) {
                case 0: // okay
                    continue;
                case 1: // already in index
                    NetlistTerm properTerm = terms.get(term.getBdd());
                    replaceVar(properTerm.getDrivee(), term.getDrivee());
                    rmTerms.add(term);
                    break;
                case -1: // fail
                    return false;
                default:
                    logger.warn("Unknwon ret Value from replace");
                    break;
            }
        }
        for(NetlistTerm t : rmTerms) {
            terms.remove(t.getBdd());
        }

        return true;
//		System.out.println("-- After --");
//		for(NetlistTerm t : unmappedTerms) {
//			System.out.println(t.toString());
//		}
//		System.out.println();
    }

    /**
     * 
     * @param newTerm
     * @param oldTerm
     * @param oldVar
     * @return 0: okay, 1: newbdd already in index, -1 not okay
     */
    public int insertInFront(NetlistTerm newTerm, NetlistTerm oldTerm, NetlistVariable oldVar) {
        if(newTerm.getDrivee() == null) {
            addConnection(getNewTmpVar(), newTerm);
        }
        return oldTerm.replace(newTerm.getDrivee(), oldVar);
    }

    /**
     * 
     * @param t
     * @param newbdd
     * @return 0: okay, 1: newbdd already in index, -1 not okay
     */
    public int alterTermBDD(NetlistTerm t, BDD newbdd) {
        return t.setBdd(newbdd);
    }

    public void changeConnection(NetlistVariable drivee, NetlistTerm driver) {
        drivee.getDriver().changeDrivee(null);
        drivee.setDriver(driver);
        if(driver.getDrivee() != null) {
            driver.getDrivee().setDriver(null);
        }
        driver.changeDrivee(drivee);
    }

    public Set<Signal> getDrivenSignalsTransitive(NetlistTerm term) {
        Set<Signal> retVal = new HashSet<>();
        Queue<NetlistVariable> check = new LinkedList<>();
        check.add(term.getDrivee());
        while(!check.isEmpty()) {
            NetlistVariable var = check.poll();
            Signal sig = getSignalByNetlistVariable(var);
            if(sig != null) {
                retVal.add(sig);
                continue;
            }
            for(NetlistTerm t2 : var.getReader()) {
                check.add(t2.getDrivee());
            }
        }
        return retVal;
    }

    public Set<NetlistTerm> getDrivingNetworkTransitive(NetlistVariable var) {
        Set<NetlistTerm> retVal = new HashSet<>();
        if(var == null) {
            return retVal;
        }
        Queue<NetlistVariable> check = new LinkedList<>();
        check.add(var);
        while(!check.isEmpty()) {
            NetlistVariable checkvar = check.poll();
            if(checkvar != var) {
//                if(quasiSignals.containsKey(checkvar)) {
//                    continue;
//                }
                Signal sig = getSignalByNetlistVariable(checkvar);
                if(sig != null) {
                    continue;
                }
            }
            NetlistTerm t2 = checkvar.getDriver();
            if(retVal.contains(t2)) {
                continue;
            }
            retVal.add(t2);
            check.addAll(BDDHelper.getVars(t2.getBdd(), this));
        }
        return retVal;
    }

    /**
     * 
     * @param oldbdd
     * @return 0: okay, 1: newbdd already in index; -1 error
     */
    int updateTermBDDIndex(BDD oldbdd) {
        NetlistTerm t = terms.get(oldbdd);
        if(t == null) {
            logger.error("Term not found in index");
            return -1;
        }
        terms.remove(oldbdd);
        if(t.getBdd().equals(oldbdd)) {
            logger.warn("Old BDD == new BDD");
        }
        if(terms.containsKey(t.getBdd())) {
            return 1;
        }
        terms.put(t.getBdd(), t);
        return 0;
    }

    public Set<NetlistTerm> getUnmappedTerms() {
        return Collections.unmodifiableSet(unmappedTerms);
    }

    public void addMapping(Mapping mapping) {
        for(NetlistTerm t : mapping.getTerms()) {
            mappedTerms.put(t, mapping);
            unmappedTerms.remove(t);
        }
        mapping.setMappingInTerms();
    }

    public void replaceMapping(Mapping mapping) {
        for(NetlistTerm t : mapping.getTerms()) {
            mappedTerms.put(t, mapping);
        }
        mapping.setMappingInTerms();
    }

    public NetlistVariable getNewTmpVar() {
        return getNetlistVariableByName("tmp" + (tmpid++));
    }

    public Set<Mapping> getMappings() {
        return new HashSet<>(mappedTerms.values());
    }

    public int getNumOfTmpSignals() {
        return tmpid;
    }

    public BDDFactory getFac() {
        return fac;
    }

    public Map<NetlistVariable, Boolean> getQuasiSignals() {
        return quasiSignals;
    }

    public Collection<NetlistTerm> getTerms() {
        return Collections.unmodifiableCollection(terms.values());
    }

    public Collection<NetlistVariable> getVars() {
        return Collections.unmodifiableCollection(nameVarMap.values());
    }

    public void removeTerm(NetlistTerm term) {
        this.terms.remove(term.getBdd());
        this.unmappedTerms.remove(term);
        this.mappedTerms.remove(term);
        term.getDrivee().setDriver(null);
        Set<NetlistVariable> vars = BDDHelper.getVars(term.getBdd(), this);
        for(NetlistVariable var : vars) {
            var.removeReader(term);
        }
    }

    public void changeNetlistVarName(NetlistVariable var, String name) {
        this.nameVarMap.remove(var.getName());
        var.changeName(name);
        this.nameVarMap.put(var.getName(), var);
    }
}
