package de.uni_potsdam.hpi.asg.logictool.techfile;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model.TechTerm;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.model.TechVariable;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.parser.BooleanExpressionParser;
import de.uni_potsdam.hpi.asg.logictool.techfile.booleanparser.parser.ParseException;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class TechLibrary {
    private static final Logger          logger       = LogManager.getLogger();

    private static final Pattern         gatepattern  = Pattern.compile("\\s*GATE\\s*\\\"(.*)\\\"\\s*([0-9.]+)\\s*(.*)=(.*;)");
    private static final Pattern         latchpattern = Pattern.compile("\\s*LATCH\\s*\\\"(.*)\\\"\\s*([0-9.]+)\\s*(.*)=(.*;)");
    private static final Pattern         seqpattern   = Pattern.compile("\\s*SEQ\\s+(\\w+)\\s+(\\w+)\\s+(\\w+)");

    private Set<Gate>                    gates;
    private BiMap<Integer, TechVariable> vars;

    private TechLibrary(BiMap<Integer, TechVariable> vars, Set<Gate> gates) {
        this.vars = vars;
        this.gates = gates;
    }

    public static TechLibrary importFromFile(File file, BDDFactory factory) {
        List<String> raw = FileHelper.getInstance().readFile(file);
        if(raw == null) {
            logger.error("Could not read gate library");
            return null;
        }
        BiMap<TechVariable, Integer> varmap = HashBiMap.create();
        Set<Gate> gates = new HashSet<>();
        Matcher m = null, m2 = null;
        BooleanExpressionParser p = null;
        TechTerm t = null;
        BDD bdd = null;
        String str = null;
        Iterator<String> it = raw.iterator();
        int line = 0;
        while(it.hasNext()) {
            str = it.next();
            line++;
            m = gatepattern.matcher(str);
            if(m.matches()) {
                p = new BooleanExpressionParser(new ByteArrayInputStream(m.group(4).getBytes()));
                try {
                    t = p.parse();
                } catch(ParseException e) {
                    logger.error("Boolean parser: " + e.getLocalizedMessage().replace("\n", " ") + "\n");
                    return null;
                }
                bdd = t.toBDD(factory, varmap);
                gates.add(new Gate(m.group(1), bdd, Float.parseFloat(m.group(2)), m.group(3), null));
            } else {
                m = latchpattern.matcher(str);
                if(m.matches()) {
                    p = new BooleanExpressionParser(new ByteArrayInputStream(m.group(4).getBytes()));
                    try {
                        t = p.parse();
                    } catch(ParseException e) {
                        logger.error("Boolean parser: " + e.getLocalizedMessage().replace("\n", " ") + "\n");
                        return null;
                    }
                    bdd = t.toBDD(factory, varmap);
                    if(!it.hasNext()) {
                        logger.error("Missing Seq statement for Latch (Line" + line + ")");
                        return null;
                    }
                    str = it.next();
                    line++;
                    m2 = seqpattern.matcher(str);
                    if(m2.matches()) {
                        if(m2.group(3).equals("ASYNCH")) {
                            if(m.group(3).equals(m2.group(1))) { //output in first line is same as in seq
                                TechVariable loopback = BooleanExpressionParser.vars.get(m2.group(2));
                                if(loopback != null) {
                                    gates.add(new Gate(m.group(1), bdd, Float.parseFloat(m.group(2)), m.group(3), loopback));
                                } else {
                                    logger.warn("Loopback Variable not found");
                                }
                            } else {
                                logger.warn("Wrong output in Seq Statement (Line" + line + ")");
                            }
                        }
                    } else {
                        logger.warn("Missing Seq statement for Latch (Line" + (line - 1) + ")");
                        continue;
                    }
                }
            }
        }

        return new TechLibrary(varmap.inverse(), gates);
    }

    public Set<Gate> getGates() {
        return gates;
    }

    public BiMap<Integer, TechVariable> getVars() {
        return vars;
    }
}
