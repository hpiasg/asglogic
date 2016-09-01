package de.uni_potsdam.hpi.asg.logictool.reset;

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

import java.util.Map;

import de.uni_potsdam.hpi.asg.common.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.reset.insert.ResetInserter;
import de.uni_potsdam.hpi.asg.logictool.srgraph.StateGraph;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTerm;

public class Reset {

    private String        resetName;
    private String        resetNameNot;
    private StateGraph    stategraph;
    private ResetInserter inserter;
    private ResetDecider  decider;

    public Reset(String resetName, StateGraph stategraph) {
        this.resetName = resetName;
        this.resetNameNot = resetName + "_not";
        this.stategraph = stategraph;
    }

    public boolean decide(Netlist netlist) {
        if(decider != null) {
            return decider.decide(netlist);
        }
        return false;
    }

    public boolean insertPreOptimisation(EspressoTable table) {
        if(inserter != null && decider != null) {
            return inserter.insertPreOptimisation(table, decider.getDecision());
        }
        return false;
    }

    public boolean insertPostSynthesis(Netlist netlist) {
        if(inserter != null && decider != null) {
            return inserter.insertPostSynthesis(netlist, decider.getDecision());
        }
        return false;
    }

    public String getName() {
        return resetName;
    }

    public String getNameNot() {
        return resetNameNot;
    }

    public StateGraph getStategraph() {
        return stategraph;
    }

    public void setDecider(ResetDecider decider) {
        this.decider = decider;
    }

    public void setInserter(ResetInserter inserter) {
        this.inserter = inserter;
    }

    public EspressoTerm getEspressoResetTerm(EspressoTable table) {
        int num = table.getInputs().length;
        StringBuilder str = new StringBuilder();
        str.append("1");
        for(int i = 0; i < (num - 1); i++) {
            str.append("-");
        }
        return new EspressoTerm(str.toString(), table.getInputs());
    }

    public Map<Signal, ResetDecision> getDecision() {
        return decider.getDecision();
    }
}
