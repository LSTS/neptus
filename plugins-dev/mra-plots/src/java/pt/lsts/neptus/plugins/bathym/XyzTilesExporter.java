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
 * Sep 15, 2020
 */
package pt.lsts.neptus.plugins.bathym;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PopUp;
import pt.lsts.imc.StationKeeping;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;

/**
 * @author zp
 *
 */
@PluginDescription(name = "XYZ Tiles", experimental = true)
public class XyzTilesExporter implements MRAExporter {

    private XyzFolder folder;
    private CorrectedPosition positions;

    @NeptusProperty(name = "Skip surface data")
    public boolean skipSurface = false;

    @NeptusProperty(name = "Skip Popup Maneuvers")
    public boolean skipPopup = true;

    @NeptusProperty(name = "Skip StationKeeping Maneuvers")
    public boolean skipStationKeeping = true;
    
    @NeptusProperty(name = "Maximum roll degrees")
    public double maxRollDegrees = 7;
    
    @NeptusProperty(name = "Tiles zoom level")
    public int tilesZoom = 16;
    
    @NeptusProperty(name = "Process every other point (instead of everyone = 1)")
    public int pointsToSkip = 5;
    
    
    public XyzTilesExporter(IMraLogGroup source) {
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return DeltaTParser.canBeApplied(source);
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        pmonitor.setMaximum(100);
        if (!MRAProperties.batchMode)
            PluginUtils.editPluginProperties(this, true);
        
        pmonitor.setMillisToDecideToPopup(0);

        pmonitor.setProgress(0);
        
        positions = new CorrectedPosition(source);
        pmonitor.setProgress(8);
        folder = new XyzFolder(new File(source.getDir(), "mra/xyz"));
        folder.setAppendToExistingFiles(false);
        folder.setZoom(tilesZoom);
        
        pmonitor.setProgress(10);

        BathymetrySwath swath;

        DeltaTParser parser = new DeltaTParser(source);
        double firstTime = parser.getFirstTimestamp();
        double lastTime = parser.getLastTimestamp();
        double timeSpan = lastTime - firstTime;
        if (timeSpan == 0)
            timeSpan = 1;

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd hh:mm");

        while ((swath = parser.nextSwath()) != null) {
            if (pmonitor.isCanceled())
                break;

            SystemPositionAndAttitude position = positions.getPosition(swath.getTimestamp() / 1000.0);

            if (shouldSkip(source, position))
                continue;

            LocationType loc = position.getPosition();

            int prog = (int) (100 * ((swath.getTimestamp() - firstTime) / timeSpan));
            pmonitor.setProgress(prog);

            for (int i = 0; i < swath.getData().length; i+= pointsToSkip) {
                BathymetryPoint bp = swath.getData()[i];
                LocationType loc2 = new LocationType(loc);
                if (bp == null)
                    continue;
                loc2.translatePosition(bp.north, bp.east, 0);
                loc2.convertToAbsoluteLatLonDepth();
                Date d = new Date(swath.getTimestamp());
                pmonitor.setNote(sdf.format(d));
                try {
                    double tide = TidePredictionFactory.getTideLevel(d);
                    double bathym = bp.depth - tide;
                    folder.addSample(loc2.getLatitudeDegs(), loc2.getLongitudeDegs(), bathym);
                }

                catch (Exception e) {
                    e.printStackTrace();
                    return "Error processing log: " + e.getMessage();
                }
            }
        }

        pmonitor.setProgress(100);
        
        try {
            folder.close();    
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        

        return I18n.textf("Data written to %file.", new File(source.getDir(), "mra/xyz"));
    }

    private int maneuverType(IMraLogGroup source, double timestamp) {
        IMCMessage msg = source.getLsfIndex().getMessageAt(PlanControlState.class.getSimpleName(), timestamp);

        if (msg == null || msg.getMessageType().getId() != PlanControlState.ID_STATIC)
            return -1;

        return ((PlanControlState) msg).getManType();
    }

    private boolean shouldSkip(IMraLogGroup source, SystemPositionAndAttitude pos) {

        if (skipSurface && pos.getDepth() < 0.5)
            return true;
        
        if (Math.abs(Math.toDegrees(pos.getRoll())) > maxRollDegrees)
            return true;

        if (skipPopup || skipStationKeeping) {
            int manType = maneuverType(source, pos.getTime() / 1000.0);
            if (manType == StationKeeping.ID_STATIC && skipStationKeeping)
                return true;

            if (manType == PopUp.ID_STATIC && skipPopup)
                return true;
        }

        return false;
    }

}
