package de.uni_potsdam.hpi.asg.logictool.synthesis;

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

import java.io.File;

import de.uni_potsdam.hpi.asg.common.invoker.InvokeReturn;
import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.logictool.io.invoker.EspressoExactWrapper;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable;
import de.uni_potsdam.hpi.asg.logictool.synthesis.model.EspressoTable.TableType;

public class EspressoOptimiser {

    public EspressoTable espressoMinimization(EspressoTable table) {
        File in = FileHelper.getInstance().newTmpFile("espresso_in.txt");
        File out = FileHelper.getInstance().newTmpFile("espresso_out.txt");
        table.setType(TableType.fr);
        table.exportToFile(in);

        InvokeReturn ret = EspressoExactWrapper.optimise(in, out);
        if(ret == null || !ret.getResult()) {
            return null;
        }

        return EspressoTable.importFromFile(out);
    }
}
