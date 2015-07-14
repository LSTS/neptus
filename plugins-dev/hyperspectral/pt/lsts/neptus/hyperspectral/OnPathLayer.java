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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: coop
 * 11 Jul 2015
 */
package pt.lsts.neptus.hyperspectral;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

import pt.lsts.imc.HyperSpecData;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * Layer where all the Hyperspectral data will be drawn.
 * @author tsmarques
 *
 */
public class OnPathLayer {
    private BufferedImage layer;
    private LocationType center;
    public List<HyperspectralData> dataset;
    
    public boolean isInitialized = false;
    
    public OnPathLayer() {
    }
    
    public BufferedImage getLayer() {
        return layer;
    }
    
    public LocationType getCenter() {
        return center;
    }
    
    private void resizeLayer(int newWidth, int newHeight) {
        /* TODO */
    }
            
    public void generateLayer(StateRenderer2D renderer, LocationType topleft , LocationType botright) {
        Point2D p1 = renderer.getScreenPosition(topleft);
        Point2D p2 = renderer.getScreenPosition(botright);
        
        double top = p1.getY();
        double left = p1.getX();
        double right = p2.getX();
        double bottom = p2.getY();
        
        layer = new BufferedImage((int)(right - left), (int)(bottom - top), BufferedImage.TYPE_INT_ARGB);
        
        /* compute layer's center */
        double centerX = left + ((right - left) / 2);
        double centerY = top + ((bottom - top) / 2);
                
        Point2D p = new Point2D.Double(centerX, centerY);
        center = renderer.getRealWorldLocation(p);
        
        Graphics2D g = (Graphics2D) layer.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for(HyperspectralData hyperspec : dataset)
            addDataToLayer(hyperspec, g, renderer);
    }
    
    /* TODO: check if current layer contains the given data. If not, resize accordingly */
    private void addDataToLayer(HyperspectralData hyperspec, Graphics2D g, StateRenderer2D renderer) {
        Graphics2D g2 = (Graphics2D) g.create();
        Point2D dataPosition = renderer.getScreenPosition(hyperspec.dataLocation);
        
        double posX = (dataPosition.getX() * (layer.getWidth()/(double) renderer.getWidth()));
        double posY = (dataPosition.getY() * (layer.getHeight()/(double) renderer.getHeight()));

        int dataX = (int)(posX - (hyperspec.data.getWidth() / 2));
        int dataY = (int)(posY - (hyperspec.data.getHeight() / 2));

        g2.drawImage(hyperspec.data, dataX, dataY, null);
//        renderer.repaint();
        g2.dispose();
    }
}
