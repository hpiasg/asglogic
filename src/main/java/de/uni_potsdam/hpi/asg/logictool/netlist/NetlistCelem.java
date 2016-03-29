package de.uni_potsdam.hpi.asg.logictool.netlist;

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

import net.sf.javabdd.BDD;

public class NetlistCelem extends NetlistTerm {

    public enum InternalArch {
        standardC, generalisedCset, generalisedCreset
    }

    public enum Arch {
        standardC, generalisedC
    }

    private NetlistVariable setInput;
    private NetlistVariable resetInput;
    private InternalArch    arch;

    public NetlistCelem(BDD bdd, NetlistVariable setInput, NetlistVariable resetInput, NetlistVariable loopback, Netlist netlist, InternalArch arch) {
        super(bdd, netlist);
        this.setInput = setInput;
        this.resetInput = resetInput;
        this.loopVar = loopback;
        this.arch = arch;
    }

    public static BDD getCelemBDD(NetlistVariable setInput, NetlistVariable resetInput, NetlistVariable loopback, InternalArch arch, Netlist netlist) {
        BDD setInpBdd = setInput.toBDD();
        BDD resetInpBdd = resetInput.toBDD();
        BDD sigBdd = loopback.toBDD();

        // NAND impl
//		BDD firstNand = (sigBdd.and(setInpBdd)).not();
//		BDD secondNand = (setInpBdd.and(resetInpBdd)).not();
//		BDD thirdNand = (resetInpBdd.and(sigBdd)).not();
//		BDD bigNand = (firstNand.andWith(secondNand).andWith(thirdNand)).not();
//		return bigNand;

        switch(arch) {
            default:
            case standardC:
                // Normal impl
                BDD o1 = setInpBdd.and(resetInpBdd);
                BDD o2 = sigBdd.and(setInpBdd.or(resetInpBdd));
                return o1.or(o2);
            case generalisedCset:
                // Set dominant impl
                return setInpBdd.or(sigBdd.and(resetInpBdd));
            case generalisedCreset:
                // Reset dominant impl
                return resetInpBdd.and(setInpBdd.or(sigBdd));
        }
    }

    public InternalArch getArch() {
        return arch;
    }

    @Override
    public int replace(NetlistVariable replacement, NetlistVariable obsolete) {
        if(this.setInput == obsolete) {
            this.setInput = replacement;
        }
        if(this.resetInput == obsolete) {
            this.resetInput = replacement;
        }
        return super.replace(replacement, obsolete);
    }

    public NetlistVariable getSetInput() {
        return setInput;
    }

    public NetlistVariable getResetInput() {
        return resetInput;
    }

    public void remove() {
        netlist.removeTerm(this);
    }
}
