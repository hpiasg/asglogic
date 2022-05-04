package de.uni_potsdam.hpi.asg.logictool.synthesis.model;

/*
 * Copyright (C) 2015 - 2018 Norman Kluge
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

import com.google.common.collect.BiMap;

import de.uni_potsdam.hpi.asg.common.stghelper.model.CFRegion;

public class Cubes {

    private BiMap<CFRegion, EspressoTerm> risingCubes;
    private BiMap<CFRegion, EspressoTerm> fallingCubes;
    private BiMap<CFRegion, EspressoTerm> highCubes;
    private BiMap<CFRegion, EspressoTerm> lowCubes;

    public Cubes(BiMap<CFRegion, EspressoTerm> risingCubes, BiMap<CFRegion, EspressoTerm> fallingCubes) {
        this.risingCubes = risingCubes;
        this.fallingCubes = fallingCubes;
    }

    public Cubes(BiMap<CFRegion, EspressoTerm> risingCubes, BiMap<CFRegion, EspressoTerm> fallingCubes, BiMap<CFRegion, EspressoTerm> highCubes, BiMap<CFRegion, EspressoTerm> lowCubes) {
        this.risingCubes = risingCubes;
        this.fallingCubes = fallingCubes;
        this.highCubes = highCubes;
        this.lowCubes = lowCubes;
    }

    public BiMap<CFRegion, EspressoTerm> getFallingCubes() {
        return fallingCubes;
    }

    public BiMap<CFRegion, EspressoTerm> getRisingCubes() {
        return risingCubes;
    }

    public BiMap<CFRegion, EspressoTerm> getHighCubes() {
        return highCubes;
    }

    public BiMap<CFRegion, EspressoTerm> getLowCubes() {
        return lowCubes;
    }

    @Override
    public String toString() {
        //@formatter:off
        return
            "\n\tRising: " + risingCubes.toString() + 
            "\n\tFalling: " + fallingCubes.toString() + 
            "\n\tHigh: " + ((highCubes != null) ? highCubes.toString() : "X") +
            "\n\tLow: " + ((lowCubes != null) ? lowCubes.toString() : "X");
        //@formatter:on
    }
}
