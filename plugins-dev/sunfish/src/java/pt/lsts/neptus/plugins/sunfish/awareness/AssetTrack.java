/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class AssetTrack {

    private String assetName = null;
    private String friendlyName = null;
    private ArrayList<AssetPosition> track = new ArrayList<AssetPosition>();
    private Color color;
    
    public AssetTrack(String assetName, Color color) {
        this.assetName = assetName;
        this.color = color;
    }

    /**
     * @return the track
     */
    public List<AssetPosition> getTrack() {
        return new ArrayList<AssetPosition>(track);
        
    }
    
    /**
     * @return the track
     */
    public List<AssetPosition> getTrack(int maxPositions, long sinceTimestamp) {
        List<AssetPosition> trackSince = getTrack();
        int count = 0;
        for (int i = trackSince.size()-1; i >= 0; i--) {
            if (count == maxPositions || trackSince.get(i).getTimestamp() < sinceTimestamp) {
                if (i == trackSince.size()-1)
                    return new ArrayList<AssetPosition>();
                return trackSince.subList(i+1, trackSince.size());
            }
            count ++;
        }
        return trackSince;
    }
    
    public AssetPosition getLatest() {
        return track.get(track.size()-1);                
    }
    
    public AssetPosition getLatest(long beforeDate) {
        AssetPosition valid = null;
        for (AssetPosition p : getTrack()) {
            if (p.getTimestamp() <= beforeDate)
                valid = p;
        }
        return valid;
    }
    
    public AssetPosition getPrediction() {
        if (track.size() > 2) {
            AssetPosition last = getLatest();
            AssetPosition butLast = track.get(track.size()-2);
            double timeDiff = last.getTimestamp()/1000.0 - butLast.getTimestamp()/1000.0;
            double dist = last.getLoc().getHorizontalDistanceInMeters(butLast.getLoc());
            double angle = butLast.getLoc().getXYAngle(last.getLoc());
            double speed = dist / timeDiff;
            double ellapsedTime = last.getAge()/1000.0;
            double offsetNorth = (ellapsedTime * speed) * Math.cos(angle);
            double offsetEast = (ellapsedTime * speed) * Math.sin(angle);
            LocationType loc = new LocationType(last.getLoc());
            loc.translatePosition(offsetNorth, offsetEast, 0).convertToAbsoluteLatLonDepth();
            return new AssetPosition(getAssetName(), loc.getLatitudeDegs(), loc.getLongitudeDegs());            
        }
        return null;
    }

    /**
     * @return the assetName
     */
    public String getAssetName() {
        return assetName;
    }
    
    /**
     * @param color the color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }
    
    public boolean addPosition(AssetPosition position) {
        if (!track.isEmpty()) {
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getTimestamp() == position.getTimestamp()) {
                    return false;
                }
                if (track.get(i).getTimestamp() > position.getTimestamp()) {
                    track.add(i, position);
                    return true;
                }
            }            
        }
        track.add(position);
        return true;
    }

    /**
     * @return the friendlyName
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @param friendlyName the friendlyName to set
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    
}
