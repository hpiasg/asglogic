package de.uni_potsdam.hpi.asg.logictool.synthesis;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.TechnologyMapper;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.GateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.Mapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.NoMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.WireMapping;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.InternalArch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.function.CElementFunctionSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.function.CElementFunctionSynthesis.ComplementaryDecision;
import de.uni_potsdam.hpi.asg.logictool.synthesis.helper.MonotonicCoverChecker;
import de.uni_potsdam.hpi.asg.logictool.synthesis.helper.RegionCalculator;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.table.CElementTableSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.table.TableSynthesis;
import net.sf.javabdd.BDD;

public class CElementSynthesis extends Synthesis {
    private static final Logger                logger      = LogManager.getLogger();
    public static final String                 separator   = "__";
    public static final String                 setEnding   = separator + "set";
    public static final String                 resetEnding = separator + "reset";

    private StateGraph                         stateGraph;
    private CElementFunctionSynthesis          syn2;
    private EspressoOptimiser                  optimiser;
    private SortedSet<Signal>                  outsignals;
    private RegionCalculator                   regCalc;
    private Map<Signal, ComplementaryDecision> networksComplementaryMap;
    private Arch                               arch;

    public CElementSynthesis(StateGraph stateGraph, Netlist netlist, String resetname, Arch arch) {
        super(netlist, resetname);
        this.stateGraph = stateGraph;
        this.optimiser = new EspressoOptimiser();
        this.regCalc = new RegionCalculator(stateGraph);
        this.outsignals = new TreeSet<>();
        this.arch = arch;
        this.networksComplementaryMap = new HashMap<>();
    }

    @Override
    public boolean doTableSynthesis() {
        for(Signal sig : stateGraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                outsignals.add(sig);
            }
        }
        TableSynthesis syn = new CElementTableSynthesis(stateGraph, outsignals, regCalc.getRegions(), resetname);
        table = syn.synthesise();
        return true;
    }

    @Override
    public boolean doTableCheck(EspressoTable table) {
        this.table = table;
        SortedSet<Signal> signals = new TreeSet<>(outsignals);
        TableSynthesis syn = null;
        EspressoTable tmptable = table;
        while(true) {
            MonotonicCoverChecker mchecker = new MonotonicCoverChecker(stateGraph, signals, regCalc.getRegions(), tmptable, resetname);
            if(mchecker.check()) {
                break;
            } else {
                if(mchecker.getMonotonicityViolatingStates() != null) {
                    regCalc.apply(mchecker.getMonotonicityViolatingStates());
                    signals = mchecker.getNonMonotonSignals();
                } else {
                    logger.error("Cannot ensure monotonic cover for " + mchecker.getNonMonotonSignals());
                    return false;
                }
            }
            syn = new CElementTableSynthesis(stateGraph, signals, regCalc.getRegions(), resetname);
            tmptable = syn.synthesise();
            tmptable = optimiser.espressoMinimization(tmptable);
            if(tmptable == null) {
                return false;
            }
            if(table == null) {
                table = tmptable;
            } else {
                table.mergeIn(tmptable, signals);
            }

        }
        return true;
    }

    @Override
    public boolean doFunctionSynthesis() {
        syn2 = new CElementFunctionSynthesis(resetname, table, stateGraph, netlist/*, explicitInverterInResetNetwork*/, arch);
        if(!syn2.fillNetlist()) {
            logger.error("Could not fill netlist");
            return false;
        }

        return true;
    }

    @Override
    public boolean doComplementaryCheck(Reset reset) {
        for(Signal sig : stateGraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                Set<NetlistVariable> ands_set = syn2.getSetnetworkMap().get(sig);
                Set<NetlistVariable> ands_reset = syn2.getResetnetworkMap().get(sig);
                BDD setbdd = null;
                if(!ands_set.isEmpty()) {
                    setbdd = netlist.getFac().zero();
                    for(NetlistVariable var : ands_set) {
                        setbdd = setbdd.or(var.getDriver().getBdd());
                    }
                }
                BDD resetbdd = null;
                if(!ands_reset.isEmpty()) {
                    resetbdd = netlist.getFac().zero();
                    for(NetlistVariable var : ands_reset) {
                        resetbdd = resetbdd.or(var.getDriver().getBdd());
                    }
                }

                if(setbdd == null && resetbdd == null) {
                    logger.error("Both networks are empty");
                    return false;
                } else if(setbdd == null) {
                    networksComplementaryMap.put(sig, ComplementaryDecision.RESETANDC);
                } else if(resetbdd == null) {
                    networksComplementaryMap.put(sig, ComplementaryDecision.SETANDC);
                } else {
                    MutableBoolean setresult = new MutableBoolean();
                    MutableBoolean resetresult = new MutableBoolean();
                    boolean complementary = true;
                    boolean onsetfull = true;
                    boolean offsetfull = true;
                    for(State s : stateGraph.getStates()) {
                        if(!BDDHelper.evaluateBDD(setresult, setbdd, s, netlist)) {
                            logger.error("Coult not eval set network for " + sig.getName());
                        }
                        if(!BDDHelper.evaluateBDD(resetresult, resetbdd, s, netlist)) {
                            logger.error("Coult not eval reset network for " + sig.getName());
                        }

                        switch(s.getStateValues().get(sig)) {
                            case rising:
                            case high:
                                if(!setresult.booleanValue()) {
                                    onsetfull = false;
                                }
                                break;
                            case low:
                            case falling:
                                if(!resetresult.booleanValue()) {
                                    offsetfull = false;
                                }
                                break;
                        }
                        if(!setresult.booleanValue() && !resetresult.booleanValue()) {
                            //set=0, reset=0
                            // => no complementary mapping
                            complementary = false;
                        }

                        if(!complementary && !onsetfull && !offsetfull) {
                            break;
                        }
                    }

                    Map<Signal, ResetDecision> resetDecision = reset.getDecision();
                    NetlistVariable var = netlist.getNetlistVariableBySignal(sig);
                    NetlistTerm celemterm = var.getDriver();
                    if(!(celemterm instanceof NetlistCelem)) {
                        logger.error("Signal " + sig.getName() + " is not driven by an Celem");
                        return false;
                    }
                    NetlistCelem celem = (NetlistCelem)celemterm;
                    InternalArch arch = celem.getArch();

                    if(complementary) {
                        networksComplementaryMap.put(sig, ComplementaryDecision.BOTH);
                    } else if(onsetfull) {
                        networksComplementaryMap.put(sig, ComplementaryDecision.SETONLY);
                    } else if(offsetfull) {
                        networksComplementaryMap.put(sig, ComplementaryDecision.RESETONLY);
                    } else {
                        networksComplementaryMap.put(sig, ComplementaryDecision.NONE);
                        switch(resetDecision.get(sig)) {
                            case NORST:
                                break;
                            case RESETRST:
                                switch(arch) {
                                    case standardC:
                                    case generalisedCreset:
                                        break;
                                    case generalisedCset:
                                        resetDecision.put(sig, ResetDecision.NORST);
                                        break;
                                }
                                break;
                            case SETRST:
                                switch(arch) {
                                    case standardC:
                                    case generalisedCset:
                                        break;
                                    case generalisedCreset:
                                        resetDecision.put(sig, ResetDecision.NORST);
                                        break;
                                }
                                break;
                            case BOTHRST:
                                switch(arch) {
                                    case generalisedCreset:
                                        resetDecision.put(sig, ResetDecision.RESETRST);
                                        break;
                                    case generalisedCset:
                                        resetDecision.put(sig, ResetDecision.SETRST);
                                        break;
                                    case standardC:
                                        break;
                                }
                                break;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean doPostMappingSynthesis(TechnologyMapper mapper) {
//		new NetlistGraph(netlist, null, true);
        for(Signal sig : stateGraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
//				System.out.println(sig.getName() + ": " + networksComplementaryMap.get(sig));
                ComplementaryDecision decision = networksComplementaryMap.get(sig);
                if(decision != ComplementaryDecision.NONE) {
//					new NetlistGraph(netlist, null, false);
                    // optimised
                    NetlistVariable var = netlist.getNetlistVariableBySignal(sig);
                    NetlistTerm celemterm = var.getDriver();
                    if(!(celemterm instanceof NetlistCelem)) {
                        logger.error("Signal " + sig.getName() + " is not driven by an Celem");
                        return false;
                    }
                    NetlistCelem celem = (NetlistCelem)celemterm;
                    boolean forceset = false, forcereset = false;
                    // get data for choosing network
                    Set<NetlistTerm> setnetwork = new HashSet<>();
                    Set<NetlistTerm> resetnetwork = new HashSet<>();
                    float setsize = getSizeOfDriverNetwork(celem.getSetInput(), setnetwork);
                    float resetsize = getSizeOfDriverNetwork(celem.getResetInput(), resetnetwork);
                    if(setsize == -1 || resetsize == -1) {
                        return false;
                    }
                    if(setsize == -2) {
                        forcereset = true;
                    }
                    if(resetsize == -2) {
                        forceset = true;
                    }

                    // choose
                    Boolean chooseset = null;
                    Boolean keepcelem = null;
                    if(!forceset && !forcereset) {
                        //no unmapped networks
                        switch(decision) {
                            case BOTH:
                                chooseset = setsize <= resetsize;
                                keepcelem = false;
                                break;
                            case RESETONLY:
                                chooseset = false;
                                keepcelem = false;
                                break;
                            case RESETANDC:
                                chooseset = false;
                                keepcelem = true;
                                break;
                            case SETONLY:
                                chooseset = true;
                                keepcelem = false;
                                break;
                            case SETANDC:
                                chooseset = true;
                                keepcelem = true;
                                break;
                            case NONE:
                                //should not happen
                                return false;
                        }
                    } else if(forceset && forcereset) {
                        //both unmapped?!
                        logger.error("Both networks of signal " + sig.getName() + " are unmapped");
                        return false;
                    } else if(forceset) {
                        //set network partly unmapped
                        switch(decision) {
                            case SETONLY:
                            case BOTH:
                                chooseset = true;
                                keepcelem = false;
                                break;
                            case SETANDC:
                                chooseset = true;
                                keepcelem = true;
                                break;
                            case RESETANDC:
                            case RESETONLY:
                                logger.error("Complementary Mapping: Only Resetnetwork can be chosen, but it is unmapped");
                                return false;
                            case NONE:
                                //should not happen
                                return false;
                        }
                    } else { //forcereset
                        //set network partly unmapped
                        switch(decision) {
                            case RESETONLY:
                            case BOTH:
                                chooseset = false;
                                keepcelem = false;
                                break;
                            case RESETANDC:
                                chooseset = false;
                                keepcelem = true;
                                break;
                            case SETANDC:
                            case SETONLY:
                                logger.error("Complementary Mapping: Only Setnetwork can be chosen, but it is unmapped");
                                return false;
                            case NONE:
                                //should not happen
                                return false;
                        }
                    }

                    if(chooseset == null || keepcelem == null) {
                        logger.error("Decision not found");
                        return false;
                    }

                    if(keepcelem) {
                        //actually SETANDC and RESETANDC is the same as NONE
                        continue;
                    }

                    // apply
                    // remove celem
                    celem.remove();
                    if(chooseset) {
                        // take set network
                        Signal inpSig = netlist.getSignalByNetlistVariable(celem.getSetInput());
                        if(inpSig != null) {
                            netlist.replaceMapping(new WireMapping(celem.getSetInput(), var, netlist));
                        } else {
                            netlist.changeConnection(var, celem.getSetInput().getDriver());
                        }
                        if(!celem.getResetInput().removeReaderTransitive(celem)) {
                            return false;
                        }
                    } else {
                        // take reset network
                        Signal inpSig = netlist.getSignalByNetlistVariable(celem.getResetInput());
                        if(inpSig != null) {
                            netlist.replaceMapping(new WireMapping(celem.getResetInput(), var, netlist));
                        } else {
                            netlist.changeConnection(var, celem.getResetInput().getDriver());
                        }
                        if(!celem.getSetInput().removeReaderTransitive(celem)) {
                            return false;
                        }
                    }
                }
            }
        }
        if(!netlist.checkNotResetNeeded()) {
            return false;
        }

//		for(Mapping map : netlist.getMappings()) {
//			System.out.println(map.toString());
//		}

//		new NetlistGraph(netlist, null, true);
        return true;
    }

    /**
     * 
     * @param var
     * @param vars
     * @return size of driver network; -1 if error happened; -2 if network was
     *         partly not mapped
     */
    private float getSizeOfDriverNetwork(NetlistVariable var, Set<NetlistTerm> terms) {
        float size = 0;
        Set<NetlistTerm> network = netlist.getDrivingNetworkTransitive(var);
        terms.addAll(network);
        for(NetlistTerm t : network) {
            Mapping mapping = t.getMapping();
            if(mapping == null) {
                logger.error("No mapping for term " + t.toString());
                return -1;
            }
            if(mapping instanceof GateMapping) {
                size += ((GateMapping)mapping).getGate().getSize();
            } else if(mapping instanceof NoMapping) {
                return -2;
            }
        }
        return size;
    }

    public Map<Signal, ComplementaryDecision> getNetworksComplementaryMap() {
        return networksComplementaryMap;
    }
}
