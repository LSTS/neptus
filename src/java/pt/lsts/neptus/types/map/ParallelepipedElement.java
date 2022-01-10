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
 * 2006/11/11
 */
package pt.lsts.neptus.types.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.swing.JFrame;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer3d.Util3D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Paulo Dias
 * @author Ze Carlos
 */
public class ParallelepipedElement extends GeometryElement {
    @Override
    public String getType() {
        return "Parallelepiped";
    }

    public ParallelepipedElement(MapGroup mg, MapType parentMap) {
        super(mg, parentMap);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }

    public ParallelepipedElement() {
        super();
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_PARALLELEPIPED;
    }
    
    @Override
    public String getTypeAbbrev() {
        return "pp";
    }

    @Override
    public Vector<LocationType> getShapePoints() {
        LocationType center = new LocationType(getCenterLocation());
        Vector<LocationType> locs = new Vector<>();

        double width = getWidth();
        double length = getLength();
        double yaw = getYawRad();

        Rectangle2D.Double tmp = new Rectangle2D.Double(-width / 2, -length / 2, width, length);

        AffineTransform rot = new AffineTransform();
        rot.rotate(-yaw);

        PathIterator it = tmp.getPathIterator(rot);

        while(!it.isDone()) {

            double[] xy = new double[6];

            int op = it.currentSegment(xy);
            if (op == PathIterator.SEG_MOVETO || op == PathIterator.SEG_LINETO) {
                LocationType loc = new LocationType(center);
                loc.translatePosition(xy[1], xy[0], 0);
                locs.add(loc);
            }
            it.next();
        }

        return locs;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D pt = renderer.getScreenPosition(getCenterLocation());
        g.translate(pt.getX(), pt.getY());
        g.rotate(getYawRad() - renderer.getRotation());

        double widthScaled = width * renderer.getZoom();
        double lengthScaled = length * renderer.getZoom();

        Rectangle2D.Double tmp = new Rectangle2D.Double(-widthScaled / 2, -lengthScaled / 2, widthScaled, lengthScaled);

        if (isSelected())
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        else
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));

        if (isFilled())
            g.fill(tmp);

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        g.draw(tmp);
    }

    public static void main(String args[]) {
        JFrame frame = new JFrame("Box and Sphere");

        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

        Canvas3D canvas = new Canvas3D(config);

        canvas.setSize(400, 400);

        SimpleUniverse universe = new SimpleUniverse(canvas);
        universe.getViewingPlatform().setNominalViewingTransform();

        BranchGroup group = new BranchGroup();
        group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        // group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        universe.addBranchGraph(group);

        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        frame.setVisible(true);
        Sphere sphere = new Sphere(1.1f, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, null);

        BranchGroup b = new BranchGroup();
        b.addChild(sphere);

        group.addChild(b);

        {
            Sphere sphere2 = new Sphere(1.0f, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, null);

            b = new BranchGroup();
            b.addChild(sphere2);

            Util3D.enablePicking(b);

            group.addChild(b);
        }

    }
}
