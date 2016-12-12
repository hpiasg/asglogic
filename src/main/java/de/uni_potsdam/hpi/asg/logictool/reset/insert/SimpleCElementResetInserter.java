package de.uni_potsdam.hpi.asg.logictool.reset.insert;

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

import java.util.EnumSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistCelem.Arch;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm.NetlistTermAnnotation;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import net.sf.javabdd.BDD;

public class SimpleCElementResetInserter extends ResetInserter {
    private static final Logger logger = LogManager.getLogger();

    private NetlistVariable     resetVar;
    private NetlistVariable     notResetVar;
    private Netlist             netlist;

    public SimpleCElementResetInserter(Reset reset, Arch arch) {
        super(reset);
    }

    @Override
    public boolean insertPreOptimisation(EspressoTable table, Map<Signal, ResetDecision> decision) {
        return true;
    }

    @Override
    public boolean insertPostSynthesis(Netlist netlist, Map<Signal, ResetDecision> decision) {
        resetVar = netlist.getNetlistVariableByName(reset.getName());
        notResetVar = netlist.getNetlistVariableByName(reset.getNameNot());
        this.netlist = netlist;

        for(Signal sig : reset.getStategraph().getAllSignals()) {
            if(sig.isInternalOrOutput()) {
                ResetDecision dec = decision.get(sig);
                if(dec == null) {
                    return false;
                }
                if(dec != ResetDecision.NORST) {
                    NetlistVariable var = netlist.getNetlistVariableBySignal(sig);
                    NetlistTerm celemterm = var.getDriver();
                    if(!(celemterm instanceof NetlistCelem)) {
                        logger.error("Signal " + sig.getName() + " is not driven by an Celem");
                        return false;
                    }
                    NetlistCelem celem = (NetlistCelem)celemterm;

                    //Splitting of resetNetwork needed?
                    boolean resetSplit = false;

                    switch(reset.getStategraph().getInitState().getStateValues().get(sig)) {
                        case rising:
                        case low:
                            // RESET
                            // setnetwork->0; resetnetwork->0
                            switch(dec) {
                                case BOTHRST:
                                    if(!buildToZero(celem.getSetInput(), celem, false)) {
                                        return false;
                                    }
                                    if(!buildToZero(celem.getResetInput(), celem, resetSplit)) {
                                        return false;
                                    }
                                    break;
                                case RESETRST:
                                    if(!buildToZero(celem.getResetInput(), celem, resetSplit)) {
                                        return false;
                                    }
                                    break;
                                case SETRST:
                                    if(!buildToZero(celem.getSetInput(), celem, false)) {
                                        return false;
                                    }
                                    break;
                                case NORST:
                                    break;
                            }
                            break;
                        case falling:
                        case high:
                            // SET
                            // setnetwork->1; resetnetwork->1
                            switch(dec) {
                                case BOTHRST:
                                    if(!buildToOne(celem.getSetInput(), celem, false)) {
                                        return false;
                                    }
                                    if(!buildToOne(celem.getResetInput(), celem, resetSplit)) {
                                        return false;
                                    }
                                    break;
                                case RESETRST:
                                    if(!buildToOne(celem.getResetInput(), celem, resetSplit)) {
                                        return false;
                                    }
                                    break;
                                case SETRST:
                                    if(!buildToOne(celem.getSetInput(), celem, false)) {
                                        return false;
                                    }
                                    break;
                                case NORST:
                                    break;

                            }
                            break;
                    }
                }
            }
        }
        return true;
    }

    private boolean buildToZero(NetlistVariable input, NetlistCelem celem, boolean splitNetwork) {
        BDD bdd = input.toBDD();
        bdd = bdd.and(notResetVar.toBDD());
        NetlistTerm newt = netlist.getNetlistTermByBdd(bdd, EnumSet.of(NetlistTermAnnotation.rstNew));
        if(netlist.insertInFront(newt, celem, input) != 0) {
            return false;
        }
        return true;
    }

    private boolean buildToOne(NetlistVariable input, NetlistTerm celem, boolean splitNetwork) {
        BDD bdd = input.toBDD();
        bdd = bdd.or(resetVar.toBDD());
        NetlistTerm newt = netlist.getNetlistTermByBdd(bdd, EnumSet.of(NetlistTermAnnotation.rstNew));
        if(netlist.insertInFront(newt, celem, input) != 0) {
            return false;
        }
        return true;
    }
}
