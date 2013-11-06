/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: canasta
 * Feb 21, 2011
 */
package pt.up.fe.dceg.neptus.plugins.uavs;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.plugins.SimpleSubPanel;

/**
 * @author canasta
 *
 */
public class UavLib {
    
    private final static String SUPERCLASS_NAME = "ContainerSubPanel";
        
    //Being private prevents java's Initialize command     
    private UavLib(){        
    }
    
    //------Specific Methods------//
    
    /**
     * Given the vehicle's current pitch a draw inclination is return to the caller 
     * @param num
     * @return double
     */
    public static double calculateDrawPitch(double num, double pitchTheshold, double drawAngle){
        
        if(num > -pitchTheshold && num < pitchTheshold)
            return 0;
        else if(num < -pitchTheshold)
            return Math.toRadians(drawAngle);
        else
            return Math.toRadians(-drawAngle);                
    }    
    
    /**
     * Given a Panel class this method returns all instances of that panel, present in the given console. This method basis its search
     * on the premise that Container Panels have a common superclass: ContainerSubPanel 
     * @param panelType
     * @param console
     * @return 
     * @return Vector<Object>
     */
    public static SimpleSubPanel findPanelInConsole(String name, ConsoleLayout console){  
                      
        for(int i = 0; i < console.getComponentCount(); i++){
            if(console.getMainPanel().getComponent(i).getClass().getSimpleName().equals(name)){               
                return (SimpleSubPanel) console.getMainPanel().getComponent(i);
            }
            else if(console.getMainPanel().getComponent(i).getClass().getSuperclass().getSimpleName().equals(SUPERCLASS_NAME)){            
                return findPanelInConsoleAux(name,(ContainerSubPanel)console.getMainPanel().getComponent(i));                
            }
        }
        return null;
    }

    /**
     * @param name
     * @param component
     * @return 
     */
    private static SimpleSubPanel findPanelInConsoleAux(String name, ContainerSubPanel container) {
       
        for(int i = 0; i < container.getSubPanels().size(); i++){
            if(container.getSubPanels().get(i).getClass().getSimpleName().equals(name)){
                return (SimpleSubPanel) container.getSubPanels().get(i);
            }
            else if(container.getSubPanels().get(i).getClass().getSuperclass().getSimpleName().equals(SUPERCLASS_NAME)){
                return findPanelInConsoleAux(name,(ContainerSubPanel)container.getComponent(i));
            }
        }
        return null;        
    }
    
    public static GradientPaint gradientColorer(int width, int height, Color color1, Color color2) {
        return new GradientPaint(new Point2D.Double(width/2,height), color1, new Point2D.Double(width/2,0), color2);
    }
    
    public static AlphaComposite makeTransparent(float alpha) {
        int type = AlphaComposite.SRC_OVER;
        return(AlphaComposite.getInstance(type, alpha));
    }
}
