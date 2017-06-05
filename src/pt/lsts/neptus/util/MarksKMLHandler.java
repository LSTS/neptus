package pt.lsts.neptus.util;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.kml.KmlReader;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by tsm on 30/05/17.
 */
public class MarksKMLHandler {
    private static final String kmlVersion = "2.2";

    /**
     * Export the given MarkElements to a KML file
     * */
    public static boolean exportKML(String exportPathStr, List<MarkElement> marks) {
        File out = new File(exportPathStr + ".kml");
        StringBuilder sb = new StringBuilder();
        sb.append(kmlHeader(out.getName()));

        marks.stream().forEach(m -> sb.append(markToPlacemarkXml(m)));
        sb.append("</Document> \n" +
                "</kml> \n");

        try {
            String xml = sb.toString();
            PrintWriter writer = new PrintWriter(out, "UTF-8");
            writer.write(xml);
            writer.close();

            NeptusLog.pub().debug(xml);

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
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

        TreeMap<String, Placemark> kmlFeatures = kml.extractFeatures();
        List<MarkElement> marks = new ArrayList<>();
        for(String fname : kmlFeatures.keySet()) {
            Geometry feature = kmlFeatures.get(fname).getGeometry();
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
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/" + kmlVersion + "\">\n" +
                "  <Document>\n" +
                "    <name>" + layerName + "</name>\n";
    }

    /**
     * Tranform a MarkElement into an XML definition, to be used
     * on KML format
     * */
    private static String markToPlacemarkXml(MarkElement m) {
        LocationType absLoc = m.getCenterLocation().getNewAbsoluteLatLonDepth();

        String id = m.getId();
        double latDeg = absLoc.getLatitudeDegs();
        double lonDeg = absLoc.getLongitudeDegs();

        return "<Placemark>\n" +
                "      <name>" + id + "</name>\n" +
                "      <styleUrl>#icon-1899-0288D1-nodesc</styleUrl>\n" +
                "      <Point>\n" +
                "        <coordinates>" + latDeg + "," + lonDeg + "</coordinates>\n" +
                "      </Point>\n" +
                "    </Placemark>\n";
    }
}
