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
 */
package pt.up.fe.dceg.neptus;

import pt.up.fe.dceg.neptus.systems.SystemsManager;

/**
 * @author Hugo
 * 
 */
public class Main {

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
       
        NeptusFactory factory;
        try {
            factory = new NeptusFactory();
            factory.config()
                                    .loadSchemas()
                                    .setupLog();
            NeptusLog.pub().warn("test");
            SystemsManager systems = factory.systems();           
        }
        catch (Exception e1) {
            NeptusLog.pub().error(e1);
            System.exit(1);
        }
    }
}
