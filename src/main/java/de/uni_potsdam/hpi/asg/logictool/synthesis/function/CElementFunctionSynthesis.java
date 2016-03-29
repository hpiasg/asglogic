package de.uni_potsdam.hpi.asg.logictool.synthesis.function;

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

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.InternalArch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.synthesis.CElementSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.EspressoValue;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;
import net.sf.javabdd.BDD;

public class CElementFunctionSynthesis extends FunctionSynthesis {
    private static final Logger logger = LogManager.getLogger();

    public enum ComplementaryDecision {
        NONE, SETONLY, RESETONLY, BOTH, SETANDC, RESETANDC
    }

    private Map<Signal, Set<NetlistVariable>> setnetworkMap;
    private Map<Signal, Set<NetlistVariable>> resetnetworkMap;
    private Arch                              arch;

    public CElementFunctionSynthesis(String resetname, EspressoTable table, StateGraph stategraph, Netlist netlist, Arch arch) {
        super(table, resetname, stategraph, netlist);
        setnetworkMap = new HashMap<>();
        resetnetworkMap = new HashMap<>();
        this.arch = arch;
        initMaps();
    }

    @Override
    public boolean initNetlist(EspressoTable table) {
        for(String colSigName : table.getTable().columnKeySet()) {
            String signame = colSigName.split(CElementSynthesis.separator)[0];
            Signal sig = findSignalByName(signame);
            if(sig == null) {
                logger.error("Could not find signal " + signame);
                return false;
            }
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

            if(colSigName.startsWith(sig.getName() + CElementSynthesis.setEnding)) {
                setnetworkMap.get(sig).add(term.getDrivee());
            } else if(colSigName.startsWith(sig.getName() + CElementSynthesis.resetEnding)) {
                resetnetworkMap.get(sig).add(term.getDrivee());
            } else {
                logger.error("Column " + colSigName + " is neither part of a set or a reset network");
                return false;
            }
        }
        return true;
    }

    private void initMaps() {
        for(Signal sig : stategraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                setnetworkMap.put(sig, new HashSet<NetlistVariable>());
                resetnetworkMap.put(sig, new HashSet<NetlistVariable>());
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

    @Override
    protected boolean postProcessing() {
        for(Signal sig : stategraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                String name = sig.getName();
                Set<NetlistVariable> ands_set = setnetworkMap.get(sig);
                Set<NetlistVariable> ands_reset = resetnetworkMap.get(sig);
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
                    logger.error("Both are empty");
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

                // Networks
                NetlistTerm setNetwork = netlist.getNetlistTermByBdd(orsetbdd);
                NetlistVariable setNetworkName = null;
                if(setNetwork.getDrivee() != null) {
                    setNetworkName = setNetwork.getDrivee();
                } else {
                    setNetworkName = netlist.getNetlistVariableByName(name + CElementSynthesis.setEnding);
                    netlist.addConnection(setNetworkName, setNetwork);
                }

                NetlistTerm resetNetwork = netlist.getNetlistTermByBdd(orresetbdd);
                NetlistVariable resetNetworkName = null;
                if(resetNetwork.getDrivee() != null) {
                    resetNetworkName = resetNetwork.getDrivee();
                } else {
                    resetNetworkName = netlist.getNetlistVariableByName(name + CElementSynthesis.resetEnding);
                    netlist.addConnection(resetNetworkName, resetNetwork);
                }

                NetlistTerm resetNotNetwork = netlist.getNetlistTermByBdd(resetNetworkName.toNotBDD());
                NetlistVariable resetNotNetworkName = null;
                if(resetNotNetwork.getDrivee() != null) {
                    resetNotNetworkName = resetNotNetwork.getDrivee();
                } else {
                    resetNotNetworkName = netlist.getNetlistVariableByName(name + CElementSynthesis.resetEnding + "_not");
                    netlist.addConnection(resetNotNetworkName, resetNotNetwork);
                }

                NetlistVariable sigvar = netlist.getNetlistVariableBySignal(sig);
                // Celem
                NetlistTerm celem = netlist.getNetlistCelem(setNetworkName, resetNotNetworkName, sigvar, iarch);
                netlist.addConnection(sigvar, celem);
            }
        }

        return true;
    }

    public Map<Signal, Set<NetlistVariable>> getResetnetworkMap() {
        return resetnetworkMap;
    }

    public Map<Signal, Set<NetlistVariable>> getSetnetworkMap() {
        return setnetworkMap;
    }
}
