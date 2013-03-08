/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 18, 2012
 * $Id:: NeptusFactory.java 9875 2013-02-06 15:38:31Z zepinto                   $:
 */
package pt.up.fe.dceg.neptus;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.configuration.PropertiesConfiguration;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.systems.SystemsManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;

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
