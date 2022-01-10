/* Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 30/05/2017
 */
package pt.lsts.neptus.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.kml.KmlReader;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;

/**
 * @author tsm
 */
public class MarksKMLHandler {
    private static final String kmlVersion = "2.2";
    private static final String NL = System.lineSeparator();

    /**
     * Export the given MarkElements to a KML file
     */
    public static boolean exportKML(String exportPathStr, List<MarkElement> marks) {
        String fxExt = FileUtil.getFileExtension(exportPathStr);
        String fxPath = fxExt.isEmpty() ? exportPathStr + ".kml" : exportPathStr; 
        
        File out = new File(fxPath);
        StringBuilder sb = new StringBuilder();
        sb.append(kmlHeader(out.getName()));

        marks.stream().forEach(m -> sb.append(markToPlacemarkXml(m)));
        sb.append("  </Document>").append(NL).append("</kml>").append(NL);

        try {
            String xml = sb.toString();
            PrintWriter writer = new PrintWriter(out, "UTF-8");
            writer.write(xml);
            writer.close();

            NeptusLog.pub().debug(xml);
        }
        catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Export the given URL to a list a of MarkElement
     * */
    public static List<MarkElement> importKML(URL url) {
        KmlReader kml = new KmlReader(url, true);

        if(!kml.streamIsOpen()) {
            NeptusLog.pub().error("Stream could not be opened.");
            return null;
        }

        TreeMap<String, Feature> kmlFeatures = kml.extractFeatures();
        List<MarkElement> marks = new ArrayList<>();
        for(String fname : kmlFeatures.keySet()) {
            Feature ft = kmlFeatures.get(fname);
            if (!(ft instanceof Placemark))
                continue;
            
            Geometry feature = ((Placemark) ft).getGeometry();
            if(!feature.getClass().getSimpleName().equals("Point"))
                continue;

            Point point = (Point) feature;
            Coordinate coords = point.getCoordinates().get(0);
            MarkElement m = new MarkElement();
            LocationType loc = new LocationType(coords.getLatitude(), coords.getLongitude());

            m.setId(fname);
            m.setCenterLocation(loc);
            marks.add(m);
        }

        return marks;
    }

    /**
     * Utility method to get KML format's header
     * */
    private static String kmlHeader(String layerName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                "<kml xmlns=\"http://www.opengis.net/kml/" + kmlVersion + "\">" + NL +
                "  <Document>" + NL +
                "    <name>" + layerName + "</name>" + NL;
    }

    /**
     * Transform a MarkElement into an XML definition, to be used
     * on KML format
     * */
    private static String markToPlacemarkXml(MarkElement m) {
        LocationType absLoc = m.getCenterLocation().getNewAbsoluteLatLonDepth();

        String id = m.getId();
        double latDeg = absLoc.getLatitudeDegs();
        double lonDeg = absLoc.getLongitudeDegs();

        return "    <Placemark>" + NL +
               "      <name>" + id + "</name>" + NL +
               "      <styleUrl>#icon-1899-0288D1-nodesc</styleUrl>" + NL +
               "      <Point>" + NL +
               "        <coordinates>" + lonDeg + "," + latDeg + "</coordinates>" + NL +
               "      </Point>" + NL +
               "    </Placemark>" + NL;
    }
}
