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
 * Author: canasta
 * Apr 22, 2014
 */
package pt.lsts.neptus.plugins.ipcam;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Auxiliary panel intended only for resizing an incoming image from its father panel. This panel allows the developer to take advantage of layout managers
 * without having to overload the paint method of each new imaging panel
 * 
 * @author canastaman
 * @version 1.0
 * @category CameraPanel 
 *
 */
class ImagePanel extends JPanel implements ComponentListener{
    private static final long serialVersionUID = 1L;
    private BufferedImage image = null;
    
    //aids the resizing process the first time the panel receives a non Null image
    private boolean initializedFactor;
    
    //image size factor for resizing purposes
    private double factor;
    
    public ImagePanel() {
        this.initializedFactor = false;
        this.addComponentListener(this);
    }
    
    protected void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (image != null) {
            
            if(!initializedFactor){
                initializedFactor = true;
                double factorw = (double) getWidth() / image.getWidth();
                double factorh = (double) getHeight() / image.getHeight();
                factor = (factorw < factorh ? factorw : factorh);
            }
            
            ((Graphics2D)g).scale(factor,factor);
            g.drawImage(image,
                    (int) ((getWidth() - factor *  image.getWidth())  / (factor * 2)),
                    (int) ((getHeight() - factor * image.getHeight())  / (factor * 2)),
                    null);
        } 
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e) {
        if (image!=null) {
            double factorw = (double) getWidth() / image.getWidth();
            double factorh = (double) getHeight() / image.getHeight();
            factor = (factorw < factorh ? factorw : factorh);
        }        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e) {
    }   
}