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
 * Author: keila
 * 04/07/2018
 */
package pt.lsts.neptus.plugins.groovy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import pt.lsts.imc.dsl.Location;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;

public class MapScript {

    private static MapScript instance = null;

    public static MapScript getInstance() {
        if (instance == null)
            instance = new MapScript();
        return instance;
    }

    private static ConsoleLayout console = null;

    public ConsoleLayout getConsole() {
        return console;
    }

    public void setConsole(ConsoleLayout c) {
        console = c;
    }

    public static List<ParallelepipedElement> dividePP(ParallelepipedElement pp, int w, int l) {
        return dividePP(pp, w, l, -1);
    }

    public static List<ParallelepipedElement> dividePP(ParallelepipedElement pp, int w, int l, int target_row) {
        List<ParallelepipedElement> result = Collections.synchronizedList(new ArrayList<>());
        LocationType[] points = new LocationType[4];
        LocationType[] wp = new LocationType[4];
        LocationType[] mdpts = new LocationType[4];
        LocationType [] pts = new LocationType[4];
        MapType m = pp.getParentMap();
        LocationType aux;

        int n_x = (int) (pp.getWidth() / w);
        double last_x = pp.getWidth() - (w * n_x);
        int n_y = (int) (pp.getLength() / l);
        double last_y = pp.getLength() - (l * n_y);

        for (int i = 0; i < 4; i++) {
            // get corners location from the original parallelepiped
            wp[i] = PlanScript.fromLocation(PlanScript.convertToLocation(pp.getShapePoints().elementAt(i)));
        }
        int row = 0;
        double inc = Math.PI / 2;
        double angle = pp.getYawRad() + inc;
        int i = 0, j = 0;
        //init rotation points
        points[0] = wp[0];
        aux = PlanScript.fromLocation((Location) PlanScript.convertToLocation(wp[3])
                .translateBy(Math.cos(angle) * w * i, Math.sin(angle) * w * i));
        aux = PlanScript.fromLocation(
                PlanScript.midpoint(PlanScript.convertToLocation(points[0]), PlanScript.convertToLocation(aux), l));
        points[3] = aux;
        aux = PlanScript.fromLocation(((Location) PlanScript.convertToLocation(points[3])
                .translateBy(Math.cos(angle) * w, Math.sin(angle) * w)));
        for (i = 0; i < n_x; i++) {
            // build horizontal parts
            ParallelepipedElement p;
            if(row<target_row)
               p = buildX(pp,wp,points,w,l,i,Color.green);
            else
               p = buildX(pp,wp,points,w,l,i,Color.red);
            p.setId("area"+row+i);
            result.add(p);
            m.addObject(p);

            if (i == (n_x - 1) && last_x > 0) {
                pts[0] = points[1];
                pts[3] = points[2];
                if(row<target_row)
                    p = buildX(pp, wp, pts, last_x, l, i,Color.green);
                else 
                    p = buildX(pp, wp, pts, last_x, l, i,Color.red);
                p.setId("area"+row+(i+1));
                result.add(p);
                m.addObject(p);
            }
            
            //init rotation points
            mdpts[0] = points[3];
            mdpts[1] = points[2];
            // build Vertical parts
            for (j = 0,row=1; j < n_y - 1; j++,row++) {
                if(row<target_row)
                    p = buildY(pp,wp,mdpts,w,l,i,Color.green);
                else
                    p = buildY(pp,wp,mdpts,w,l,i,Color.red);
                p.setId("area"+row+i);
                result.add(p);
                m.addObject(p);
                
                if (i == (n_x - 1) && last_x > 0) {
                    pts[0] = mdpts[1];
                    pts[3] = mdpts[2];
                    if(row<target_row)
                        p = buildX(pp, wp, pts, last_x, l, i,Color.green);
                    else 
                        p = buildX(pp, wp, pts, last_x, l, i,Color.red);
                    p.setId("area"+row+(i+1));
                    result.add(p);
                    m.addObject(p);
                }
              //excess
                if(pts!=null && j == (n_y - 2) && i == (n_x - 1) ){
                    if(last_y > 0 && last_x > 0) {
                        pts[0] = pts[3];
                        pts[1] = pts[2];
                        double z = pp.getWidth()/last_x -1;
                        if(row<target_row)
                            p = buildY(pp, wp, pts, last_x, last_y, z,Color.green);
                        else
                            p = buildY(pp, wp, pts, last_x, last_y, z,Color.red);
                        
                        p.setId("area"+(n_y)+(n_x));
                        result.add(p);
                        m.addObject(p);
                    }
                }
                mdpts[0] = mdpts[3];
                mdpts[1] = mdpts[2];
                // next vertices
                points[0] = points[1];
                points[3] = points[2];
            }
            // build the last parallelepiped here
            if (last_y > 0) {
                if(row<target_row)
                    p = buildY(pp, wp, mdpts, w, last_y, i,Color.green);
                else
                    p = buildY(pp, wp, mdpts, w, last_y, i,Color.red);
                
                p.setId("area"+row+i);
                result.add(p);
                m.addObject(p);
            }
            row=0;
        }
        return result;
    }

    /**
     * @param mdpts
     * @param w width of the parallelepiped
     * @param l length of the parallelepiped
     */
    private static ParallelepipedElement buildY(ParallelepipedElement pp, LocationType[] wp, LocationType[] mdpts, double w, double l,
            double i, Color colour) {
        double inc = Math.PI / 2;
        double angle = pp.getYawRad() + inc;
        LocationType aux;
        MapGroup mg = pp.getMapGroup();
        MapType m = pp.getParentMap();
        aux = PlanScript.fromLocation((Location) PlanScript.convertToLocation(wp[3])
                .translateBy(Math.cos(angle) * w * i, Math.sin(angle) * w * i));
        aux = PlanScript.fromLocation(
                PlanScript.midpoint(PlanScript.convertToLocation(mdpts[0]), PlanScript.convertToLocation(aux), l));
        mdpts[3] = aux;
        aux = PlanScript.fromLocation(((Location) PlanScript.convertToLocation(mdpts[3])
                .translateBy(Math.cos(angle) * w, Math.sin(angle) * w)));
        mdpts[2] = aux;
        ParallelepipedElement pap = new ParallelepipedElement(mg, m);
        pap.setYawDeg(pp.getYawDeg());
        pap.setCenterLocation(centroid(mdpts));
        pap.setHeight(0);
        pap.setWidth(w);
        pap.setLength(l);
        pap.setColor(colour);
        return pap;
    }

    /**
     * @param points
     * @param w width of the parallelepiped
     * @param l length of the parallelepiped
     */
    private static ParallelepipedElement buildX(ParallelepipedElement pp, LocationType[] wp, LocationType[] points, double w, double l,
            int i, Color colour) {
        double inc = Math.PI / 2;
        double angle = pp.getYawRad() + inc;
        LocationType aux;
        MapGroup mg = pp.getMapGroup();
        MapType m = pp.getParentMap();
        // build horizontal parts
        aux = PlanScript.fromLocation(((Location) PlanScript.convertToLocation(points[0])
                .translateBy(Math.cos(angle) * w, Math.sin(angle) * w)));
        points[1] = aux;
        aux = PlanScript.fromLocation(((Location) PlanScript.convertToLocation(points[3])
                .translateBy(Math.cos(angle) * w, Math.sin(angle) * w)));
        points[2] = aux;
        ParallelepipedElement pap = new ParallelepipedElement(mg, m);
        pap.setYawDeg(pp.getYawDeg());
        pap.setCenterLocation(centroid(points));
        pap.setHeight(0);
        pap.setWidth(w);
        pap.setLength(l);
        pap.setColor(colour);
        
        return pap;
    }

    /**
     * @param pts
     * @return
     */
    private static LocationType centroid(LocationType[] pts) {
        return CoordinateUtil.computeLocationsCentroid(Arrays.asList(pts));
    }

    public static ParallelepipedElement pp(int w, int l, LocationType... wps) {
        if (console != null) {
            ParallelepipedElement pap;
            MapGroup mg = MapGroup.getMapGroupInstance(console.getMission());// MapGroup mg = pp.getMapGroup();
            MapType m = mg.getMaps()[0];// MapType m = pp.getParentMap();
            pap = new ParallelepipedElement(mg, m);
            pap.setCenterLocation(centroid(wps));
            pap.setColor(Color.cyan);
            pap.setHeight(0);
            pap.setWidth(w);
            pap.setLength(l);
            m.addObject(pap);
            return pap;
        }
        return null;
    }

    public static MarkElement mark(String name, LocationType loc) {
        if (console != null) {
            MapGroup mg = MapGroup.getMapGroupInstance(console.getMission());
            MapType m = mg.getMaps()[0];
            MarkElement elem = new MarkElement(mg, m);
            elem.setId(name);
            elem.setCenterLocation(loc);
            m.addObject(elem);
            console.getMission().save(false);
            console.warnMissionListeners();
            return elem;
        }
        return null;
    }

    public static MarkElement mark(String name, Location loc) {
        return mark(name, PlanScript.fromLocation(loc));
    }

}
