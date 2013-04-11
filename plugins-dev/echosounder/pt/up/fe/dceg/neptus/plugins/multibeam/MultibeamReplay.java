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
 * Author: José Correia
 * Jul 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.multibeam;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.replay.LogReplayLayer;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;

/**
 * @author jqcorreia
 *
 */
@LayerPriority(priority = 1)
public class MultibeamReplay implements LogReplayLayer {

    int lod = 0;
    BufferedImage img;
    Point2D pos;
    
    ColorMap cm;
    
    Color colors[] = new Color[] {Color.black, Color.red, Color.white, Color.yellow, Color.orange,Color.green, Color.BLUE};
    MultibeamData multibeamData;
    
    @Override
    public void cleanup() {
        img = null;
        if (multibeamData !=  null)
            multibeamData.locationList.clear();
        multibeamData = null;
    }
    
    public MultibeamReplay() {
        File cacheFolder = new File(".cache/multibeam");
        if(!cacheFolder.exists())
            cacheFolder.mkdir();
    }
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        if(lod != renderer.getLevelOfDetail())
        {
            lod = renderer.getLevelOfDetail();
            double res[] = multibeamData.topLeftLT.getDistanceInPixelTo(multibeamData.bottomRightLT, lod);
            img = new BufferedImage((int)res[0], (int)res[1], BufferedImage.TYPE_INT_ARGB);
            
            for(Double[] pos : multibeamData.locationList) {
                img.setRGB(Math.abs((int) (pos[0]* Math.pow(2, lod - 22))), Math.abs((int) (pos[1]* Math.pow(2, lod - 22))), cm.getColor(pos[2]).getRGB());
            }
        }
        pos = renderer.getScreenPosition(multibeamData.topLeftLT);
        g.translate(pos.getX(), pos.getY());
        g.drawImage(img, 0, 0, null);
        
        double diffHeight = multibeamData.maxHeight - multibeamData.minHeight;
        
        g.translate(-pos.getX(),-pos.getY());
        for(int i = 0; i < 200; i++) {
            g.setColor(cm.getColor(multibeamData.minHeight + diffHeight / 200 * i));
            g.drawRect(10, 100+i, 10, 1);
            if(i % 50 == 0 || i == 0 || i == 200-1) {
                g.setColor(Color.black);
                g.drawString(""+Math.round(multibeamData.minHeight + diffHeight / 200 * i)+"m", 30, 100+i);
            }
        }
 
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("multibeam.83P") != null;
    }

    @Override
    public String getName() {
        return "Multibeam Layer";
    }

    @Override
    public void parse(IMraLogGroup source) {
        multibeamData = MultibeamData.build(source.getFile("multibeam.83P"), source);
        double heights[] = new double[colors.length];
        double diffHeight = multibeamData.maxHeight - multibeamData.minHeight; 
       
        NeptusLog.pub().info("<###>Min Height : " + multibeamData.minHeight + " Max Height : " + multibeamData.maxHeight);
        for(int i = 0; i < colors.length ; i++)
            heights[i] = multibeamData.minHeight + diffHeight/colors.length * i;
        cm = new InterpolationColorMap(heights, colors);
    }

    @Override
    public String[] getObservedMessages() {
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

}
