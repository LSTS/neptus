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

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.i18n.I18n;
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
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(name="XYZ Exporter")
public class XyzExporter implements MRAExporter {

    @NeptusProperty(name="Export EstimatedState-derived points")
    public boolean exportEstimatedState = false;

    @NeptusProperty(name="Export DVL's Distance-derived points")
    public boolean exportDistance = false;

    @NeptusProperty(name="Export Multibeam Sonar points")
    public boolean exportMultibeam = true;

    @NeptusProperty(name="Tide Correction")
    public boolean tideCorrection = true;
    
    @NeptusProperty(name="Filename to write to", editable = false)
    public File file = new File(".");

    private TidePredictionFinder finder = null;
    private BufferedWriter writer = null;
    private ProgressMonitor pmonitor;
    
    public XyzExporter(IMraLogGroup source) {
        file = new File(source.getDir(), "mra/bathymetry.xyz");
        finder = TidePredictionFactory.create(source);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");        
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        pmonitor.setMaximum(100);
        PluginUtils.editPluginProperties(this, true);
        this.pmonitor = pmonitor;
        try {
            writer = new BufferedWriter(new FileWriter(file));
        }
        catch (Exception e) {
            e.printStackTrace();
            return I18n.textf("%name while trying to write to file: %message.", e.getClass().getSimpleName(), e.getMessage()); 
        }

        if (exportEstimatedState) {
            pmonitor.setNote(I18n.text("Processing EstimatedState data"));
            processEstimatedStates(source);
        }
        
        if (exportMultibeam) {
            pmonitor.setNote(I18n.text("Processing Multibeam data"));
            processMultibeam(source);
        }
        
        if (exportDistance) {
            pmonitor.setNote(I18n.text("Processing DVL data"));
            processDvl(source);
        }
        
        try {
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace(); 
        }
        
        return I18n.textf("File written to %file.", file.getAbsolutePath());
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
            addSample(d, pos.getPosition(s.getTimestamp()).getPosition(), depth);
        }
    }

    private void processPoints(BathymetryParser parser) {

        BathymetrySwath swath;
        
        /*long firstTime = parser.getFirstTimestamp();
        long lastTime = parser.getLastTimestamp();
        long total = lastTime - firstTime;
        */
        System.out.println(parser.getLastTimestamp());
        System.out.println(parser.getFirstTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd hh:mm");
        
        while ((swath = parser.nextSwath()) != null) {
            
            if (pmonitor.isCanceled())
                break;
            /*
            long pos = ((swath.getTimestamp() - firstTime)*100) / total;
            pmonitor.setProgress((int)pos);
            */
            LocationType loc = swath.getPose().getPosition();

            for (BathymetryPoint bp : swath.getData()) {
                LocationType loc2 = new LocationType(loc);
                if (bp == null)
                    continue;
                loc2.translatePosition(bp.north, bp.east, 0);
                Date d = new Date(swath.getTimestamp());
                pmonitor.setNote(sdf.format(d));
                addSample(d, loc2, bp.depth);
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
            writer.write(String.format(Locale.US, "%.8f %.8f %.2f\n", loc.getLongitudeDegs(), loc.getLatitudeDegs(),
                    Math.abs(depth) - tide));
        }
        catch (Exception e) {
            e.printStackTrace();
        }        
    }

    @Override
    public String getName() {
        return "Export as .xyz";
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
