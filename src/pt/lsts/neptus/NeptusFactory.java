/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Hugo Dias
 * Oct 18, 2012
 */
package pt.lsts.neptus;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.configuration.PropertiesConfiguration;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.systems.SystemsManager;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.imc.IMCDefinition;

/**
 * @author Hugo
 * 
 */
public class NeptusFactory {
    public static final String CONFIG_FILE = "conf/config.properties";

    private final PropertiesConfiguration config;
    private final IMCDefinition imcDefinition;

    private final NeptusConfig neptusConfig;
    private final SystemsManager systems;
    private final ImcMsgManager imcManager;
    

    public NeptusFactory() throws Exception {

        config = new PropertiesConfiguration(CONFIG_FILE);
        neptusConfig = new NeptusConfig(config);
        new NeptusProperties(neptusConfig)
                            .load(config.getString("core.general-properties"));

//        GeneralPreferencesNew.setPropertiesLoader(neptusProperties);
        GeneralPreferences.generateImcId();
        
        systems = new SystemsManager(neptusConfig);
        
        imcDefinition = new IMCDefinition(new FileInputStream(new File(config.getString("core.imc"))));
        imcManager = new ImcMsgManager(imcDefinition);
        imcManager.start();
        
        ImcMsgManager.registerBusListener(systems);
        
    }

    public NeptusConfig config() {
        return neptusConfig;
    }

    /**
     * @return the systems
     */
    public SystemsManager systems() {
        return systems;
    }

    public IMCDefinition getIMCDefinition() {
        return imcDefinition;
    }
}
