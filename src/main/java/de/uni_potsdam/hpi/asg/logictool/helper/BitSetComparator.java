package de.uni_potsdam.hpi.asg.logictool.helper;

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

import java.util.BitSet;
import java.util.Comparator;

public class BitSetComparator implements Comparator<BitSet> {

    @Override
    public int compare(BitSet a, BitSet b) {
        for(int i = 0; i < a.length(); i++) {
            if(a.get(i) != b.get(i)) {
                if(a.get(i)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
        return 0;
    }
}
