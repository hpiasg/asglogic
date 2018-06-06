package de.uni_potsdam.hpi.asg.logictool.synthesis;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.mapping.TechnologyMapper;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.GateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.Mapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.NoMapping;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.InternalArch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.helper.AdvancedMonotonicCoverChecker;
import de.uni_potsdam.hpi.asg.logictool.synthesis.helper.RegionCalculator;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;

public class AdvancedSynthesis {
    private static final Logger          logger      = LogManager.getLogger();

    public static final String           separator   = "__";
    public static final String           setEnding   = separator + "set";
    public static final String           resetEnding = separator + "reset";
    public static final String           celemEnding = separator + "celem";
    public static final String           highEnding  = separator + "high";
    public static final String           lowEnding   = separator + "low";

    protected StateGraph                 stateGraph;
    protected Netlist                    netlist;
    protected String                     resetname;
    protected EspressoTable              table;
    protected RegionCalculator           regCalc;

    private AdvancedFunctionSynthesis    syn2;
    private EspressoOptimiser            optimiser;
    private SortedSet<Signal>            outsignals;
    private Arch                         arch;

    private Map<Signal, Boolean>         highCubesImplementable;
    private Map<Signal, Boolean>         lowCubesImplementable;
    private Map<Signal, Boolean>         celemCubesImplementable;
    private Map<Signal, NetlistVariable> highImplVar;
    private Map<Signal, NetlistVariable> lowImplVar;
    private Map<Signal, NetlistVariable> celemImplVar;

    public AdvancedSynthesis(StateGraph stateGraph, Netlist netlist, String resetname, Arch arch) {
        this.stateGraph = stateGraph;
        this.netlist = netlist;
        this.resetname = resetname;
        this.optimiser = new EspressoOptimiser();
        this.outsignals = new TreeSet<>();
        this.arch = arch;
        this.celemCubesImplementable = new HashMap<>();
    }

    public boolean doRegionCalculation() {
        regCalc = RegionCalculator.create(stateGraph);
        if(regCalc == null) {
            return false;
        }
        return true;
    }

    public boolean doTableSynthesis() {
        for(Signal sig : stateGraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                outsignals.add(sig);
                celemCubesImplementable.put(sig, true);
            }
        }
        AdvancedTableSynthesis syn = new AdvancedTableSynthesis(stateGraph, outsignals, regCalc.getRegions(), resetname);
        table = syn.synthesise();
        return true;
    }

    public boolean doTableCheck(EspressoTable table) {
        this.table = table;
        SortedSet<Signal> signals = new TreeSet<>(outsignals);
        AdvancedTableSynthesis syn = null;
        EspressoTable tmptable = table;
        while(true) {
            AdvancedMonotonicCoverChecker mchecker = new AdvancedMonotonicCoverChecker(stateGraph, signals, regCalc.getRegions(), tmptable, resetname);
            if(mchecker.check()) {
                this.highCubesImplementable = mchecker.getHighCubesMonotonicCover();
                this.lowCubesImplementable = mchecker.getLowCubesMonotonicCover();
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
            syn = new AdvancedTableSynthesis(stateGraph, signals, regCalc.getRegions(), resetname);
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

    public boolean doFunctionSynthesis() {
        syn2 = new AdvancedFunctionSynthesis(resetname, table, stateGraph, netlist, arch, highCubesImplementable, lowCubesImplementable);
        if(!syn2.fillNetlist()) {
            logger.error("Could not fill netlist");
            return false;
        }
        this.celemImplVar = syn2.getCelemImplVar();
        this.highImplVar = syn2.getHighImplVar();
        this.lowImplVar = syn2.getLowImplVar();

        return true;
    }

    public boolean doComplementaryCheck(Reset reset) {
//        new NetlistGraph(netlist, null, true);
        for(Signal sig : stateGraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                if(!improveCelemResetDecision(reset, sig)) {
                    return false;
                }
//                int celemSize = getDrivingNetworkLiterals(celemImplVar.get(sig));
//                int highSize = highCubesImplementable.get(sig) ? getDrivingNetworkLiterals(highImplVar.get(sig)) : Integer.MAX_VALUE;
//                int lowSize = lowCubesImplementable.get(sig) ? getDrivingNetworkLiterals(lowImplVar.get(sig)) : Integer.MAX_VALUE;
            }
        }
        return true;
    }

    private boolean improveCelemResetDecision(Reset reset, Signal sig) {
        Map<Signal, ResetDecision> celemResetDecision = reset.getDecision();
        NetlistTerm celemterm = celemImplVar.get(sig).getDriver();
        if(!(celemterm instanceof NetlistCelem)) {
            logger.error("Signal " + sig.getName() + " is not driven by an Celem");
            return false;
        }
        NetlistCelem celem = (NetlistCelem)celemterm;
        InternalArch arch = celem.getArch();

        switch(celemResetDecision.get(sig)) {
            case NORST:
                break;
            case RESETRST:
                switch(arch) {
                    case standardC:
                    case generalisedCreset:
                        break;
                    case generalisedCset:
                        celemResetDecision.put(sig, ResetDecision.NORST);
                        break;
                }
                break;
            case SETRST:
                switch(arch) {
                    case standardC:
                    case generalisedCset:
                        break;
                    case generalisedCreset:
                        celemResetDecision.put(sig, ResetDecision.NORST);
                        break;
                }
                break;
            case BOTHRST:
                switch(arch) {
                    case generalisedCreset:
                        celemResetDecision.put(sig, ResetDecision.RESETRST);
                        break;
                    case generalisedCset:
                        celemResetDecision.put(sig, ResetDecision.SETRST);
                        break;
                    case standardC:
                        break;
                }
                break;
        }
        return true;
    }

    public boolean doPostMappingSynthesis(TechnologyMapper mapper) {
//        new NetlistGraph(netlist, null, true);
        Set<NetlistTerm> termsToKepp = new HashSet<>();
        for(Signal sig : stateGraph.getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                float celemSize = -5f;
                if(celemCubesImplementable.get(sig)) {
                    celemSize = getSizeOfDriverNetwork(celemImplVar.get(sig));
                }
                if(celemSize < 0) {
                    celemSize = Float.MAX_VALUE;
                }

                float highSize = -5f;
                if(highCubesImplementable.get(sig)) {
                    highSize = getSizeOfDriverNetwork(highImplVar.get(sig));
                }
                if(highSize < 0) {
                    highSize = Float.MAX_VALUE;
                }

                float lowSize = -5f;
                if(lowCubesImplementable.get(sig)) {
                    lowSize = getSizeOfDriverNetwork(lowImplVar.get(sig));
                }
                if(lowSize < 0) {
                    lowSize = Float.MAX_VALUE;
                }

                SortedSet<Float> set = new TreeSet<>();
                set.add(celemSize);
                set.add(highSize);
                set.add(lowSize);

                float smallest = set.first();
                if(smallest == Float.MAX_VALUE) {
                    logger.error("No implementation left for signal " + sig.getName());
                    return false;
                }

                if(smallest == highSize) {
                    // high impl
                    termsToKepp.addAll(netlist.getDrivingNetworkTransitive(highImplVar.get(sig)));
//                    netlist.addConnection(netlist.getNetlistVariableBySignal(sig), highImplVar.get(sig).getDriver());
//                    netlist.addMapping(new WireMapping(highImplVar.get(sig), netlist.getNetlistVariableBySignal(sig), netlist));
                    netlist.changeNetlistVarName(highImplVar.get(sig), sig.getName());
                } else if(smallest == lowSize) {
                    // low impl
                    termsToKepp.addAll(netlist.getDrivingNetworkTransitive(lowImplVar.get(sig)));
                    netlist.changeNetlistVarName(lowImplVar.get(sig), sig.getName());
                } else if(smallest == celemSize) {
                    //Celem impl
                    termsToKepp.addAll(netlist.getDrivingNetworkTransitive(celemImplVar.get(sig)));
                    netlist.changeNetlistVarName(celemImplVar.get(sig), sig.getName());
                } else {
                    logger.error("No implementation for signal " + sig.getName());
                }
            }
        }

        Set<NetlistTerm> termCopy = new HashSet<>(netlist.getTerms());
        for(NetlistTerm term : termCopy) {
            if(!termsToKepp.contains(term)) {
                netlist.removeTerm(term);
            }
        }

//                System.out.println(sig.getName());
//                System.out.println("\tCelem: " + celemSize);
//                System.out.println("\tHigh: " + highSize);
//                System.out.println("\tLow: " + lowSize);

//				System.out.println(sig.getName() + ": " + networksComplementaryMap.get(sig));
//                ComplementaryDecision decision = networksComplementaryMap.get(sig);
//                if(decision != ComplementaryDecision.NONE) {
////					new NetlistGraph(netlist, null, false);
//                    // optimised
//                    NetlistVariable var = netlist.getNetlistVariableBySignal(sig);
//                    NetlistTerm celemterm = var.getDriver();
//                    if(!(celemterm instanceof NetlistCelem)) {
//                        logger.error("Signal " + sig.getName() + " is not driven by an Celem");
//                        return false;
//                    }
//                    NetlistCelem celem = (NetlistCelem)celemterm;
//                    boolean forceset = false, forcereset = false;
//                    // get data for choosing network
//                    Set<NetlistTerm> setnetwork = new HashSet<>();
//                    Set<NetlistTerm> resetnetwork = new HashSet<>();
//                    float setsize = getSizeOfDriverNetwork(celem.getSetInput(), setnetwork);
//                    float resetsize = getSizeOfDriverNetwork(celem.getResetInput(), resetnetwork);
//                    if(setsize == -1 || resetsize == -1) {
//                        return false;
//                    }
//                    if(setsize == -2) {
//                        forcereset = true;
//                    }
//                    if(resetsize == -2) {
//                        forceset = true;
//                    }
//
//                    // choose
//                    Boolean chooseset = null;
//                    Boolean keepcelem = null;
//                    if(!forceset && !forcereset) {
//                        //no unmapped networks
//                        switch(decision) {
//                            case BOTH:
//                                chooseset = setsize <= resetsize;
//                                keepcelem = false;
//                                break;
//                            case RESETONLY:
//                                chooseset = false;
//                                keepcelem = false;
//                                break;
//                            case RESETANDC:
//                                chooseset = false;
//                                keepcelem = true;
//                                break;
//                            case SETONLY:
//                                chooseset = true;
//                                keepcelem = false;
//                                break;
//                            case SETANDC:
//                                chooseset = true;
//                                keepcelem = true;
//                                break;
//                            case NONE:
//                                //should not happen
//                                return false;
//                        }
//                    } else if(forceset && forcereset) {
//                        //both unmapped?!
//                        logger.error("Both networks of signal " + sig.getName() + " are unmapped");
//                        return false;
//                    } else if(forceset) {
//                        //set network partly unmapped
//                        switch(decision) {
//                            case SETONLY:
//                            case BOTH:
//                                chooseset = true;
//                                keepcelem = false;
//                                break;
//                            case SETANDC:
//                                chooseset = true;
//                                keepcelem = true;
//                                break;
//                            case RESETANDC:
//                            case RESETONLY:
//                                logger.error("Complementary Mapping: Only Resetnetwork can be chosen, but it is unmapped");
//                                return false;
//                            case NONE:
//                                //should not happen
//                                return false;
//                        }
//                    } else { //forcereset
//                        //set network partly unmapped
//                        switch(decision) {
//                            case RESETONLY:
//                            case BOTH:
//                                chooseset = false;
//                                keepcelem = false;
//                                break;
//                            case RESETANDC:
//                                chooseset = false;
//                                keepcelem = true;
//                                break;
//                            case SETANDC:
//                            case SETONLY:
//                                logger.error("Complementary Mapping: Only Setnetwork can be chosen, but it is unmapped");
//                                return false;
//                            case NONE:
//                                //should not happen
//                                return false;
//                        }
//                    }
//
//                    if(chooseset == null || keepcelem == null) {
//                        logger.error("Decision not found");
//                        return false;
//                    }
//
//                    if(keepcelem) {
//                        //actually SETANDC and RESETANDC is the same as NONE
//                        continue;
//                    }
//
//                    // apply
//                    // remove celem
//                    celem.remove();
//                    if(chooseset) {
//                        // take set network
//                        Signal inpSig = netlist.getSignalByNetlistVariable(celem.getSetInput());
//                        if(inpSig != null) {
//                            netlist.replaceMapping(new WireMapping(celem.getSetInput(), var, netlist));
//                        } else {
//                            netlist.changeConnection(var, celem.getSetInput().getDriver());
//                        }
//                        if(!celem.getResetInput().removeReaderTransitive(celem)) {
//                            return false;
//                        }
//                    } else {
//                        // take reset network
//                        Signal inpSig = netlist.getSignalByNetlistVariable(celem.getResetInput());
//                        if(inpSig != null) {
//                            netlist.replaceMapping(new WireMapping(celem.getResetInput(), var, netlist));
//                        } else {
//                            netlist.changeConnection(var, celem.getResetInput().getDriver());
//                        }
//                        if(!celem.getSetInput().removeReaderTransitive(celem)) {
//                            return false;
//                        }

//            }
//
//        }
//        if(!netlist.checkNotResetNeeded())
//
//        {
//            return false;
//        }

//		for(Mapping map : netlist.getMappings()) {
//			System.out.println(map.toString());
//		}

//		new NetlistGraph(netlist, null, true);
        return true;
    }

//    /**
//     * 
//     * @param var
//     * @param vars
//     * @return size of driver network; -1 if error happened; -2 if network was
//     *         partly not mapped
//     */
//    private float getSizeOfDriverNetwork(NetlistVariable var, Set<NetlistTerm> terms) {
//        float size = 0;
//        Set<NetlistTerm> network = netlist.getDrivingNetworkTransitive(var);
//        terms.addAll(network);
//        for(NetlistTerm t : network) {
//            Mapping mapping = t.getMapping();
//            if(mapping == null) {
//                logger.error("No mapping for term " + t.toString());
//                return -1;
//            }
//            if(mapping instanceof GateMapping) {
//                size += ((GateMapping)mapping).getGate().getSize();
//            } else if(mapping instanceof NoMapping) {
//                return -2;
//            }
//        }
//        return size;
//    }

    private float getSizeOfDriverNetwork(NetlistVariable var) {
        Set<NetlistTerm> terms = new HashSet<>();
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

//    private int getDrivingNetworkLiterals(NetlistVariable var) {
//        Set<NetlistTerm> network = netlist.getDrivingNetworkTransitive(var);
//        int retVal = 0;
//        for(NetlistTerm t : network) {
//            retVal += BDDHelper.getVars(t.getBdd(), netlist).size();
//        }
//        return retVal;
//    }

    public Netlist getNetlist() {
        return netlist;
    }

    public EspressoTable getTable() {
        return table;
    }

    public Map<Signal, NetlistVariable> getCelemImplVar() {
        return celemImplVar;
    }

    public Map<Signal, NetlistVariable> getHighImplVar() {
        return highImplVar;
    }

    public Map<Signal, NetlistVariable> getLowImplVar() {
        return lowImplVar;
    }

    public Map<Signal, Boolean> getHighCubesImplementable() {
        return highCubesImplementable;
    }

    public Map<Signal, Boolean> getLowCubesImplementable() {
        return lowCubesImplementable;
    }

    public Map<Signal, Boolean> getCelemCubesImplementable() {
        return celemCubesImplementable;
    }
}
