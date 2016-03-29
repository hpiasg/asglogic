package de.uni_potsdam.hpi.asg.logictool.mapping.seqanddeco;

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

import java.util.Comparator;

public class SequenceFrontCmp implements Comparator<IOBehaviour> {

    @Override
    public int compare(IOBehaviour arg0, IOBehaviour arg1) {
        int cmpStart = arg0.getStart().compareTo(arg1.getStart());
        if(cmpStart == 0) {
            int i = 0;
            while(true) {
                if(arg0.getSequence().size() == i && arg1.getSequence().size() == i) {
                    return 0;
                }
                if(arg0.getSequence().size() == i) {
                    return -1;
                }
                if(arg1.getSequence().size() == i) {
                    return 1;
                }
                int cmpT = arg0.getSequence().get(i).compareTo(arg1.getSequence().get(i));
                if(cmpT != 0) {
                    return cmpT;
                }
                i++;
            }
        } else {
            return cmpStart;
        }
    }
}
