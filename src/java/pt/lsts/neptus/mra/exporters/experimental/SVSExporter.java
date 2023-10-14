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
 * Author: zp
 * Jan 22, 2014
 */
package pt.lsts.neptus.mra.exporters.experimental;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SoundSpeed;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.VehicleMedium;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Export SVS to CSV", description="Export data from sound velocity sensor and temperature to CSV.", experimental=true)
public class SVSExporter implements MRAExporter {

    private static final ArrayList<String> TEMPERATURE_SENSORS = new ArrayList<>();
    {
        TEMPERATURE_SENSORS.add("CTD");
        TEMPERATURE_SENSORS.add("Water Quality Sensor");
        TEMPERATURE_SENSORS.add("Depth Sensor");
    }
    
    public SVSExporter(IMraLogGroup source) {

    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().getEntityId("SoundSpeed") != -1;
    };

    private String finish(BufferedWriter writer, int count) {
        try {
            writer.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        return I18n.textf("Exported %count samples", count);
    }


    private String write(BufferedWriter writer, SystemPositionAndAttitude pos, EstimatedState state, VehicleMedium mmedium, SoundSpeed speed, Temperature temp) throws IOException {
        LocationType loc = IMCUtils.parseLocation(state).convertToAbsoluteLatLonDepth();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        // ignore old temperature values
        if (temp != null && (temp.getTimestampMillis() - state.getTimestampMillis()) > 1000)
            temp = null;
        
        if (pos==null)
            return I18n.text("Error positions is Empty");

        try {
            String medium = "UNKNOWN";
            if (mmedium != null)
                medium = mmedium.getMedium().toString();

            writer.write(speed.getTimestamp()+", "+
                    dateFormat.format(speed.getDate())+", "+
                    speed.getSourceName()+", "+
                    loc.getLatitudeDegs()+", "+
                    loc.getLongitudeDegs()+", "+
                    ((pos != null)? pos.getPosition().getLatitudeDegs() : loc.getLatitudeDegs())+", "+
                    ((pos != null)? pos.getPosition().getLongitudeDegs() : loc.getLongitudeDegs())+", "+
                    state.getAlt()+", "+
                    state.getDepth()+", "+
                    medium+", "+
                    speed.getValue() + ", " + 
                    ((temp != null) ? temp.getValue() : "") + "\n");
            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);
            return I18n.textf("Error writing to file: %error", e.getMessage());       
        }        
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        LinkedHashMap<String, LinkedHashMap<Integer, IMCMessage> > lastMessages = new LinkedHashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date start = new Date((long)(source.getLsfIndex().getStartTime()*1000));
        Date end = new Date((long)(source.getLsfIndex().getEndTime()*1000));
        String startSel = "";
        while(startSel.isEmpty()) {
            startSel = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(), I18n.text("Select start time (UTC)"), sdf.format(start));        
            if (startSel == null)
                return I18n.text("Cancelled by the user");
            try {
                start = sdf.parse(startSel);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
                startSel = "";
                continue;
            }
        }

        String endSel = "";
        while (endSel.isEmpty()) {
            endSel = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(), I18n.text("Select end time (UTC)"), sdf.format(end));
            if (endSel == null)
                return I18n.text("Cancelled by the user");
            try {
                end = sdf.parse(endSel);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
                endSel = "";
                continue;
            }
        }

        if (start.after(end)) {
            return I18n.text("Start time must be before end time");
        }

        //System.out.println(start +" --> "+end);

        LsfIndex index = source.getLsfIndex();

        pmonitor.setMaximum(index.getNumberOfMessages());
        pmonitor.setMinimum(index.getNumberOfMessages());
        pmonitor.setNote(I18n.text("Creating output folder..."));
        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();
        pmonitor.setNote(I18n.text("Generating corrected positions..."));
        pmonitor.setProgress(10);
        CorrectedPosition cp = new CorrectedPosition(source);
        pmonitor.setNote("Exporting...");
        int count = 0;
        File out = new File(dir, "SoundSpeed.csv");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(out));
            writer.write("timestamp, gmt_time, vehicle, latitude, longitude, corrected_lat, corrected_lon, altitude, depth, medium, sound speed, temperature\n");
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return I18n.textf("Error creating output file: %error", e.getMessage());            
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        int i = index.advanceToTime(0, start.getTime()/1000.0);

        
        
        while (index.timeOf(i) < end.getTime()/1000.0) {
            String vehicle = index.sourceNameOf(i);
            
            if (!lastMessages.containsKey(vehicle))
                lastMessages.put(vehicle, new LinkedHashMap<Integer, IMCMessage>());
            try {
                switch (index.typeOf(i)) {
                    case EstimatedState.ID_STATIC:
                        EstimatedState estate = index.getMessage(i, EstimatedState.class);
                        lastMessages.get(vehicle).put(EstimatedState.ID_STATIC, estate);
                        break;
                    case VehicleMedium.ID_STATIC:
                        VehicleMedium medium = index.getMessage(i, VehicleMedium.class);
                        lastMessages.get(vehicle).put(VehicleMedium.ID_STATIC, medium);
                        break;
                    
                    case Temperature.ID_STATIC:
                        String entity = index.entityNameOf(i);
                        
                        if (TEMPERATURE_SENSORS.contains(entity))
                            lastMessages.get(vehicle).put(Temperature.ID_STATIC, index.getMessage(i, Temperature.class));
                        break;
                    case SoundSpeed.ID_STATIC:
                        SoundSpeed sspeed = index.getMessage(i, SoundSpeed.class);
                        lastMessages.get(vehicle).put(SoundSpeed.ID_STATIC, sspeed);
                        String error = write(writer, cp.getPosition(index.timeOf(i)),(EstimatedState) lastMessages.get(vehicle).get(EstimatedState.ID_STATIC), 
                                (VehicleMedium)lastMessages.get(vehicle).get(VehicleMedium.ID_STATIC),
                                (SoundSpeed)lastMessages.get(vehicle).get(SoundSpeed.ID_STATIC),
                                (Temperature)lastMessages.get(vehicle).get(Temperature.ID_STATIC)
                                );
                        if (error != null)
                            return error;
                        count++;
                        break;
                    default:
                        break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            pmonitor.setProgress(++i);
        }

        return finish(writer, count);
    }

    private static void exportRecursively(File root) {
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                exportRecursively(f);
            }
            else if (f.getName().endsWith(".lsf") || f.getName().endsWith(".lsf.gz")) {
                if (!f.getAbsolutePath().contains("trex_plan"))
                    continue;
                try {
                    System.out.println(export(f));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String export(File source) throws Exception {

        IMraLogGroup group = new LsfLogSource(source, null);
        SVSExporter exporter = new SVSExporter(group);
        ProgressMonitor pm = new ProgressMonitor(null, "Processing "+source.getAbsolutePath(), "processing", 0, 1000);
        if (exporter.canBeApplied(group)) {
            System.out.println("processing "+source.getAbsolutePath());
            String res = exporter.process(group, pm);
            pm.close();
            return res;
        }
        return null;
    }

    public static void main(String[] args) {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int op = chooser.showOpenDialog(null);
        if (op == JFileChooser.APPROVE_OPTION) {
            File root = chooser.getSelectedFile();
            exportRecursively(root);

        }
    }
}
