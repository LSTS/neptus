/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto ?
 * 
 */
package pt.lsts.neptus.plugins.spot;

import java.text.SimpleDateFormat;

public class SpotMessage implements Comparable<SpotMessage> {

    public final double latitude, longitude;
    public final long timestamp;
    String id;

    public SpotMessage(double lat, double lon, long timestamp, String id) {
        this.latitude = lat;
        this.longitude = lon;
        this.timestamp = timestamp;
        this.id = id;
    }

    @Override
    public int compareTo(SpotMessage o) {
        return (int) (timestamp - o.timestamp);
    }

    @Override
    public int hashCode() {
        return new String("" + latitude + longitude + id + timestamp).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    public static String latString(double latitude, String posneg) {
        String ns = posneg.substring(0, 1);
        if (latitude < 0) {
            ns = posneg.substring(1);
            latitude = -latitude;
        }
        String ret = ((int) latitude) + ns;
        latitude -= (int) latitude;

        latitude *= 60;
        ret += (int) latitude + "'";
        latitude -= (int) latitude;
        ret += (float) (latitude * 60);

        return ret;
    }

    public String asVehicleState() {
        SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.SSS");
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");

        String xml = "<VehicleState id='" + id + "' time='" + time.format(timestamp) + "' date='"
                + date.format(timestamp) + "'>\n";
        xml += "  <coordinate>\n    <id>id</id>\n    <name>name</name>\n    <coordinate>\n";
        xml += "      <latitude>" + latString(latitude, "NS") + "</latitude>\n";
        xml += "      <longitude>" + latString(longitude, "EW") + "</longitude>\n";
        xml += "      <depth>0</depth>\n    </coordinate>\n  </coordinate>\n";
        xml += "  <attitude><phi>0</phi><theta>0</theta><psi>0</psi></attitude>\n  <imc/>\n";
        xml += "</VehicleState>\n";
        return xml;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return id + " (" + latitude + ", " + longitude + ") " + timestamp;
    }
}
