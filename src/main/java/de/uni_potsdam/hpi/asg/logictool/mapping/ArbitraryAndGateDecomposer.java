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

import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.logictool.helper.BDDHelper;
import de.uni_potsdam.hpi.asg.logictool.netlist.Netlist;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistTerm;
import de.uni_potsdam.hpi.asg.logictool.netlist.NetlistVariable;
import net.sf.javabdd.BDD;

public class ArbitraryAndGateDecomposer {
    private static final Logger logger = LogManager.getLogger();

    private Netlist             netlist;

    public ArbitraryAndGateDecomposer(Netlist netlist) {
        this.netlist = netlist;
    }

    public boolean decomposeAND(NetlistTerm term) {

        Set<NetlistVariable> vars = BDDHelper.getVars(term.getBdd(), netlist);
        int num = vars.size() / 2;
        int id = 0;
        Iterator<NetlistVariable> it = vars.iterator();

        BDD t1bdd = netlist.getFac().one();
        while(id < num) {
            NetlistVariable var = it.next();
            Boolean ispos = BDDHelper.isPos(term.getBdd(), var);
            if(ispos == null) {
                logger.error("Not clear if variable pos or neg");
                return false;
            }
            if(ispos) {
                t1bdd = t1bdd.and(var.toBDD());
            } else {
                t1bdd = t1bdd.and(var.toNotBDD());
            }
            id++;
        }

        BDD t2bdd = netlist.getFac().one();
        while(id < vars.size()) {
            NetlistVariable var = it.next();
            Boolean ispos = BDDHelper.isPos(term.getBdd(), var);
            if(ispos == null) {
                logger.error("Not clear if variable pos or neg");
                return false;
            }
            if(ispos) {
                t2bdd = t2bdd.and(var.toBDD());
            } else {
                t2bdd = t2bdd.and(var.toNotBDD());
            }
            id++;
        }

        NetlistTerm term1 = netlist.getNetlistTermByBdd(t1bdd);
        NetlistTerm term2 = netlist.getNetlistTermByBdd(t2bdd);
        NetlistVariable var1 = null;
        if(term1.getDrivee() != null) {
            var1 = term1.getDrivee();
        } else {
            var1 = netlist.getNewTmpVar();
        }
        NetlistVariable var2 = null;
        if(term2.getDrivee() != null) {
            var2 = term2.getDrivee();
        } else {
            var2 = netlist.getNewTmpVar();
        }

        netlist.addConnection(var1, term1);
        netlist.addConnection(var2, term2);

        BDD newbdd = netlist.getFac().one();
        newbdd = newbdd.and(var1.toBDD());
        newbdd = newbdd.and(var2.toBDD());
        netlist.alterTermBDD(term, newbdd);

        return true;
    }

}
