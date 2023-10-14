/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Canasta
 * 19 de Dez de 2010
 */
package pt.lsts.neptus.plugins.uavs.painters.background;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;

import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * Neptus Uav painter which is intended to be used by Uav panels to draw necessary information on the screen. This painter functions as a background setter for Uav panels. It allows the
 * creation of gradient backgrounds with up to 2 colors.
 * 
 * <p>Accepted arguments in <code>LinkedHashMap< String,Object ></code> <b>receivedArgs</b></p> 
 * <ul>
 *  <li><b>Key:</b> name+".Color" <b>Value:</b> Color[2] </li>
 *  <li><b>Key:</b> name+".DrawPoint" <b>Value:</b> int[2] </li>
 *  <li><b>Key:</b> name+".Size" <b>Value:</b> int[2] </li>
 * </ul>
 * 
 * @author canastaman
 * @version 2.0
 * @category UavPainter 
 * 
 */
public class UavCoverLayerPainter implements IUavPainter{
    
    private String name;
    
    private Point2D[] gradientPoints;
    private Shape shape;
    private GradientPaint backgroundColor;    
    private Color[] colors;

    //X | Y
    private int[] drawPoints;
    
    //Width | Height
    private int[] size;
    
    private LinkedHashMap<String,Object> receivedArgs;
    
    public UavCoverLayerPainter(String name) {
        this.name = name;
        this.backgroundColor = null;
        this.shape = null;
        this.drawPoints = new int[2];
        this.size = new int[2];
        this.gradientPoints = new Point2D[2];
        this.colors = new Color[2];
        this.receivedArgs = new LinkedHashMap<String,Object>();
    }
    
    //------Implemented Interfaces------//
    
	//IUavPainter_BEGIN
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        boolean update = false;
        receivedArgs = (LinkedHashMap<String, Object>) args;
        
        //checks for updates regarding window size
        if(receivedArgs.get(name+".Size") != null){
            size = (int[]) receivedArgs.get(name+".Size");
            gradientPoints[0] = new Point2D.Double(size[0]>>1,size[1]);
            gradientPoints[1] = new Point2D.Double(size[0]>>1,0);
            update = true;
        }
                       
        //checks for updates regarding color change
        if(receivedArgs.get(name+".Color") != null){
            colors = (Color[]) receivedArgs.get(name+".Color"); 
            update = true;
        }
        
        //checks for updates drawing start point
        if(receivedArgs.get(name+".DrawPoint") != null){
            drawPoints = (int[]) receivedArgs.get(name+".DrawPoint");
            update = true;
        }
        
        if(update){
            backgroundColor = new GradientPaint(gradientPoints[0], colors[0], gradientPoints[1], colors[1]);
            shape = new Rectangle(drawPoints[0], drawPoints[1], size[0], size[1]); 
        }
                
        g.setPaint(backgroundColor);                
        g.fill(shape);   
        
    }
    //IUavPainter_END
    
}
