package de.uni_potsdam.hpi.asg.logictool.srgraph;

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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class SimulationStepFactory extends BasePooledObjectFactory<SimulationStep> {

    @Override
    public SimulationStep create() throws Exception {
        return new SimulationStep();
    }

    @Override
    public PooledObject<SimulationStep> wrap(SimulationStep obj) {
        return new DefaultPooledObject<SimulationStep>(obj);
    }

    @Override
    public void passivateObject(PooledObject<SimulationStep> p) throws Exception {
        p.getObject().getMarking().clear();
        p.getObject().setFireTrans(null);
        p.getObject().setState(null);
    }
}
