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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.renderer3d;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.SimpleUniverse;

import pt.lsts.neptus.NeptusLog;

public class RayTest extends MouseAdapter {

    private PickCanvas pickCanvas;
    private PickTool pickTool;

    public RayTest() {
        Frame frame = new Frame("Box and Sphere");

        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

        Canvas3D canvas = new Canvas3D(config);

        canvas.setSize(400, 400);

        SimpleUniverse universe = new SimpleUniverse(canvas);

        BranchGroup group = new BranchGroup();

        // create a color cube

        Vector3f vector = new Vector3f(-0.3f, 0.0f, 0.0f);

        Transform3D transform = new Transform3D();

        transform.setTranslation(vector);

        TransformGroup transformGroup = new TransformGroup(transform);

        ColorCube cube = new ColorCube(0.4);

        transformGroup.addChild(cube);

        group.addChild(transformGroup);

        // create a sphere

        Vector3f vector2 = new Vector3f(+0.3f, 0.0f, 0.0f);

        Transform3D transform2 = new Transform3D();

        transform2.setTranslation(vector2);

        TransformGroup transformGroup2 = new TransformGroup(transform2);

        Appearance appearance = new Appearance();

        appearance.setPolygonAttributes(

        new PolygonAttributes(PolygonAttributes.POLYGON_LINE,

        PolygonAttributes.CULL_BACK, 0.0f));

        Sphere sphere = new Sphere(0.3f, appearance);

        transformGroup2.addChild(sphere);

        group.addChild(transformGroup2);

        universe.getViewingPlatform().setNominalViewingTransform();

        Util3D.enablePicking(group);

        universe.addBranchGraph(group);

        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent winEvent) {

                System.exit(0);

            }

        });

        frame.add(canvas);

        pickCanvas = new PickCanvas(canvas, group);

        // pickCanvas.setMode(PickCanvas.BOUNDS);
        pickCanvas.setMode(PickCanvas.GEOMETRY);

        pickTool = new PickTool(group);
        pickTool.setMode(PickTool.GEOMETRY);

        canvas.addMouseListener(this);

        frame.pack();

        frame.setVisible(true);
    }

    public static void main(String[] args) {

        new RayTest();

    }

    public void mouseClicked(MouseEvent e) {
        pickTool.setShapeRay(new Point3d(10, 0.1, 0), new Vector3d(-10, 0, 0));
        // pickTool.
        PickResult result = pickTool.pickClosest();
        if (result == null) {

            NeptusLog.pub().info("<###>---Nothing picked---");

        }
        else {
            NeptusLog.pub().info("<###>--------------- picked---");
            // result.setFirstIntersectOnly(true);
            // NeptusLog.pub().info("<###> "+result);

            // NeptusLog.pub().info("<###>Coordinates:"+result.getClosestIntersection(new
            // Point3d(-10,0.1,0)).getPointCoordinates());
            NeptusLog.pub().info("<###>Coordinates to world:"
                    + result.getClosestIntersection(new Point3d(10, 0.1, 0)).getPointCoordinatesVW());

            Primitive p = (Primitive) result.getNode(PickResult.PRIMITIVE);

            Shape3D s = (Shape3D) result.getNode(PickResult.SHAPE3D);

            if (p != null) {

                NeptusLog.pub().info("<###> "+p.getClass().getName());

            }
            else if (s != null) {

                NeptusLog.pub().info("<###> "+s.getClass().getName());

            }
            else {

                NeptusLog.pub().info("<###>null");

            }

        }
        NeptusLog.pub().info("<###>--------------- end  picked---");
        /*
         * pickCanvas.setShapeLocation(e);
         * 
         * result = pickCanvas.pickClosest();
         * 
         * if (result == null) {
         * 
         * NeptusLog.pub().info("<###>Nothing picked");
         * 
         * } else {
         * 
         * NeptusLog.pub().info("<###> "+result);
         * 
         * Primitive p = (Primitive)result.getNode(PickResult.PRIMITIVE);
         * 
         * Shape3D s = (Shape3D)result.getNode(PickResult.SHAPE3D);
         * 
         * if (p != null) {
         * 
         * NeptusLog.pub().info("<###> "+p.getClass().getName());
         * 
         * } else if (s != null) {
         * 
         * NeptusLog.pub().info("<###> "+s.getClass().getName());
         * 
         * } else{
         * 
         * NeptusLog.pub().info("<###>null");
         * 
         * }
         * 
         * }
         */

    }

} // end of class Pick 