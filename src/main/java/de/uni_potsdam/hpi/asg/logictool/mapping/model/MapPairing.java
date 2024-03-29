package de.uni_potsdam.hpi.asg.logictool.mapping.model;

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

import net.sf.javabdd.BDDPairing;

public class MapPairing {

    private BDDPairing            bddpair;
    private Map<Integer, Integer> intpair; // gate -> term

    public MapPairing(BDDPairing bddpair, Map<Integer, Integer> intpair) {
        this.bddpair = bddpair;
        this.intpair = intpair;
    }

    public BDDPairing getBddpair() {
        return bddpair;
    }

    public Map<Integer, Integer> getIntpair() {
        return intpair;
    }
}
