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
 * Author: jqcorreia
 * Nov 16, 2012
 */
package pt.up.fe.dceg.neptus.mra;

import java.io.Serializable;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 *
 */
public class LogMarker implements Serializable, Comparable<LogMarker> {
    private static final long serialVersionUID = 1L;

    public String label;
    public double timestamp;
    public int x;
    public int y;
    public int w;
    public int h;
    /**
     * Latitude in radians
     */
    public double lat;
    /**
     * Longitude in radians
     */
    public double lon;
    
    /**
     * @param label
     * @param timestamp
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public LogMarker(String label, double timestamp, double lat, double lon, int x, int y, int w, int h) {
        super();
        this.label = label;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    
    @Override
    public int compareTo(LogMarker o) {
        if (o.timestamp > timestamp)
            return -1;
        else if (o.timestamp < timestamp)
            return 1;
        else
            return 0;
    }
    
    public LocationType getLocation() {
        return new LocationType(Math.toDegrees(lat), Math.toDegrees(lon));
    }
}