package de.uni_potsdam.hpi.asg.logictool.stg;

/*
 * Copyright (C) 2014 - 2016 Norman Kluge
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni_potsdam.hpi.asg.common.io.FileHelper;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Place;
import de.uni_potsdam.hpi.asg.logictool.stg.model.STG;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Signal.SignalType;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition;
import de.uni_potsdam.hpi.asg.logictool.stg.model.Transition.Edge;

public class GFile {
    private static final Logger  logger              = LogManager.getLogger();

    private static final Pattern markingPattern      = Pattern.compile(".marking\\s*\\{\\s*(.*)\\s*\\}\\s*");
    private static final Pattern transPattern        = Pattern.compile("(\\w+)([+-])/?(\\d*)");
    private static final Pattern dummyPattern        = Pattern.compile("(\\w+)/?(\\d*)");
    private static final Pattern markingTransPattern = Pattern.compile("<(.*),(.*)>");
    private static final Pattern interfacePattern    = Pattern.compile("(.inputs|.outputs|.internal|.dummy)\\s*(.*)");

    private static int           tmpPlaceId          = 0;

    public static STG importFromFile(File file) {
        STG retVal = null;
        if((retVal = readGFile(file)) != null) {
            return retVal;
        } else {
            logger.error("Could not read file: " + file.getAbsolutePath());
        }
        return null;
    }

    private static STG readGFile(File file) {
        boolean inputs = false;
        boolean outputs = false;
        boolean dummies = false;
        boolean internals = false;
        boolean markings = false;
        boolean graphlines = false;

        try {
            logger.debug("Reading G-file " + file.getCanonicalPath());
        } catch(IOException e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
        STG stg = new STG(file);

        boolean graphmode = false;
        boolean coordmode = false;
        List<String> lines = FileHelper.getInstance().readFile(file);
        if(lines == null) {
            logger.error("File not found: " + file.getAbsolutePath());
            return null;
        }
        for(String line : lines) {
            if(line.startsWith("#") || line.startsWith(".model") || line.equals("")) {
            } else if(line.startsWith(".inputs")) {
                graphmode = false;
                coordmode = false;
                if(!inputs) {
                    for(String str : parseTypeLine(line)) {
                        stg.addSignal(str, SignalType.input);
                    }
                    inputs = true;
                } else {
                    logger.error("Input section doubled in " + file.getAbsolutePath());
                    return null;
                }
            } else if(line.startsWith(".outputs")) {
                graphmode = false;
                coordmode = false;
                if(!outputs) {
                    for(String str : parseTypeLine(line)) {
                        stg.addSignal(str, SignalType.output);
                    }
                    outputs = true;
                } else {
                    logger.error("Output section doubled in " + file.getAbsolutePath());
                    return null;
                }
            } else if(line.startsWith(".dummy")) {
                graphmode = false;
                coordmode = false;
                if(!dummies) {
                    for(String str : parseTypeLine(line)) {
                        stg.addSignal(str, SignalType.dummy);
                    }
                    dummies = true;
                } else {
                    logger.error("Dummy section doubled in " + file.getAbsolutePath());
                    return null;
                }
            } else if(line.startsWith(".internal")) {
                graphmode = false;
                coordmode = false;
                if(!internals) {
                    for(String str : parseTypeLine(line)) {
                        stg.addSignal(str, SignalType.internal);
                    }
                    internals = true;
                } else {
                    logger.error("Internal section doubled in " + file.getAbsolutePath());
                    return null;
                }
            } else if(line.startsWith(".graph")) {
                if(!graphlines) {
                    graphmode = true;
                    coordmode = false;
                    graphlines = true;
                } else {
                    logger.error("Graph section doubled in " + file.getAbsolutePath());
                    return null;
                }
            } else if(line.startsWith(".marking")) {
                graphmode = false;
                coordmode = false;
                if(!markings) {
                    parseMarking(stg, line);
                } else {
                    logger.error("Marking section doubled in " + file.getAbsolutePath());
                    return null;
                }
            } else if(line.startsWith(".coordinates")) {
                coordmode = true;
                graphmode = false;
            } else if(line.startsWith(".end")) {
                break;
            } else if(graphmode) {
                if(!parseTransition(stg, line)) {
                    System.err.println("Could not parse line: " + line);
                    return null;
                }
            } else if(coordmode) {
            } else {
                logger.warn("Not interpreted: " + line);
            }
        }

        return stg;
    }

    private static List<String> parseTypeLine(String line) {
        Matcher m = interfacePattern.matcher(line);
        if(m.matches()) {
            return Arrays.asList(m.group(2).split(" "));
        } else {
            System.err.println("InterfacePattern does not match");
        }
        return null;
    }

    private static boolean parseTransition(STG stg, String line) {
        Matcher m = null;
        Matcher m2 = null;
        Transition firstT = null;
        Place firstP = null;
        Transition trans = null;
        Place p = null;

        String[] split = line.split(" ");

        m = transPattern.matcher(split[0]);
        m2 = dummyPattern.matcher(split[0]);
        if(m.matches()) {
            firstT = stg.getTransitionOrAdd(m.group(1), getEdge(m.group(2)), getId(m.group(3)));
        } else if(m2.matches()) {
            if(stg.getSignal(m2.group(1)) != null) {
                firstT = stg.getTransitionOrAdd(m2.group(1), null, getId(m2.group(2)));
            } else {
                firstP = stg.getPlaceOrAdd(split[0]);
            }
        } else {
            System.err.println("First element in line " + line + " could not be parsed");
        }

        for(int i = 1; i < split.length; i++) {
            m = transPattern.matcher(split[i]);
            m2 = dummyPattern.matcher(split[i]);

            // Transition?
            trans = null;
            p = null;
            if(m.matches()) {
                trans = stg.getTransitionOrAdd(m.group(1), getEdge(m.group(2)), getId(m.group(3)));
            } else if(m2.matches()) { //m2 matches
                if(stg.getSignal(m2.group(1)) != null) {
                    trans = stg.getTransitionOrAdd(m2.group(1), null, getId(m2.group(2)));
                } else {
                    p = stg.getPlaceOrAdd(split[i]);
                }
            } else {
                logger.error("Element " + i + " in line " + line + " could not be parsed");
            }

            if(trans != null) {
                if(firstT != null) {
                    String id = "tmpP" + tmpPlaceId++;
                    p = new Place(id);
                    stg.addPlace(id, p);
                    firstT.addPostPlace(p);
                    trans.addPrePlace(p);
                    p.addPreTransition(firstT);
                    p.addPostTransition(trans);
                } else if(firstP != null) {
                    trans.addPrePlace(firstP);
                    firstP.addPostTransition(trans);
                } else {
                    logger.error("First Element is null in line " + line);
                }
            } else if(p != null) {
                if(firstT != null) {
                    firstT.addPostPlace(p);
                    p.addPreTransition(firstT);
                } else if(firstP != null) {
                    System.err.println("Place connected to a place? " + line);
                } else {
                    logger.error("First Element is null in line " + line);
                }
            } else {
                logger.error("Element " + i + " in line " + line + " is null");
            }
        }
        return true;
    }

    private static int getId(String str) {
        return (str.equals("")) ? 0 : Integer.parseInt(str);
    }

    private static Edge getEdge(String str) {
        if(str.equals("+")) {
            return Edge.rising;
        } else if(str.equals("-")) {
            return Edge.falling;
        } else {
            logger.error("Unknown edge: " + str);
            return null;
        }
    }

    private static void parseMarking(STG stg, String line) {
        List<Place> markedPlaces = new ArrayList<Place>();
        Matcher m0 = markingPattern.matcher(line);
        if(m0.matches()) {
            String[] splitted = m0.group(1).split(" ");
            Matcher m = null;
            Matcher m2 = null;
            Matcher m3 = null;
            for(String str : splitted) {
                m = markingTransPattern.matcher(str);
                if(m.matches()) {
                    Transition t1 = null, t2 = null;
                    m2 = transPattern.matcher(m.group(1));
                    m3 = dummyPattern.matcher(m.group(1));
                    if(m2.matches()) {
                        t1 = stg.getTransition(m2.group(1), getEdge(m2.group(2)), getId(m2.group(3)));
                    } else if(m3.matches()) {
                        t1 = stg.getTransition(m3.group(1), null, getId(m3.group(2)));
                    } else {
                        logger.error("Marking element #1 not parseable from marking " + str);
                    }
                    m2 = transPattern.matcher(m.group(2));
                    m3 = dummyPattern.matcher(m.group(2));
                    if(m2.matches()) {
                        t2 = stg.getTransition(m2.group(1), getEdge(m2.group(2)), getId(m2.group(3)));
                    } else if(m3.matches()) {
                        t2 = stg.getTransition(m3.group(1), null, getId(m3.group(2)));
                    } else {
                        logger.error("Marking element #2 not parseable from marking " + str);
                    }
                    Place place = null;
                    for(Place p : t1.getPostset()) {
                        for(Place p2 : t2.getPreset()) {
                            if(p == p2) {
                                place = p;
                                break;
                            }
                        }
                        if(place != null) {
                            break;
                        }
                    }
                    if(place == null) {
                        logger.error("Transitions of marking have no place in between: " + str);
                    }
                    markedPlaces.add(place);
                } else {
                    Place p = stg.getPlace(str);
                    if(p == null) {
                        logger.error("Place for marking not found: " + str);
                    }
                    markedPlaces.add(p);
                }
            }
            stg.setInitMarking(markedPlaces);
        } else {
            logger.error("Not a valid marking set: " + line);
            return;
        }
    }

    public static boolean writeGFile(STG stg, File file) {
        StringBuilder text = new StringBuilder();
        String newline = FileHelper.getNewline();
        text.append("#Generated by some ASG tool " + new Date().toString() + newline + newline);
        Set<Signal> inputs = new HashSet<Signal>();
        Set<Signal> outputs = new HashSet<Signal>();
        Set<Signal> internals = new HashSet<Signal>();
        Set<Signal> dummies = new HashSet<Signal>();
        for(Signal sig : stg.getSignals()) {
            switch(sig.getType()) {
                case dummy:
                    dummies.add(sig);
                    break;
                case input:
                    inputs.add(sig);
                    break;
                case internal:
                    internals.add(sig);
                    break;
                case output:
                    outputs.add(sig);
                    break;
                default:
            }
        }
        if(!inputs.isEmpty()) {
            text.append(".inputs");
            for(Signal sig : inputs) {
                text.append(" " + sig.getName());
            }
            text.append(newline);
        }
        if(!outputs.isEmpty()) {
            text.append(".outputs");
            for(Signal sig : outputs) {
                text.append(" " + sig.getName());
            }
            text.append(newline);
        }
        if(!internals.isEmpty()) {
            text.append(".internal");
            for(Signal sig : internals) {
                text.append(" " + sig.getName());
            }
            text.append(newline);
        }
        if(!dummies.isEmpty()) {
            text.append(".dummy");
            for(Signal sig : dummies) {
                text.append(" " + sig.getName());
            }
            text.append(newline);
        }
        text.append(newline);
        text.append(".graph" + newline);
        for(Entry<String, Place> entry : stg.getPlaces().entrySet()) {
            if(!entry.getValue().getPostset().isEmpty()) {
                text.append(entry.getKey());
                for(Transition t : entry.getValue().getPostset()) {
                    text.append(" " + t.outputForGFile());
                }
                text.append(newline);
            }
        }
        for(Transition t : stg.getTransitions()) {
            if(!t.getPostset().isEmpty()) {
                text.append(t.outputForGFile());
                for(Place p : t.getPostset()) {
                    text.append(" " + p.outputForGFile());
                }
                text.append(newline);
            }
        }
        text.append(newline);
        text.append(".marking { ");
        for(Place p : stg.getInitMarking()) {
            text.append(p.outputForGFile() + " ");
        }
        text.append("}" + newline);
        text.append(".end");

        return FileHelper.getInstance().writeFile(file, text.toString());
    }
}
