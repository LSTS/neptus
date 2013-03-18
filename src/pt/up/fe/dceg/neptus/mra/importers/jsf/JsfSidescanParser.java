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
 * Feb 7, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.plugins.sidescan.SidescanLine;
import pt.up.fe.dceg.neptus.plugins.sidescan.SidescanParser;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author jqcorreia
 *
 */
public class JsfSidescanParser implements SidescanParser {

    JsfParser parser;
    ColorMap colormap = ColorMapFactory.createBronzeColormap();

    public JsfSidescanParser(File f) {
        parser = new JsfParser(f);
    }
    
    @Override
    public long firstPingTimestamp() {
        return parser.getFirstTimeStamp();
    }

    @Override
    public long lastPingTimestamp() {
        return parser.getLastTimeStamp();
    }
    
    @Override
    public ArrayList<Integer> getSubsystemList() {
        return parser.index.subSystemsList;
    }

    @Override
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int lineWidth, int subsystem) {
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        
        ArrayList<JsfSonarData> ping = parser.getPingAt(timestamp1, subsystem);
        ArrayList<JsfSonarData> nextPing;
        
        if(ping.size() == 0) return list;
        int ypos = 0;
        while(ping.get(0).getTimestamp() < timestamp2) {
            int size = 1;
            JsfSonarData sboard = null;
            JsfSonarData pboard = null;

            for (JsfSonarData temp : ping) {
                if(temp != null) {
                    if (temp.getHeader().getChannel() == 0) {
                        pboard = temp;
                    }
                    if (temp.getHeader().getChannel() == 1) {
                        sboard = temp;
                    }
                }
            }
            // From here portboard channel (pboard var) will be the reference
            BufferedImage line = new BufferedImage(pboard.getNumberOfSamples() + sboard.getNumberOfSamples(), 1, BufferedImage.TYPE_INT_RGB);

            double min = 0, max = 0;
            
//            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
//                double r = pboard.getData()[i];
//                min = Math.min(r, min);
//                max = Math.max(r, max);
//            }
            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
                double r = pboard.getData()[i];
                max += r;
            }
            for (int i = 0; i < sboard.getNumberOfSamples(); i++) {
                double r = sboard.getData()[i];
                min += r;
            }
            max /= (double)pboard.getNumberOfSamples() * 0.05;
            min /= (double)sboard.getNumberOfSamples() * 0.05;
            float horizontalScale = (float)line.getWidth() / (pboard.getRange() * 2f);
            float verticalScale = horizontalScale;
        
            nextPing = parser.nextPing(subsystem);
            
            float secondsUntilNextPing = (nextPing.get(0).getTimestamp() - ping.get(0).getTimestamp()) / 1000f;
            float speed = ping.get(0).getSpeed();
            
            size = (int) (secondsUntilNextPing * speed * verticalScale);
            if (size <= 0) {
                size = 1;
            }
            size = 1;
            double lineScale = (double) lineWidth / ((double) pboard.getNumberOfSamples() + (double) sboard.getNumberOfSamples());
            double lineSize = Math.ceil(Math.max(1, lineScale * size));
            
//            System.out.println(secondsUntilNextPing + " " + speed + " " + lineSize);
            
            // Draw Portboard
            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
//                double r = pboard.getRange() - (i * (pboard.getRange() / pboard.getNumberOfSamples()));
                double r =  i / (double)pboard.getNumberOfSamples();
                double gain;
//                if (r <= 1)
//                    gain = 1;
//                else    
                    gain = Math.abs(30.0 * Math.log(r));
//                System.out.println("#1 - " + gain + "  " + r);
//                  gain = 0;
                double pb = pboard.getData()[i] * Math.pow(10, gain / 100);
                line.setRGB(i, 0, colormap.getColor(pb / max).getRGB());
            }
            
            // Draw Starboard
            for (int i = 0; i < sboard.getNumberOfSamples(); i++) {
                double r = 1 - (i / (double)sboard.getNumberOfSamples());
                double gain;
                
                gain = Math.abs(30.0 * Math.log(r));
//                System.out.println("#2 - " + gain + "  " + r);
                double sb = sboard.getData()[i] * Math.pow(10, gain / 100);
                line.setRGB(i + pboard.getNumberOfSamples(), 0, colormap.getColor(sb / min).getRGB());
            }
            
            ypos += (int) lineSize;
            
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitude((pboard.getLat() / 10000.0) / 60.0);
            pose.getPosition().setLongitude((pboard.getLon() / 10000.0) / 60.0);
            pose.setAltitude(1);
            pose.setYaw(Math.toRadians(pboard.getHeading() / 100));
            
            list.add(new SidescanLine(ping.get(0).getTimestamp(),lineWidth, (int)lineSize, ypos, ping.get(0).getRange(), pose, ImageUtils.getScaledImage(line, lineWidth, (int) lineSize, true)));

            ping = nextPing;
        }
        return list;
    }
}
