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

public class BitSetHelper {

    public static String formatBitset(BitSet bitset, int width) {
        StringBuilder str = new StringBuilder();
        for(int i = (width - 1); i >= 0; i--) {
            if(bitset.get(i)) {
                str.append("1");
            } else {
                str.append("0");
            }
        }
        return str.toString();
    }

    public static boolean isHammingDistanceOne(BitSet a, BitSet b) {
        int distance = 0;
        int i = 0;
        while(i < a.size()) {
            if(a.get(i) != b.get(i)) {
                distance++;
            }
            i++;
            if(distance > 1) {
                return false;
            }
        }
        if(distance == 1) {
            return true;
        }

        return false;
    }

    public static void dualNext(BitSet b) {
        int index = b.nextClearBit(0);
        b.set(index);
        b.flip(0, index);
    }

    /*private static int getHammingDistance(BitSet a, BitSet b) {
    	int distance = 0;
        int i = 0;
        while(i < a.size()) {
        	if(a.get(i) != b.get(i)) {
        		distance++;
        	}
        	i++;
        }
        return distance;
    }*/
}
