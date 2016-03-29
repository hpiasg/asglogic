package de.uni_potsdam.hpi.asg.logictool.reset.decision;

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
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.srgraph.State.Value;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import net.sf.javabdd.BDD;

public class AdvancedCElementResetDecider extends ResetDecider {
    private static final Logger      logger = LogManager.getLogger();

    private Map<Signal, Set<Signal>> dependencies;

    public AdvancedCElementResetDecider(Reset reset, Arch arch) {
        super(reset);
        dependencies = new HashMap<>();
    }

    @Override
    public boolean decide(Netlist netlist) {
        for(Signal sig : reset.getStategraph().getAllSignals()) {
            if(sig.isInternalOrOutput()) {
//				logger.debug("Check " + sig.getName());
//				logger.debug(reset.getStategraph().getInitState().getStateValues().get(sig));
                dependencies.put(sig, new HashSet<Signal>());
                if(!checkResetting(sig, netlist)) {
                    return false;
                }
            }
        }

//		new ResetDependencyGraph(dependencies, reset.getStategraph(), true);
        //TODO: fix circular
        for(Entry<Signal, Set<Signal>> entry : dependencies.entrySet()) {
            for(Signal depsig : entry.getValue()) {
                if(dependencies.containsKey(depsig)) {
                    if(dependencies.get(depsig).contains(entry.getKey())) {
                        logger.warn("Circular Dependency!");
                    }
                }
            }
        }

        return true;
    }

    private boolean checkResetting(Signal sig, Netlist netlist) {
        NetlistVariable var = netlist.getNetlistVariableBySignal(sig);
        NetlistTerm celemterm = var.getDriver();
        if(!(celemterm instanceof NetlistCelem)) {
            logger.error("Signal " + sig.getName() + " is not driven by an Celem");
            return false;
        }
        NetlistCelem celem = (NetlistCelem)celemterm;

        BDD setnetworkbdd = computeNetwork(celem.getSetInput(), netlist, sig);
        BDD resetnetworkbdd = computeNetwork(celem.getResetInput(), netlist, sig);
        if(setnetworkbdd == null || resetnetworkbdd == null) {
            return false;
        }

//		logger.debug("Set: " + BDDHelper.getFunctionString(setnetworkbdd, netlist));
//		logger.debug("Reset: " + BDDHelper.getFunctionString(resetnetworkbdd, netlist));

        switch(reset.getStategraph().getInitState().getStateValues().get(sig)) {
            case falling:
            case high:
                if(setnetworkbdd.isOne() && resetnetworkbdd.isOne()) {
                    decision.put(sig, ResetDecision.NORST);
                } else if(setnetworkbdd.isOne()) {
                    decision.put(sig, ResetDecision.RESETRST);
                } else if(resetnetworkbdd.isOne()) {
                    decision.put(sig, ResetDecision.SETRST);
                } else {
                    decision.put(sig, ResetDecision.BOTHRST);
                }
                break;
            case low:
            case rising:
                if(setnetworkbdd.isZero() && resetnetworkbdd.isZero()) {
                    decision.put(sig, ResetDecision.NORST);
                } else if(setnetworkbdd.isZero()) {
                    decision.put(sig, ResetDecision.RESETRST);
                } else if(resetnetworkbdd.isZero()) {
                    decision.put(sig, ResetDecision.SETRST);
                } else {
                    decision.put(sig, ResetDecision.BOTHRST);
                }
                break;
        }

//		logger.debug(decision.get(sig));
        return true;
    }

    public BDD computeNetwork(NetlistVariable start, Netlist netlist, Signal thissig) {
        Signal sig = netlist.getSignalByNetlistVariable(start);
        if(sig != null) {
            return restrictSig(netlist, start.toBDD(), sig);
        }

        BDD bdd = start.toBDD();
        Queue<NetlistVariable> check = new LinkedList<>();
        check.add(start);
        while(!check.isEmpty()) {
            NetlistVariable checkVar = check.poll();
            sig = netlist.getSignalByNetlistVariable(checkVar);
            if(sig != null) {
                dependencies.get(thissig).add(sig);
                continue;
            }
            NetlistTerm t = checkVar.getDriver();
            bdd = BDDHelper.mergeBDDs(bdd, checkVar, t.getBdd(), netlist);
            check.addAll(BDDHelper.getVars(t.getBdd(), netlist));
        }

//		System.out.println(BDDHelper.getFunctionString(bdd, netlist));

        for(Signal sig2 : reset.getStategraph().getAllSignals()) {
            if(sig2 == thissig) {
                continue;
            }
            bdd = restrictSig(netlist, bdd, sig2);
        }

        return bdd;
    }

    private BDD restrictSig(Netlist netlist, BDD bdd, Signal sig) {
        Value val = reset.getStategraph().getInitState().getStateValues().get(sig);
        NetlistVariable var = netlist.getNetlistVariableBySignal(sig);
        switch(val) {
            case high:
            case falling:
                return bdd.restrict(var.toBDD());
            case low:
            case rising:
                return bdd.restrict(var.toNotBDD());
        }
        return null;
    }
}
