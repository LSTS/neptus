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
 * Feb 5, 2013
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author jqcorreia
 *
 */
public class ImcSidescanParser implements SidescanParser {
    IMraLog pingParser;
    IMraLog stateParser;
    
    ColorMap colormap = ColorMapFactory.createBronzeColormap();

    long firstTimestamp = -1;
    long lastTimestamp = -1;
    
    public ImcSidescanParser(IMraLogGroup source) {
        pingParser = source.getLog("SonarData");
        stateParser = source.getLog("EstimatedState");
    }
    
    @Override
    public long firstPingTimestamp() {
        if(firstTimestamp != -1 ) return firstTimestamp;
        firstTimestamp = pingParser.firstLogEntry().getTimestampMillis();
        return firstTimestamp;
    };
    
    @Override
    public long lastPingTimestamp() {
        if(lastTimestamp != -1 ) return lastTimestamp;
        lastTimestamp = pingParser.getLastEntry().getTimestampMillis();
        return lastTimestamp;
    }
    
    public ArrayList<Integer> getSubsystemList() {
        // For now just return a list with 1 item. In the future IMC will accomodate various SonarData subsystems
        ArrayList<Integer> l = new ArrayList<Integer>();
        l.add(1);
        return l;
    };

    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int lineWidth, int subsystem, SidescanConfig config) {
        
        // Preparation
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        int[] iData = null;
        BufferedImage line = null;
        Image scaledLine = null;
        pingParser.firstLogEntry();
        
        IMCMessage ping = pingParser.getEntryAtOrAfter(timestamp1);
        if (ping == null)
            return list;
//FIXME
//        if (ping.getDouble("frequency") != freq || ping.getInteger("type") != SonarData.TYPE.SIDESCAN.value()) {
//            ping = getNextMessageWithFrequency(pingParser, freq);
//        }
       
        if (ping.getInteger("type") != SonarData.TYPE.SIDESCAN.value()) {
            ping = getNextMessageWithFrequency(pingParser, 0); //FIXME
        }
        IMCMessage state = stateParser.getEntryAtOrAfter(ping.getTimestampMillis());

        // Null guards
        if (ping == null || state == null)
            return list;

        int range = ping.getInteger("range");
        if (range == 0)
            range = ping.getInteger("max_range");

        int totalsize = 0;
        float secondsUntilNextPing = 0;
        double speed = 0;

        if (iData == null) {
            iData = new int[ping.getRawData("data").length];
        }

        while (ping.getTimestampMillis() <= timestamp2) {
            // Null guards
            if (ping == null || state == null)
                break;

            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.setAltitude(state.getDouble("alt"));
            pose.getPosition().setLatitudeRads(state.getDouble("lat"));
            pose.getPosition().setLongitudeRads(state.getDouble("lon"));
            pose.setYaw(state.getDouble("psi"));
            pose.getPosition().setOffsetNorth(state.getDouble("x"));
            pose.getPosition().setOffsetEast(state.getDouble("y"));
            pose.setU(state.getDouble("u"));
            float horizontalScale = (float) ping.getRawData("data").length / (range * 2f);
            float verticalScale = horizontalScale;

            // Time elapsed and speed calculation
            IMCMessage nextPing = getNextMessageWithFrequency(pingParser, 0); // WARNING: This advances the
                                                                                   // parser
            if (nextPing == null)
                break;

            secondsUntilNextPing = (nextPing.getTimestampMillis() - ping.getTimestampMillis()) / 1000f;
            speed = state.getDouble("u");

            // Finally the 'height' of the ping in pixels
            int size = (int) (secondsUntilNextPing * speed * verticalScale);
            if (size <= 0) {
                size = 1;
            }
            else if (secondsUntilNextPing > 0.5) {
                // TODO This is way too much time between shots. Maybe mark it on the plot?
                // For now put 1 as ysize
                size = 1;
            }

            // Image building. Calculate and draw a line, scale it and save it
            byte[] data = ping.getRawData("data");
            int[] colors = new int[data.length];
            line = new BufferedImage(data.length, size, BufferedImage.TYPE_INT_RGB);

            int pos;

            for (int c = 0; c < data.length; c++) {
                iData[c] = data[c] & 0xFF;
                pos = c;
                colors[pos] = config.colorMap.getColor(iData[c] / 255.0).getRGB();
            }

            for (int c = 0; c < size; c++) {
                line.setRGB(0, c, colors.length, 1, colors, 0, colors.length);
            }

            double lineScale = (double) lineWidth / (double) data.length;
            double lineSize = Math.ceil(Math.max(1, lineScale * size));
            scaledLine = ImageUtils.getScaledImage(line, lineWidth, (int) lineSize, true);
            
            totalsize += (int) (lineSize);
            
            list.add(new SidescanLine(ping.getTimestampMillis(), scaledLine.getWidth(null), (int) lineSize, totalsize, range, pose, scaledLine));

            ping = pingParser.getCurrentEntry(); // This parser was already advanced so only get current entry
            state = stateParser.getEntryAtOrAfter(ping.getTimestampMillis());
        }
        pingParser.firstLogEntry();
        stateParser.firstLogEntry();
        return list;
    }
    
    public long getCurrentTime() {
        return pingParser.currentTimeMillis();
    }
    
    public IMCMessage getNextMessageWithFrequency(IMraLog parser, double freq) {
        IMCMessage msg;
        while((msg = parser.nextLogEntry()) != null) {
            if(msg.getInteger("type") == SonarData.TYPE.SIDESCAN.value()) {
                return msg;
            }
        }
        return null;
    }
}
