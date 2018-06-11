package de.uni_potsdam.hpi.asg.logictool.io.invoker;

/*
 * Copyright (C) 2018 Norman Kluge
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.invoker.InvokeReturn;

public class EspressoExactWrapper {
    private static final Logger logger = LogManager.getLogger();

    public static InvokeReturn optimise(File inFile, File outFile) {
        InvokeReturn ret = EspressoInvoker.optimise(inFile, outFile, true, 10000);
        if(ret == null || !ret.getResult()) {
            logger.info("Espresso exact failed (Timeout?). Try heuristic solution");
            return EspressoInvoker.optimise(inFile, outFile, false, 0);
        }
        return ret;
    }
}
