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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import de.uni_potsdam.hpi.asg.common.iohelper.FileHelper;
import de.uni_potsdam.hpi.asg.common.stg.model.Signal;

public class EspressoTable {
    private static final Logger  logger              = LogManager.getLogger();

    private static final String  linebreak           = System.getProperty("line.separator");

    private static final Pattern inputsPattern       = Pattern.compile(".i\\s*(\\d+)");
    private static final Pattern outputsPattern      = Pattern.compile(".o\\s*(\\d+)");
    private static final Pattern inputNamePattern    = Pattern.compile(".ilb\\s*(.+)");
    private static final Pattern outputNamePattern   = Pattern.compile(".ob\\s*(.+)");
    private static final Pattern tableEntriesPattern = Pattern.compile(".p\\s*(\\d+)");
    private static final Pattern tablePattern        = Pattern.compile("\\s*([01-]+)\\s*([01]+)");
    private static final Pattern endPattern          = Pattern.compile(".e");

    public enum EspressoValue {
        zero, one, dontcare
    }

    public enum TableType {
        fr, none
    }

    private TableType                                  type;
    private String[]                                   inputs;
    private Table<EspressoTerm, String, EspressoValue> table;

    public EspressoTable(String[] inputs, Table<EspressoTerm, String, EspressoValue> table) {
        this.table = table;
        this.inputs = inputs;
        this.type = TableType.none;
    }

    public boolean exportToFile(File file) {
        StringBuilder str = new StringBuilder();

        // num in/outputs
        int num = inputs.length;
        int numOuts = table.columnKeySet().size();
        str.append(".i " + num + linebreak + ".o " + numOuts + linebreak);

        //input names
        str.append(".ilb ");
        for(String in : inputs) {
            str.append(in + " ");
        }
        str.replace(str.length() - 1, str.length(), linebreak);

        // output names
        SortedSet<String> sortedOutputs = new TreeSet<String>(table.columnKeySet());
        str.append(".ob ");
        for(String out : sortedOutputs) {
            str.append(out + " ");
        }
        str.replace(str.length() - 1, str.length(), linebreak);

        // incomplete table
        switch(type) {
            case fr:
                str.append(".type fr" + linebreak);
                break;
            case none:
        }

        // table begin
        int p = table.rowKeySet().size();
        str.append(".p " + p + linebreak);
        for(Entry<EspressoTerm, Map<String, EspressoValue>> row : table.rowMap().entrySet()) {
            str.append(row.getKey().getLine() + " ");
            for(String out : sortedOutputs) {
                EspressoValue val = (row.getValue().get(out) == null) ? EspressoValue.dontcare : row.getValue().get(out);
                switch(val) {
                    case one:
                        str.append("1");
                        break;
                    case zero:
                        str.append("0");
                        break;
                    case dontcare:
                        str.append("-");
                        break;
                    default:
                        logger.warn("Unknwon EspressoValue: " + val);
                }
            }
            str.append(linebreak);
        }

        // finish
        str.append(".e");

        return FileHelper.getInstance().writeFile(file, str.toString());
    }

    public static EspressoTable importFromFile(File file) {
        List<String> lines = FileHelper.getInstance().readFile(file);
        if(lines == null) {
            logger.error("Could not read-in results from espresso");
            return null;
        }

        String[] inputs = null;
        String[] outputs = null;
        Table<EspressoTerm, String, EspressoValue> table = HashBasedTable.create();

        Matcher m = null;
        for(String str : lines) {
            m = tablePattern.matcher(str);
            if(m.matches()) {
                if(outputs != null && inputs != null) {
                    EspressoTerm t = new EspressoTerm(m.group(1), inputs);
                    int i = 0;
                    for(String out : outputs) {
                        switch(m.group(2).charAt(i)) {
                            case '0':
                                table.put(t, out, EspressoValue.zero);
                                break;
                            case '1':
                                table.put(t, out, EspressoValue.one);
                                break;
                            case '-':
                                table.put(t, out, EspressoValue.dontcare);
                                break;
                            default:
                                System.err.println("Unknown char in table: " + m.group(2).charAt(i));
                        }
                        i++;
                    }
                } else {
                    System.err.println("No in/outputs");
                    return null;
                }
                continue;
            }
            m = inputsPattern.matcher(str);
            if(m.matches()) {
                continue;
            }
            m = outputsPattern.matcher(str);
            if(m.matches()) {
                continue;
            }
            m = inputNamePattern.matcher(str);
            if(m.matches()) {
                inputs = m.group(1).split(" ");
                continue;
            }
            m = outputNamePattern.matcher(str);
            if(m.matches()) {
                outputs = m.group(1).split(" ");
                continue;
            }
            m = tableEntriesPattern.matcher(str);
            if(m.matches()) {
                continue;
            }
            m = endPattern.matcher(str);
            if(m.matches()) {
                break;
            }

            System.err.println("No match: " + str);
        }

        return new EspressoTable(inputs, table);
    }

    public void mergeIn(EspressoTable tmptable, SortedSet<Signal> signals) {
        for(Signal sig : signals) {
            table.column(sig.getName()).clear();
            for(Entry<EspressoTerm, EspressoValue> entry : tmptable.getTable().column(sig.getName()).entrySet()) {
                table.put(entry.getKey(), sig.getName(), entry.getValue());
            }
        }

    }

    public Table<EspressoTerm, String, EspressoValue> getTable() {
        return table;
    }

    public String[] getInputs() {
        return inputs;
    }

    public void setType(TableType type) {
        this.type = type;
    }
}
