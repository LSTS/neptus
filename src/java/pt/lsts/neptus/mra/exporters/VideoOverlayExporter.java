/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * May 9, 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaTool;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaToolAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.IndexScanner;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.MraVehiclePosHud;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
//@PluginDescription(name="Video Overlay")
public class VideoOverlayExporter implements MRAExporter {

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    File videoIn = null;
    File videoOut = null;

    public VideoOverlayExporter(IMraLogGroup source) {

    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        JFileChooser chooser = GuiUtils.getFileChooser(source.getDir());
        chooser.setDialogTitle(I18n.text("Video file"));
        int op = chooser.showOpenDialog(null);
        if (op == JFileChooser.APPROVE_OPTION) {
            videoIn = chooser.getSelectedFile();

            videoOut = new File(chooser.getSelectedFile().getAbsolutePath() + ".out.avi");
        }
        else
            return I18n.text("User cancelled the operation");
        String time = JOptionPane.showInputDialog(I18n.text("Enter the log time, in seconds when the video starts"));
        double startTime = 0;
        try {
            startTime = Double.parseDouble(time);
        }
        catch (Exception e) {
            e.printStackTrace();
            return I18n.text("Given time was not understood");
        }

        IMediaReader reader = ToolFactory.makeReader(videoIn.toString());
        reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        IMediaWriter writer = ToolFactory.makeWriter(videoOut.toString(), reader);
        long totalTime = reader.getContainer().getDuration();
        pmonitor.setMaximum((int)totalTime/1000000);
        double start = source.getLsfIndex().getStartTime() + startTime;
        IMediaTool addTimeStamp = new TimeStampTool(start, source, pmonitor);
        reader.addListener(addTimeStamp);
        addTimeStamp.addListener(writer);
        writer.addListener(ToolFactory.makeViewer());
        while (reader.readPacket() == null) {

        }

        return "done";
    }

    static class TimeStampTool extends MediaToolAdapter {
        private final ProgressMonitor pmonitor;
        private final double timeOffset;
        private final IndexScanner scanner;
        private final IndexScanner tempScanner;
        private final MraVehiclePosHud hud;
        JLabel lbl = new JLabel("<html>?</html>");
        public TimeStampTool(double timeOffset, IMraLogGroup log, ProgressMonitor pmonitor) {
            this.pmonitor = pmonitor;
            this.timeOffset = timeOffset;
            scanner = new IndexScanner(log.getLsfIndex());
            tempScanner = new IndexScanner(log.getLsfIndex());
            hud = new MraVehiclePosHud(log, 200, 200);
            lbl.setBounds(0, 0, 200, 190);
            lbl.setOpaque(false);
        }

        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            Graphics2D g = event.getImage().createGraphics();

            pmonitor.setProgress((int)(event.getPicture().getTimeStamp()/1000000));
            try {
                scanner.setTime(timeOffset+event.getPicture().getTimeStamp()/1000000.0);
                EstimatedState state = scanner.next(EstimatedState.class);
                tempScanner.setTime(timeOffset+event.getPicture().getTimeStamp()/1000000.0);
                Temperature temp = tempScanner.next(Temperature.class, "CTD");
                if (state != null) {
                    String depth = String.format(Locale.US, "%.2f", state.getDepth());
                    String roll = String.format(Locale.US, "%.1f", Math.toDegrees(state.getPhi()));
                    String pitch = String.format(Locale.US, "%.1f", Math.toDegrees(state.getTheta()));
                    String yaw = String.format(Locale.US, "%.1f", Math.toDegrees(state.getPsi()));
                    String t = String.format(Locale.US, "%.2f", temp.getValue());
                    LocationType loc = IMCUtils.parseLocation(state);
                    loc.convertToAbsoluteLatLonDepth();
                    lbl.setText("<html><h3>"+loc.getLatitudeAsPrettyString()+"<br>"+loc.getLongitudeAsPrettyString()+"</h3>"+
                            "<h3>Depth: "+depth+" m<br>" +
                            "Roll: "+roll+" º<br>" +
                            "Pitch: "+pitch+" º<br>" +
                            "Yaw: "+yaw+" º<br>"+
                            "Temp.: "+t+" ºC</h3></html>");
                    g.drawImage(hud.getImage(timeOffset+event.getPicture().getTimeStamp()/1000000.0), event.getPicture().getWidth()-210, 10, null);
                    g.translate(10, 10);
                    lbl.setForeground(Color.black);
                    lbl.paint(g);
                    lbl.setForeground(Color.white);
                    g.translate(-2, -2);
                    lbl.paint(g);
                }
            }
            catch (Exception e) {
                return;
            }

            super.onVideoPicture(event);
        }
    }
}
