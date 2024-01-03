/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Mar 31, 2015
 */
package com.inovaworks;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.Gson;

import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class Observation {

    public String id = UUID.randomUUID().toString();
    public FeatureOfInterest featureOfInterest = new FeatureOfInterest();
    public long phenomenonTime = System.currentTimeMillis();
    public long observationTime = System.currentTimeMillis();
    public ArrayList<ObservedProperty> observedProperties = new ArrayList<ObservedProperty>(); 
    public Procedure procedure = new Procedure();
    
    private static Gson gson = null;
    
    public void addProperty(ObservedProperty prop) {
        observedProperties.add(prop);
    }
    
    public static class FeatureOfInterest {
        public String type = "Unknown";
        public String id = "n/a";
    }
    
    public static class Procedure {
        // type: "reading" or type: "calculated" or type: "fusion"
        public enum ObservationTypeEnum { reading, calculated, fusion };

        public String type = ObservationTypeEnum.reading.toString();
        public String sensor = "";
        public String sensorHumanName = "";    
    }
   
    
    public static Observation parseJSON(Reader jsonReader) {
        synchronized (Observation.class) {
            if (gson == null)
                gson = new Gson();
        }
        return gson.fromJson(jsonReader, Observation.class);
    }
    
    public static Observation parseJSON(String json) {
        return parseJSON(new StringReader(json));
    }

    public String toJSON() {
        synchronized (Observation.class) {
            if (gson == null)
                gson = new Gson();
        }
        return gson.toJson(this);
    }
    
    public static void main(String[] args) throws Exception {
        Observation o = Observation
                .parseJSON(new FileReader(new File(FileUtil.getResourceAsFileKeepName("com/inovaworks/example.json"))));
        System.out.println(o.toJSON());
    }
}
