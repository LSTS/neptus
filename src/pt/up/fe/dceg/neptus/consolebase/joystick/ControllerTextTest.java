/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Rui Gonçalves
 * 2009/09/23
 * $Id:: ControllerTextTest.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.consolebase.joystick;

/**
 * @author Rui Gonçalves
 *
 */

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class ControllerTextTest {
    ControllerEnvironment ce;
    /** Creates a new instance of ControllerScanner */
    public ControllerTextTest() {
        ce = ControllerEnvironment.getDefaultEnvironment();
        System.out.println("Controller Env = "+ce.toString());
        
        
        Controller[] ca = ce.getControllers();
        for(int i =0;i<ca.length;i++){
            System.out.println(ca[i].getName());
            System.out.println("Type: "+ca[i].getType().toString());
            Component[] components = ca[i].getComponents();
            System.out.println("Component Count: "+components.length);
            for(int j=0;j<components.length;j++){
                System.out.println("Component "+j+": "+components[j].getName());
                System.out.println("    Identifier: "+
                        components[j].getIdentifier().getName());
                System.out.print("    ComponentType: ");
                if (components[j].isRelative()) {
                    System.out.print("Relative");
                } else {
                    System.out.print("Absolute");
                }
                if (components[j].isAnalog()) {
                    System.out.print(" Analog");
                } else {
                    System.out.print(" Digital");
                }
            }
            System.out.println("---------------------------------");
        }
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ControllerTextTest();
    }
}
