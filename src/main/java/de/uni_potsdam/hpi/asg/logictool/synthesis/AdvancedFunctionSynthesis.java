package de.uni_potsdam.hpi.asg.logictool.synthesis;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.InternalArch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;
import net.sf.javabdd.BDD;

public class AdvancedFunctionSynthesis {
    private static final Logger               logger = LogManager.getLogger();

    private Arch                              arch;

    private Map<Signal, Set<NetlistVariable>> setAndNetwork;
    private Map<Signal, Set<NetlistVariable>> resetAndNetwork;
    private Map<Signal, Set<NetlistVariable>> highAndNetwork;
    private Map<Signal, Set<NetlistVariable>> lowAndNetwork;

    private Map<Signal, Boolean>              highCubesImplementable;
    private Map<Signal, Boolean>              lowCubesImplementable;
    private Map<Signal, Boolean>              celemCubesImplementable;

    private Map<Signal, NetlistVariable>      highImplVar;
    private Map<Signal, NetlistVariable>      lowImplVar;
    private Map<Signal, NetlistVariable>      celemImplVar;

    protected EspressoTable                   table;
    protected String                          resetname;
    protected Netlist                         netlist;
    protected StateGraph                      stategraph;

    public AdvancedFunctionSynthesis(String resetname, EspressoTable table, StateGraph stategraph, Netlist netlist, Arch arch, Map<Signal, Boolean> highCubesImplementable, Map<Signal, Boolean> lowCubesImplementable) {
        this.table = table;
        this.resetname = resetname;
        this.stategraph = stategraph;
        this.netlist = netlist;
        setAndNetwork = new HashMap<>();
        resetAndNetwork = new HashMap<>();
        highAndNetwork = new HashMap<>();
        lowAndNetwork = new HashMap<>();
        this.arch = arch;
        this.highCubesImplementable = highCubesImplementable;
        this.lowCubesImplementable = lowCubesImplementable;
        this.celemCubesImplementable = new HashMap<>();
        this.highImplVar = new HashMap<>();
        this.lowImplVar = new HashMap<>();
        this.celemImplVar = new HashMap<>();
        initMaps();
    }

    public boolean fillNetlist() {
        if(!initNetlist(table)) {
            logger.error("Netlist could not be initialised");
            return false;
        }
        if(!postProcessing()) {
            logger.error("Netlist could not be postprocessed");
            return false;
        }
        return true;
    }

    // create ands
    public boolean initNetlist(EspressoTable table) {
        for(String colSigName : table.getTable().columnKeySet()) {
            String signame = colSigName.split(AdvancedSynthesis.separator)[0];
            Signal sig = findSignalByName(signame);
            netlist.getNetlistVariableBySignal(sig);
            if(sig == null) {
                logger.error("Could not find signal " + signame);
                return false;
            }

            if(colSigName.startsWith(sig.getName() + AdvancedSynthesis.setEnding)) {
                NetlistTerm term = createNetlistTerm(table, colSigName);
                setAndNetwork.get(sig).add(term.getDrivee());
            } else if(colSigName.startsWith(sig.getName() + AdvancedSynthesis.resetEnding)) {
                NetlistTerm term = createNetlistTerm(table, colSigName);
                resetAndNetwork.get(sig).add(term.getDrivee());
            } else if(colSigName.startsWith(sig.getName() + AdvancedSynthesis.highEnding)) {
                if(highCubesImplementable.get(sig)) {
                    NetlistTerm term = createNetlistTerm(table, colSigName);
                    highAndNetwork.get(sig).add(term.getDrivee());
                }
            } else if(colSigName.startsWith(sig.getName() + AdvancedSynthesis.lowEnding)) {
                if(lowCubesImplementable.get(sig)) {
                    NetlistTerm term = createNetlistTerm(table, colSigName);
                    lowAndNetwork.get(sig).add(term.getDrivee());
                }
            } else {
                logger.error("Column " + colSigName + " is neither part of a set or a reset network");
                return false;
            }
        }
        return true;
    }

    private NetlistTerm createNetlistTerm(EspressoTable table, String colSigName) {
        BDD bdd = netlist.getFac().zero();
        for(Entry<EspressoTerm, EspressoValue> col : table.getTable().column(colSigName).entrySet()) {
            if(col.getValue() == EspressoValue.one) {
                EspressoTerm term = col.getKey();
                bdd = bdd.orWith(term.getAsBdd(netlist));
            }
        }
        NetlistVariable sigVar = netlist.getNetlistVariableByName(colSigName);
        NetlistTerm term = netlist.getNetlistTermByBdd(bdd);
        netlist.addConnection(sigVar, term);
        return term;
    }

    private void initMaps() {
        for(Signal sig : stategraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                setAndNetwork.put(sig, new HashSet<NetlistVariable>());
                resetAndNetwork.put(sig, new HashSet<NetlistVariable>());
                if(highCubesImplementable.get(sig)) {
                    highAndNetwork.put(sig, new HashSet<NetlistVariable>());
                }
                if(lowCubesImplementable.get(sig)) {
                    lowAndNetwork.put(sig, new HashSet<NetlistVariable>());
                }
            }
        }
    }

    private Signal findSignalByName(String string) {
        for(Signal sig : stategraph.getAllSignals()) {
            if(sig.getName().equals(string)) {
                return sig;
            }
        }
        return null;
    }

    // create ors and celem
    protected boolean postProcessing() {
        for(Signal sig : stategraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                String name = sig.getName();
                boolean implementationSuccess = false;

                if(createCelemImplementation(sig, name)) {
                    implementationSuccess = true;
                }
                celemCubesImplementable.put(sig, implementationSuccess);

                if(highCubesImplementable.get(sig)) {
                    if(createHighImplementation(sig, name)) {
                        implementationSuccess = true;
                    }
                }

                if(lowCubesImplementable.get(sig)) {
                    if(createLowImplementation(sig, name)) {
                        implementationSuccess = true;
                    }
                }

                if(!implementationSuccess) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean createLowImplementation(Signal sig, String name) {
        Set<NetlistVariable> ands_low = lowAndNetwork.get(sig);
        NetlistVariable lowNetworkName = null;
        BDD orbdd_low = netlist.getFac().zero();
        if(ands_low.isEmpty()) {
            logger.warn("Low-Network is empty: " + name + ", " + ands_low.toString());
        } else {
            for(NetlistVariable var : ands_low) {
                orbdd_low = orbdd_low.or(var.toBDD());
            }
        }
        NetlistTerm lowNetwork = netlist.getNetlistTermByBdd(orbdd_low);
        if(lowNetwork.getDrivee() != null) {
            lowNetworkName = lowNetwork.getDrivee();
        } else {
            lowNetworkName = netlist.getNetlistVariableByName(name + AdvancedSynthesis.lowEnding);
            netlist.addConnection(lowNetworkName, lowNetwork);
        }
        NetlistTerm lowNotNetwork = netlist.getNetlistTermByBdd(lowNetworkName.toNotBDD());
        NetlistVariable lowNotNetworkName = null;
        if(lowNotNetwork.getDrivee() != null) {
            lowNotNetworkName = lowNotNetwork.getDrivee();
        } else {
            lowNotNetworkName = netlist.getNetlistVariableByName(name + AdvancedSynthesis.lowEnding + "_not");
            netlist.addConnection(lowNotNetworkName, lowNotNetwork);
        }
        lowImplVar.put(sig, lowNotNetworkName);
        return true;
    }

    private boolean createHighImplementation(Signal sig, String name) {
        Set<NetlistVariable> ands_high = highAndNetwork.get(sig);
        BDD orbdd_high = netlist.getFac().zero();
        if(ands_high.isEmpty()) {
            logger.warn("High-Network is empty: " + name + ", " + ands_high.toString());
        } else {
            for(NetlistVariable var : ands_high) {
                orbdd_high = orbdd_high.or(var.toBDD());
            }
        }
        NetlistTerm highNetwork = netlist.getNetlistTermByBdd(orbdd_high);
        NetlistVariable highNetworkName = null;
        if(highNetwork.getDrivee() != null) {
            highNetworkName = highNetwork.getDrivee();
        } else {
            highNetworkName = netlist.getNetlistVariableByName(name + AdvancedSynthesis.highEnding);
            netlist.addConnection(highNetworkName, highNetwork);
        }
        highImplVar.put(sig, highNetworkName);
        return true;
    }

    private boolean createCelemImplementation(Signal sig, String name) {
        Set<NetlistVariable> ands_set = setAndNetwork.get(sig);
        Set<NetlistVariable> ands_reset = resetAndNetwork.get(sig);
        BDD orsetbdd = netlist.getFac().zero();
        BDD orresetbdd = netlist.getFac().zero();
        if(ands_set.isEmpty()) {
            logger.warn("Set-Network is empty: " + name + ", set: " + ands_set.toString() + ", reset: " + ands_reset.toString());
        } else {
            for(NetlistVariable var : ands_set) {
                orsetbdd = orsetbdd.or(var.toBDD());
            }
        }
        if(ands_reset.isEmpty()) {
            logger.warn("Reset-Network is empty: " + name + ", set: " + ands_set.toString() + ", reset: " + ands_reset.toString());
        } else {
            for(NetlistVariable var : ands_reset) {
                orresetbdd = orresetbdd.or(var.toBDD());
            }
        }
        if(ands_set.isEmpty() && ands_reset.isEmpty()) {
            logger.warn("Set and Reset network are empty: " + name);
            return false;
        }

        InternalArch iarch = null;
        switch(arch) {
            case generalisedC:
                switch(stategraph.getInitState().getStateValues().get(sig)) {
                    case falling:
                    case high:
                        iarch = InternalArch.generalisedCset;
                        break;
                    case rising:
                    case low:
                        iarch = InternalArch.generalisedCreset;
                        break;
                }
                break;
            case standardC:
                iarch = InternalArch.standardC;
                break;
        }

        // Networks (set, reset)
        NetlistTerm setNetwork = netlist.getNetlistTermByBdd(orsetbdd);
        NetlistVariable setNetworkName = null;
        if(setNetwork.getDrivee() != null) {
            setNetworkName = setNetwork.getDrivee();
        } else {
            setNetworkName = netlist.getNetlistVariableByName(name + AdvancedSynthesis.setEnding);
            netlist.addConnection(setNetworkName, setNetwork);
        }

        NetlistTerm resetNetwork = netlist.getNetlistTermByBdd(orresetbdd);
        NetlistVariable resetNetworkName = null;
        if(resetNetwork.getDrivee() != null) {
            resetNetworkName = resetNetwork.getDrivee();
        } else {
            resetNetworkName = netlist.getNetlistVariableByName(name + AdvancedSynthesis.resetEnding);
            netlist.addConnection(resetNetworkName, resetNetwork);
        }

        NetlistTerm resetNotNetwork = netlist.getNetlistTermByBdd(resetNetworkName.toNotBDD());
        NetlistVariable resetNotNetworkName = null;
        if(resetNotNetwork.getDrivee() != null) {
            resetNotNetworkName = resetNotNetwork.getDrivee();
        } else {
            resetNotNetworkName = netlist.getNetlistVariableByName(name + AdvancedSynthesis.resetEnding + "_not");
            netlist.addConnection(resetNotNetworkName, resetNotNetwork);
        }

        NetlistVariable celemVar = netlist.getNetlistVariableByName(name + AdvancedSynthesis.celemEnding);
        // Celem
        NetlistTerm celem = netlist.getNetlistCelem(setNetworkName, resetNotNetworkName, celemVar, iarch);
        netlist.addConnection(celemVar, celem);

        celemImplVar.put(sig, celemVar);
        return true;
    }

    public Map<Signal, Set<NetlistVariable>> getResetnetworkMap() {
        return resetAndNetwork;
    }

    public Map<Signal, Set<NetlistVariable>> getSetnetworkMap() {
        return setAndNetwork;
    }

    public Map<Signal, NetlistVariable> getHighImplVar() {
        return highImplVar;
    }

    public Map<Signal, NetlistVariable> getLowImplVar() {
        return lowImplVar;
    }

    public Map<Signal, NetlistVariable> getCelemImplVar() {
        return celemImplVar;
    }
}
