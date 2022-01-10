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
 * 20??/??/??
 */
package pt.lsts.neptus.renderer3d;

import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.picking.PickTool;

public class Util3D {

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mp.MapChangeListener#mapChanged(pt.lsts.neptus.mp.MapChangeEvent)
     */

    public static float pickBug = 0.001f;

    // projection texture size
    public static int FORMAT_SIZE = 128;
    public static BoundingSphere BOUNDS3D = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.MAX_VALUE);

    public static Shape3D foundImageShape(TransformGroup model) {
        // IntersectionUtils i;
        Enumeration<?> childs = model.getAllChildren();
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            if (child instanceof Shape3D) {
                return (Shape3D) child;
            }
            else if (child instanceof TransformGroup) {
                return foundImageShape((TransformGroup) child);
            }
        }
        return null;
    }

    public static Box foundBox(TransformGroup model) {
        // IntersectionUtils i;
        Enumeration<?> childs = model.getAllChildren();
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            if (child instanceof Box) {
                return (Box) child;
            }
            else if (child instanceof TransformGroup) {
                return foundBox((TransformGroup) child);
            }

        }
        return null;
    }

    public static boolean removeBackgroud(Group model) {
        // IntersectionUtils i;
        Enumeration<?> childs = model.getAllChildren();
        // System.err.println("desceu n:"+model.numChildren());
        boolean flag = false;
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            // System.err.println(child);
            // ((Group)(((Background)child))).
            if (child instanceof Background) {
                ((Group) (((Background) child).getParent())).removeChild((Background) child);
                // System.err.println("removeu"+child);
                flag = true;
            }

            if (child instanceof Group) {
                flag = removeBackgroud((Group) child);
            }

        }
        return flag;
    }

    public static Sphere foundSphere(TransformGroup model) {

        Enumeration<?> childs = model.getAllChildren();
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            if (child instanceof Sphere) {
                // NeptusLog.pub().info("<###>-------------FOUND-------------");
                return (Sphere) child;
            }
            else if (child instanceof TransformGroup) {
                return foundSphere((TransformGroup) child);
            }

        }
        return null;
    }

    public static Cylinder foundCylinder(TransformGroup model) {
        Enumeration<?> childs = model.getAllChildren();
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            if (child instanceof Cylinder) {
                // NeptusLog.pub().info("<###>-------------FOUND-------------");
                return (Cylinder) child;
            }
            else if (child instanceof TransformGroup) {
                return foundCylinder((TransformGroup) child);
            }
        }
        return null;
    }

    public static TransformGroup makeAxis(boolean leters, double size) {
        TransformGroup model = new TransformGroup();
        Sphere sphere = new Sphere(0.25f);

        RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean depthBufferEnable,
                true, // boolean depthBufferWriteEnable,
                1.0f, // float alphaTestValue,
                RenderingAttributes.ALWAYS, // int alphaTestFunction,
                true, // boolean visible,
                true, // boolean ignoreVertexColors,
                false, // boolean rasterOpEnable,
                RenderingAttributes.ROP_COPY // int rasterOp
        );
        Color3f c = new Color3f(1.0f, 1.0f, 1.0f);
        ColoringAttributes coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);

        Point3d listp1[] = new Point3d[2];
        listp1[0] = new Point3d(size, 0.0, 0.0);
        listp1[1] = new Point3d(0., 0.0, 0.0);

        // listp1[1]=new Point3d(-2,0.0,0.0);

        Point3d listp2[] = new Point3d[2];
        listp2[0] = new Point3d(0.0, size, 0.0);
        listp2[1] = new Point3d(0.0, 0.0, 0.0);

        // listp2[1]=new Point3d(0.0,-2,0.0);

        Point3d listp3[] = new Point3d[2];

        listp3[0] = new Point3d(0.0, 0.0, size);
        listp3[1] = new Point3d(0.0, 0.0, 0.0);

        // listp3[1]=new Point3d(0.0,0.0,-2.);

        LineArray myLines1 = new LineArray(listp1.length, LineArray.COORDINATES);
        myLines1.setCoordinates(0, listp1);

        LineArray myLines2 = new LineArray(listp1.length, LineArray.COORDINATES);
        myLines2.setCoordinates(0, listp2);
        LineArray myLines3 = new LineArray(listp1.length, LineArray.COORDINATES);
        myLines3.setCoordinates(0, listp3);

        Appearance appearance1 = new Appearance();
        appearance1.setRenderingAttributes(renderingAttributes);
        c = new Color3f(1.0f, 0.0f, 0.0f);
        coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        appearance1.setColoringAttributes(coloringAttributes);
        Shape3D shape3D1 = new Shape3D(myLines1, appearance1);

        Appearance appearance2 = new Appearance();
        appearance2.setRenderingAttributes(renderingAttributes);
        c = new Color3f(0.0f, 1.0f, 0.0f);
        coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        appearance2.setColoringAttributes(coloringAttributes);
        Shape3D shape3D2 = new Shape3D(myLines2, appearance2);

        Appearance appearance3 = new Appearance();
        appearance3.setRenderingAttributes(renderingAttributes);
        c = new Color3f(0.3f, 0.3f, 1.0f);
        coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        appearance3.setColoringAttributes(coloringAttributes);
        Shape3D shape3D3 = new Shape3D(myLines3, appearance3);

        c = new Color3f(1.0f, 1.0f, 1.0f);
        coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        Appearance appearance4 = new Appearance();
        appearance4.setRenderingAttributes(renderingAttributes);
        appearance4.setColoringAttributes(coloringAttributes);
        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setTransparency(0.3f);
        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
        appearance4.setTransparencyAttributes(trans);
        sphere.setAppearance(appearance4);

        Transform3D t = new Transform3D();
        t.setScale(0.1);
        model.setTransform(t);
        model.addChild(shape3D1);
        model.addChild(shape3D2);
        model.addChild(shape3D3);
        // model.addChild(sphere);

        if (leters) {
            // -------------------------XX
            TransformGroup xtextTrans = new TransformGroup();
            Transform3D xtrans2 = new Transform3D();
            xtrans2.setTranslation(new Vector3d(2., 0., 0.));
            xtextTrans.setTransform(xtrans2);

            // Text2D (java.lang.String text, Color3f color,
            // java.lang.String fontName, int fontSize, int fontStyle)
            Text2D xtext2D = new Text2D("N", new Color3f(1f, 0f, 0f), "Helvetica", 100, Font.BOLD | Font.ITALIC);
            xtextTrans.setTransform(xtrans2);
            xtextTrans.addChild(xtext2D);

            TransformGroup xtextTrans2 = new TransformGroup();
            Transform3D xtrans3 = new Transform3D();

            xtrans3.rotX(Math.PI);
            xtrans2.mul(xtrans3);
            Text2D xtext2D2 = new Text2D("N", new Color3f(1f, 0f, 0f), "Helvetica", 100, Font.BOLD | Font.ITALIC);

            xtextTrans2.setTransform(xtrans2);
            xtextTrans2.addChild(xtext2D2);

            /*
             * Appearance appGFront = new Appearance(); PolygonAttributes p = new PolygonAttributes (); p.setCullFace
             * (PolygonAttributes.CULL_NONE); appGFront.setPolygonAttributes(p); text2D.setAppearance(appGFront);
             */

            model.addChild(xtextTrans);
            model.addChild(xtextTrans2);

            // ---------------------yy
            TransformGroup ytextTrans = new TransformGroup();
            Transform3D ytrans2 = new Transform3D();

            ytrans2.setTranslation(new Vector3d(0., 2., 0.));

            ytextTrans.setTransform(ytrans2);

            // Text2D (java.lang.String text, Color3f color,
            // java.lang.String fontName, int fontSize, int fontStyle)
            Text2D ytext2D = new Text2D("E", new Color3f(0f, 1f, 0f), "Helvetica", 100, Font.BOLD | Font.ITALIC);
            ytextTrans.setTransform(ytrans2);
            ytextTrans.addChild(ytext2D);

            TransformGroup ytextTrans2 = new TransformGroup();
            Transform3D ytrans3 = new Transform3D();

            ytrans3.rotY(Math.PI);
            ytrans2.mul(ytrans3);
            Text2D ytext2D2 = new Text2D("E", new Color3f(0f, 1f, 0f), "Helvetica", 100, Font.BOLD | Font.ITALIC);

            ytextTrans2.setTransform(ytrans2);
            ytextTrans2.addChild(ytext2D2);

            /*
             * Appearance appGFront = new Appearance(); PolygonAttributes p = new PolygonAttributes (); p.setCullFace
             * (PolygonAttributes.CULL_NONE); appGFront.setPolygonAttributes(p); text2D.setAppearance(appGFront);
             */

            model.addChild(ytextTrans);
            model.addChild(ytextTrans2);

            // ----------------------------zz
            TransformGroup ztextTrans = new TransformGroup();
            Transform3D ztrans2 = new Transform3D();
            Transform3D ztrans3 = new Transform3D();
            ztrans2.setTranslation(new Vector3d(0., 0., 2.));
            ztrans3.rotY(Math.PI / 2);
            ztrans2.mul(ztrans3);
            ztextTrans.setTransform(ztrans2);

            // Text2D (java.lang.String text, Color3f color,
            // java.lang.String fontName, int fontSize, int fontStyle)
            Text2D ztext2D = new Text2D("D", new Color3f(0.7f, 0.7f, 1f), "Helvetica", 100, Font.BOLD | Font.ITALIC);
            ztextTrans.setTransform(ztrans2);
            ztextTrans.addChild(ztext2D);

            TransformGroup ztextTrans2 = new TransformGroup();

            ztrans3.rotX(Math.PI);
            ztrans2.mul(ztrans3);

            Text2D ztext2D2 = new Text2D("D", new Color3f(0.7f, 0.7f, 1f), "Helvetica", 100, Font.BOLD | Font.ITALIC);

            ztextTrans2.setTransform(ztrans2);
            ztextTrans2.addChild(ztext2D2);

            /*
             * Appearance appGFront = new Appearance(); PolygonAttributes p = new PolygonAttributes (); p.setCullFace
             * (PolygonAttributes.CULL_NONE); appGFront.setPolygonAttributes(p); text2D.setAppearance(appGFront);
             */

            model.addChild(ztextTrans);
            model.addChild(ztextTrans2);
        }

        return model;
    }

    public static Color Color3fToColor(Color3f colorf) {
        Color ret = colorf.get();
        return ret;
    }

    public static Color3f ColorToColor3f(Color color) {
        Color3f ret = new Color3f();
        ret.set(color);
        return ret;
    }

    // private Vector<Shape3D> gShapeObjects=new Vector<Shape3D>();

    public static Point3f getModelDim(TransformGroup model) {
        Vector<Shape3D> gShapeObjects = new Vector<Shape3D>();
        GetShapeNodes(model, gShapeObjects);
        // gShapeObjects
        Point3f ret = new Point3f();
        Point3f retmin = new Point3f();
        Point3f retmax = new Point3f();

        for (Shape3D s : gShapeObjects) {
            Enumeration<?> list = s.getAllGeometries();

            // NeptusLog.pub().info("<###>tem elementos ? "+list.hasMoreElements());
            for (; list.hasMoreElements();) {
                Geometry g = (Geometry) list.nextElement();
                // NeptusLog.pub().info("<###>Elemento: "+g);
                GeometryInfo gi = new GeometryInfo((GeometryArray) g);
                Point3f[] pointlist = gi.getCoordinates();
                // Point3d[] pointlist=((GeometryArray)g).getCoordRef3d();
                for (Point3f p : pointlist) {
                    // NeptusLog.pub().info("<###>estudando o vertice"+p);
                    if (p.x > retmax.x)
                        retmax.x = p.x;
                    if (p.y > retmax.y)
                        retmax.y = p.y;
                    if (p.z > retmax.z)
                        retmax.z = p.z;

                    if (p.x < retmin.x)
                        retmax.x = p.x;
                    if (p.y < retmin.y)
                        retmax.y = p.y;
                    if (p.z < retmin.z)
                        retmax.z = p.z;
                }
            }
        }

        if (retmin.x < 0)
            retmin.x *= -1;
        if (retmin.y < 0)
            retmin.y *= -1;
        if (retmin.z < 0)
            retmin.z *= -1;

        if (retmax.x < 0)
            retmax.x *= -1;
        if (retmax.y < 0)
            retmax.y *= -1;
        if (retmax.z < 0)
            retmax.z *= -1;

        if (retmin.x > retmax.x)
            ret.x = retmin.x;
        else
            ret.x = retmax.x;
        if (retmin.y > retmax.y)
            ret.y = retmin.y;
        else
            ret.y = retmax.y;
        if (retmin.z > retmax.z)
            ret.z = retmin.z;
        else
            ret.z = retmax.z;

        return ret;
    }

    public static void GetShapeNodes(BranchGroup parentGroup, Vector<Shape3D> gShapeObjects) {
        for (int i = 0; i < parentGroup.numChildren(); i++) {
            Node tNode = parentGroup.getChild(i);
            if (tNode.getClass() == BranchGroup.class) {
                GetShapeNodes((BranchGroup) tNode, gShapeObjects);
            }
            else if (tNode.getClass() == TransformGroup.class) {
                GetShapeNodes((TransformGroup) tNode, gShapeObjects);
            }
            else if (tNode.getClass() == Shape3D.class) {
                gShapeObjects.add((Shape3D) tNode);
            }
        }
    }

    private static void GetShapeNodes(TransformGroup parentGroup, Vector<Shape3D> gShapeObjects) {
        for (int i = 0; i < parentGroup.numChildren(); i++) {
            Node tNode = parentGroup.getChild(i);
            if (tNode.getClass() == BranchGroup.class) {
                GetShapeNodes((BranchGroup) tNode, gShapeObjects);
            }
            else if (tNode.getClass() == TransformGroup.class) {
                GetShapeNodes((TransformGroup) tNode, gShapeObjects);
            }
            else if (tNode.getClass() == Shape3D.class) {
                // NeptusLog.pub().info("<###>encontri");
                gShapeObjects.add((Shape3D) tNode);
            }
        }
    }

    public static void enablePicking(Node node) {
        node.setPickable(true);
        node.setCapability(Node.ENABLE_PICK_REPORTING);

        try {
            Group group = (Group) node;

            for (Enumeration<?> e = group.getAllChildren(); e.hasMoreElements();) {
                enablePicking((Node) e.nextElement());
            }
        }

        catch (ClassCastException e) {

            // if not a group node, there are no children so ignore exception

        }

        try {

            Shape3D shape = (Shape3D) node;
            /*
             * if(node.isLive()) NeptusLog.pub().info("<###> "+node+" node está vivo"); else if(node.isCompiled())
             * NeptusLog.pub().info("<###> "+node+" node está compilado"); else NeptusLog.pub().info("<###> "+node+" node nem vivo nem comp");
             */
            PickTool.setCapabilities(node, PickTool.INTERSECT_FULL);

            // NeptusLog.pub().info("<###>ok em "+node);

            for (Enumeration<?> e = shape.getAllGeometries(); e.hasMoreElements();) {

                Geometry g = (Geometry) e.nextElement();

                g.setCapability(Geometry.ALLOW_INTERSECT);

            }

        }

        catch (ClassCastException e) {

            // not a Shape3D node ignore exception

        }

    }

    // private static Texture2D createTexture(int SS) {
    // int btype = 0;
    // int itype = 0;
    // int ttype = 0;
    // boolean byRef = true;
    // String os = System.getProperty("os.name");
    // //NeptusLog.pub().info("<###>running on " + os);
    // if ( os.startsWith("W") || os.startsWith("w")) {
    // btype = BufferedImage.TYPE_3BYTE_BGR;
    // itype = ImageComponent.FORMAT_RGB;
    // ttype = Texture.RGB;
    // byRef = true;
    // }else if (os.startsWith("S") || os.startsWith("s")){
    // btype = BufferedImage.TYPE_4BYTE_ABGR;
    // itype = ImageComponent.FORMAT_RGBA;
    // ttype = Texture.RGBA;
    // byRef = true;
    //
    // } else {
    // btype = BufferedImage.TYPE_3BYTE_BGR;
    // itype = ImageComponent.FORMAT_RGB;
    // ttype = Texture.RGB;
    // byRef = false;
    // }
    //
    //
    // int j = 0;
    // byte alpha_1 = (byte)0xff;
    //
    // BufferedImage bimg = new BufferedImage(SS, SS, btype);
    // byte[] byteData = ((DataBufferByte)bimg.getRaster().getDataBuffer()).getData();
    // if ( btype == BufferedImage.TYPE_4BYTE_ABGR) {
    // j = 0;
    // for ( int i = 0; i < SS*SS; i++) {
    // byteData[j] = alpha_1;
    // byteData[j+1] = (byte)192;
    // byteData[j+2] = (byte)0;
    // byteData[j+3] = (byte)192;
    // j += 4;
    // }
    // } else {
    // j = 0;
    // for ( int i = 0; i < SS*SS; i++) {
    // byteData[j] = (byte)192;
    // byteData[j+1] = (byte)0;
    // byteData[j+2] = (byte)192;
    // j += 3;
    // }
    //
    // }
    // ImageComponent2D imgcmp = new ImageComponent2D(itype, bimg, byRef, true);
    // Texture2D tex1 = new Texture2D(Texture2D.BASE_LEVEL, ttype, SS, SS);
    // tex1.setImage(0, imgcmp);
    // tex1.setCapability(Texture.ALLOW_IMAGE_WRITE);
    // return tex1;
    // }
    //
    // public static float getPickBug() {
    // if(pickBug<0)
    // return pickBug+=0.0001f;
    // else
    // return pickBug-=0.0001f;
    // }
    //
    // static public Vector3d setTransform (Vector3d vec,double roll,double pitch,double yaw)
    // {
    // Transform3D ret = new Transform3D();// return transform
    // Transform3D xrot = new Transform3D();
    // Transform3D yrot = new Transform3D();
    // Transform3D zrot = new Transform3D();
    //
    //
    //
    // xrot.rotX(roll);
    // yrot.rotY(pitch);
    // zrot.rotZ(yaw);
    //
    //
    // ret.mul(zrot);
    // ret.mul(xrot);
    // ret.mul(yrot);
    // Vector3d retvector = new Vector3d();
    //
    // ret.transform(vec,retvector);
    // return retvector;
    // }

    static public Point3d setTransform(Point3d vec, double roll, double pitch, double yaw) {
        Transform3D ret = new Transform3D();// return transform
        Transform3D xrot = new Transform3D();
        Transform3D yrot = new Transform3D();
        Transform3D zrot = new Transform3D();

        xrot.rotX(roll);
        yrot.rotY(pitch);
        zrot.rotZ(yaw);

        ret.mul(zrot);
        ret.mul(xrot);
        ret.mul(yrot);
        Point3d retvector = new Point3d();
        ret.transform(vec, retvector);
        return retvector;
    }

    static public void setCameraPosition3Points(Camera3D cam, Point3d a, Point3d b, Point3d c) {
        // System.err.println("entrou");
        Vector3d va = new Vector3d(a);
        Vector3d vb = new Vector3d(b);
        Vector3d vc = new Vector3d(c);

        va.x = vb.x - va.x;
        va.y = vb.y - va.y;
        va.z = vb.z - va.z;

        vb.x = vc.x - va.x;
        vb.y = vc.y - va.y;
        vb.z = vc.z - va.z;

        // coordenadas esfericas(2 angulos)
        // vc.
        vc.cross(va, vb);
        vc.normalize();
        double psi = Math.atan(vc.y / vc.x);
        double theta = Math.acos(vc.z);
        cam.setPsi(psi);
        cam.setTheta(theta);

        // calculo do zoom (distância ao centro)
        double rho = a.distance(b);
        if (rho < b.distance(c))
            rho = b.distance(c);
        if (rho < c.distance(a))
            rho = c.distance(a);

        cam.setRho(rho);

        // translação para o ponto médio
        Vector3d pivot = new Vector3d((a.x + b.x + c.x) / 3, (a.y + b.y + c.y) / 3, (a.z + b.z + c.z) / 3);
        cam.setPivot(pivot);
    }

    static public void setCameraPositionBox(Camera3D cam, Point3d a, Point3d b) {
        // angulo de abertura da camera e, User mode
        double captAng = 2 * Math.toDegrees(Math.atan(1 / 3.45));

        Vector3d center = new Vector3d();

        center.x = (a.x + b.x) / 2;
        center.y = (a.y + b.y) / 2;
        center.z = (a.z + b.z) / 2;

        // double initSulthDistamce;

        // deslocamento horizontal em relação ao centro
        double deslocHor;
        if (a.x > b.x) {
            // initSulthDistamce=b.x;
            deslocHor = (a.x - b.x) / 2;
        }
        else {
            // initSulthDistamce=a.x;
            deslocHor = (b.x - a.x) / 2;
        }

        double altaux = Math.sin(Math.toRadians(captAng / 2)) * deslocHor;
        double Rho = Math.sqrt(altaux * altaux + deslocHor * deslocHor);

        // andamento mais para sul para apanhar o objecto todo (parte vertical)
        double tamVert;
        if (a.z > b.z) {
            tamVert = a.z - b.z;
        }
        else {
            tamVert = b.z - a.z;
        }

        tamVert = (tamVert / 2) + altaux;

        // andmento para sul , parte horizontal
        double andSul = Math.sqrt((Rho * Rho) - (tamVert * tamVert));

        double tamHor;
        if (a.y > b.y) {
            tamHor = (a.y - b.y) / 2;
        }
        else {
            tamHor = (b.y - a.y) / 2;
        }

        double andSul2 = Math.sin(Math.toRadians(captAng / 2)) * tamHor;

        if (andSul > andSul2)
            center.x = center.x - andSul;
        else
            center.x = center.x - andSul2;

        // por a camara a olhar para norte com o horizonte na parte superior
        cam.setPhi(Math.PI);
        cam.setPsi(-Math.PI / 2);
        cam.setTheta(Math.PI - Math.toRadians(45 + 27));
        cam.setPivot(center);
        cam.setRho(Rho);
        cam.setProjection(View.PERSPECTIVE_PROJECTION);
    }

    public static TransformGroup getSphere(Color ca, double scalex, double scaley, double scalez) {

        Sphere sphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, null);

        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

        Transform3D t = new Transform3D();
        Transform3D t2 = new Transform3D();
        t.setScale(new Vector3d(scalex, scaley, scalez));
        t2.setScale(1);
        t.mul(t2);
        TransformGroup model = new TransformGroup();
        model.setCapability(TransformGroup.ALLOW_CHILDREN_READ);

        Appearance appearance3 = new Appearance();

        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        trans.setTransparency(0.5f);

        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);

        appearance3.setTransparencyAttributes(trans);

        Material mat = new Material();
        mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
        mat.setCapability(Material.ALLOW_COMPONENT_READ);
        Color3f c = new Color3f();
        c.set(ca);

        mat.setDiffuseColor(c);
        mat.setSpecularColor(c);

        appearance3.setMaterial(mat);

        sphere.setAppearance(appearance3);

        model.addChild(sphere);
        model.setTransform(t);
        sphere.getAppearance();

        return model;
    }

    public static TransformGroup getCylinder(Color ca, double scalex, double scaley, double scalez, boolean sideTop) {

        Cylinder cylinder = new Cylinder(1.0f, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, null);

        cylinder.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        cylinder.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

        Transform3D t = new Transform3D();
        Transform3D t2 = new Transform3D();
        t.setScale(new Vector3d(scalex, scaley, scalez));
        t2.rotX(Math.PI / 2);// setScale(1);
        t.mul(t2);
        TransformGroup model = new TransformGroup();
        model.setCapability(TransformGroup.ALLOW_CHILDREN_READ);

        Appearance appearance3 = new Appearance();

        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        trans.setTransparency(0.5f);

        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);

        appearance3.setTransparencyAttributes(trans);

        Material mat = new Material();
        mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
        mat.setCapability(Material.ALLOW_COMPONENT_READ);
        Color3f c = new Color3f();
        c.set(ca);

        mat.setDiffuseColor(c);
        mat.setSpecularColor(c);

        appearance3.setMaterial(mat);
        /*
         * RenderingAttributes renderingAttributes = new RenderingAttributes( true, // boolean depthBufferEnable, true,
         * // boolean depthBufferWriteEnable, 0.5f, // float alphaTestValue, RenderingAttributes.ALWAYS, // int
         * alphaTestFunction, true, // boolean visible, true, // boolean ignoreVertexColors, false, // boolean
         * rasterOpEnable, RenderingAttributes.ROP_COPY // int rasterOp );
         * 
         * //ColoringAttributes coloringAttributes = new ColoringAttributes(c,ColoringAttributes.SHADE_GOURAUD);
         * appearance3.setRenderingAttributes(renderingAttributes);
         */
        PolygonAttributes p = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0.0f);
        appearance3.setPolygonAttributes(p);
        cylinder.setAppearance(appearance3);

        if (sideTop) {
            cylinder.removeChild(1);
            cylinder.removeChild(1);

        }
        else
            cylinder.removeChild(0);

        model.addChild(cylinder);
        model.setTransform(t);
        // cylinder.getAppearance();

        return model;
    }

}
