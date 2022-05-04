package de.uni_potsdam.hpi.asg.logictool.reset.insert;

/*
 * Copyright (C) 2014 - 2018 Norman Kluge
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
import de.uni_potsdam.hpi.asg.logictool.reset.decision.AdvancedResetDecider;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.FullReset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.NoReset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import net.sf.javabdd.BDD;

public class AdvancedResetInserter extends ResetInserter {
    private static final Logger          logger = LogManager.getLogger();

    private NetlistVariable              resetVar;
    private NetlistVariable              notResetVar;
    private Netlist                      netlist;

    private Map<Signal, Boolean>         highCubesImplementable;
    private Map<Signal, Boolean>         lowCubesImplementable;
    private Map<Signal, NetlistVariable> highImplVar;
    private Map<Signal, NetlistVariable> lowImplVar;
    private Map<Signal, NetlistVariable> celemImplVar;

    public AdvancedResetInserter(Reset reset, Arch arch) {
        super(reset);
    }

    public void setData(Map<Signal, NetlistVariable> celemImplVar, Map<Signal, Boolean> highCubesImplementable, Map<Signal, NetlistVariable> highImplVar, Map<Signal, Boolean> lowCubesImplementable, Map<Signal, NetlistVariable> lowImplVar) {
        this.celemImplVar = celemImplVar;
        this.highCubesImplementable = highCubesImplementable;
        this.highImplVar = highImplVar;
        this.lowCubesImplementable = lowCubesImplementable;
        this.lowImplVar = lowImplVar;
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
                if(!insertRstCelem(decision, sig)) {
                    return false;
                }
                if(highCubesImplementable.get(sig)) {
                    if(!insertRstHigh(sig)) {
                        highCubesImplementable.put(sig, false);
                    }
                }
                if(lowCubesImplementable.get(sig)) {
                    if(!insertRstLow(sig)) {
                        lowCubesImplementable.put(sig, false);
                    }
                }
            }
        }
        return true;
    }

    private boolean insertRstLow(Signal sig) {
        ResetDecider dec = reset.getDecider();
        boolean insertRst = false;
        if(dec instanceof AdvancedResetDecider) {
            insertRst = ((AdvancedResetDecider)dec).getLowReset().get(sig);
        } else if(dec instanceof FullReset) {
            insertRst = true;
        } else if(dec instanceof NoReset) {
            insertRst = false;
        } else {
            logger.error("Unknwon reset decider");
            return false;
        }
        if(insertRst) {
            switch(reset.getStategraph().getInitState().getStateValues().get(sig)) {
                case rising:
                case low:
                    //RESET: low->1
                    if(!buildToOne(lowImplVar.get(sig))) {
                        return false;
                    }
                    break;
                case falling:
                case high:
                    // SET: low->0 
                    if(!buildToZero(lowImplVar.get(sig))) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    private boolean insertRstHigh(Signal sig) {
        ResetDecider dec = reset.getDecider();
        boolean insertRst = false;
        if(dec instanceof AdvancedResetDecider) {
            insertRst = ((AdvancedResetDecider)dec).getHighReset().get(sig);
        } else if(dec instanceof FullReset) {
            insertRst = true;
        } else if(dec instanceof NoReset) {
            insertRst = false;
        } else {
            logger.error("Unknwon reset decider");
            return false;
        }
        if(insertRst) {
            switch(reset.getStategraph().getInitState().getStateValues().get(sig)) {
                case rising:
                case low:
                    //RESET: high->0
                    if(!buildToZero(highImplVar.get(sig))) {
                        return false;
                    }
                    break;
                case falling:
                case high:
                    // SET: high->1 
                    if(!buildToOne(highImplVar.get(sig))) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    private boolean insertRstCelem(Map<Signal, ResetDecision> decision, Signal sig) {
        ResetDecision dec = decision.get(sig);
        if(dec == null) {
            return false;
        }
        if(dec != ResetDecision.NORST) {
            NetlistVariable var = celemImplVar.get(sig);
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
                            if(!buildToZeroCelem(celem.getSetInput(), celem, false)) {
                                return false;
                            }
                            if(!buildToZeroCelem(celem.getResetInput(), celem, resetSplit)) {
                                return false;
                            }
                            break;
                        case RESETRST:
                            if(!buildToZeroCelem(celem.getResetInput(), celem, resetSplit)) {
                                return false;
                            }
                            break;
                        case SETRST:
                            if(!buildToZeroCelem(celem.getSetInput(), celem, false)) {
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
                            if(!buildToOneCelem(celem.getSetInput(), celem, false)) {
                                return false;
                            }
                            if(!buildToOneCelem(celem.getResetInput(), celem, resetSplit)) {
                                return false;
                            }
                            break;
                        case RESETRST:
                            if(!buildToOneCelem(celem.getResetInput(), celem, resetSplit)) {
                                return false;
                            }
                            break;
                        case SETRST:
                            if(!buildToOneCelem(celem.getSetInput(), celem, false)) {
                                return false;
                            }
                            break;
                        case NORST:
                            break;

                    }
                    break;
            }
        }
        return true;
    }

    private boolean buildToZero(NetlistVariable var) {
        NetlistVariable tmpVar = netlist.getNewTmpVar();
        netlist.addConnection(tmpVar, var.getDriver());
        BDD bdd = tmpVar.toBDD();
        bdd = bdd.and(notResetVar.toBDD());
        NetlistTerm newt = netlist.getNetlistTermByBdd(bdd, EnumSet.of(NetlistTermAnnotation.rstNew));
        netlist.addConnection(var, newt);
        return true;
    }

    private boolean buildToOne(NetlistVariable var) {
        NetlistVariable tmpVar = netlist.getNewTmpVar();
        netlist.addConnection(tmpVar, var.getDriver());
        BDD bdd = tmpVar.toBDD();
        bdd = bdd.or(resetVar.toBDD());
        NetlistTerm newt = netlist.getNetlistTermByBdd(bdd, EnumSet.of(NetlistTermAnnotation.rstNew));
        netlist.addConnection(var, newt);
        return true;
    }

    private boolean buildToZeroCelem(NetlistVariable input, NetlistTerm term, boolean splitNetwork) {
        BDD bdd = input.toBDD();
        bdd = bdd.and(notResetVar.toBDD());
        NetlistTerm newt = netlist.getNetlistTermByBdd(bdd, EnumSet.of(NetlistTermAnnotation.rstNew));
        if(netlist.insertInFront(newt, term, input) != 0) {
            return false;
        }
        return true;
    }

    private boolean buildToOneCelem(NetlistVariable input, NetlistTerm term, boolean splitNetwork) {
        BDD bdd = input.toBDD();
        bdd = bdd.or(resetVar.toBDD());
        NetlistTerm newt = netlist.getNetlistTermByBdd(bdd, EnumSet.of(NetlistTermAnnotation.rstNew));
        if(netlist.insertInFront(newt, term, input) != 0) {
            return false;
        }
        return true;
    }
}
