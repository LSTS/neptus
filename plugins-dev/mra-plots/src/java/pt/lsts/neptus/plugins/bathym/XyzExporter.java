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
 * Nov 21, 2014
 */
package pt.lsts.neptus.plugins.bathym;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import org.apache.commons.io.FileUtils;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PopUp;
import pt.lsts.imc.StationKeeping;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.mra.importers.lsf.DVLBathymetryParser;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ZipUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(name="XYZ Exporter")
public class XyzExporter implements MRAExporter {

    /** Line ending to use */
    private static final String LINE_ENDING = "\r\n";
    /** Comment char to use */
    private static final String COMMENT_CHAR = "#";
    /** Comment string to use */
    private static final String COMMENT_STRING = COMMENT_CHAR + " ";
    
    /** The data spacer chooser */
    public enum SeparatorChar {
        SPACE(" "),
        COMMA(", ");

        private String txt = " ";

        private SeparatorChar(String txt) {
            this.txt = txt;
        }

        /** Return the spacer text */
        public String getText() {
            return txt;
        }
    };

    @NeptusProperty(name = "Export EstimatedState-derived points")
    public boolean exportEstimatedState = false;

    @NeptusProperty(name = "Export DVL's Distance-derived points")
    public boolean exportDistance = false;

    @NeptusProperty(name = "Export Multibeam Sonar points")
    public boolean exportMultibeam = true;

    @NeptusProperty(name = "Tide Correction")
    public boolean tideCorrection = true;
    
    @NeptusProperty(name = "Filename to write to", category = "Output", editable = false)
    public File file = new File(".");
    
    @NeptusProperty(name="Compress Output", category = "Output")
    public boolean compressOutput = true;

    @NeptusProperty(name = "Data spacer")
    public SeparatorChar spacer = SeparatorChar.COMMA;
    
    @NeptusProperty(name = "Skip surface data")
    public boolean skipSurface = false;
    
    @NeptusProperty(name = "Skip Popup Maneuvers")
    public boolean skipPopup = true;
        
    @NeptusProperty(name = "Skip StationKeeping Maneuvers")
    public boolean skipStationKeeping = true;
        
    @NeptusProperty(name = "Write XYZ header")
    public boolean writeHeader = false;
    
    private TidePredictionFinder finder = null;
    private BufferedWriter writer = null;
    private ProgressMonitor pmonitor;
    
    public XyzExporter(IMraLogGroup source) {
        file = new File(source.getDir(), "mra/bathymetry.xyz");
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");        
    }

    private IMraLogGroup source;
    
    private void writeCustomHeader() throws Exception {
        
            // Writing header
            writer.write(COMMENT_STRING + "XYZ Data" + LINE_ENDING);
            double startTimeSeconds = source.getLsfIndex().getStartTime();
            writer.write(COMMENT_STRING + "Date of data: " 
                    + DateTimeUtil.dateFormatterUTC.format(new Date((long) (startTimeSeconds * 1E3))) 
                    + LINE_ENDING);
            writer.write(COMMENT_STRING + "Tide corrected: " + (tideCorrection ? "yes (" 
                    + finder.getName() + ")" : "no") + LINE_ENDING);
            
            // Data source info
            String dataSource = "";
            if (exportEstimatedState)
                dataSource = "navigation";
            if (exportMultibeam)
                dataSource += (dataSource.isEmpty() ? "" : ", ") + "multibeam";
            if (exportDistance)
                dataSource += (dataSource.isEmpty() ? "" : ", ") + "dvl";
            writer.write(COMMENT_STRING + "Data source: " + dataSource + LINE_ENDING);
            writer.write(COMMENT_STRING + LINE_ENDING);

            writer.write(COMMENT_STRING + "Longitude, Latitude, Depth" + LINE_ENDING);
            writer.write(COMMENT_STRING + "(decimal degrees, decimal degrees, meters)" + LINE_ENDING);        
    }
    
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        pmonitor.setMaximum(100);
        PluginUtils.editPluginProperties(this, true);
        this.pmonitor = pmonitor;
        this.pmonitor.setMillisToDecideToPopup(0);

        this.pmonitor.setProgress(0);
        this.source = source;

        finder = TidePredictionFactory.create(source);

        if (finder == null)
            tideCorrection = false;
        
        try {
            writer = new BufferedWriter(new FileWriter(file));

            if (writeHeader)
               writeCustomHeader();            
        }
        catch (Exception e) {
            e.printStackTrace();
            return I18n.textf("%name while trying to write to file: %message.", e.getClass().getSimpleName(),
                    e.getMessage());
        }

        this.pmonitor.setProgress(10);
        
        if (exportEstimatedState) {
            pmonitor.setNote(I18n.text("Processing EstimatedState data"));
            processEstimatedStates(source);
        }
        
        this.pmonitor.setProgress(30);
        if (exportMultibeam) {
            pmonitor.setNote(I18n.text("Processing Multibeam data"));
            processMultibeam(source);
        }
        
        this.pmonitor.setProgress(60);
        if (exportDistance) {
            pmonitor.setNote(I18n.text("Processing DVL data"));
            processDvl(source);
        }
        
        this.pmonitor.setProgress(80);
        try {
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace(); 
        }

        String outputPath = file.getName();
        if (compressOutput) {
            this.pmonitor.setProgress(90);
            pmonitor.setNote(I18n.text("Compressing output"));
            ZipUtils.zipDir(file.getAbsolutePath() + ".zip", file.getAbsolutePath());
            outputPath += ".zip";
            pmonitor.setNote("Deleting file");
            FileUtils.deleteQuietly(file);
        }
        
        this.pmonitor.setProgress(100);
        
        return I18n.textf("File written to %file.", outputPath);
    }
    
    private int maneuverType(double timestamp) {
        IMCMessage msg = source.getLsfIndex().getMessageAt(PlanControlState.class.getSimpleName(), timestamp);
        
        if (msg == null)
            return -1;

        if (msg.getClass() != PlanControlState.class) {
            return -1; //Temp fix for buggy return
        }
        return ((PlanControlState)msg).getManType();
    }
    
    private boolean shouldSkip(SystemPositionAndAttitude pos) {
        
        if (skipSurface && pos.getDepth() < 0.5)
            return true;
                    
        if (skipPopup || skipStationKeeping) {
            int manType = maneuverType(pos.getTime()/1000.0);
            if (manType == StationKeeping.ID_STATIC && skipStationKeeping)
                return true;
            
            if (manType == PopUp.ID_STATIC && skipPopup)
                return true;            
        }
        
        return false;
    }

    private void processEstimatedStates(IMraLogGroup source) {
        CorrectedPosition pos = new CorrectedPosition(source);
        double firstTime = source.getLsfIndex().getStartTime();
        double lastTime = source.getLsfIndex().getEndTime();
        double timeSpan = lastTime - firstTime;
        
        for (EstimatedState s : source.getLsfIndex().getIterator(EstimatedState.class)) {
            
            if (pmonitor.isCanceled())
                break;
            
            int prog =(int) (100 * ((s.getTimestamp() - firstTime) / timeSpan));
            pmonitor.setProgress(prog);
            
            Date d = s.getDate();
            double depth = s.getDepth() + s.getAlt();
            
            SystemPositionAndAttitude position = pos.getPosition(s.getTimestamp());
            if (!shouldSkip(position))
                addSample(d, position.getPosition(), depth);
        }
    }

    private void processPoints(BathymetryParser parser) {

        BathymetrySwath swath;
        
        double firstTime = parser.getFirstTimestamp();
        double lastTime = parser.getLastTimestamp();
        double timeSpan = lastTime - firstTime;
        if (timeSpan == 0)
            timeSpan = 1;

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd hh:mm");
        CorrectedPosition pos = new CorrectedPosition(source);
        
        while ((swath = parser.nextSwath()) != null) {
            if (pmonitor.isCanceled())
                break;

            SystemPositionAndAttitude position = pos.getPosition(swath.getTimestamp()/1000.0);
            
            if (shouldSkip(position))
                continue;
            
            LocationType loc = position.getPosition();
            
            int prog = (int) (100 * ((swath.getTimestamp() - firstTime) / timeSpan));
            pmonitor.setProgress(prog);

            for (BathymetryPoint bp : swath.getData()) {
                LocationType loc2 = new LocationType(loc);
                if (bp == null)
                    continue;
                loc2.translatePosition(bp.north, bp.east, 0);
                Date d = new Date(swath.getTimestamp());
                pmonitor.setNote(sdf.format(d));
                addSample(d, loc2, bp.depth + (loc.getDepth() >= 0 ? loc.getDepth() : 0));
            }
        }
    }

    private void processMultibeam(IMraLogGroup source) {
        processPoints(new DeltaTParser(source));
    }

    private void processDvl(IMraLogGroup source) {
        processPoints(new DVLBathymetryParser(source));
    }

    private void addSample(Date date, LocationType loc, double depth) {
        double tide = 0;
        if (tideCorrection) {
            try {
                tide = finder.getTidePrediction(date, false);            
            }
            catch (Exception e) {           
            }
        }

        loc.convertToAbsoluteLatLonDepth();

        try {
            String outStr = String.format(Locale.US, "%.8f" + spacer.getText() + "%.8f" 
                    + spacer.getText() + "%.2f" + LINE_ENDING, loc.getLongitudeDegs(), 
                    loc.getLatitudeDegs(), Math.abs(depth) - tide);
            writer.write(outStr);
        }
        catch (Exception e) {
            e.printStackTrace();
        }        
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File("/home/zp/Desktop/BathymFunchal/Stripped/data.xyz")));
        String line = reader.readLine();
        LocationType ref = null;
        while (line != null) {
            String[] parts = line.split("\t");
            double lon = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            double alt = Double.parseDouble(parts[2]);
            if (ref == null)
                ref = new LocationType(lat, lon);
            else {
                LocationType loc = new LocationType(lat, lon);
                double[] offsets = loc.getOffsetFrom(ref);
                System.out.println(offsets[1]+"\t"+offsets[0]+"\t"+alt);
            }
            line = reader.readLine();
        }
        reader.close();
    }

}
