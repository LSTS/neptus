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
 * Author: zp
 * Apr 3, 2018
 */
package pt.lsts.neptus.endurance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * This class holds a track (composed of several states) of an Asset
 * @author zp
 *
 */
public class AssetTrack {

    private TreeMap<Date, AssetState> states = new TreeMap<>();
    private TreeMap<Date, AssetState> plan = new TreeMap<>();

    /**
     * Set the asset's plan (future states)
     * @param plan The plan to attribute to this track
     */
    public void setPlan(Plan plan) {
        this.plan.clear();
        if (plan == null)
            return;
        
        for (Waypoint wpt : plan.waypoints()) {
            if (wpt.getArrivalTime() != null && wpt.getArrivalTime().after(new Date())) {
                AssetState state = AssetState.builder().withLatitude(wpt.getLatitude())
                        .withLongitude(wpt.getLongitude()).withTimestamp(wpt.getArrivalTime()).build();
                this.plan.put(wpt.getArrivalTime(), state);
            }
        }
    }

    /**
     * Set a received asset state
     * @param state The state to add to the track
     */
    public void setState(AssetState state) {
        
        synchronized (states) {
            if (state.getTimestamp() != null)
                states.put(state.getTimestamp(), state);
        }
    }

    /**
     * Clear all states
     */
    public void clear() {
        synchronized (states) {
            states.clear();
        }
        plan.clear();
    }

    /**
     * Calculate a state by interpolating previous and next states
     * @param d The date of the state to calculate
     * @return A state for the given date or <code>null</code> if it cannot be calculated
     */
    public AssetState synthesize(Date d) {
        TreeMap<Date, AssetState> track;

        synchronized (states) {
            track = new TreeMap<>(states);
        }

        // if we are looking for something in the future, also account for the plan
        if (d.after(new Date()) && plan != null && !plan.isEmpty()) {
            track.putAll(plan);
        }

        Entry<Date, AssetState> nextOne = track.ceilingEntry(d);
        Entry<Date, AssetState> previousOne = track.floorEntry(d);

        if (nextOne == null && previousOne == null) {
            System.err.println("Could not synthesize state for " + d);
            return null;
        }

        if (nextOne == null && previousOne != null) {
            return previousOne.getValue();
        }

        if (previousOne == null && nextOne != null) {
            return nextOne.getValue();
        }

        double timeDiff = nextOne.getKey().getTime() - previousOne.getKey().getTime();
        LocationType nextLoc = new LocationType(nextOne.getValue().getLatitude(), nextOne.getValue().getLongitude());
        LocationType prevLoc = new LocationType(previousOne.getValue().getLatitude(),
                previousOne.getValue().getLongitude());
        double[] offsets = nextLoc.getOffsetFrom(prevLoc);
        LocationType loc = new LocationType(prevLoc);
        double ratio = (d.getTime() - previousOne.getKey().getTime()) / timeDiff;
        loc.translatePosition(offsets[0] * ratio, offsets[1] * ratio, 0).convertToAbsoluteLatLonDepth();
        double heading = Math.atan2(offsets[1], offsets[0]);

        return AssetState.builder().withHeading(Math.toDegrees(heading)).withLatitude(loc.getLatitudeDegs())
                .withLongitude(loc.getLongitudeDegs()).withTimestamp(d).build();
    }

    /**
     * Read an AssetTrack from file system
     * @param f The file from where to read
     * @return The read AssetTrack
     */
    public static AssetTrack deserialize(File f) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream iis = new ObjectInputStream(new FileInputStream(f))) {
            return (AssetTrack) iis.readObject();    
        }
        
    }
    
    /**
     * Writes an AssetTrack to disk
     * @param f The file where to write the track
     */
    public void serialize(File f) throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(this);                
        }
    }
    
    /**
     * Converts this track into a textual representation
     */
    public String toString() {
        TreeMap<Date, AssetState> track;

        synchronized (states) {
            track = new TreeMap<>(states);
            track.putAll(plan);
        }
        
        StringBuilder sb = new StringBuilder();
        for (Entry<Date, AssetState> entry : track.entrySet()) {
            sb.append(entry.getKey()+" :: "+entry.getValue().getLatitude()+" / "+entry.getValue().getLongitude()+"\n");
        }
        
        return sb.toString();
    }    
}
