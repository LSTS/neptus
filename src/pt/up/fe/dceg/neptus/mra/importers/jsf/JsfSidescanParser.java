/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Feb 7, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
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
    public SidescanLine nextSidescanLine(double freq, int lineWidth) {
        return null;
    }

    @Override
    public SidescanLine getSidescanLineAt(long timestamp, double freq, int lineWidth) {
        return null;
    }

    @Override
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int lineWidth, int subsystem) {
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        
        ArrayList<JsfSonarData> ping = parser.getPingAt(timestamp1, subsystem);
        
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
            BufferedImage line = new BufferedImage(pboard.getNumberOfSamples() * 2, 1, BufferedImage.TYPE_INT_RGB);

//            float min = Float.MAX_VALUE, max = 0;
//
////            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
////                float r = pboard.getData()[i];
////                min = Math.min(r, min);
////                max = Math.max(r, max);
////            }

            for (int i = 0; i < pboard.getNumberOfSamples(); i++) {
                line.setRGB(i, 0, colormap.getColor(pboard.getData()[i] / 100).getRGB());
                line.setRGB(i + pboard.getNumberOfSamples(), 0, colormap.getColor(sboard.getData()[i] / 100).getRGB());
            }
            // line = Scalr.resize(line, lineWidth, 1, (BufferedImageOp)null);
            // line = (BufferedImage) ImageUtils.getScaledImage(line, lineWidth, 1, true);
            ypos += size;
            list.add(new SidescanLine(lineWidth, 1, ypos, 45, new EstimatedState(), ImageUtils.getScaledImage(line, lineWidth, (int) 1, true)));

            ping = parser.nextPing(subsystem);
        }
        return list;
    }
}
