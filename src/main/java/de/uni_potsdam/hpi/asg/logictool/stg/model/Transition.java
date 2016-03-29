package de.uni_potsdam.hpi.asg.logictool.stg.model;

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

import java.util.ArrayList;
import java.util.List;

import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal.SignalType;

public class Transition implements Comparable<Transition> {

    public enum Edge {
        rising, falling
    }

    private int         id;
    private Edge        edge;
    private Signal      signal;

    private List<Place> postset;
    private List<Place> preset;

    public Transition(int id, Signal signal, Edge edge) {
        this.id = id;
        this.signal = signal;
        this.edge = edge;
        this.preset = new ArrayList<Place>(1);
        this.postset = new ArrayList<Place>(1);
    }

    public Edge getEdge() {
        return edge;
    }

    public Signal getSignal() {
        return signal;
    }

    public List<Place> getPostset() {
        return postset;
    }

    public List<Place> getPreset() {
        return preset;
    }

    public int getId() {
        return id;
    }

    public void addPostPlace(Place post) {
        this.postset.add(post);
    }

    public void addPrePlace(Place pre) {
        this.preset.add(pre);
    }

    @Override
    public String toString() {

        if(signal.getType() == SignalType.dummy) {
            return signal.toString() + ((id != 0) ? "/" + id : "");
        }
        return signal.toString() + ((edge == Edge.falling) ? "-" : "+") + ((id != 0) ? "/" + id : "");
    }

    @Override
    public int compareTo(Transition o) {
        int cmpSigName = this.signal.getName().compareTo(o.getSignal().getName());
        if(cmpSigName == 0) {
            if(this.edge == Edge.falling && o.edge == Edge.rising) {
                return -1;
            } else if(this.edge == Edge.rising && o.edge == Edge.falling) {
                return 1;
            } else {
                return Integer.compare(this.id, o.id);
            }
        } else {
            return cmpSigName;
        }
    }

    public void setId(int id) {
        this.id = id;
    }

    public String outputForGFile() {
        if(signal.getType() == SignalType.dummy) {
            return signal.toString() + ((id != 0) ? "/" + id : "");
        }
        return signal.toString() + ((edge == Edge.falling) ? "-" : "+") + ((id != 0) ? "/" + id : "");
    }
}
