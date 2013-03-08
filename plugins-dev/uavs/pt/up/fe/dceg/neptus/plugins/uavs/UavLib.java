/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by canasta
 * Feb 21, 2011
 * $Id:: UavLib.java 9615 2012-12-30 23:08:28Z pdias                            $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;

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
