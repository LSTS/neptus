/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 2006/11/11
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer3d.Util3D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

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
