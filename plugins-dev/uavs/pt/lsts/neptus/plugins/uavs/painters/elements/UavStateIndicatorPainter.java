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
 * Author: christian
 * 22.10.2012
 */
package pt.lsts.neptus.plugins.uavs.painters.elements;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Hashtable;

import pt.lsts.neptus.plugins.uavs.IndicatorButton;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author Christian Fuchs
 * @version 0.3
 * Paints buttons indicating (sub-)system status with color coding
 */
public class UavStateIndicatorPainter implements IUavPainter{

//--------------declarations-----------------------------------//  
    
    // Hashtable that contains the received arguments
    Hashtable<String,IndicatorButton> entities = new Hashtable<String,IndicatorButton>();
    
    // Integer to count the number of entities in the argument
    int numOfEntities;
    
    // Integer to store the computed size of the buttons
    int buttonSize;
    
    // Two integers to track where the last button was put
    int originX;
    int originY;
    
    // minimum size of the indicator buttons in pixels
    private static final int minButtonSize = 40;
    
//--------------end of declarations----------------------------//
    
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        // check if the argument is empty - if it is, don't do anything
        if (args != null){
            
            entities = (Hashtable<String,IndicatorButton>) args;
            
            // count the number of entities in the arguments
            numOfEntities = entities.size();
            
            // calculate how big the buttons should be
            buttonSize = buttonSize(width, height, numOfEntities);
            
            // Keep track of how much the origin is moved in x-direction
            originX = 0;
            originY = 0;
            
            for(String entity: entities.keySet()){
                                
                // draw the indicator
                drawIndicator(entities.get(entity));
                
                // move the origin
                moveOrigin(g, width);
            }
        }  
    }
    
    // method that computes the size of the indicators based on the panels dimensions and the number of indicators to be painted
    private int buttonSize(int width, int height, int count){
                
        if (count == 0){
            return 0;
        }
        
        int size = 0;
                
        size = width / (count + ((count + 1) / 2));
        
        if (size < minButtonSize){
            size = buttonSize(width, height, count-1);
        }
        
        if (size > (height / 2)){
            return (height / 2);
        }
        else return size;
        
    }
    
    // method that moves the painting origin for the next indicator button
    private void moveOrigin(Graphics2D g, int width){
       
        // move the x-coordinate of the origin further down for the next entity
        g.translate(buttonSize + 10, 0);
        originX += (buttonSize + 10);
        
        // if the next position is too much to the right, move to the next line
        if ((width - originX) < (buttonSize + 10)){
            g.translate(-originX, buttonSize + 10);
            originX = 0;
            originY += (buttonSize + 10);
        }
    }
    
    private void drawIndicator(IndicatorButton button){
        
        button.setMaximumSize(new Dimension(buttonSize, buttonSize));
        button.setPreferredSize(new Dimension(buttonSize, buttonSize));
        button.setSize(buttonSize, buttonSize);
        button.setLocation(originX, originY);
        button.setVisible(true);
    }
}
