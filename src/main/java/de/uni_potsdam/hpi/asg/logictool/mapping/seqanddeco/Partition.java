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

import java.util.Set;

public class Partition implements Comparable<Partition> {

    private Set<PartitionPart> partition;
    private int                cost;

    public Partition(Set<PartitionPart> partition, int cost) {
        this.partition = partition;
        this.cost = cost;
    }

    public Set<PartitionPart> getPartition() {
        return partition;
    }

    @Override
    public int compareTo(Partition o) {
        return Integer.compare(o.cost, cost);
    }

    @Override
    public String toString() {
        return partition.toString() + ": " + cost;
    }
}
