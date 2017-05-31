package pt.lsts.neptus.util;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.kml.KmlReader;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by tsm on 30/05/17.
 */
public class MarksKMLHandler {
    public static boolean exportKML(String exportPathStr, List<MarkElement> marks) {
        return false;
    }

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
}
