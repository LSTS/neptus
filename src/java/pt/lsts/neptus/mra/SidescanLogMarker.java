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
 * Author: jqcorreia
 * Aug 26, 2013
 */
package pt.lsts.neptus.mra;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;

/**
 * This will be serializable, so no name changes of the fields!
 *
 * @author jqcorreia
 */
public class SidescanLogMarker extends LogMarker {
    private static final long serialVersionUID = 1L;
    private static final int CURRENT_VERSION = 1;
    private double x;
    private double y;
    private int w;
    private int h;
    private double wMeters;// width in meters
    private int subSys;// created on subSys
    private String colorMap;
    private boolean point;
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
    public SidescanLogMarker(String label, double timestamp, double lat, double lon,
                             double x, double y, int w, int h,
                             int subSys, ColorMap colorMap) {
        super(label, timestamp, lat, lon);
        this.setX(x);
        this.setY(y);
        this.setW(w);
        this.setH(h);
        this.setSubSys(subSys);
        this.setColorMap(colorMap.toString());
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
        this.setX(x);
        this.setY(y);
        this.setW(w);
        this.setH(h);
        this.setwMeters(wMeters);
        this.setSubSys(subSys);
        this.setColorMap(colorMap.toString());
    }

    public void setDefaults(int subSys) {//reset Defaults for N/A values
        if (this.getSubSys() == 0) {
            this.setSubSys(subSys);
        }
        if (getColorMap() == null) {
            setColorMap(ColorMapFactory.createBronzeColormap().toString());
        }
        if (this.getW() == 0 && this.getH() == 0) {
            this.setPoint(true);
        }
    }

    public void fixLocation(double latRads, double lonRads) {
        this.setLatRads(latRads);
        this.setLonRads(lonRads);
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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public double getwMeters() {
        return wMeters;
    }

    public void setwMeters(double wMeters) {
        this.wMeters = wMeters;
    }

    public int getSubSys() {
        return subSys;
    }

    public void setSubSys(int subSys) {
        this.subSys = subSys;
    }

    public String getColorMap() {
        return colorMap;
    }

    public void setColorMap(String colorMap) {
        this.colorMap = colorMap;
    }

    public boolean isPoint() {
        return point;
    }

    public void setPoint(boolean point) {
        this.point = point;
    }
}
