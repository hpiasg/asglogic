package de.uni_potsdam.hpi.asg.logictool.srgraph;

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

import de.uni_potsdam.hpi.asg.common.stg.model.Place;
import de.uni_potsdam.hpi.asg.common.stg.model.Transition;

public class SimulationStep {

    private List<Place> marking;
    private Transition  fireTrans;
    private State       state;

    public SimulationStep() {
        marking = new ArrayList<Place>();
    }

    public void addPlaces(List<Place> places) {
        this.marking.addAll(places);
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setFireTrans(Transition fireTrans) {
        this.fireTrans = fireTrans;
    }

    public List<Place> getMarking() {
        return marking;
    }

    public Transition getFireTrans() {
        return fireTrans;
    }

    public State getState() {
        return state;
    }
}
