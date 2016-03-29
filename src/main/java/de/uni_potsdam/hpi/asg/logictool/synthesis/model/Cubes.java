package de.uni_potsdam.hpi.asg.logictool.synthesis.model;

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

import com.google.common.collect.BiMap;

public class Cubes {

    private BiMap<CFRegion, EspressoTerm> risingCubes;
    private BiMap<CFRegion, EspressoTerm> fallingCubes;

    public Cubes(BiMap<CFRegion, EspressoTerm> risingCubes, BiMap<CFRegion, EspressoTerm> fallingCubes) {
        this.risingCubes = risingCubes;
        this.fallingCubes = fallingCubes;
    }

    public BiMap<CFRegion, EspressoTerm> getFallingCubes() {
        return fallingCubes;
    }

    public BiMap<CFRegion, EspressoTerm> getRisingCubes() {
        return risingCubes;
    }

    @Override
    public String toString() {
        return "\n\tRising: " + risingCubes.toString() + "\n\tFalling: " + fallingCubes.toString();
    }
}
