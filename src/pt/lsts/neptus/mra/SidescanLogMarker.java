/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Aug 26, 2013
 */
package pt.lsts.neptus.mra;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;

/**
 * This will be serializable, so no name changes of the fields!
 * @author jqcorreia
 * 
 */
public class SidescanLogMarker extends LogMarker {
    private static final long serialVersionUID = 1L;
    
    private static final int CURRENT_VERSION = 1;

    public double x;
    public double y;
    public int w;
    public int h;
    public double wMeters;// width in meters
    public int subSys;// created on subSys
    public String colorMap;
    public boolean point;

    /** Added version info. For the loaded old marks this value will be 0. */
    private int sidescanMarkVersion = CURRENT_VERSION;
    
    /**
     * @param label
     * @param timestamp
     * @param lat
     * @param lon
     * @param x
     * @param y
     * @param w
     * @param h
     * @param subSys
     * @param colorMap
     */
    public SidescanLogMarker(String label, double timestamp, double lat, double lon, double x, double y, int w, int h,
            int subSys, ColorMap colorMap) {
        super(label, timestamp, lat, lon);
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.subSys = subSys;
        this.colorMap = colorMap.toString();
    }

    /**
     * @param label
     * @param timestamp
     * @param lat
     * @param lon
     * @param x
     * @param y
     * @param w
     * @param h
     * @param wMeters
     * @param subSys
     * @param colorMap
     */
    public SidescanLogMarker(String label, double timestamp, double lat, double lon, double x, double y, int w, int h,
            double wMeters, int subSys, ColorMap colorMap) {
        super(label, timestamp, lat, lon);
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.wMeters = wMeters;
        this.subSys = subSys;
        this.colorMap = colorMap.toString();
    }

    public void setDefaults(int subSys){//reset Defaults for N/A values
        if (this.subSys==0){
            this.subSys=subSys;
        }
        if (colorMap==null){
            colorMap = ColorMapFactory.createBronzeColormap().toString();
        }
        if (this.w==0 && this.h==0){
            this.point=true;
        }
    }

    public void fixLocation(double latRads, double lonRads) {
        this.lat = latRads;
        this.lon = lonRads;
    }
    
    /**
     * @return the SidescanMarkVersion
     */
    public int getSidescanMarkVersion() {
        return sidescanMarkVersion;
    }
    
    /**
     * This will set the version to {@link #CURRENT_VERSION} (currently {@value #CURRENT_VERSION})
     */
    public void resetSidescanMarkVersion() {
        this.sidescanMarkVersion = CURRENT_VERSION;
    }
}
