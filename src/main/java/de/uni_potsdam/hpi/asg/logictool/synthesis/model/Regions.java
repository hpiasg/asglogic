package de.uni_potsdam.hpi.asg.logictool.synthesis.model;

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

import java.util.List;

public class Regions {

    private List<CFRegion> risingRegions;
    private List<CFRegion> fallingRegions;

    public Regions(List<CFRegion> risingRegions, List<CFRegion> fallingRegions) {
        this.risingRegions = risingRegions;
        this.fallingRegions = fallingRegions;
    }

    public List<CFRegion> getFallingRegions() {
        return fallingRegions;
    }

    public List<CFRegion> getRisingRegions() {
        return risingRegions;
    }
}
