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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.mapping.merge.MergeSimulationStep;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.GateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.IntermediateGateMapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.Mapping;
import de.uni_potsdam.hpi.asg.logictool.mapping.model.WireMapping;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import net.sf.javabdd.BDD;

public class GateMerger {
    private static final Logger logger = LogManager.getLogger();

    private Netlist             netlist;
    private TechnologyMapper    mapper;

    public GateMerger(Netlist netlist, TechnologyMapper mapper) {
        this.netlist = netlist;
        this.mapper = mapper;
    }

    public boolean merge() {

//		new NetlistGraph(netlist, null, true);

        Queue<MergeSimulationStep> steps = new LinkedList<>();
        for(NetlistTerm t : netlist.getTerms()) {
            if(t.mergingAllowed()) {
                steps.add(new MergeSimulationStep(null, new HashSet<NetlistTerm>(), t.getLoopVar(), t.getDrivee(), null, t, t.buildingLoopbackAllowed()));
            }
        }

        Map<NetlistTerm, Set<Mapping>> matches = new HashMap<>();

        MergeSimulationStep step = null;
        while(!steps.isEmpty()) {
            step = steps.poll();
            evaluateStep(step, steps, matches);
        }

//		for(Entry<NetlistTerm, Set<GateMapping>> entry : matches.entrySet()) {
//			System.out.println(entry.getKey().getDrivee());
//			for(GateMapping g : entry.getValue()) {
//				System.out.println("\t" + g.toString());
//			}
//		}

        Map<Integer, Set<NetlistTerm>> cluster = new HashMap<>();
        Map<NetlistTerm, Integer> idmap = new HashMap<>();
        int id = 0;
        for(NetlistTerm t : netlist.getTerms()) {
            Set<NetlistTerm> tmp = new HashSet<>();
            tmp.add(t);
            cluster.put(id, tmp);
            idmap.put(t, id);
            id++;
        }

//		for(Entry<NetlistTerm, Integer> entry : idmap.entrySet()) {
//			System.out.println(entry.getKey().getDrivee() + " -> " + entry.getValue());
//		}
//		System.out.println("---");
        for(Entry<NetlistTerm, Set<Mapping>> entry : matches.entrySet()) {
            int entryid = idmap.get(entry.getKey());
            for(Mapping m : entry.getValue()) {
                for(NetlistTerm t : m.getTerms()) {
                    int termid = idmap.get(t);
                    if(termid != entryid) {
                        for(NetlistTerm t2 : cluster.get(termid)) {
                            idmap.put(t2, entryid);
                        }
                        cluster.get(entryid).addAll(cluster.get(termid));
                        cluster.remove(termid);
                    }
                }
            }
        }

//		for(Entry<Integer, Set<NetlistTerm>> entry : cluster.entrySet()) {
//			System.out.print(entry.getKey() + ": ");
//			for(NetlistTerm t : entry.getValue()) {
//				System.out.print(t.getDrivee() + ", ");
//			}
//			System.out.println();
//		}

        for(Set<NetlistTerm> terms : cluster.values()) {
            if(terms.size() == 1) {
                continue;
            }
            Set<Mapping> gms = new HashSet<>();
            for(NetlistTerm t : terms) {
                gms.addAll(matches.get(t));
            }
            SortedMap<Float, List<Set<Mapping>>> possibleResults = new TreeMap<>();
            for(Set<Mapping> x : Sets.powerSet(gms)) {
                Set<NetlistTerm> tmpterms = new HashSet<>(terms);
                float size = 0;
                boolean validSol = true;
                for(Mapping g : x) {
                    for(NetlistTerm t : g.getTerms()) {
                        if(tmpterms.contains(t)) {
                            tmpterms.remove(t);
                        } else {
                            validSol = false;
                            break;
                        }
                    }
                    if(!validSol) {
                        break;
                    }
                    if(g instanceof GateMapping) {
                        size += ((GateMapping)g).getGate().getSize();
                    } else if(g instanceof WireMapping) {
                        size += 0;
                    } else {
                        validSol = false;
                        break;
                    }
                }
                if(validSol && tmpterms.isEmpty()) {
                    if(!possibleResults.containsKey(size)) {
                        possibleResults.put(size, new ArrayList<Set<Mapping>>());
                    }
                    possibleResults.get(size).add(x);
                }
            }

//			System.out.println(terms);
//			System.out.println(possibleResults);
//			System.out.println("---");

            Set<Mapping> result = possibleResults.get(possibleResults.firstKey()).get(0);
            for(Mapping m : result) {
                if(m instanceof GateMapping) {
                    GateMapping g = (GateMapping)m;
                    if(BDDHelper.isBuffer(g.getGate().getExpression())) {
                        if(g.getMapping().values().size() != 1) {
                            logger.error("Buffer vars != 1");
                            return false;
                        }
                        NetlistVariable driver = g.getMapping().values().iterator().next();
                        if(driver == null) {
                            logger.error("Buffer driver null");
                            return false;
                        }
                        WireMapping nm = new WireMapping(driver, g.getDrivee(), netlist, g.getTerms());
                        netlist.replaceMapping(nm);
                        continue;
                    }
                    netlist.replaceMapping(g);
                }
            }
        }

        return true;
    }

    private void evaluateStep(MergeSimulationStep step, Queue<MergeSimulationStep> steps, Map<NetlistTerm, Set<Mapping>> matches) {

        NetlistVariable loopvar = step.getLoopvar();
        if(step.getLoopvar() != null && step.getNewTerm().getLoopVar() != null) {
            if(step.getLoopvar() != step.getNewTerm().getLoopVar()) {
                logger.warn("Term with two different loopvars not supported");
                return;
            }
        } else if(step.getNewTerm().getLoopVar() != null) {
            loopvar = step.getNewTerm().getLoopVar();
        }

        Set<NetlistTerm> terms = new HashSet<>(step.getTerms());
        terms.add(step.getNewTerm());

        BDD newbdd = null;
        Mapping match = null;
        if(step.getBdd() == null && step.getReplvar() == null) {
            newbdd = step.getNewTerm().getBdd();
            match = step.getNewTerm().getMapping();
        } else {
            newbdd = BDDHelper.mergeBDDs(step.getBdd(), step.getReplvar(), step.getNewTerm().getBdd(), netlist);
            if(newbdd == null) {
                logger.warn("Could not merge BDDs");
                return;
            }
            IntermediateGateMapping igmap = mapper.map(newbdd, loopvar, step.isBuildLoopbackAllowed());
            if(igmap == null) {
                // merged term is not mappable
                return;
            }
            match = new GateMapping(terms, step.getDrivee(), igmap);
        }

        for(NetlistTerm t : terms) {
            if(!matches.containsKey(t)) {
                matches.put(t, new HashSet<Mapping>());
            }
            matches.get(t).add(match);
        }

        for(NetlistVariable var : BDDHelper.getVars(newbdd, netlist)) {
            if(var == loopvar) {
                continue;
            }
            if(var.getReader().size() > 1) {
                continue;
            }
            Signal sig = netlist.getSignalByNetlistVariable(var);
            if(sig != null) {
                continue;
            }
            boolean buildLoopbackAllowed = (step.isBuildLoopbackAllowed()) ? step.getNewTerm().buildingLoopbackAllowed() : false;
            steps.add(new MergeSimulationStep(newbdd, terms, loopvar, step.getDrivee(), var, var.getDriver(), buildLoopbackAllowed));
        }
    }
}
