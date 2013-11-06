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
 * Author: Paulo Dias
 * 15/05/2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.util.lbl.LBLTriangulationHelper;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.acoustic.RangePainter;

/**
 * @author pdias
 *
 */
@LayerPriority(priority = -50)
public class LBLRangesReplay implements LogReplayLayer {

    private LinkedList<TransponderElement> transponders = new LinkedList<TransponderElement>();

    private LocationType start = new LocationType();
    private LinkedList<LocationType> triangulatedRangesPointsList = new LinkedList<LocationType>();
   
    private LinkedList<RangePainter> rangeFixPainter = new LinkedList<RangePainter>();

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
    public boolean canBeApplied(IMraLogGroup source) {
        return (source.getLog("LblRangeAcceptance") != null && source.getLog("LblConfig") != null);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.replay.LLFReplayLayer#parse(pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup)
     */
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
            
            for (TransponderElement te : transponders) {
                RangePainter rp = new RangePainter(te.getCenterLocation()) {
                    @Override
                    public void callParentRepaint() {
                    }
                };
                rangeFixPainter.add(rp);
            }
            
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
//            IMraLog lblRangesLog = source.getLog("LBLRanges");
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
//                String accep = m.getValue("acceptance").toString();
                
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

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.replay.LLFReplayLayer#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return new String[] { "LblRangeAcceptance" };
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mra.replay.LLFReplayLayer#onMessage(pt.up.fe.dceg.neptus.imc.IMCMessage)
     */
    @Override
    public void onMessage(IMCMessage message) {
        
        if ("LblRangeAcceptance".equalsIgnoreCase(message.getAbbrev())) {
            
            if (rangeFixPainter.isEmpty())
                return;
            int id = message.getInteger("id");
            double range = message.getDouble("range");
            String accep = message.getString("acceptance");
            String reason = message.getString("reason");
            // if (reason != null) NeptusLog.pub().info("<###> "+reason);

            
            RangePainter rp = rangeFixPainter.get(id);
            rp.setRange(range);
            rp.setAccepted(accep == null || "ACCEPTED".equalsIgnoreCase(accep) ? true : false);
            rp.setRejectionReason(reason);
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(Color.magenta);
        for (RangePainter rp : rangeFixPainter) {
            rp.paint((Graphics2D) g.create(), renderer);
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

}
