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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 22/05/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;

/**
 * Utility to create an load base data (location and speed from and to XML.
 * For speed Uses NeptusProperty with "Speed" and "Speed Units" as names.
 * 
 * @author pdias
 *
 */
public class ManeuversXMLUtil {

    private static final String BASE_POINT = "basePoint";

    private ManeuversXMLUtil() {
    }

    public static Document createBaseDoc(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        root.addAttribute("kind", "automatic");
        return document;
    }

    public static <M extends LocatedManeuver> Element addLocation(Element root, M maneuver) {
        return addLocation(root, maneuver.getManeuverLocation(), BASE_POINT);
    }

    public static <M extends LocatedManeuver> Element addLocation(Element root, M maneuver, String locElementName) {
        return addLocation(root, maneuver.getManeuverLocation(), locElementName);
    }

    public static Element addLocation(Element root, ManeuverLocation loc) {
        return addLocation(root, loc, BASE_POINT);
    }

    public static Element addLocation(Element root, ManeuverLocation loc, String locElementName) {
        Element basePoint = root.addElement(locElementName);
        Element point = loc.asElement("point");
        basePoint.add(point);
        return basePoint;
    }

    public static <M extends LocatedManeuver> ManeuverLocation parseLocation(Element root, M maneuver, String locElementName) {
        ManeuverLocation loc = parseLocation(root, locElementName);
        maneuver.setManeuverLocation(loc);
        return loc;
    }

    public static <M extends LocatedManeuver> ManeuverLocation parseLocation(Element root, M maneuver) {
        ManeuverLocation loc = parseLocation(root);
        maneuver.setManeuverLocation(loc);
        return loc;
    }

    public static ManeuverLocation parseLocation(Element root) {
        return parseLocation(root, BASE_POINT);
    }

    public static ManeuverLocation parseLocation(Element root, String locElementName) {
        Node node = root.selectSingleNode("//" + locElementName + "/point");
        ManeuverLocation loc = new ManeuverLocation();
        loc.load(node.asXML());
        return loc;
    }

    public static <M extends Maneuver> Element addSpeed(Element root, M maneuver) throws Exception {
        Field sField = getFieldByName(maneuver, "Speed");
        if (sField == null)
            sField = getFieldByName(maneuver, "speed");
        Field sUField = getFieldByName(maneuver, "Speed Units");
        if (sUField == null)
            sUField = getFieldByName(maneuver, "Speed units");
        if (sUField == null)
            sUField = getFieldByName(maneuver, "speed units");
        if (sUField == null)
            sUField = getFieldByName(maneuver, "speedUnits");

        if (sField != null && sUField != null) {
            double speed = (double) sField.get(maneuver);
            Maneuver.SPEED_UNITS speedUnits = (SPEED_UNITS) sUField.get(maneuver);
            return addSpeed(root, speed, speedUnits);
        }
        throw new Exception("No Speed to found!");
    }

    public static Element addSpeed(Element root, double speed, Maneuver.SPEED_UNITS units) {
        Element speedElm = root.addElement("speed");
        speedElm.addAttribute("unit", units.getString());
        speedElm.setText(String.valueOf(speed));
        return speedElm;
    }

    public static double parseSpeed(Element root) {
        Node speedNode = root.selectSingleNode("//speed");
        double speed = Double.parseDouble(speedNode.getText());
//        speedUnits = SPEED_UNITS.parse(speedNode.valueOf("@unit"));
        return speed;
    }
    
    public static Maneuver.SPEED_UNITS parseSpeedUnits(Element root) {
        Node speedNode = root.selectSingleNode("//speed");
//        double speed = Double.parseDouble(speedNode.getText());
        SPEED_UNITS speedUnits = SPEED_UNITS.parse(speedNode.valueOf("@unit"));
        return speedUnits;
    }

    public static <M extends Maneuver>  double parseSpeed(Element root, M maneuver) throws Exception {
        Node speedNode = root.selectSingleNode("//speed");
        double speed = Double.parseDouble(speedNode.getText());
        SPEED_UNITS speedUnits = SPEED_UNITS.parse(speedNode.valueOf("@unit"));
        Field sField = getFieldByName(maneuver, "Speed");
        if (sField == null)
            sField = getFieldByName(maneuver, "speed");
        sField.set(maneuver, speed);
        Field sUField = getFieldByName(maneuver, "Speed Units");
        if (sUField == null)
            sUField = getFieldByName(maneuver, "Speed units");
        if (sUField == null)
            sUField = getFieldByName(maneuver, "speed units");
        if (sUField == null)
            sUField = getFieldByName(maneuver, "speedUnits");
        sUField.set(maneuver, speedUnits);
        return speed;
    }

    // -----------------------------------------------------------------

    private static Field[] getFields(Object o) {
        Class<?> c;
        if (o instanceof Class<?>)
            c = (Class<?>) o;
        else
            c = o.getClass();

        HashSet<Field> fields = new LinkedHashSet<>();
        for (Field f : c.getFields())
            fields.add(f);
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            fields.add(f);
        }
        return fields.toArray(new Field[0]);
    }

    private static Field getFieldByName(Object obj, String name) {
        Field[] fields = getFields(obj);
        for (Field f : fields) {
            NeptusProperty a = f.getAnnotation(NeptusProperty.class);

            if (a != null) {
                f.setAccessible(true);
                String nf = a.name();
                if (nf == null) {
                    nf = f.getName();
                    name = name.replaceAll(" ", "");
                }
                if (name.equalsIgnoreCase(nf))
                    return f;
            }
        }
        return null;
    }
}
