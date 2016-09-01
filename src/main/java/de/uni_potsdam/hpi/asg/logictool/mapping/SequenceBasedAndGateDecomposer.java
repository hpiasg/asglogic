package de.uni_potsdam.hpi.asg.logictool.mapping;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;
import de.uni_potsdam.hpi.asg.common.stggraph.AbstractState.Value;
import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.AndDecoSGHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.IOBehaviour;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.IOBehaviourSimulationStep;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.IOBehaviourSimulationStepFactory;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.IOBehaviourSimulationStepPool;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.Partition;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.PartitionPart;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.QuasiSignal;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.SequenceBackCmp;
import de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco.SequenceFrontCmp;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm.NetlistTermAnnotation;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class SequenceBasedAndGateDecomposer {
    private static final Logger                 logger = LogManager.getLogger();

    private Netlist                             netlist;
    private BDDFactory                          factory;
    private Map<Signal, Integer>                sigidmap;
    private BiMap<NetlistVariable, QuasiSignal> quasimap;
    private AndDecoSGHelper                     sghelper;
    private StateGraph                          origsg;

    private IOBehaviourSimulationStepPool       pool;
    private long                                rmSub  = 0;
    private long                                rmFall = 0;
    private IOBehaviourSimulationStep           root;

    public SequenceBasedAndGateDecomposer(StateGraph stategraph, Netlist netlist) {
        this.netlist = netlist;
        this.factory = JFactory.init(1000, 250);
        this.sigidmap = new HashMap<>();
        this.quasimap = HashBiMap.create();
        this.origsg = stategraph;
        this.sghelper = new AndDecoSGHelper(stategraph.getSTG().getFile());
    }

    public boolean decomposeAND(NetlistTerm term) {

        logger.info("Decomposition of " + term.toString());

        Set<Signal> signals = netlist.getDrivenSignalsTransitive(term);
        if(signals.isEmpty()) {
            logger.warn("No signal(s) for term " + term + " found");
            return false;
        } else if(signals.size() > 1) {
            logger.warn("Term " + term + " drives more than one signal. This is not supported yet");
            return false;
        }
        Signal origsig = signals.iterator().next();
        if(!isAOC(term, origsig)) {
            logger.warn("Algorithm not applicable for non-AOC architectures");
            return false;
        }

        int startgatesize = BDDHelper.numberOfVars(term.getBdd());

        BDD bdd = term.getBdd();
        Set<Signal> origrelevant = findRelevantSigs(bdd);
        if(origrelevant == null) {
            return false;
        }

        StateGraph sg2 = sghelper.getNewStateGraph(origrelevant, origsig);
        if(sg2 == null) {
            logger.warn("Failed to generate new SG. Using the original one.");
            sg2 = origsg;
        }

        BiMap<Signal, Signal> sigmap = HashBiMap.create();
        Set<Signal> relevant = new HashSet<>();
        boolean found;
        for(Signal oldSig : origrelevant) {
            found = false;
            for(Signal newSig : sg2.getAllSignals()) {
                if(oldSig.getName().equals(newSig.getName())) {
                    sigmap.put(oldSig, newSig);
                    found = true;
                    break;
                }
            }
            if(!found) {
                logger.error("Signal " + oldSig.getName() + " not found");
                return false;
            }
            relevant.add(sigmap.get(oldSig));
        }
        found = false;
        for(Signal newSig : sg2.getAllSignals()) {
            if(origsig.getName().equals(newSig.getName())) {
                sigmap.put(origsig, newSig);
                found = true;
                break;
            }
        }
        if(!found) {
            logger.error("Signal " + origsig.getName() + " not found");
            return false;
        }
        Signal sig = sigmap.get(origsig);

        Map<Signal, Boolean> posnegmap = getInputsPosOrNeg(term, sigmap);
        BDD newbdd = factory.one();
        for(Entry<Signal, Boolean> entry : posnegmap.entrySet()) {
            if(entry.getValue()) {
                newbdd = newbdd.andWith(getPosBDD(entry.getKey()));
            } else {
                newbdd = newbdd.andWith(getNegBDD(entry.getKey()));
            }
            if(entry.getKey() instanceof QuasiSignal) {
                relevant.add(entry.getKey());
            }
        }

        Set<State> startStates = new HashSet<>();
        for(State s : sg2.getStates()) {
            for(Entry<Transition, State> entry2 : s.getNextStates().entrySet()) {
                if(entry2.getKey().getSignal() == sig) {
                    startStates.add(entry2.getValue());
                }
            }
        }

        List<List<Signal>> fallingPartitions = new ArrayList<>();
        for(Signal sig2 : relevant) {
            List<Signal> tmp = new ArrayList<>();
            tmp.add(sig2);
            fallingPartitions.add(tmp);
        }

        SortedSet<IOBehaviour> sequencesFront = new TreeSet<>(new SequenceFrontCmp());
        SortedSet<IOBehaviour> sequencesBack = new TreeSet<>(new SequenceBackCmp());
        Set<IOBehaviour> newSequences = new HashSet<>();
        Set<IOBehaviour> rmSequences = new HashSet<>();
        Deque<IOBehaviourSimulationStep> steps = new ArrayDeque<>();

        pool = new IOBehaviourSimulationStepPool(new IOBehaviourSimulationStepFactory());
        pool.setMaxTotal(-1);

        try {
            root = pool.borrowObject();
        } catch(Exception e) {
            e.printStackTrace();
            logger.error("Could not borrow object");
            return false;
        }

        IOBehaviourSimulationStep newStep;
        for(State s : startStates) {
            try {
                newStep = pool.borrowObject();
            } catch(Exception e) {
                e.printStackTrace();
                logger.error("Could not borrow object");
                return false;
            }
            root.getNextSteps().add(newStep);
            newStep.setPrevStep(root);
            newStep.setStart(s);
            newStep.setNextState(s);
            steps.add(newStep);
        }

        if(steps.isEmpty()) {
            return false;
        }

        final long checkThreshold = 100;

        long stepsEvaledTotal = 0;
        IOBehaviourSimulationStep step = null;
        while(!steps.isEmpty()) {
            step = steps.removeLast();
//			System.out.println("#Step: " + step.toString());
            getNewSteps(step, sig, newSequences, steps, relevant);
            stepsEvaledTotal++;
            if(newSequences.size() >= checkThreshold) {
                removeCandidates(sequencesFront, sequencesBack, newSequences, rmSequences);
            }
        }
        removeCandidates(sequencesFront, sequencesBack, newSequences, rmSequences);
        logger.debug("Sequences: " + sequencesFront.size() + " - Tmp Sequences: " + newSequences.size() + " - Steps to evaluate: " + steps.size() + " - Steps evaluated: " + stepsEvaledTotal);
        logger.debug("Pool: " + "Created: " + pool.getCreatedCount() + ", Borrowed: " + pool.getBorrowedCount() + ", Returned: " + pool.getReturnedCount() + ", Active: " + pool.getNumActive() + ", Idle: " + pool.getNumIdle());
        logger.debug("RmSub: " + rmSub + " // RmFall: " + rmFall);

        SortedSet<IOBehaviour> sequences = new TreeSet<>(sequencesFront);
        sequencesFront.clear();
        sequencesBack.clear();
//		System.out.println(sequences.toString());

        List<IOBehaviour> falling = new ArrayList<>();
        List<IOBehaviour> rising = new ArrayList<>();
        List<IOBehaviour> constant = new ArrayList<>();
        if(!categoriseSequences(newbdd, sequences, falling, rising, constant)) {
            return false;
        }
//		System.out.println("Falling:");
//		for(IOBehaviour beh : falling) {
//			System.out.println(beh.toString());
//		}
//		System.out.println("Rising:");
//		for(IOBehaviour beh : rising) {
//			System.out.println(beh.toString());
//		}
//		System.out.println("Constant:");
//		for(IOBehaviour beh : constant) {
//			System.out.println(beh.toString());
//		}

        fallingPartitions = getPossiblePartitionsFromFalling(falling, relevant);
//		System.out.println("FallingPartitions: " + fallingPartitions.toString());

        Map<Integer, List<Partition>> partitions = getPartitions(relevant, startgatesize);
        if(partitions == null) {
            logger.error("There was a problem while creating partions for signal " + sig.getName());
            return false;
        }

//		System.out.println("Init:");
//		for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
//			System.out.println(entry.getKey());
//			for(Partition p : entry.getValue()) {
//				System.out.println("\t" + p.toString());
//			}
//		}

        filterPartitions(partitions, fallingPartitions);
        if(partitions.isEmpty()) {
            logger.error("No suitable partions found");
            return false;
        }

//		System.out.println("After filter Falling:");
//		for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
//			System.out.println(entry.getKey());
//			for(Partition p : entry.getValue()) {
//				System.out.println("\t" + p.toString());
//			}
//		}

//		System.out.println("posneg: " + posnegmap.toString());

        setPartitionBDDs(partitions, posnegmap);

        if(!checkRising(rising, partitions)) {
            logger.error("Check rising failed");
            return false;
        }
        if(partitions.isEmpty()) {
            logger.error("No suitable partions found");
            return false;
        }

//		System.out.println("After filter Rising:");
//		for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
//			System.out.println(entry.getKey());
//			for(Partition p : entry.getValue()) {
//				System.out.println("\t" + p.toString());
//			}
//		}

        if(!checkConstant(constant, partitions)) {
            logger.error("Check constant failed");
            return false;
        }
        if(partitions.isEmpty()) {
            logger.error("No suitable partions found");
            return false;
        }

//		System.out.println("After filter Constant:");
//		for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
//			System.out.println(entry.getKey());
//			for(Partition p : entry.getValue()) {
//				System.out.println("\t" + p.toString());
//			}
//		}

        applyDecoResult(term, partitions, posnegmap, sigmap);
        return true;
    }

    private boolean isAOC(NetlistTerm origterm, Signal sig) {
        // is AND?
        if(!BDDHelper.isAndGate(origterm.getBdd())) {
            return false;
        }
        NetlistVariable var = origterm.getDrivee();
        while(true) {
            if(var.getReader().size() != 1) {
                return false;
            }
            NetlistTerm term = var.getReader().iterator().next();
            // any numbers of orgates and explicit reset gates allowed
            // ending with the celem for this signal
            if(BDDHelper.isOrGate(term.getBdd())) {
                var = term.getDrivee();
            } else if(term.containsAnnotation(NetlistTermAnnotation.rstNew)) {
                var = term.getDrivee();
            } else if(term instanceof NetlistCelem) {
                NetlistCelem celem = (NetlistCelem)term;
                if(celem.getLoopVar() == netlist.getNetlistVariableBySignal(sig)) {
                    return true;
                }
                return false;
            } else {
                return false;
            }
        }
    }

    private void filterPartitions(Map<Integer, List<Partition>> partitions, List<List<Signal>> fallingPartitions) {
        List<Partition> rmPart = new ArrayList<>();
        List<Integer> rmKey = new ArrayList<>();
        for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
            rmPart.clear();
            for(Partition part : entry.getValue()) {
                boolean forcebreak = false;
                for(PartitionPart partpart : part.getPartition()) {
                    for(List<Signal> fallpartpart : fallingPartitions) {
                        boolean containsone = false;
                        for(Signal sig : fallpartpart) {
                            if(partpart.getPart().contains(sig)) {
                                containsone = true;
                                break;
                            }
                        }
                        if(containsone) {
                            if(!partpart.getPart().containsAll(fallpartpart)) {
                                rmPart.add(part);
                                forcebreak = true;
                                break;
                            }
                        }

                    }
                    if(forcebreak) {
                        break;
                    }
                }
            }
            entry.getValue().removeAll(rmPart);
            if(entry.getValue().isEmpty()) {
                rmKey.add(entry.getKey());
            }
        }
        for(int i : rmKey) {
            partitions.remove(i);
        }
    }

    private void removeCandidates(SortedSet<IOBehaviour> sequencesFront, SortedSet<IOBehaviour> sequencesBack, Set<IOBehaviour> newSequences, Set<IOBehaviour> rmSequences) {
        removeSubSequences(sequencesFront, sequencesBack, newSequences, rmSequences); //new->front,back ; set rm
        sequencesBack.removeAll(rmSequences);
        sequencesFront.removeAll(rmSequences);
        newSequences.removeAll(rmSequences);
        if(rmSequences.size() > 0) {
            rmSub += rmSequences.size();
            logger.debug("rmSub removed " + rmSequences.size() + " candidates");
        }
//		checkFalling(newSequences, rmSequences, term, relevant, partitions); //set rm
//		sequencesBack.removeAll(rmSequences);
//		sequencesFront.removeAll(rmSequences);
        newSequences.clear();
//		if(rmSequences.size() > 0) {
//			rmFall += rmSequences.size();
//			logger.debug("chkFall removed " + rmSequences.size() + " candidates");
//		}
    }

    private void applyDecoResult(NetlistTerm term, Map<Integer, List<Partition>> partitions, Map<Signal, Boolean> posnegmap, BiMap<Signal, Signal> sigmap) {
        BiMap<Signal, Signal> sigmapinv = sigmap.inverse();
        if(partitions.isEmpty()) {
            return;
        }
        Entry<Integer, List<Partition>> entry = partitions.entrySet().iterator().next();
        if(entry.getValue().isEmpty()) {
            return;
        }
        Partition p = entry.getValue().get(0);
//		System.out.println(partitions);
        logger.debug("Used Partition: " + p.getPartition().toString());

        Set<NetlistVariable> firstlevelands = new HashSet<>();
        for(PartitionPart part : p.getPartition()) {
            if(part.getPart().size() == 1) {
                Signal s = part.getPart().iterator().next();
                NetlistVariable var = null;
                if(s instanceof QuasiSignal) {
                    var = quasimap.inverse().get((QuasiSignal)s);
                } else {
                    var = netlist.getNetlistVariableBySignal(sigmapinv.get(s));
                }
                firstlevelands.add(var);
                continue;
            }
            BDD tmpBDD = netlist.getFac().one();
            for(Signal sig : part.getPart()) {
                NetlistVariable var = null;
                if(sig instanceof QuasiSignal) {
                    var = quasimap.inverse().get((QuasiSignal)sig);
                } else {
                    Signal netlistsig = sigmapinv.get(sig);
                    var = netlist.getNetlistVariableBySignal(netlistsig);
                }
                if(posnegmap.get(sig)) {
                    tmpBDD = tmpBDD.andWith(var.toBDD());
                } else {
                    tmpBDD = tmpBDD.andWith(var.toNotBDD());
                }
            }
            NetlistTerm tmpterm = netlist.getNetlistTermByBdd(tmpBDD);
            NetlistVariable tmpvar = netlist.getNewTmpVar();
            netlist.addConnection(tmpvar, tmpterm);
            firstlevelands.add(tmpterm.getDrivee());
        }
        BDD newbdd = netlist.getFac().one();
        for(NetlistVariable var : firstlevelands) {
            Signal sig = netlist.getSignalByNetlistVariable(var);
            boolean pos = true;
            if(sig != null) {
                pos = posnegmap.get(sigmap.get(sig));
            }

            if(pos) {
                newbdd = newbdd.and(var.toBDD());
            } else {
                newbdd = newbdd.and(var.toNotBDD());
            }
        }
        netlist.alterTermBDD(term, newbdd);
//		new NetlistGraph(netlist, Arrays.asList("rD_9"), true);
    }

    private boolean checkConstant(List<IOBehaviour> constant, Map<Integer, List<Partition>> partitions) {

        List<Partition> rmPart = new ArrayList<>();
        List<Integer> rmKey = new ArrayList<>();
        for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
            for(Partition part : entry.getValue()) {
                boolean forcebreak = false;
                for(IOBehaviour beh : constant) {
                    for(PartitionPart partpart : part.getPartition()) {
                        BDD partbdd = partpart.getBdd();
                        List<Boolean> result = evaluateBDD(partbdd, beh.getStart(), beh.getSequence());
                        if(result == null) {
                            logger.error("Problem while eval bdd");
                            return false;
                        }
                        boolean curr = result.get(0);
                        int changes = 0;
                        for(boolean r : result) {
                            if(curr != r) {
                                changes++;
                                curr = r;
                            }
                        }
                        if(changes > 0) {
                            rmPart.add(part);
                            //logger.error("Changes in constant");
                            //return false;
                            break;
                        }
                    }
                    if(forcebreak) {
                        break;
                    }
                }
            }
            entry.getValue().removeAll(rmPart);
            if(entry.getValue().isEmpty()) {
                rmKey.add(entry.getKey());
            }
        }
        for(int i : rmKey) {
            partitions.remove(i);
        }
        return true;
    }

    private boolean checkRising(List<IOBehaviour> rising, Map<Integer, List<Partition>> partitions) {

        List<Partition> rmPart = new ArrayList<>();
        List<Integer> rmKey = new ArrayList<>();
        for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
            for(Partition part : entry.getValue()) {
                boolean forcebreak = false;
                for(IOBehaviour beh : rising) {
                    for(PartitionPart partpart : part.getPartition()) {
                        BDD partbdd = partpart.getBdd();
                        List<Boolean> result = evaluateBDD(partbdd, beh.getStart(), beh.getSequence());
                        if(result == null) {
                            logger.error("Problem while eval bdd");
                            return false;
                        }
//						System.out.println(beh.getSequence().toString());
//						System.out.println(result.toString());
                        boolean curr = result.get(0);
                        int changes = 0;
                        for(boolean r : result) {
                            if(curr != r) {
                                changes++;
                                curr = r;
                            }
                        }
                        if(changes > 1) {
                            rmPart.add(part);
                            //logger.error("Non monotous changes in rising: " + beh.toString());
                            //return false;
                            break;
                        }
                    }
                    if(forcebreak) {
                        break;
                    }
                }
            }
            entry.getValue().removeAll(rmPart);
            if(entry.getValue().isEmpty()) {
                rmKey.add(entry.getKey());
            }
        }
        for(int i : rmKey) {
            partitions.remove(i);
        }
        return true;
    }

    private void setPartitionBDDs(Map<Integer, List<Partition>> partitions, Map<Signal, Boolean> posnegmap) {
        for(Entry<Integer, List<Partition>> entry : partitions.entrySet()) {
            for(Partition p : entry.getValue()) {
                for(PartitionPart part : p.getPartition()) {
                    BDD partbdd = factory.one();
                    for(Signal sig : part.getPart()) {
                        BDD sig2bdd = null;
                        boolean posneg = posnegmap.get(sig);
                        if(posneg) {
                            sig2bdd = getPosBDD(sig);
                        } else {
                            sig2bdd = getNegBDD(sig);
                        }
                        partbdd = partbdd.andWith(sig2bdd);
                    }
                    part.setBdd(partbdd);
                }
            }
        }
    }

    private BDD getPosBDD(Signal sig) {
        if(!sigidmap.containsKey(sig)) {
            sigidmap.put(sig, factory.extVarNum(1));
        }
        return factory.ithVar(sigidmap.get(sig));
    }

    private BDD getNegBDD(Signal sig) {
        if(!sigidmap.containsKey(sig)) {
            sigidmap.put(sig, factory.extVarNum(1));
        }
        return factory.nithVar(sigidmap.get(sig));
    }

    private Map<Signal, Boolean> getInputsPosOrNeg(NetlistTerm term, Map<Signal, Signal> sigmap) {
        Map<Signal, Boolean> posnegmap = new HashMap<>();
        for(NetlistVariable var : BDDHelper.getVars(term.getBdd(), netlist)) {
            Boolean b = BDDHelper.isPos(term.getBdd(), var);
            if(b == null) {
                logger.error("PosNeg error");
                return null;
            }
            Signal tmpsig = netlist.getSignalByNetlistVariable(var);
            if(tmpsig == null) {
                if(netlist.getQuasiSignals().keySet().contains(var)) {
                    if(!quasimap.containsKey(var)) {
                        quasimap.put(var, new QuasiSignal(var.getName()));
                    }
                    tmpsig = quasimap.get(var);
                    if(!sigmap.containsKey(tmpsig)) {
                        sigmap.put(tmpsig, tmpsig);
                    }
                } else {
                    logger.error("And input is not a Signal");
                    return null;
                }
            }
            posnegmap.put(sigmap.get(tmpsig), b);
        }
        return posnegmap;
    }

    private Map<Integer, List<Partition>> getPartitions(Set<Signal> signals, int startgatesize) {
        // cost function
        SortedMap<Integer, List<Partition>> retVal = new TreeMap<>(Collections.reverseOrder());
        Set<Set<Set<Signal>>> parts = getTailCombinations(signals);
        if(parts == null) {
            return null;
        }
        for(Set<Set<Signal>> partition : parts) {
            int cost = 0;
            Set<PartitionPart> parts2 = new HashSet<>();
            for(Set<Signal> partpart : partition) {
                parts2.add(new PartitionPart(partpart));
                if(partpart.size() != 1) {
                    cost += partpart.size();
                }
            }
            if(partition.size() != 1) {
                cost += partition.size();
            }

            if(!retVal.containsKey(cost)) {
                retVal.put(cost, new ArrayList<Partition>());
            }
            retVal.get(cost).add(new Partition(parts2, cost));
        }

//		System.out.println("Startgatesize: " + startgatesize);
        // filter too large
        List<Partition> rmPart = new ArrayList<>();
        List<Integer> rmKey = new ArrayList<>();
        for(Entry<Integer, List<Partition>> entry : retVal.entrySet()) {
//			System.out.println(entry.getKey());
            rmPart.clear();
            for(Partition p : entry.getValue()) {
//				System.out.println("\t" + p.toString());
                if(p.getPartition().size() >= startgatesize) {
//					System.out.println("Rm: " + p);
                    rmPart.add(p);
                    continue;
                }
                for(PartitionPart p2 : p.getPartition()) {
                    if(p2.getPart().size() >= startgatesize) {
//						System.out.println("Rm: " + p);
                        rmPart.add(p);
                        continue;
                    }
                }
            }
            entry.getValue().removeAll(rmPart);
            if(entry.getValue().isEmpty()) {
                rmKey.add(entry.getKey());
            }
        }
        for(int i : rmKey) {
            retVal.remove(i);
        }

        return retVal;
    }

    private Set<Set<Set<Signal>>> getTailCombinations(Set<Signal> sigs) {
        try {
            Set<Set<Set<Signal>>> retVal = new HashSet<>();
            for(Set<Signal> s : Sets.powerSet(sigs)) {
                if(s.isEmpty()) {
                    continue;
                }
                SetView<Signal> diff = Sets.difference(sigs, s);
                if(diff.isEmpty()) {
                    Set<Set<Signal>> tmp = new HashSet<>();
                    tmp.add(s);
                    retVal.add(tmp);
                } else {
                    Set<Set<Set<Signal>>> tail = getTailCombinations(diff);
                    for(Set<Set<Signal>> x : tail) {
                        Set<Set<Signal>> tmp = new HashSet<>();
                        tmp.add(s);
                        for(Set<Signal> s2 : x) {
                            tmp.add(s2);
                        }
                        retVal.add(tmp);
                    }
                }
            }
            return retVal;
        } catch(IllegalArgumentException e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    private List<List<Signal>> getPossiblePartitionsFromFalling(List<IOBehaviour> falling, Set<Signal> relevant) {
        List<List<Signal>> partitions = new ArrayList<>();
        for(Signal sig : relevant) {
            List<Signal> tmp = new ArrayList<>();
            tmp.add(sig);
            partitions.add(tmp);
        }
        for(IOBehaviour beh : falling) {
            List<Signal> currlist = null;
            for(Transition t : beh.getSequence()) {
                Signal sig2 = t.getSignal();
                if(relevant.contains(sig2)) {
                    if(currlist == null) {
                        currlist = findInPartions(partitions, sig2);
                    } else {
                        List<Signal> second = findInPartions(partitions, sig2);
                        if(currlist == second) {
                            continue;
                        }
                        currlist.addAll(second);
                        partitions.remove(second);
                    }
                }
            }
        }
        return partitions;
    }

    private Set<Signal> findRelevantSigs(BDD bdd) {
        Set<Signal> relevant = new HashSet<>();
        int id = 0;
        for(int val : bdd.varProfile()) {
            if(val == 1) {
                Signal sig = netlist.getSignalByBddId(id);
                if(sig == null) {
                    NetlistVariable pseudosig = netlist.getNetlistVariableByBddId(id);
                    if(!netlist.getQuasiSignals().keySet().contains(pseudosig)) {
                        logger.error("And Input " + pseudosig.getName() + " is not a Signal");
                        return null;
                    }
                } else {
                    relevant.add(sig);
                }
            } else if(val > 1) {
                logger.warn("BDD var > 1");
            }
            id++;
        }
        return relevant;
    }

    private boolean categoriseSequences(BDD bdd, SortedSet<IOBehaviour> sequences, List<IOBehaviour> falling, List<IOBehaviour> rising, List<IOBehaviour> constant) {
        for(IOBehaviour beh : sequences) {
            BDD startBDD = bdd;
            MutableBoolean resultStart = new MutableBoolean();
            if(!evaluateBDD(resultStart, startBDD, beh.getStart())) {
                logger.error("Something went wrong");
                return false;
            }
            BDD endBDD = bdd;
            MutableBoolean resultEnd = new MutableBoolean();
            if(!evaluateBDD(resultEnd, endBDD, beh.getEnd())) {
                logger.error("Something went wrong");
                return false;
            }

            if(resultStart.isTrue() && resultEnd.isFalse()) {
                falling.add(beh);
                //System.out.println("uups? falling?");
            } else if(resultStart.isFalse() && resultEnd.isTrue()) {
                rising.add(beh);
            } else if(resultStart.isFalse() && resultEnd.isFalse()) {
                constant.add(beh);
            } else {
                logger.error("Const 1 should not happen");
                return false;
            }
            //System.out.println(resultStart.booleanValue() + "=>" + resultEnd.booleanValue() + " : " + beh);
        }
        return true;
    }

    private void removeSubSequences(SortedSet<IOBehaviour> sequencesFront, SortedSet<IOBehaviour> sequencesBack, Set<IOBehaviour> newSequences, Set<IOBehaviour> rmSequences) {
        rmSequences.clear();
        sequencesFront.addAll(newSequences);
        Iterator<IOBehaviour> it = sequencesFront.iterator();
        if(!it.hasNext()) {
            //TODO: why?
            return;
        }
        IOBehaviour curr = it.next();
        while(it.hasNext()) {
            IOBehaviour next = it.next();
            if(newSequences.contains(curr)) {
                if(curr.getStart().compareTo(next.getStart()) == 0) {
                    int i = 0;
                    while(true) {
                        if(curr.getSequence().size() == i) {
                            rmSequences.add(curr);
                            break;
                        }
                        //System.out.println(curr.toString() + " vs " + next.toString());
                        int cmpT = curr.getSequence().get(i).compareTo(next.getSequence().get(i));
                        if(cmpT != 0) {
                            break;
                        }
                        //gleich, check next trans
                        i++;
                    }
                }
            }
            curr = next;
        }
        newSequences.removeAll(rmSequences);
        sequencesBack.addAll(newSequences);
        it = sequencesBack.iterator();
        curr = it.next();
        while(it.hasNext()) {
            IOBehaviour next = it.next();
            if(newSequences.contains(curr)) {
                if(curr.getEnd().compareTo(next.getEnd()) == 0) {
                    int i = 0;
                    while(true) {
                        if(curr.getSequence().size() == i) {
                            rmSequences.add(curr);
                            break;
                        }
                        int cmpT = curr.getSequence().get(curr.getSequence().size() - i - 1).compareTo(next.getSequence().get(next.getSequence().size() - i - 1));
                        if(cmpT != 0) {
                            break;
                        }
                        //gleich, check next trans
                        i++;
                    }
                }
            }
            curr = next;
        }
    }

    private List<Signal> findInPartions(List<List<Signal>> partitions, Signal sig) {
        for(List<Signal> list : partitions) {
            if(list.contains(sig)) {
                return list;
            }
        }
        logger.error("Variable " + sig.getName() + " not found");
        return null;
    }

    private List<Boolean> evaluateBDD(BDD bddParam, State startState, List<Transition> sequence) {
        List<Boolean> retVal = new ArrayList<>();
        for(int i = sequence.size() - 1; i >= 0; i--) {
            BDD bdd = bddParam.or(factory.zero());
            ListIterator<Transition> it = sequence.listIterator(sequence.size() - i);
            List<Signal> alreadyset = new ArrayList<>();
            while(it.hasPrevious()) {
                BDD sigbdd = null;
                Transition t = it.previous();
                if(!alreadyset.contains(t.getSignal())) {
                    switch(t.getEdge()) {
                        case falling:
                            sigbdd = getNegBDD(t.getSignal());
                            break;
                        case rising:
                            sigbdd = getPosBDD(t.getSignal());
                            break;
                    }
                    bdd = bdd.restrictWith(sigbdd);
                    alreadyset.add(t.getSignal());
                }
            }

            for(Entry<Signal, Value> entry : startState.getStateValues().entrySet()) {
                if(!alreadyset.contains(entry.getKey())) {
                    BDD sigbdd = null;
                    switch(entry.getValue()) {
                        case falling:
                        case high:
                            sigbdd = getPosBDD(entry.getKey());
                            break;
                        case low:
                        case rising:
                            sigbdd = getNegBDD(entry.getKey());
                            break;
                    }
                    bdd = bdd.restrictWith(sigbdd);
                }
            }
            for(Entry<NetlistVariable, Boolean> entry : netlist.getQuasiSignals().entrySet()) {
                BDD sigbdd = null;
                if(entry.getValue()) {
                    //true => Normally 1
                    sigbdd = getPosBDD(quasimap.get(entry.getKey()));
                } else {
                    sigbdd = getNegBDD(quasimap.get(entry.getKey()));
                }
                bdd = bdd.restrictWith(sigbdd);
            }

            if(bdd.isOne()) {
                retVal.add(true);
            } else if(bdd.isZero()) {
                retVal.add(false);
            } else {
                logger.error("BDD not restricted enough?!");
                return null;
            }
        }

        return retVal;
    }

    private boolean evaluateBDD(MutableBoolean result, BDD bdd, State state) {
        BDD bdd2 = bdd.and(factory.one());
        for(Entry<Signal, Value> entry : state.getStateValues().entrySet()) {
            BDD sigbdd = null;
            switch(entry.getValue()) {
                case falling:
                case high:
                    sigbdd = getPosBDD(entry.getKey());
                    break;
                case low:
                case rising:
                    sigbdd = getNegBDD(entry.getKey());
                    break;
            }
            bdd2 = bdd2.restrictWith(sigbdd);
        }
        for(Entry<NetlistVariable, Boolean> entry : netlist.getQuasiSignals().entrySet()) {
            BDD sigbdd = null;
            if(entry.getValue()) {
                //true => Normally 1
                sigbdd = getPosBDD(quasimap.get(entry.getKey()));
            } else {
                sigbdd = getNegBDD(quasimap.get(entry.getKey()));
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

    private void getNewSteps(IOBehaviourSimulationStep step, Signal sig, Set<IOBehaviour> newSequences, Deque<IOBehaviourSimulationStep> newSteps, Set<Signal> relevant) {

        int occurrences = Collections.frequency(step.getStates(), step.getNextState());
        if(occurrences >= 2) {
            boolean isLoop = false;
            int[] a = new int[occurrences];
            int index = 0;
            for(int i = step.getStates().size() - 1; i >= 0; i--) {
                if(step.getStates().get(i) == step.getNextState()) {
                    a[index++] = i;
                }
            }

            Map<Integer, List<State>> lists = new HashMap<>();

            int endindex = step.getStates().size();
            List<State> xlist = null;
            for(int startindex : a) {
                xlist = step.getStates().subList(startindex, endindex);
                lists.put(endindex, xlist);
                endindex = startindex;
            }

            boolean finished = false;
            int smallesidloop = -1;
            for(Entry<Integer, List<State>> l1 : lists.entrySet()) {
                for(Entry<Integer, List<State>> l2 : lists.entrySet()) {
                    if(l1.getKey() != l2.getKey()) {
                        if(l1.getValue().equals(l2.getValue())) {
//							System.out.println("loop: listmatch!");
                            isLoop = true;
                            smallesidloop = l1.getKey() < l2.getKey() ? l1.getKey() : l2.getKey();
                            xlist = l1.getValue();
                            finished = true;
                            break;
                        }
                    }
                }
                if(finished) {
                    break;
                }
            }

            if(isLoop) {
//				System.out.println("Loop: " + xlist + ", listsize: " + lists.size());
//				System.out.println("Smallestid : " + smallesidloop + ", size: " + step.getStates().size());
                step.findStateAndClean(step.getStates().size() - smallesidloop - 1, pool, newSteps);
                return;
            }
        }

        for(Entry<Transition, State> entry : step.getNextState().getNextStates().entrySet()) {
            //System.out.println(entry.toString());
            if(entry.getKey().getSignal() == sig) {
                List<Transition> seq = new ArrayList<>(step.getSequence());
                IOBehaviour beh = new IOBehaviour(seq, step.getStart(), step.getNextState());
                newSequences.add(beh);
            } else {
                IOBehaviourSimulationStep newStep;
                try {
                    newStep = pool.borrowObject();
                } catch(Exception e) {
                    e.printStackTrace();
                    logger.error("Could not borrow object");
                    return;
                }
                newStep.getSequence().addAll(step.getSequence());
                if(relevant.contains(entry.getKey().getSignal())) {
                    newStep.getSequence().add(entry.getKey());
                }
                newStep.setStart(step.getStart());
                newStep.setNextState(entry.getValue());
                newStep.getStates().addAll(step.getStates());
                newStep.getStates().add(step.getNextState());
                newStep.setPrevStep(step);
                step.getNextSteps().add(newStep);
                newSteps.add(newStep);
            }
        }
        step.killIfCan(pool);
    }
}
