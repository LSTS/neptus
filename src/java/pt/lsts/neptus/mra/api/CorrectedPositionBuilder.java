/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 09/05/2016
 */
package pt.lsts.neptus.mra.api;

import java.util.ArrayList;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * This helper class calculates the adjusted position from estimated ones.
 * 
 * This works by feeding {@link EstimatedState}s to {@link #update(EstimatedState)} method.
 * 
 * Once finished, just call {@link #getPositions()}, a finish up will me call to final creation of positions list. Once
 * this is called this will be locked for further updates. To unlock call {@link #reset()} and you can restart the
 * process.
 * 
 * @author pdias
 * @author zp (original code)
 */
public class CorrectedPositionBuilder {

    private ArrayList<SystemPositionAndAttitude> positions = new ArrayList<>();

    private ArrayList<EstimatedState> nonAdjusted = new ArrayList<>();
    private ArrayList<LocationType> nonAdjustedLocs = new ArrayList<>();

    private LocationType lastLoc = null;
    private double lastTime = 0;

    private boolean finished = false;

    public CorrectedPositionBuilder() {
    }

    /**
     * This will reset the instance and {@link #update(EstimatedState)} can be called.
     */
    public void reset() {
        positions.clear();
        resetVariables();
        finished = false;
    }

    private void resetVariables() {
        nonAdjusted.clear();
        nonAdjustedLocs.clear();

        lastLoc = null;
        lastTime = 0;
    }

    /**
     * This will update the positions with an estimated position.
     * 
     * @param es
     * @return
     */
    public boolean update(EstimatedState es) {
        if (finished)
            return false;

        LocationType thisLoc = new LocationType();
        thisLoc.setLatitudeRads(es.getLat());
        thisLoc.setLongitudeRads(es.getLon());
        if (es.getDepth() > 0)
            thisLoc.setDepth(es.getDepth());
        if (es.getAlt() > 0)
            thisLoc.setDepth(-es.getAlt());
        thisLoc.translatePosition(es.getX(), es.getY(), 0);
        double speed = Math.sqrt(es.getU() * es.getU() + es.getV() * es.getV() + es.getW() * es.getW());

        thisLoc.convertToAbsoluteLatLonDepth();

        if (lastLoc != null) {
            double expectedDiff = speed * (es.getTimestamp() - lastTime);

            lastTime = es.getTimestamp();

            double diff = lastLoc.getHorizontalDistanceInMeters(thisLoc);
            if (diff < expectedDiff * 3) {
                nonAdjusted.add(es);
                nonAdjustedLocs.add(thisLoc);
            }
            else {
                if (!nonAdjusted.isEmpty()) {
                    double[] adjustment = thisLoc.getOffsetFrom(lastLoc);
                    EstimatedState firstNonAdjusted = nonAdjusted.get(0);
                    double timeOfAdjustment = es.getTimestamp() - firstNonAdjusted.getTimestamp();
                    double xIncPerSec = adjustment[0] / timeOfAdjustment;
                    double yIncPerSec = adjustment[1] / timeOfAdjustment;

                    for (int i = 0; i < nonAdjusted.size(); i++) {
                        EstimatedState adj = nonAdjusted.get(i);
                        LocationType loc = nonAdjustedLocs.get(i);
                        loc.translatePosition(xIncPerSec * (adj.getTimestamp() - firstNonAdjusted.getTimestamp()),
                                yIncPerSec * (adj.getTimestamp() - firstNonAdjusted.getTimestamp()), 0);

                        loc.convertToAbsoluteLatLonDepth();
                        loc.setDepth(adj.getDepth());
                        SystemPositionAndAttitude p = new SystemPositionAndAttitude(adj);
                        p.setPosition(loc);
                        p.setAltitude(adj.getAlt());
                        p.setTime((long) (adj.getTimestamp() * 1000));
                        positions.add(p);
                    }
                    nonAdjusted.clear();
                    nonAdjustedLocs.clear();
                    nonAdjusted.add(es);
                    nonAdjustedLocs.add(thisLoc);
                }
            }
        }
        else {
            SystemPositionAndAttitude p = new SystemPositionAndAttitude(es);
            p.setPosition(thisLoc);
            p.setAltitude(es.getAlt());
            p.setTime((long) (es.getTimestamp() * 1000));
            positions.add(p);
        }
        lastLoc = thisLoc;
        lastTime = es.getTimestamp();

        return true;
    }

    private void finishUp() {
        for (int i = 0; i < nonAdjusted.size(); i++) {
            EstimatedState adj = nonAdjusted.get(i);
            LocationType loc = nonAdjustedLocs.get(i);
            loc.convertToAbsoluteLatLonDepth();
            loc.setDepth(adj.getDepth());
            SystemPositionAndAttitude p = new SystemPositionAndAttitude(adj);
            p.setPosition(loc);
            p.setAltitude(adj.getAlt());
            p.setTime((long) (adj.getTimestamp() * 1000));
            positions.add(p);
        }

        finished = true;
        resetVariables();
    }

    /**
     * This will return the corrected position list and locks this up to further updates.
     * 
     * @return the positions
     */
    public ArrayList<SystemPositionAndAttitude> getPositions() {
        if (!finished)
            finishUp();

        return positions;
    }
}
