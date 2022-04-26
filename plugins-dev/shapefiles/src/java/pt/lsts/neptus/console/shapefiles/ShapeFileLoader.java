/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 31/01/2015
 */
package pt.lsts.neptus.console.shapefiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader;
import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences;
import org.nocrala.tools.gis.data.esri.shapefile.header.ShapeFileHeader;
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;

/**
 * @author pdias
 *
 */
public class ShapeFileLoader {

    /**
     * 
     */
    public ShapeFileLoader() {
    }

    public static ShapeFileObject loadShapeFileObject(File fx) {
        ShapeFileObject ret = null;
        FileInputStream is = null;
        try {
            is = new FileInputStream(fx);
            ValidationPreferences prefs = new ValidationPreferences();
            prefs.setMaxNumberOfPointsPerShape(800000);

            ShapeFileReader r = new ShapeFileReader(is, prefs);

            ShapeFileHeader h = r.getHeader();
            System.out.println("The shape type of this files is " + h.getShapeType());

            ArrayList<AbstractShape> shapes = new ArrayList<>();
            int total = 0;
            AbstractShape s;
            while ((s = r.next()) != null) {
                switch (s.getShapeType()) {
                    case POINT:
                    case MULTIPOINT_Z:
                    case POLYGON:
                    case POLYLINE:
                        shapes.add(s);
                        break;
                    case MULTIPATCH:
                    case MULTIPOINT:
                    case MULTIPOINT_M:
                    case NULL:
                    case POINT_M:
                    case POINT_Z:
                    case POLYGON_M:
                    case POLYGON_Z:
                    case POLYLINE_M:
                    case POLYLINE_Z:
                    default:
                      System.out.println("Read other type of shape." + s.getShapeType());
                }
                total++;
            }

            System.out.println("Total shapes read: " + total);
            if (!shapes.isEmpty())
                ret = new ShapeFileObject(fx.getName(), shapes);
            
            return ret;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (is != null)
                try {
                    is.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
