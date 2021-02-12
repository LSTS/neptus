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

import java.util.Date;
import java.util.LinkedHashMap;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author zp
 *
 */
public class AssetPosition implements Comparable<AssetPosition> {
    private String assetName;
    private LocationType loc = null;
    private double yaw = Double.NaN;
    private double sog = Double.NaN;
    private long timestamp;
    private double accuracy = Double.NaN;
    private String source = "unknown";
    private String type = "Sensor";
    private LinkedHashMap<String, String> extraInfo = new LinkedHashMap<String, String>();
    
    public AssetPosition(String asset, double latDegrees, double lonDegrees) {
        this.timestamp = System.currentTimeMillis();
        this.assetName = asset;
        this.loc = new LocationType(latDegrees, lonDegrees);
        this.loc.setHeight(0);
    }

    public AssetPosition(String asset, double latDegrees, double lonDegrees, double height) {
        this.timestamp = System.currentTimeMillis();
        this.assetName = asset;
        this.loc = new LocationType(latDegrees, lonDegrees);
        this.loc.setHeight(height);
    }

    @Override
    public String toString() {
        return assetName+"("+new Date(timestamp)+": "+loc+")";
    }
    
    /**
     * @return the assetName
     */
    public String getAssetName() {
        return assetName;
    }
   
    /**
     * @return the loc
     */
    public LocationType getLoc() {
        return loc;
    }
    
    /**
     * @return the yaw
     */
    public double getYaw() {
        return yaw;
    }

    /**
     * @param yaw the yaw to set
     */
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    /**
     * @return the sog
     */
    public double getSog() {
        return sog;
    }

    /**
     * @param sog the sog to set
     */
    public void setSog(double sog) {
        this.sog = sog;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the accuracy
     */
    public double getAccuracy() {
        return accuracy;
    }

    /**
     * @param accuracy the accuracy to set
     */
    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }
    
    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
    
    public long getAge() {
        return System.currentTimeMillis() - getTimestamp();
    }

    @Override
    public int compareTo(AssetPosition o) {
        return new Long(getTimestamp()).compareTo(o.getTimestamp());
    }
    
    @Override
    public int hashCode() {
        return (assetName + "_" + (getTimestamp()/1000)).hashCode();
    }
    
    public void putExtra(String key, String value) {
        extraInfo.put(key, value);
    }
    
    public String getHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>").
            append("<table><tr><th colspan='2'>"+getAssetName()+"</th></tr>").
            append("<tr><td>Type:</td><td>"+getType()+"</td></tr>").
            append("<tr><td>Source:</td><td>"+getSource()+"</td></tr>").
            append("<tr><td>Timestamp:</td><td>"+new Date(getTimestamp())+"</td></tr>").
            append("<tr><td>Age:</td><td>"+DateTimeUtil.milliSecondsToFormatedString(getAge())+"</td></tr>").
            append("<tr><td>Location:</td><td>"+getLoc()+"</td></tr>");
        
        for (String key : extraInfo.keySet())
            sb.append("<tr><td>"+key+":</td><td>"+extraInfo.get(key)+"</td></tr>");
        
        sb.append("</table></html>");
        return sb.toString();
    }
}
