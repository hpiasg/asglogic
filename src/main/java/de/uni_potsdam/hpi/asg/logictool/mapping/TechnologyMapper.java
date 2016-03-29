package de.uni_potsdam.hpi.asg.logictool.mapping;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.GateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.IntermediateGateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.MapPairing;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.NoMapping;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.synthesis.CElementSynthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.Synthesis;
import de.uni_potsdam.hpi.asg.logictool.synthesis.function.CElementFunctionSynthesis.ComplementaryDecision;
import de.uni_potsdam.hpi.asg.logictool.techfile.Gate;
import de.uni_potsdam.hpi.asg.logictool.techfile.TechLibrary;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model.TechVariable;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDPairing;

public class TechnologyMapper {
    private static final Logger            logger = LogManager.getLogger();

    private TechLibrary                    techlib;
    private Netlist                        netlist;
    private Synthesis                      syn;
    private boolean                        unsafeanddeco;

    private SequenceBasedAndGateDecomposer seqanddeco;
    private ArbitraryAndGateDecomposer     dcanddeco;
    private OrGateDecomposer               ordeco;
    private Set<NetlistTerm>               unmappableTerms;

    public TechnologyMapper(StateGraph stateGraph, Netlist netlist, TechLibrary techlib, Synthesis syn, boolean unsafeanddeco) {
        this.netlist = netlist;
        this.techlib = techlib;
        this.syn = syn;
        this.seqanddeco = new SequenceBasedAndGateDecomposer(stateGraph, netlist);
        this.ordeco = new OrGateDecomposer(netlist);
        this.dcanddeco = new ArbitraryAndGateDecomposer(netlist);
        this.unmappableTerms = new HashSet<NetlistTerm>();
        this.unsafeanddeco = unsafeanddeco;
    }

    public boolean mapAll() {
        int decomposings = 0;
        do {
            internalMap();
            decomposings = decompose();
        } while(decomposings > 0);

        if(!fixComplementaryNetworksForUnmappedSignals()) {
            logger.error("Complementary mapping failed");
        }

        logger.debug("Signals: " + netlist.getTerms().size() +
        //", New: " + netlist.getNumOfTmpSignals() +
            ", Mapped: " + netlist.getMappings().size() + ", Missing: " + netlist.getUnmappedTerms().size() + " (" + netlist.getUnmappedTerms().toString() + ")");

        if(netlist.getUnmappedTerms().size() > 0) {
            Set<NetlistVariable> vars = new HashSet<>();
            for(NetlistTerm term : netlist.getUnmappedTerms()) {
                vars.add(term.getDrivee());
            }
            logger.error("Technology mapping failed for signals: " + vars.toString());
            return false;
        }

        logger.info("Technology mapping was successful for all signals");
        return true;
    }

    private boolean fixComplementaryNetworksForUnmappedSignals() {
        if(netlist.getUnmappedTerms().isEmpty()) {
            return true;
        }
        CElementSynthesis syn2 = null;
        if(syn instanceof CElementSynthesis) {
            syn2 = (CElementSynthesis)syn;
        }
        if(syn2 == null) {
            logger.warn("Complementary networks mapping is just implemented for CElem synthesis");
            return false;
        }

        for(NetlistTerm term : netlist.getUnmappedTerms()) {
            Set<Signal> signals = netlist.getDrivenSignalsTransitive(term);
            if(signals.isEmpty()) {
                logger.warn("Signal for term " + term + " not found");
                return false;
            } else if(signals.size() > 1) {
                logger.warn("Term " + term + " drives more than one signal. This is not yet supported");
                return false;
            }
            Signal sig = signals.iterator().next();

            if(syn2.getNetworksComplementaryMap().get(sig) == ComplementaryDecision.NONE) {
                logger.error("Complementary mapping of signal " + sig.getName() + " failed");
                continue;
            }

            NetlistTerm celemterm = netlist.getNetlistVariableBySignal(sig).getDriver();
            if(!(celemterm instanceof NetlistCelem)) {
                logger.error("Signal " + sig.getName() + " is not driven by an Celem");
                return false;
            }
            NetlistCelem celem = (NetlistCelem)celemterm;
            Set<NetlistTerm> setNetwork = netlist.getDrivingNetworkTransitive(celem.getSetInput());
            Set<NetlistTerm> resetNetwork = netlist.getDrivingNetworkTransitive(celem.getResetInput());

            if(setNetwork.contains(term) && resetNetwork.contains(term)) {
                logger.error("Term is part of both Set- and Reset-Network");
                return false;
            } else if(setNetwork.contains(term)) {
                // this unmappable term is part of set network
                if(syn2.getNetworksComplementaryMap().get(sig) == ComplementaryDecision.SETONLY) {
                    logger.error("Complementary mapping of signal " + sig.getName() + " failed");
                    continue;
                }
                // check if reset network is fully mapped
                boolean allmapped = true;
                for(NetlistTerm t2 : resetNetwork) {
                    if(netlist.getUnmappedTerms().contains(t2)) {
                        allmapped = false;
                        break;
                    }
                }
                if(allmapped) {
                    netlist.addMapping(new NoMapping(term));
                } else {
                    logger.error("Complementary mapping of signal " + sig.getName() + " failed");
                    return false;
                }
            } else if(resetNetwork.contains(term)) {
                // this unmappable term is part of reset network
                if(syn2.getNetworksComplementaryMap().get(sig) == ComplementaryDecision.RESETONLY) {
                    logger.error("Complementary mapping of signal " + sig.getName() + " failed");
                    continue;
                }
                // check if set network is fully mapped
                boolean allmapped = true;
                for(NetlistTerm t2 : setNetwork) {
                    if(netlist.getUnmappedTerms().contains(t2)) {
                        allmapped = false;
                        break;
                    }
                }
                if(allmapped) {
                    netlist.addMapping(new NoMapping(term));
                } else {
                    logger.error("Complementary mapping of signal " + sig.getName() + " failed");
                    return false;
                }
            } else {
                logger.error("Term should be part of (Re)Set-Network");
                return false;
            }
        }
        return true;
    }

    private int decompose() {
        int numOfDeco = 0;
        for(NetlistTerm term : new ArrayList<>(netlist.getUnmappedTerms())) {
            if(!unmappableTerms.contains(term)) {
                if(BDDHelper.isAndGate(term.getBdd())) {
                    if(seqanddeco.decomposeAND(term)) {
                        numOfDeco++;
                    } else {
                        if(unsafeanddeco) {
                            if(dcanddeco.decomposeAND(term)) {
                                logger.warn("Unsafe AND Deco applied");
                                numOfDeco++;
                            } else {
                                unmappableTerms.add(term);
                            }
                        } else {
                            unmappableTerms.add(term);
                        }
                    }
                } else if(BDDHelper.isOrGate(term.getBdd())) {
                    if(ordeco.decomposeOR(term)) {
                        numOfDeco++;
                    }
                }
            }
        }
        return numOfDeco;
    }

    private int internalMap() {
        int numOfMaps = 0;
        Queue<NetlistTerm> todo = new LinkedList<>(netlist.getUnmappedTerms());
        while(!todo.isEmpty()) {
            if(map(todo.poll())) {
                numOfMaps++;
            }
        }
        return numOfMaps;
    }

    public boolean map(NetlistTerm func) {
        IntermediateGateMapping igmapping = map(func.getBdd(), func.getLoopVar(), func.buildingLoopbackAllowed());
        if(igmapping == null) {
            return false;
        }
        netlist.addMapping(new GateMapping(func, igmapping));
        return true;
    }

    public IntermediateGateMapping map(BDD funcbdd, NetlistVariable funcloopvar, boolean buildLoopbackAllowed) {
        for(Gate gate : techlib.getGates()) {
            if(funcloopvar == null && gate.getLoopInput() != null) {
                //func without loopback -> gate without loopback 
                continue;
            }

            if(gate.getExpression().equals(funcbdd)) {
                // gnd and vcc
                Map<TechVariable, NetlistVariable> mapping = new HashMap<>();
                return new IntermediateGateMapping(gate, mapping);
            }

            MapPairing foundpairing = null;
            List<MapPairing> pairs = getPairs(getVars(funcbdd), getVars(gate.getExpression()));
            if(pairs == null) {
                continue;
            }

            if(funcloopvar != null && gate.getLoopInput() != null) {
                // only take pairings where both loopvars are paired
                int funcvarid = funcloopvar.getId();
                int gatevarid = techlib.getVars().inverse().get(gate.getLoopInput());
                Queue<MapPairing> check = new LinkedList<>(pairs);
                while(!check.isEmpty()) {
                    MapPairing pair = check.poll();
                    if(pair.getIntpair().get(gatevarid) != funcvarid) {
                        pairs.remove(pair);
                    }
                }
            }
            if(funcloopvar != null && gate.getLoopInput() == null) {
                //have to build loopback
                if(!buildLoopbackAllowed) {
                    continue;
                }

                //nothing to do. builded automatically
            }

            for(MapPairing p : pairs) {
                BDD temp = gate.getExpression().replace(p.getBddpair());
                if(temp.equals(funcbdd)) {
                    //System.out.println(p.toString());
                    foundpairing = p;
                    break;
                }
            }

            if(foundpairing != null) {
                Map<TechVariable, NetlistVariable> mapping = new HashMap<>();
                for(Entry<Integer, Integer> entry : foundpairing.getIntpair().entrySet()) {
                    mapping.put(techlib.getVars().get(entry.getKey()), netlist.getNetlistVariableByBddId(entry.getValue()));
                }
                return new IntermediateGateMapping(gate, mapping);
            }
        }
        return null;
    }

    private int[] getVars(BDD bdd) {
        List<Integer> store = new ArrayList<Integer>();
        int id = 0;
        for(int num : bdd.varProfile()) {
            if(num > 0) {
                store.add(id);
            }
            id++;
        }
        int[] retVal = new int[store.size()];
        int intid = 0;
        for(int intval : store) {
            retVal[intid++] = intval;
        }
        return retVal;
    }

    private List<MapPairing> getPairs(int[] funcids, int[] gateids) {
        if(funcids.length != gateids.length) {
            return null;
        }
        List<int[]> perms = permutations(funcids);
        List<MapPairing> pairs = new ArrayList<MapPairing>();
        for(int[] perm : perms) {
            BDDPairing bddpair = netlist.getFac().makePair();
            Map<Integer, Integer> intpair = new HashMap<Integer, Integer>();
            for(int i = 0; i < gateids.length; i++) {
                bddpair.set(gateids[i], perm[i]);
                intpair.put(gateids[i], perm[i]);
            }
            pairs.add(new MapPairing(bddpair, intpair));
        }
        return pairs;
    }

    //Source: http://stackoverflow.com/questions/20906214/permutation-algorithm-for-array-of-integers-in-java
    private ArrayList<int[]> permutations(int[] a) {
        ArrayList<int[]> ret = new ArrayList<int[]>();
        permutation(a, 0, ret);
        return ret;
    }

    private void permutation(int[] arr, int pos, ArrayList<int[]> list) {
        if(arr.length - pos == 1) {
            list.add(arr.clone());
        } else {
            for(int i = pos; i < arr.length; i++) {
                swap(arr, pos, i);
                permutation(arr, pos + 1, list);
                swap(arr, pos, i);
            }
        }
    }

    private void swap(int[] arr, int pos1, int pos2) {
        int h = arr[pos1];
        arr[pos1] = arr[pos2];
        arr[pos2] = h;
    }
}
