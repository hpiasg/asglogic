package de.uni_potsdam.hpi.asg.logictool.io;

/*
 * Copyright (C) 2015 - 2017 Norman Kluge
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "logicconfig")
@XmlAccessorType(XmlAccessType.NONE)
public class Config {

    //@formatter:off
    
    @XmlElement(name = "tools", required = true)
    public ToolConfig toolconfig;
    @XmlElement(name = "workdir", required = false)
    public String  workdir;
    
    //@formatter:on
}
