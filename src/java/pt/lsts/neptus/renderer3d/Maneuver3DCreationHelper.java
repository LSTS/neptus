/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 1 de Fev de 2013
 */
package pt.lsts.neptus.renderer3d;

import java.awt.Color;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.Material;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;

/**
 * @author pdias
 * 
 */
public class Maneuver3DCreationHelper {

    public static Obj3D getModel3DManeuverForRender(Maneuver man) {

        if (man instanceof FollowTrajectory)
            return getFollowPathModel3D((FollowTrajectory) man);
        else if (man instanceof Loiter)
            return getLoiterModel3D((Loiter) man);
        else if (man instanceof RowsManeuver)
            return getRowsManeuverModel3D((RowsManeuver) man);
        else if (man instanceof StationKeeping)
            return getStationKeepingModel3D((StationKeeping) man);

        return null;
    }

    public static Obj3D getFollowPathModel3D(FollowTrajectory follow) {
        Obj3D ret = new Obj3D();

        ret.setModel3D(getPointLineList3Ddouble(follow.getPathPoints()));

        boolean b = true;
        for (double[] p : follow.getPathPoints()) {
            Obj3D aux = new Obj3D();
            if (b)
                aux.setModel3D(Util3D.getSphere(Color.GREEN, 0.6, 0.6, 0.6));
            else
                aux.setModel3D(Util3D.getSphere(Color.RED, 0.6, 0.6, 0.6));
            b = !b;

            aux.setPos(p);

            ret.addObj3D(aux);
        }
        // ret.setPos(getPosition());
        return ret;
    }

    public static Point3d[] getPath(Vector<Point3d> offsets3D) {
        Vector<Point3d> points = offsets3D;

        if (points == null || points.size() <= 1)
            return new Point3d[] {};

        Point3d[] pts = new Point3d[(points.size() * 2) - 2];
        int i = 1;
        int x = 1;
        pts[0] = (Point3d) points.firstElement();
        while (i < points.size() - 1) {
            pts[x] = (Point3d) points.get(i);
            x++;
            pts[x] = (Point3d) points.get(i);
            i++;
            x++;
        }
        pts[pts.length - 1] = (Point3d) points.get(points.size() - 1);

        return pts;
    }

    public static TransformGroup getPointLineList3Ddouble(List<double[]> list) {
        Vector<Point3d> pointsv = new Vector<Point3d>();

        for (double[] p : list) {
            Point3d point = new Point3d(p);
            pointsv.add(point);
        }

        return getPointLineList3D(pointsv);
    }

    public static TransformGroup getPointLineList3D(Vector<Point3d> points) {
        Shape3D shape3D;
        TransformGroup model = new TransformGroup();
        Appearance appearance = new Appearance();

        Material mat = new Material();
        Color3f c = new Color3f(1.0f, 0.0f, 0.0f);
        c.set(Color.YELLOW);
        mat.setEmissiveColor(c);
        mat.setAmbientColor(c);
        mat.setDiffuseColor(c);
        // mat.setSpecularColor(c);
        // mat.setShininess(20.0f);
        appearance.setMaterial(mat);
        // appearance.set

        Point3d myCoords[] = getPath(points);

        if (myCoords.length == 0)
            return null;

        LineArray myLines = new LineArray(myCoords.length, LineArray.COORDINATES);
        myLines.setCoordinates(0, myCoords);

        RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean depthBufferEnable,
                true, // boolean depthBufferWriteEnable,
                0.5f, // float alphaTestValue,
                RenderingAttributes.ALWAYS, // int alphaTestFunction,
                true, // boolean visible,
                true, // boolean ignoreVertexColors,
                false, // boolean rasterOpEnable,
                RenderingAttributes.ROP_COPY // int rasterOp
        );

        ColoringAttributes coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        appearance.setRenderingAttributes(renderingAttributes);
        appearance.setColoringAttributes(coloringAttributes);
        shape3D = new Shape3D(myLines, appearance);

        // Transform3D t=new Transform3D();
        // Point3d p=new Point3d();
        // double[] offsets = getCenterLocation().getOffsetFrom(new LocationType());
        // t.set(offsets[1],offsets[0],offsets[2] ));
        // moset
        model.addChild(shape3D);

        return model;

    }

    public static Obj3D getLoiterModel3D(Loiter loiter) {
        Obj3D ret = new Obj3D();
        ret.setModel3D(Util3D.getSphere(Color.WHITE, loiter.getRadius(), loiter.getRadius(), 0.6));
        return ret;
    }

    public static Obj3D getRowsManeuverModel3D(RowsManeuver rows) {
        Obj3D ret = new Obj3D();

        ret.setModel3D(getPointLineList3Ddouble(rows.getPathPoints()));

        boolean b = true;
        for (double[] p : rows.getPathPoints()) {
            Obj3D aux = new Obj3D();
            if (b)
                aux.setModel3D(Util3D.getSphere(Color.GREEN, 0.6, 0.6, 0.6));
            else
                aux.setModel3D(Util3D.getSphere(Color.RED, 0.6, 0.6, 0.6));
            b = !b;

            aux.setPos(p);

            ret.addObj3D(aux);
        }
        // ret.setPos(getPosition());
        return ret;
    }

    public static Obj3D getStationKeepingModel3D(StationKeeping sk) {
        Obj3D ret = new Obj3D();

        TransformGroup model = new TransformGroup();
        model.addChild(Util3D.getCylinder(Color.RED, sk.getRadius(), sk.getRadius(), 0.6, true));
        model.addChild(Util3D.getCylinder(Color.WHITE, sk.getRadius(), sk.getRadius(), 0.6, false));

        ret.setModel3D(model);

        return ret;
    }

}
