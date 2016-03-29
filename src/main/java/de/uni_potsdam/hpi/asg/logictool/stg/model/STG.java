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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition.Edge;

public class STG {

    private Map<String, Signal>      signals;
    private List<Transition>         transitions;
    private SortedMap<String, Place> places;
    private List<Place>              initMarking;
    private File                     file;

    public STG(File file) {
        signals = new HashMap<String, Signal>();
        transitions = new ArrayList<Transition>();
        places = new TreeMap<String, Place>();
        this.file = file;
    }

    public void addSignal(String name, SignalType type) {
        Signal sig = new Signal(name, type);
        signals.put(name, sig);
    }

    public Place getPlace(String str) {
        return intenalGetPlace(str, false);
    }

    public Place getPlaceOrAdd(String str) {
        return intenalGetPlace(str, true);
    }

    private Place intenalGetPlace(String str, boolean add) {
        if(places.containsKey(str)) {
            return places.get(str);
        } else {
            if(add) {
                Place p = new Place(str);
                places.put(str, p);
                return p;
            } else {
                return null;
            }
        }
    }

    public void addPlace(String name, Place place) {
        this.places.put(name, place);
    }

    public Transition getTransition(String signalName, Edge edge, int id) {
        return internalGetTransition(signalName, edge, id, false);
    }

    public Transition getTransitionOrAdd(String signalName, Edge edge, int id) {
        return internalGetTransition(signalName, edge, id, true);
    }

    private Transition internalGetTransition(String signalName, Edge edge, int id, boolean add) {
        if(signals.containsKey(signalName)) {
            Signal sig = signals.get(signalName);
            Transition trans = null;
            for(Transition t : sig.getTransitions()) {
                if((t.getEdge() == edge) && (t.getId() == id)) {
                    trans = t;
                    break;
                }
            }
            if(trans == null) {
                if(add) {
                    trans = new Transition(id, sig, edge);
                    sig.addTransition(trans);
                    transitions.add(trans);
                }
            }
            return trans;
        } else {
            System.err.println("Signal " + signalName + " not found");
            return null;
        }
    }

    public void setInitMarking(List<Place> marking) {
        this.initMarking = marking;
    }

    public List<Place> getInitMarking() {
        return initMarking;
    }

    public SortedMap<String, Place> getPlaces() {
        return places;
    }

    public Collection<Signal> getSignals() {
        return signals.values();
    }

    public Signal getSignal(String str) {
        return signals.get(str);
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public File getFile() {
        return file;
    }
}
