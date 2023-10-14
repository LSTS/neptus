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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 15/05/2012
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.imc.LblRangeAcceptance;
import pt.lsts.imc.LblRangeAcceptance.ACCEPTANCE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.util.lbl.LBLTriangulationHelper;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author pdias
 *
 */
@LayerPriority(priority = -50)
@PluginDescription(name="LBL Ranges", icon="pt/lsts/neptus/plugins/acoustic/transponder.png")
public class LBLRangesReplay implements LogReplayLayer {

    private LinkedList<TransponderElement> transponders = new LinkedList<TransponderElement>();
    private LocationType start = new LocationType();
    private LinkedList<LocationType> triangulatedRangesPointsList = new LinkedList<LocationType>();

    private LinkedHashMap<Integer, String> beaconIds = new LinkedHashMap<>();

    private LinkedHashMap<String, RangePainter> rangeFixPainter = new LinkedHashMap<>();

    private LBLTriangulationHelper lblTriangulationHelper = null;

    @Override
    public String getName() {
        return I18n.text("LBL Ranges");
    }

    @Override
    public void cleanup() {
        transponders.clear();
        triangulatedRangesPointsList.clear();
        rangeFixPainter.clear();
        start = null;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return (source.getLog("LblRangeAcceptance") != null && source.getLog("LblConfig") != null);
    }

    @Override
    public void parse(IMraLogGroup source) {

        TransponderElement[] transpondersArray = LogUtils.getTransponders(source);
        if (transpondersArray.length == 0)
            return;
        transponders.addAll(Arrays.asList(transpondersArray));

        try {
            start = LogUtils.getStartupPoint(source);
            if (start == null)
                start = LogUtils.getHomeRef(source);

            try {
                // trying to use the first valid GPSFix as the last known location / start
                IMraLog gpsFix = source.getLog("GpsFix");
                //                IMraLog estimatedState = source.getLog("EstimatedState");

                if (gpsFix != null) {
                    IMCMessage m;
                    while ((m = gpsFix.nextLogEntry()) != null) {            
                        double lat = Math.toDegrees(m.getDouble("lat"));
                        double lon = Math.toDegrees(m.getDouble("lon")) ;
                        LinkedHashMap<String, Boolean> validity = m.getBitmask("validity");

                        if (validity.get("VALID_POS"))
                            start = new LocationType(lat, lon);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            IMraLog lblRangeAcceptancesLog = source.getLog("LblRangeAcceptance");

            if (lblTriangulationHelper == null)
                lblTriangulationHelper = new LBLTriangulationHelper(
                        transponders.toArray(new TransponderElement[transponders.size()]), start);
            else
                lblTriangulationHelper.reset(transponders.toArray(new TransponderElement[transponders.size()]), start);

            boolean useEstimatedStateToFixLastKnownPos = false;
            IMraLog esLog = source.getLog("EstimatedState");
            if (esLog != null)
                useEstimatedStateToFixLastKnownPos = true;

            IMCMessage m;
            while ((m = lblRangeAcceptancesLog.nextLogEntry()) != null) {    
                double id = m.getInteger("id");
                double range = m.getDouble("range");
                
                if (useEstimatedStateToFixLastKnownPos) {
                    IMCMessage esEntry = esLog.getEntryAtOrAfter(m.getTimestampMillis());
                    LocationType locES = LogUtils.getLocation(esEntry);
                    if (locES != null) {
                        lblTriangulationHelper.resetLastKnownPos(locES, esEntry.getTimestampMillis());
                    }
                }

                LocationType loc = lblTriangulationHelper.updateRangeAccepted((long) id, range, m.getTimestampMillis());
                if (loc != null)
                    triangulatedRangesPointsList.add(loc);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {};
    }

    @Subscribe
    public void on(LblRangeAcceptance message) {
        synchronized (rangeFixPainter) {
            if (rangeFixPainter.isEmpty())
                return;
        }
        
        String id = beaconIds.get((int)message.getId());
                
        if (id == null)
            return;
        
        double range = message.getRange();
        boolean accepted = message.getAcceptance() == ACCEPTANCE.ACCEPTED;
        String reason = message.getAcceptance().name();

        synchronized (rangeFixPainter) {
            RangePainter rp = rangeFixPainter.get(id);
            rp.setRange(range);
            rp.setAccepted(accepted);
            rp.setRejectionReason(reason);            
        }
        
    }

    @Subscribe
    public void on(LblConfig config) {
        synchronized (rangeFixPainter) {
            Vector<LblBeacon> beacons = config.getBeacons();
            for (int id = 0; id < beacons.size(); id++) {
                LblBeacon b = beacons.get(id);
                beaconIds.put(id, b.getBeacon());
                double lat = Math.toDegrees(b.getLat());
                double lon = Math.toDegrees(b.getLon());
                double depth = b.getDepth();
                LocationType lt = new LocationType();
                lt.setLatitudeDegs(lat);
                lt.setLongitudeDegs(lon);
                lt.setDepth(depth);
                if (rangeFixPainter.containsKey(b.getBeacon()))
                    rangeFixPainter.get(b.getBeacon()).setCurLoc(lt);
                else {
                    rangeFixPainter.put(b.getBeacon(), new RangePainter(lt) {
                        public void callParentRepaint() {}
                    });
                }                
            }
        }        
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(Color.magenta);
        synchronized (rangeFixPainter) {
            for (RangePainter rp : rangeFixPainter.values()) {
                rp.paint((Graphics2D) g.create(), renderer);
            }
        }
       
        for (LocationType loc : triangulatedRangesPointsList) {
            Point2D pt = renderer.getScreenPosition(loc);
            g.drawLine((int) pt.getX() - 3, (int) pt.getY(), (int) pt.getX() + 3, (int) pt.getY());
            g.drawLine((int) pt.getX(), (int) pt.getY() - 3, (int) pt.getX(), (int) pt.getY() + 3);
        }
    }
    @Override
    public boolean getVisibleByDefault() {
        return true;
    }
    
    @Override
    public void onMessage(IMCMessage message) {
        // TODO Auto-generated method stub
        
    }

}
