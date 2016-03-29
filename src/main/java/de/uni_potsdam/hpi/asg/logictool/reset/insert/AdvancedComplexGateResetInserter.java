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

import java.util.Map;

import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.reset.Reset;
import de.uni_potsdam.hpi.asg.logictool.reset.decision.ResetDecider.ResetDecision;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;

public class AdvancedComplexGateResetInserter extends ResetInserter {

    public AdvancedComplexGateResetInserter(Reset reset) {
        super(reset);
    }

    @Override
    public boolean insertPreOptimisation(EspressoTable table, Map<Signal, ResetDecision> decision) {
        return false;
    }

    @Override
    public boolean insertPostSynthesis(Netlist netlist, Map<Signal, ResetDecision> decision) {
        return false;
    }

//	@Override
//	public boolean postSynthesisStep(EspressoTable table, Map<String, NetlistTerm> terms) {
//		
//		Map<String, Signal> sigMap = new HashMap<String, Signal>();
//		for(String str : table.getInputs()) {
//			sigMap.put(str, getSignal(str));
//		}
//		
//		Map<String, EspressoValue> rstLevel = new HashMap<String, EspressoTable.EspressoValue>();
//		
//		BDDFactory fac = JFactory.init(10000, 1000);
//		Map<NetlistVariable, Integer> varmap = new HashMap<NetlistVariable, Integer>();
//		Map<String, BDD> bddmap = new HashMap<String, BDD>(); 
//		
//		for(Entry<String, NetlistTerm> entry : terms.entrySet()) {
//			bddmap.put(entry.getKey(), entry.getValue().toBDD(fac, varmap));
//		}
//		
//		Map<String, NetlistVariable> varnamemap = new HashMap<String, NetlistVariable>();
//		for(NetlistVariable var : varmap.keySet()) {
//			varnamemap.put(var.getName(), var);
//		}
//		
//		// rst col for exisiting rows = 0
////		for(EspressoTerm t : table.getTable().rowKeySet()) {
////			t.setLine("0" + t.getLine().substring(1, t.getLine().length()));
////		}
//		
//		// inputs=0 => which signal gets 0/1/depending on intenaloroutput
//		for(Entry<String, BDD> entry : bddmap.entrySet()) {
//			BDD bdd = entry.getValue();
//			for(String inp : table.getInputs()) {
//				Signal sig = sigMap.get(inp);
//				if(sig == null && inp.equals(resetName)) {
//					continue;
//				}
//				if(sig.getType() == SignalType.input) {
//					NetlistVariable v = varnamemap.get(sig.getName());
//					int id = varmap.get(v);
//					bdd = bdd.restrict(fac.nithVar(id));
//				}
//			}
//			Signal sig = sigMap.get(entry.getKey());
//			Value val = init.getStateValues().get(sig);
//			if(bdd.isOne()) {
//				if(val == Value.low || val == Value.rising) {
//					rstLevel.put(entry.getKey(), EspressoValue.zero);
//				} else {
//					rstLevel.put(entry.getKey(), EspressoValue.dontcare);
//				}
//			} else if(bdd.isZero()) {
//				if(val == Value.high || val == Value.falling) {
//					rstLevel.put(entry.getKey(), EspressoValue.one);
//				} else {
//					rstLevel.put(entry.getKey(), EspressoValue.dontcare);
//				}
//			} else { // dependecies with internal signals
//				//TODO: check if circular
//				if(val == Value.high || val == Value.falling) {
//					rstLevel.put(entry.getKey(), EspressoValue.one);
//				} else if(val == Value.low || val == Value.rising) {
//					rstLevel.put(entry.getKey(), EspressoValue.zero);
//				}
//			}
//		}
//		
//		// insert in table
//		EspressoTerm rstterm = getEspressoResetTerm(table);
//		List<String> outputs = new ArrayList<String>(table.getTable().columnKeySet());
//		Table<EspressoTerm, String, EspressoValue> tmptable = HashBasedTable.create();
//		
//		Map<String, EspressoTerm> esprterms = new HashMap<String, EspressoTerm>();
//		for(EspressoTerm t : table.getTable().rowKeySet()) {
//			esprterms.put(t.getLine(), t);
//		}
//		
//		for(String sigName : outputs) {
//			switch(rstLevel.get(sigName)) {
//				case zero:
//				case one:
//					for(Entry<EspressoTerm, EspressoValue> entry : table.getTable().column(sigName).entrySet()) {
//						EspressoValue val = entry.getValue();
//						if(val == EspressoValue.one || val == EspressoValue.zero) {
//							EspressoTerm t = entry.getKey();
//							String newline = "0" + t.getLine().substring(1, t.getLine().length());
//							EspressoTerm newterm = null;
//							if(!esprterms.containsKey(newline)) {
//								newterm = new EspressoTerm(newline, table.getInputs());
//								esprterms.put(newline, newterm);
//							} 
//							newterm = esprterms.get(newline);
//							tmptable.put(t, sigName, EspressoValue.dontcare);
//							tmptable.put(newterm, sigName, val);
//						}
//					}
//					tmptable.put(rstterm, sigName, rstLevel.get(sigName));
//					break;
//				case dontcare:
//					break;
//			}
//		}
//		
//		table.getTable().putAll(tmptable);
//		
//		// minimize table
////		File in = new File("espresso_in_rst.txt");
////		File out = new File("espresso_out_rst.txt");
////		table.setType(TableType.none);
////		table.exportToFile(in);
////		Invoker.invokeEspresso(in.getAbsolutePath(), out.getAbsolutePath());
////		table = EspressoTable.importFromFile(out);
//		
//		return true;
//	}
}
