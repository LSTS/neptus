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
 * Author: zp
 * Jan 22, 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.Conductivity;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.VehicleMedium;
import pt.lsts.imc.lsf.IndexScanner;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Export CTD data to CSV")
public class CTDExporter implements MRAExporter {

    public CTDExporter(IMraLogGroup source) {

    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().getEntityId("CTD") != -1;
    };

    @Override
    public String getName() {
        return PluginUtils.getPluginDescription(getClass());
    }

    private String finish(BufferedWriter writer, int count) {
        try {
            writer.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        return "Exported "+count+" samples";
    }
    @SuppressWarnings("resource")
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date start = new Date((long)(source.getLsfIndex().getStartTime()*1000));
        Date end = new Date((long)(source.getLsfIndex().getEndTime()*1000));
        String startSel = "";
        while(startSel.isEmpty()) {
        startSel = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(), I18n.text("Select start time (UTC)"), sdf.format(start));        
            if (startSel == null)
                return "Cancelled by the user";
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
                return "Cancelled by the user";
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
            return "Start time must be before end time";
        }
        
        //System.out.println(start +" --> "+end);
        
        LsfIndex index = source.getLsfIndex();
        IndexScanner scanner = new IndexScanner(index);
        pmonitor.setMaximum(index.getNumberOfMessages());
        pmonitor.setMinimum(index.getNumberOfMessages());
        pmonitor.setNote("Creating output folder...");
        boolean containsSalinity = source.getLsfIndex().containsMessagesOfType("Salinity");
        
        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();
        pmonitor.setNote("Generating corrected positions...");
        pmonitor.setProgress(10);
        CorrectedPosition cp = new CorrectedPosition(source);
        
        pmonitor.setNote("Exporting...");
        int count = 0;
        File out = new File(dir, "CTD.csv");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(out));

            writer.write("timestamp, gmt_time, latitude, longitude, corrected_lat, corrected_lon, conductivity, temperature"+(containsSalinity? ",salinity ": "")+", depth, medium\n");
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return "Error creating output file: "+e.getMessage();            
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            scanner.setTime(start.getTime()/1000.0);
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass().getSimpleName()+": "+e.getMessage();
        }
        while (true) {
            Conductivity c = scanner.next(Conductivity.class, "CTD");
            
            if (c == null || c.getTimestampMillis() > end.getTime())
                return finish(writer, count);

            int idx = scanner.getIndex();

            VehicleMedium m = scanner.next(VehicleMedium.class);
            try {
                scanner.setIndex(idx);
            }
            catch (Exception e) {
                e.printStackTrace();                
            }
            
            Salinity s = null;
            if (containsSalinity)
                s = scanner.next(Salinity.class);
            
            Temperature t = scanner.next(Temperature.class, "CTD");
            if (t == null)
                return finish(writer, count);
            EstimatedState d = scanner.next(EstimatedState.class);
            
            if (d == null)
                return finish(writer, count);
            count ++;

            LocationType loc = IMCUtils.parseLocation(d).convertToAbsoluteLatLonDepth();
            SystemPositionAndAttitude p = cp.getPosition(d.getTimestamp());
            if (p==null)
                return "error positions is Empty";
            
            try {
                String medium = "UNKNOWN";
                if (m != null)
                    medium = m.getMedium().toString();

                writer.write(t.getTimestamp()+", "+
                        dateFormat.format(t.getDate())+", "+
                        loc.getLatitudeDegs()+", "+
                        loc.getLongitudeDegs()+", "+
                        ((p != null)? p.getPosition().getLatitudeDegs() : loc.getLatitudeDegs())+", "+
                        ((p != null)? p.getPosition().getLongitudeDegs() : loc.getLongitudeDegs())+", "+
                        c.getValue()+", "+
                        t.getValue()+", "+
                        (containsSalinity? s.getValue()+", " : "")+
                        d.getDepth()+", "+
                        medium+"\n");          
            }
            catch (Exception e) {
                e.printStackTrace();
                NeptusLog.pub().error(e);
                return "Error writing to file: "+e.getMessage();       
            }
            pmonitor.setProgress(scanner.getIndex());            
        }
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
        CTDExporter exporter = new CTDExporter(group);
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
