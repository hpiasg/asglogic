package de.uni_potsdam.hpi.asg.logictool.io.invoker;

/*
 * Copyright (C) 2017 - 2018 Norman Kluge
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
import java.util.Arrays;
import java.util.List;

import de.uni_potsdam.hpi.asg.common.invoker.ExternalToolsInvoker;
import de.uni_potsdam.hpi.asg.common.invoker.InvokeReturn;
import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;

public class EspressoInvoker extends ExternalToolsInvoker {

    private EspressoInvoker() {
        super("espresso");
    }

    public static InvokeReturn optimise(File inFile, File outFile, boolean exact, int timeout) {
        return new EspressoInvoker().internalOptimise(inFile, outFile, exact, timeout);
    }

    private InvokeReturn internalOptimise(File inFile, File outFile, boolean exact, int timeout) {
        List<String> params = new ArrayList<>();
        //@formatter:off
        params.addAll(Arrays.asList(
            "-of", "-eonset", "-Dso" //"-S1", // "-Dexact", //, //"-Dexact", //"-Dso","-Dso", "-S1" 
        ));
        //@formatter:on
        if(exact) {
            params.add("-S1");
        }
        params.add(inFile.getName());

        addInputFilesToCopy(inFile);

        setTimeout(timeout);
        InvokeReturn ret = run(params, "espresso_" + inFile.getName());
        errorHandling(ret);
        if(!ret.getResult()) {
            return ret;
        }
        if(!FileHelper.getInstance().writeFile(outFile, ret.getOutputStr())) {
            ret.setResult(false);
        }
        return ret;
    }
}
