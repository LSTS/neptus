/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jul 16, 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.VideoCreator;

/**
 * @author zp
 */
@PluginDescription(name="Video Legend")
public class VideoLegendExporter implements MRAExporter {

    private static final int width = 120, height = 140;
    protected SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public VideoLegendExporter(IMraLogGroup source) {

    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        CorrectedPosition corPos = CorrectedPosition.getInstance(source);

        
        VideoCreator creator = null;
        try {
            creator = new VideoCreator(new File(source.getDir(), "overlay.mp4"), width, height);           
        }
        catch (Exception e1) {
            e1.printStackTrace();
            return I18n.text("Not able to create video");
        }
        
        pmonitor.setNote(I18n.text("Starting up"));
        BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        int count = 0;
        long startTime = corPos.getPositions().iterator().next().getTime();
        int size = corPos.getPositions().size();
        pmonitor.setMaximum(size);
        
        for (SystemPositionAndAttitude pos : corPos.getPositions()) {
            pmonitor.setNote(I18n.text("Generating frame "+(count+1)+" of "+size));
            if (pmonitor.isCanceled())
                break;
            try {
                drawLegend(pos, g);
                pmonitor.setProgress(count++);
                creator.addFrame(tmp, pos.getTime() - startTime);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }                                
        }
        creator.closeStreams();
        pmonitor.close();     
        return I18n.textf("Video legend exported to %f",new File(source.getDir(), "overlay.mp4").getAbsolutePath());
    }

    protected void drawLegend(SystemPositionAndAttitude state, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        if (state == null)
            return;
        long timeUTC = state.getTime();
        String depth = GuiUtils.getNeptusDecimalFormat(1).format(state.getPosition().getDepth());
        String alt = Double.isNaN(state.getW())? "N/A" : GuiUtils.getNeptusDecimalFormat(1).format(state.getW());
        String speed = GuiUtils.getNeptusDecimalFormat(1).format(state.getU());
        int roll = (int)Math.toDegrees(state.getRoll());
        int pitch = (int)Math.toDegrees(state.getPitch());
        int yaw = (int)Math.toDegrees(state.getYaw());
        String lat = CoordinateUtil.latitudeAsString(state.getPosition().getLatitudeDegs(), false, 2);
        String lon = CoordinateUtil.longitudeAsString(state.getPosition().getLongitudeDegs(), false, 2);

        Vector<String> details = new Vector<>();
        details.add(lat);
        details.add(lon);
        details.add(I18n.text("Time")+": "+sdf.format(new Date(timeUTC)));
        details.add(I18n.text("Depth")+": "+depth);
        details.add(I18n.text("Altitude")+": "+alt);
        details.add(I18n.text("Roll")+": "+roll);
        details.add(I18n.text("Pitch")+": "+pitch);
        details.add(I18n.text("Yaw")+": "+yaw);
        details.add(I18n.text("Speed")+": "+speed);

        g.setColor(new Color(32,32,64));
        g.fill(new Rectangle2D.Double(0, 0, width, height));
        g.setColor(Color.white);

        for (int y = 15; !details.isEmpty(); y += 15) {
            g.drawString(details.firstElement(), 5, y);
            details.remove(0);
        }
    }
    
    protected Integer legendWidth = null;
    protected int getLegendWidth(Vector<String> strs, Graphics2D g) {
        if (legendWidth == null) {
            int maxSize = 0;
            for (String s : strs) {
                Rectangle2D r = g.getFontMetrics(g.getFont()).getStringBounds(s, g);
                if (r.getWidth() > maxSize)
                    maxSize = (int) Math.ceil(r.getWidth());
            }
            legendWidth = maxSize;
        }
        return legendWidth;
    }
}
