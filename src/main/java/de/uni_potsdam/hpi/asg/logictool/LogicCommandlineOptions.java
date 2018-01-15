package de.uni_potsdam.hpi.asg.logictool;

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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import de.uni_potsdam.hpi.asg.common.iohelper.CommandlineOptions;
import de.uni_potsdam.hpi.asg.common.misc.CommonConstants;

public class LogicCommandlineOptions extends CommandlineOptions {

    public boolean parseCmdLine(String[] args) {
        return super.parseCmdLine(args, "Usage: ASGlogic [options] <gfile>\nOptions:");
    }

    //@formatter:off

    @Option(name = "-lib", metaVar = "<technologyfile>", usage = "Technology library for technology mapping (GenLib format)")
    private File technology;
    @Option(name = "-out", metaVar = "<file>", usage = "Synthesis outfile, default is logic" + CommonConstants.VERILOG_FILE_EXTENSION)
    private File synthesisOutfile = new File(System.getProperty("user.dir") + File.separator + "logic" + CommonConstants.VERILOG_FILE_EXTENSION);

    @Option(name = "-csc", metaVar = "<toolcode>", usage = "External tool to solve csc: M: mpsat, [N]: none, P: petrify")
    private String cscSolving = "N";

    @Option(name = "-o", metaVar = "<level>", usage = "Outputlevel: 0:nothing\n1:errors\n2:+warnings\n[3:+info]")
    private int outputlevel = 3;
    @Option(name = "-log", metaVar = "<logfile>", usage = "Define output Logfile, default is logic" + CommonConstants.LOG_FILE_EXTENSION)
    private File logfile = new File(System.getProperty("user.dir"), "logic" + CommonConstants.LOG_FILE_EXTENSION);
    @Option(name = "-debug")
    private boolean debug = false;
    @Option(name = "-tooldebug")
    private boolean tooldebug = false;
    
    @Option(name = "-zip", metaVar = "<zipfile>", usage = "Zip outfile with all temporary files, default is logic" + CommonConstants.ZIP_FILE_EXTENSION)
    private File workfile = new File(System.getProperty("user.dir"), "logic" + CommonConstants.ZIP_FILE_EXTENSION);
    @Option(name = "-cfg", metaVar = "<configfile>", usage = "Config file, default is " + LogicMain.DEF_CONFIG_FILE_NAME)
    private File configfile = LogicMain.DEF_CONFIG_FILE;
    @Option(name = "-toolcfg", metaVar = "<configfile>", usage = "External tools config file, default is " + LogicMain.DEF_TOOL_CONFIG_FILE_NAME)
    private File toolconfigfile = LogicMain.DEF_TOOL_CONFIG_FILE;
    @Option(name = "-w", metaVar = "<workingdir>", usage = "Working directory. If not given, the value in configfile is used. If there is no entry, 'logicwork*' in the os default tmp dir is used.")
    private File workingdir = null;

    @Option(name = "-arch", metaVar = "<Arch>", usage = "Architecture: [gC], sC")
    private String arch = "gC";
    @Option(name = "-rst", metaVar = "<Type>", usage = "ResetType:\n[ondemand]: reset is inserted, if signal is not self-reseting\nfull: reset logic is inserted for all signals")
    private String resettype = "ondemand";

    @Option(name = "-unsafeanddeco")
    private boolean unsafeanddeco = false;

    @Argument(metaVar = "STG in g format", required = true)
    private File gfile;

    //@formatter:on

    public int getOutputlevel() {
        return outputlevel;
    }

    public File getLogfile() {
        return logfile;
    }

    public File getSynthesisOutfile() {
        return synthesisOutfile;
    }

    public File getConfigfile() {
        return configfile;
    }

    public File getWorkfile() {
        return workfile;
    }

    public boolean isDebug() {
        return debug;
    }

    public File getGfile() {
        return gfile;
    }

    public File getTechnology() {
        return technology;
    }

    public File getWorkingdir() {
        return workingdir;
    }

    public String getArch() {
        return arch;
    }

    public String getResettype() {
        return resettype;
    }

    public boolean isUnsafeanddeco() {
        return unsafeanddeco;
    }

    public String getCscSolving() {
        return cscSolving;
    }

    public File getToolConfigFile() {
        return toolconfigfile;
    }

    public boolean isTooldebug() {
        return tooldebug;
    }
}
