package de.uni_potsdam.hpi.asg.logictool.io;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@XmlRootElement(name = "logicconfig")
@XmlAccessorType(XmlAccessType.NONE)
public class Config {
    private static final Logger logger = LogManager.getLogger();

    //@formatter:off
    
    @XmlElement(name = "tools", required = true)
    public ToolConfig toolconfig;
    @XmlElement(name = "workdir", required = false)
    public String  workdir;
    
    //@formatter:on

    public static Config readIn(File file) {
        try {
            if(file.exists()) {
                JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                return (Config)jaxbUnmarshaller.unmarshal(file);
            } else {
                logger.error("File " + file.getAbsolutePath() + " not found");
                return null;
            }
        } catch(JAXBException e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    public static boolean writeOut(Config cfg, String filename) {
        try {
            Writer fw = new FileWriter(filename);
            JAXBContext context = JAXBContext.newInstance(Config.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(cfg, fw);
            return true;
        } catch(JAXBException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        } catch(IOException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }
}
