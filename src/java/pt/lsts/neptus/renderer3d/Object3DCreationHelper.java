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
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.Material;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.dom4j.Node;

import com.mnstarfire.loaders3d.Inspector3DS;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.scenegraph.io.SceneGraphFileReader;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.CylinderElement;
import pt.lsts.neptus.types.map.DynamicElement;
import pt.lsts.neptus.types.map.EllipsoidElement;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.Model3DElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.types.map.SimpleMapElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.X3dParse;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.viewer3d.Viewer3D;

/**
 * @author pdias (code essentially from RJPG)
 * 
 */
public class Object3DCreationHelper {

    public static TransformGroup getModel3DForRender(AbstractElement elem) {

        if (elem instanceof HomeReferenceElement)
            return getHomeReferenceModel3D();
        else if (elem instanceof MarkElement)
            return getMarkModel3D();
        else if (elem instanceof CylinderElement)
            return getCylinderModel3D((CylinderElement) elem);
        else if (elem instanceof EllipsoidElement)
            return getElipsoidModel3D((EllipsoidElement) elem);
        else if (elem instanceof ImageElement)
            return getImageModel3D((ImageElement) elem);
        else if (elem instanceof Model3DElement)
            return getModel3DModel3D((Model3DElement) elem);
        else if (elem instanceof ParallelepipedElement)
            return getParallelepipedModel3D((ParallelepipedElement) elem);
        else if (elem instanceof PathElement)
            return getPathModel3D((PathElement) elem);
        else if (elem instanceof TransponderElement)
            return getTransponderModel3D((TransponderElement) elem);
        else if (elem instanceof ScatterPointsElement)
            return ((ScatterPointsElement) elem).getModel3D(); //FIXME
        else if (elem instanceof PlanElement)
            return null;
        else if (elem instanceof SimpleMapElement)
            return getMarkModel3D();
        
        return null;
    }
    
    public static Obj3D getPlanModel3D(PlanType plan) {
        MissionType mission = plan.getMissionType();
        GraphType graph = plan.getGraph();
        // Vector<Point3d> pointsConnection3D = new Vector<Point3d>();
        Maneuver[] mans = graph.getManeuversSequence();
        Obj3D o3d = new Obj3D();
        double[] lastpoint = null;
        if (mans != null && mans.length > 0) {
            for (int i = 0; i < mans.length; i++) {
                if (mans[i] instanceof LocatedManeuver) {
                    LocatedManeuver man = (LocatedManeuver) mans[i];

                    Obj3D subObj3d = new Obj3D();

                    subObj3d.setModel3D(getSphere());
                    double auxPos[] = new double[3];
                    auxPos = man.getManeuverLocation().getOffsetFrom(mission.getHomeRef());
                    auxPos[2] = -auxPos[2];
                    subObj3d.setPos(auxPos);
                    o3d.addObj3D(subObj3d);

                    double[] fp = man.getStartLocation().getOffsetFrom(mission.getHomeRef());
                    fp[2] = -fp[2];
                    // pointsConnection3D.add(new Point3d(fp));

                    double[] lp = man.getEndLocation().getOffsetFrom(mission.getHomeRef());
                    lp[2] = -lp[2];
                    // pointsConnection3D.add(new Point3d(lp));

                    if (lastpoint != null) {
                        Obj3D segmentObj3d = new Obj3D();

                        segmentObj3d.setModel3D(getsegment3D(new Point3d(lastpoint), new Point3d(fp)));
                        o3d.addObj3D(segmentObj3d);
                    }

                    lastpoint = lp;

//                    if (mans[i] instanceof Model3DProvider) {
//                        Obj3D manObj3d = ((Model3DProvider) mans[i]).getModel3D();
//                        if (manObj3d != null) {
//                            // auxPos[2]=-auxPos[2];
//                            manObj3d.setPos(auxPos);
//                            o3d.addObj3D(manObj3d);
//                        }
//                    }
                    Obj3D manObj3d = Maneuver3DCreationHelper.getModel3DManeuverForRender(mans[i]);
                    if (manObj3d != null) {
                        // auxPos[2]=-auxPos[2];
                        manObj3d.setPos(auxPos);
                        o3d.addObj3D(manObj3d);
                    }
                }
                else {
                    lastpoint = null;
                }
            }
        }
        return o3d;
    }

    public static TransformGroup getSphere() {

        Sphere sphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, null);

        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

        Transform3D t = new Transform3D();
        Transform3D t2 = new Transform3D();
        t.setScale(new Vector3d(1, 1, 1));
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
        c.set(Color.BLUE);

        mat.setDiffuseColor(c);
        mat.setSpecularColor(c);

        appearance3.setMaterial(mat);

        sphere.setAppearance(appearance3);

        model.addChild(sphere);
        model.setTransform(t);
        sphere.getAppearance();

        return model;
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

    public static TransformGroup getPath3D(Vector<Point3d> points) {
        Shape3D shape3D;
        TransformGroup model = new TransformGroup();
        Appearance appearance = new Appearance();

        Material mat = new Material();
        Color3f c = new Color3f(1.0f, 0.0f, 0.0f);
        c.set(Color.WHITE);
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

    public static TransformGroup getsegment3D(Point3d p1, Point3d p2) {
        Vector<Point3d> v = new Vector<Point3d>();
        v.add(p1);
        v.add(p2);
        return getPath3D(v);
    }

    // --------------------------------------------------------------------------

    public static TransformGroup getVehicleModel3D(String pathTo3DModel) {
        /*
         * TransformGroup themodel;
         * NeptusLog.pub().info("loading model:"+ConfigFetch.resolvePath(this.getModel3DHref())); Inspector3DS
         * obj3Dloader = new Inspector3DS(ConfigFetch.resolvePath(this.getModel3DHref().replace(".j3d",".3ds"))); //
         * constructor obj3Dloader.parseIt(); // process the file themodel = obj3Dloader.getModel(); // get the
         * resulting 3D
         */

        File file = new File(ConfigFetch.resolvePath(pathTo3DModel /* this.getModel3DHref() */));
        if (file != null) {
            if ("3ds".equalsIgnoreCase(FileUtil.getFileExtension(file))) {
                try {
                    Inspector3DS loader = new Inspector3DS(file.getAbsolutePath()); // constructor
                    loader.parseIt(); // process the file
                    TransformGroup theModel1 = loader.getModel(); // get the resulting 3D
                    NeptusLog.waste().info("Point to view window " + Util3D.getModelDim(theModel1));

                    return theModel1;
                }
                catch (RuntimeException e) {
                    NeptusLog.pub().error("Error loading vehicle model (3DS)\n" + e);
                }
            }
            else if ("wrl".equalsIgnoreCase(FileUtil.getFileExtension(file))) {
                try {
                    Loader myFileLoader = null; // holds the file loader
                    Scene myVRMLScene = null; // holds the loaded scene
                    BranchGroup myVRMLModel = null; // BG of the VRML scene
                    // create an instance of the Loader
                    myFileLoader = new org.web3d.j3d.loaders.VRML97Loader();
                    myFileLoader.setBasePath(file.getParent());
                    // myFileLoader.setFlags(org.web3d.j3d.loaders.VRML97Loader.LOAD_ALL);
                    // Load the scene from your VRML97 file
                    myVRMLScene = myFileLoader.load(file.getAbsolutePath());

                    // Obtain the root BranchGroup for the Scene
                    myVRMLModel = myVRMLScene.getSceneGroup();

                    TransformGroup scene = new TransformGroup();
                    @SuppressWarnings("unchecked")
                    Enumeration<Group> enume = myVRMLModel.getAllChildren();
                    while (enume.hasMoreElements()) {
                        Group next = enume.nextElement();
                        myVRMLModel.removeChild(next);
                        scene.addChild(next);
                    }
                    return scene;
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error loading vehicle model (WRL)\n" + e);
                }

            }
            else if ("x3d".equalsIgnoreCase(FileUtil.getFileExtension(file))
                    || "x3dv".equalsIgnoreCase(FileUtil.getFileExtension(file))) {
                try {
                    Loader myFileLoader = null; // holds the file loader
                    Scene myVRMLScene = null; // holds the loaded scene
                    BranchGroup myVRMLModel = null; // BG of the VRML scene
                    // create an instance of the Loader
                    myFileLoader = new org.web3d.j3d.loaders.X3DLoader();
                    myFileLoader.setBasePath(file.getParent());
                    // myFileLoader.setFlags(org.web3d.j3d.loaders.X3DLoader.LOAD_ALL);
                    // Load the scene from your VRML97 file
                    myVRMLScene = myFileLoader.load(file.getAbsolutePath());

                    // Obtain the root BranchGroup for the Scene
                    myVRMLModel = myVRMLScene.getSceneGroup();
                    TransformGroup scene = new TransformGroup();
                    @SuppressWarnings("unchecked")
                    Enumeration<Group> enume = myVRMLModel.getAllChildren();
                    while (enume.hasMoreElements()) {
                        Group next = enume.nextElement();
                        myVRMLModel.removeChild(next);
                        scene.addChild(next);
                    }
                    return scene;
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error loading vehicle model (X3D)\n");
                }

            }
            else if ("j3d".equalsIgnoreCase(FileUtil.getFileExtension(file))) {
                BranchGroup bg = null;

                try {
                    SceneGraphFileReader filer = new SceneGraphFileReader(file);
                    bg = (filer.readAllBranchGraphs())[0];
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (bg == null) {
                    NeptusLog.pub().error("Error loading vehicle model\n");
                }
                TransformGroup scene = new TransformGroup();

                @SuppressWarnings("unchecked")
                Enumeration<Group> enume = bg.getAllChildren();
                while (enume.hasMoreElements()) {
                    Group next = enume.nextElement();
                    bg.removeChild(next);
                    scene.addChild(next);
                }
                return scene;

            }
            else
                NeptusLog.pub().error("Error loading vehicle model - Invalid file type.\n");

        }
        return new TransformGroup();

        // ---------------------READ-------------------

        /*
         * //--------------------WRITE-------------------- BranchGroup scene = new BranchGroup(); Enumeration<Group>
         * enume = themodel.getAllChildren(); while (enume.hasMoreElements()) { Group next = enume.nextElement();
         * themodel.removeChild(next); scene.addChild(next); }
         * 
         * 
         * //String file=this.getModel3DHref(); //file.replace(".3ds", ".j3d");
         * 
         * OutputStream outS; try { String aux=ConfigFetch.resolvePath(this.getModel3DHref())+".j3d"; File f=new
         * File(aux); SceneGraphFileWriter filew=new SceneGraphFileWriter(f, null, false, "genereted by Neptus", null);
         * filew.writeBranchGraph(scene); System.err.println("vehicle w:"+f.getPath()+"\n"+aux); filew.close(); } catch
         * (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); } catch
         * (UnsupportedUniverseException e) { // TODO Auto-generated catch block e.printStackTrace(); }
         * 
         * //new SceneGraphFileWriter(new File(ConfigFetch.resolvePath(file)),null, false, "Generated by Neptus",
         * flyData );
         * 
         * return themodel;
         */
    }

    public static JDialog open3DViewerDialogForModelPath(Component parentComponent, String title, String model3dPath) {
        Window parent = SwingUtilities.windowForComponent(parentComponent);
        JDialog dialog = (parent == null ? new JDialog((Frame) ConfigFetch.getSuperParentFrame(), title) : new JDialog(
                parent, title));
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setSize(500, 500);
        try {
            TransformGroup trans = Object3DCreationHelper.getVehicleModel3D(model3dPath); // vehicle.getModel3D();
            Viewer3D v3d = new Viewer3D(trans);
            v3d.disableMenuBar();
            dialog.add(v3d);
            GuiUtils.centerOnScreen(dialog);
        }
        catch (Exception e) {
            e.printStackTrace();
            dialog.add(new JLabel(I18n.textf("Error loading 3D model %modelPath", model3dPath)));
        }
        dialog.setVisible(true);
        return dialog;
    }

    // -------------------------------------------------------------------------------------------

    public static TransformGroup getHomeReferenceModel3D() {
        TransformGroup model = new TransformGroup();
        Sphere sphere = new Sphere(1.f);

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
        model.addChild(sphere);
        model.addChild(Util3D.makeAxis(false, 100));

        return model;
    }

    public static TransformGroup getMarkModel3D() {
        TransformGroup model = new TransformGroup();
        Sphere sphere = new Sphere();

        RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean depthBufferEnable,
                true, // boolean depthBufferWriteEnable,
                1.0f, // float alphaTestValue,
                RenderingAttributes.ALWAYS, // int alphaTestFunction,
                true, // boolean visible,
                true, // boolean ignoreVertexColors,
                false, // boolean rasterOpEnable,
                RenderingAttributes.ROP_COPY // int rasterOp
        );
        Color3f c = new Color3f(1.0f, 0.0f, 0.0f);
        ColoringAttributes coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        Appearance appearance = new Appearance();
        appearance.setRenderingAttributes(renderingAttributes);
        appearance.setColoringAttributes(coloringAttributes);

        Point3d listp[] = new Point3d[6];
        listp[0] = new Point3d(4, 0.0, 0.0);
        listp[1] = new Point3d(-4, 0.0, 0.0);
        listp[2] = new Point3d(0.0, 4, 0.0);
        listp[3] = new Point3d(0.0, -4, 0.0);
        listp[4] = new Point3d(0.0, 0.0, 4);
        listp[5] = new Point3d(0.0, 0.0, -4);

        LineArray myLines = new LineArray(listp.length, LineArray.COORDINATES);
        myLines.setCoordinates(0, listp);
        Shape3D shape3D = new Shape3D(myLines, appearance);

        c = new Color3f(1.0f, 1.0f, 0.0f);
        coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        Appearance appearance2 = new Appearance();
        appearance2.setRenderingAttributes(renderingAttributes);
        appearance2.setColoringAttributes(coloringAttributes);
        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setTransparency(0.3f);
        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
        appearance2.setTransparencyAttributes(trans);
        sphere.setAppearance(appearance2);

        Transform3D t = new Transform3D();
        t.setScale(0.1);
        model.setTransform(t);
        model.addChild(shape3D);
        model.addChild(sphere);
        return model;
    }

    public static TransformGroup getCylinderModel3D(CylinderElement cylinder) {

        com.sun.j3d.utils.geometry.Cylinder sphere = new com.sun.j3d.utils.geometry.Cylinder(1f, 2f,
                com.sun.j3d.utils.geometry.Cylinder.GENERATE_TEXTURE_COORDS
                        | com.sun.j3d.utils.geometry.Cylinder.GENERATE_NORMALS, null);
        // sphere.setCapability(com.sun.j3d.utils.geometry.Cylinder.ENABLE_APPEARANCE_MODIFY);
        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

        Enumeration<?> childs = sphere.getAllChildren();
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            if (child instanceof Shape3D) {
                Shape3D aux = (Shape3D) child;
                aux.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
                aux.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
                // NeptusLog.pub().info("<###> "+i);
            }
        }

        Transform3D t = new Transform3D();
        Transform3D t2 = new Transform3D();

        t2.setIdentity();
        t.setScale(new Vector3d(cylinder.getLength(), cylinder.getWidth(), cylinder.getHeight()));
        t2.setScale(0.5);
        t.mul(t2);
        t2.setIdentity();
        t2.rotX(Math.PI / 2);
        t.mul(t2);
        TransformGroup model = new TransformGroup();
        model.setCapability(TransformGroup.ALLOW_CHILDREN_READ);

        // InputStream ist = getClass().getResourceAsStream("/images/textures/hardmetal.png");
        // String inFileName = StreamUtil.copyStreamToTempFile(ist).getAbsolutePath();

        // Toolkit tk = Toolkit.getDefaultToolkit();

        // Image src_img = GuiUtils.getImage("images/apimage.jpg");
        // System.err.println("size "+src_img.getWidth(null)+" "+src_img.getHeight(null));
        // Image src_img=GuiUtils.getScaledImage(src_img1,256,128,true);
        /* System.err.println("size "+src_img.getWidth(null)+" "+src_img.getHeight(null)); */
        // tk.createImage();
        // Image src_img = tk.createImage("c:/lsts.gif");
        // BufferedImage buf_img = null;
        /*
         * if(!(src_img instanceof BufferedImage)) { // create a component anonymous inner class to give us the // image
         * observer we need to get the width and height of // the source image. Component obs = new Component() { };
         * 
         * int width = src_img.getWidth(obs); int height = src_img.getHeight(obs);
         * 
         * // construct the buffered image from the source data. buf_img = new BufferedImage(width, height,
         * BufferedImage.TYPE_INT_RGB);
         * 
         * Graphics g = buf_img.getGraphics(); g.drawImage(src_img, 0, 0, null); g.dispose(); } else buf_img =
         * (BufferedImage)src_img;
         */
        // ImageComponent img_comp =
        // new ImageComponent2D(ImageComponent.FORMAT_RGBA8, buf_img);

        /*
         * Texture2D text=new Texture2D(Texture2D.BASE_LEVEL, Texture.RGBA, img_comp.getWidth(), img_comp.getHeight());
         */

        Appearance appearance3 = new Appearance();
        // NewTextureLoader
        // TextureLoader loader = new TextureLoader(inFileName,"LUMINANCE",new Container());

        // TextureLoader loader = new TextureLoader(inFileName,null);
        // TextureLoader loader = new TextureLoader(src_img,null);
        // JComponent I= new JScrollPane(loader.getTexture().getImage(0));
        // ImagePanel J =new ImagePanel(createLightMap().getImage(0));

        // Mostrar a imagem associada à textura
        // ImagePanel J =new ImagePanel(loader.getImage().getImage());
        // GuiUtils.testFrame(J,"image,");

        // System.err.println("image :"+loader.getImage().getImage());
        // loader.getImage().getImage();
        // J.getGraphics().drawImage(loader.getImage().getImage(),0,0, new Component() { });
        // J.add(loader.getImage().getImage().getGraphics());

        // ImageComponent2D image=loader.getImage();

        // text.setImage(0,image);
        // System.err.println("sada"+loader.getImage().getWidth());
        // System.err.println("sadasdfsd"+loader.getTexture().getHeight());
        // Texture texture = TextureLoader(inFileName,new String("RGB")).getTexture();

        /*
         * TextureAttributes texta=new TextureAttributes(); texta.setTextureMode(TextureAttributes.MODULATE);
         * appearance3.setTextureAttributes(texta);
         */

        // System.err.println("?????????????????"+texture);
        if (cylinder.texture != null) {
            Texture GogglesOn = new TextureLoader(cylinder.texture, null).getTexture();

            // scale na textura
            TextureAttributes myta = new TextureAttributes();
            Transform3D texturetrans = new Transform3D();
            texturetrans.setScale(new Vector3d(cylinder.getLength(), cylinder.getWidth(), cylinder.getHeight()));
            myta.setTextureTransform(texturetrans);
            myta.setTextureMode(TextureAttributes.MODULATE);
            appearance3.setTextureAttributes(myta);

            appearance3.setTexture(GogglesOn);
        }
        else {
            /*
             * InputStream ist = getClass().getResourceAsStream("/images/textures/hardmetal.png"); String inFileName =
             * StreamUtil.copyStreamToTempFile(ist).getAbsolutePath(); Texture GogglesOn = new
             * TextureLoader(inFileName,null).getTexture();
             * 
             * //scale na textura TextureAttributes myta=new TextureAttributes(); Transform3D texturetrans = new
             * Transform3D(); texturetrans.setScale(new Vector3d(length,width,height));
             * myta.setTextureTransform(texturetrans); myta.setTextureMode(TextureAttributes.MODULATE) ;
             * appearance3.setTextureAttributes(myta);
             * 
             * appearance3.setTexture(GogglesOn);
             */
        }

        // appearance3.setTexture(text);
        // buf_img.flush();

        Material mat = new Material();
        mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
        mat.setCapability(Material.ALLOW_COMPONENT_READ);
        Color3f c = new Color3f();
        c.set(cylinder.getColor());

        mat.setDiffuseColor(c);
        mat.setSpecularColor(c);
        mat.setShininess(cylinder.shininess);

        // appearance3.setColoringAttributes(new ColoringAttributes(1, 1, 1, ColoringAttributes.SHADE_GOURAUD));

        // mat.setEmissiveColor(c);
        // //mat.setAmbientColor(c);
        // //mat.setDiffuseColor(c);
        // mat.setSpecularColor(c);
        // mat.setShininess(20.0f);
        appearance3.setMaterial(mat);
        appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);

        trans.setTransparency(((float) cylinder.getTransparency()) / 100f);
        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
        appearance3.setTransparencyAttributes(trans);

        /*
         * //sphere.setCapability(Box.ENABLE_APPEARANCE_MODIFY); RenderingAttributes renderingAttributes = new
         * RenderingAttributes( true, // boolean depthBufferEnable, true, // boolean depthBufferWriteEnable, 0.0f, //
         * float alphaTestValue, RenderingAttributes.ALWAYS, // int alphaTestFunction, true, // boolean visible, true,
         * // boolean ignoreVertexColors, false, // boolean rasterOpEnable, RenderingAttributes.ROP_COPY // int rasterOp
         * );
         * 
         * //ColoringAttributes coloringAttributes = new ColoringAttributes(c,ColoringAttributes.SHADE_GOURAUD);
         * appearance3.setRenderingAttributes(renderingAttributes);
         */
        sphere.setAppearance(appearance3);

        // cube.setCapability(Box.);
        // NeptusLog.pub().info("<###> "+sphere);
        model.addChild(sphere);
        model.setTransform(t);
        // model.setCapability(BranchGroup.ALLOW_DETACH); //rdiclo :-)
        // Util3D.enablePicking(model);
        return model;

    }

    public static TransformGroup getElipsoidModel3D(EllipsoidElement ellipsoid) {

        Sphere sphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, null);
        // Cylinder sphere=new Cylinder();
        // (1.0f, Sphere.ENABLE_APPEARANCE_MODIFY|Shape3D., null);
        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        sphere.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        // new Sphere();
        /*
         * sphere.getShape().setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_READ);
         * sphere.getShape().setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_WRITE);
         * sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);
         * sphere.setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_READ);
         * sphere.setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_WRITE);
         * sphere.setCapability(Shape3D.ALLOW_APPEARANCE_READ); sphere.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
         * sphere.setCapability(Sphere.ALLOW_AUTO_COMPUTE_BOUNDS_READ);
         * sphere.setCapability(Sphere.ALLOW_AUTO_COMPUTE_BOUNDS_WRITE); sphere.setCapability(Sphere.ALLOW_BOUNDS_READ);
         * sphere.setCapability(Sphere.ALLOW_BOUNDS_WRITE); sphere.setCapability(Sphere.ALLOW_CHILDREN_EXTEND);
         * sphere.setCapability(Sphere.ALLOW_CHILDREN_READ); sphere.setCapability(Sphere.ALLOW_CHILDREN_WRITE);
         * sphere.setCapability(Sphere.ALLOW_COLLIDABLE_READ); sphere.setCapability(Sphere.ALLOW_COLLIDABLE_WRITE);
         * sphere.setCapability(Sphere.ALLOW_COLLISION_BOUNDS_READ);
         * sphere.setCapability(Sphere.ALLOW_COLLISION_BOUNDS_WRITE);
         * sphere.setCapability(Sphere.ALLOW_LOCAL_TO_VWORLD_READ); sphere.setCapability(Sphere.ALLOW_PICKABLE_READ);
         * sphere.setCapability(Sphere.ALLOW_PICKABLE_WRITE);
         */

        // sphere.setCapability(sphere.);
        // sphere.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        // sphere.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        // sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);

        Transform3D t = new Transform3D();
        Transform3D t2 = new Transform3D();
        t.setScale(new Vector3d(ellipsoid.getLength(), ellipsoid.getWidth(), ellipsoid.getHeight()));
        t2.setScale(0.5);
        t.mul(t2);
        TransformGroup model = new TransformGroup();
        model.setCapability(TransformGroup.ALLOW_CHILDREN_READ);

        Appearance appearance3 = new Appearance();

        if (ellipsoid.texture != null) {
            Texture GogglesOn = new TextureLoader(ellipsoid.texture, null).getTexture();

            // scale na textura
            TextureAttributes myta = new TextureAttributes();
            Transform3D texturetrans = new Transform3D();
            texturetrans.setScale(new Vector3d(ellipsoid.getLength(), ellipsoid.getWidth(), ellipsoid.getHeight()));
            myta.setTextureTransform(texturetrans);
            myta.setTextureMode(TextureAttributes.MODULATE);
            appearance3.setTextureAttributes(myta);

            appearance3.setTexture(GogglesOn);
        }
        else {
            /*
             * InputStream ist = getClass().getResourceAsStream("/images/textures/hardmetal.png"); String inFileName =
             * StreamUtil.copyStreamToTempFile(ist).getAbsolutePath(); Texture GogglesOn = new
             * TextureLoader(inFileName,null).getTexture();
             * 
             * //scale na textura TextureAttributes myta=new TextureAttributes(); Transform3D texturetrans = new
             * Transform3D(); texturetrans.setScale(new Vector3d(length,width,height));
             * myta.setTextureTransform(texturetrans); myta.setTextureMode(TextureAttributes.MODULATE) ;
             * appearance3.setTextureAttributes(myta);
             * 
             * appearance3.setTexture(GogglesOn);
             */
        }

        Material mat = new Material();
        mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
        mat.setCapability(Material.ALLOW_COMPONENT_READ);
        Color3f c = new Color3f();
        c.set(ellipsoid.getColor());

        mat.setDiffuseColor(c);
        mat.setSpecularColor(c);
        mat.setShininess(ellipsoid.shininess);

        // mat.setEmissiveColor(c);
        // //mat.setAmbientColor(c);
        // //mat.setDiffuseColor(c);
        // mat.setSpecularColor(c);
        // mat.setShininess(20.0f);
        appearance3.setMaterial(mat);
        // appearance3.setCapability()
        /*
         * appearance3.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
         * appearance3.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_LINE_ATTRIBUTES_READ);
         * appearance3.setCapability(Appearance.ALLOW_LINE_ATTRIBUTES_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_MATERIAL_READ);
         * appearance3.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_POINT_ATTRIBUTES_READ);
         * appearance3.setCapability(Appearance.ALLOW_POINT_ATTRIBUTES_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
         * appearance3.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
         * appearance3.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_TEXTURE_READ);
         * appearance3.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_TEXGEN_READ);
         * appearance3.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_TEXTURE_UNIT_STATE_READ);
         * appearance3.setCapability(Appearance.ALLOW_TEXTURE_UNIT_STATE_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
         * appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
         * 
         * appearance3.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
         * appearance3.setCapability(Appearance.ALLOW_MATERIAL_READ);
         */

        appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        trans.setTransparency(((float) ellipsoid.getTransparency()) / 100f);

        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);

        appearance3.setTransparencyAttributes(trans);

        /*
         * RenderingAttributes renderingAttributes = new RenderingAttributes( true, // boolean depthBufferEnable, true,
         * // boolean depthBufferWriteEnable, 0.0f, // float alphaTestValue, RenderingAttributes.ALWAYS, // int
         * alphaTestFunction, true, // boolean visible, true, // boolean ignoreVertexColors, false, // boolean
         * rasterOpEnable, RenderingAttributes.ROP_COPY // int rasterOp );
         * 
         * //ColoringAttributes coloringAttributes = new ColoringAttributes(c,ColoringAttributes.SHADE_GOURAUD);
         * appearance3.setRenderingAttributes(renderingAttributes);
         */
        sphere.setAppearance(appearance3);

        // cube.setCapability(Box.);
        // NeptusLog.pub().info("<###> "+sphere);
        model.addChild(sphere);
        model.setTransform(t);
        sphere.getAppearance();
        // model.setCapability(BranchGroup.ALLOW_DETACH); //
        // Util3D.enablePicking(model);
        return model;
    }

    public static TransformGroup getModel3DModel3D(Model3DElement model) {
        TransformGroup themodel = new TransformGroup();

        if (model.getModel3DFilename() == null)
            return null;
        File fx = null;

        try {
            fx = new File(ConfigFetch.resolvePath(model.getModel3DFilename()));
        }
        catch (Exception e) {
            return null;
        }

        String extension = FileUtil.getFileExtension(fx);
        if (extension.equalsIgnoreCase("3ds")) {
            Inspector3DS obj3Dloader = new Inspector3DS(ConfigFetch.resolvePath(model.getModel3DFilename())); // constructor
            obj3Dloader.parseIt(); // process the file
            themodel = obj3Dloader.getModel(); // get the resulting 3D
        }
        else if (extension.equalsIgnoreCase("x3d")) {
            X3dParse parse = new X3dParse();
            parse.setFileX3d(fx.getAbsolutePath().toString());
            try {
                themodel = parse.parse();
            }
            catch (Exception e) {
                NeptusLog.pub().error("Model3dElement parse file error", e);
                GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
            }
            return null;
        }
        else if (extension.equalsIgnoreCase("wrl")) {
            Loader myFileLoader = null; // holds the file loader
            Scene myVRMLScene = null; // holds the loaded scene
            BranchGroup myVRMLModel = null; // BG of the VRML scene
            try {
                // create an instance of the Loader
                myFileLoader = new org.web3d.j3d.loaders.VRML97Loader();

                myFileLoader.setBasePath(fx.getParent());
                myFileLoader.setFlags(org.web3d.j3d.loaders.VRML97Loader.LOAD_BEHAVIOR_NODES);
                // Load the scene from your VRML97 file
                myVRMLScene = myFileLoader.load(fx.getAbsolutePath());

                // Obtain the root BranchGroup for the Scene
                myVRMLModel = myVRMLScene.getSceneGroup();

                // themodel = new TransformGroup();
                @SuppressWarnings("unchecked")
                Enumeration<Node> enume = myVRMLModel.getAllChildren();
                while (enume.hasMoreElements()) {
                    javax.media.j3d.Node next = (javax.media.j3d.Node) enume.nextElement();
                    // System.err.println(next);
                    myVRMLModel.removeChild((javax.media.j3d.Node) next);
                    themodel.addChild((javax.media.j3d.Node) next);
                }

                if (Util3D.removeBackgroud(themodel))
                    NeptusLog.pub().info("<###>tem back");
            }
            catch (Exception e) {
                NeptusLog.pub().error("Model3dElement parse file error", e);
                GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
            }
        }
        else {
            themodel = null;
            NeptusLog.pub().error("Model3dElement: Invalid 3D file type [" + extension + "].");
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), "Loader Model3dElement", "Invalid 3D file type ["
                    + extension + "].");
        }

        Transform3D t = new Transform3D();
        t.setIdentity();
        t.setScale(model.getModel3DScale());
        themodel.setTransform(t);
        Util3D.enablePicking(themodel);
        return themodel;
    }

    public static TransformGroup getParallelepipedModel3D(ParallelepipedElement pipe) {

        // Box cube=new Box(3,4,2,null); //altera mesmo as mapping coords
        // float bug=Util3D.getPickBug();
        Box cube = new Box(1.0f, 1.0f, 1.0f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, null);
        // cube.setCapability(Box.ALLOW_PICKABLE_WRITE);

        Enumeration<?> childs = cube.getAllChildren();
        while (childs.hasMoreElements()) {
            Object child = childs.nextElement();
            if (child instanceof Shape3D) {
                Shape3D aux = (Shape3D) child;
                aux.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
                aux.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
            }
        }
        cube.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        cube.getChild(0).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        cube.setCapability(Box.ENABLE_APPEARANCE_MODIFY);
        // Cylinder cube= new Cylinder();
        Transform3D t = new Transform3D();
        Transform3D t2 = new Transform3D();
        t.setScale(new Vector3d(pipe.getLength(), pipe.getWidth(), pipe.getHeight()));

        t2.setScale(0.5);
        t.mul(t2);

        TransformGroup model = new TransformGroup();
        model.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        model.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        model.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        model.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

        Appearance appearance3 = new Appearance();
        // System.err.println("?????????????????"+texture);
        // texture = TexturesHolder.getTextureByName("asphalt").getTextureImage();
        if (pipe.texture != null) {
            Texture GogglesOn = new TextureLoader(pipe.texture, null).getTexture();

            // scale na textura
            TextureAttributes myta = new TextureAttributes();
            Transform3D texturetrans = new Transform3D();
            texturetrans.setScale(new Vector3d(pipe.getLength(), pipe.getWidth(), pipe.getHeight()));
            myta.setTextureTransform(texturetrans);
            myta.setTextureMode(TextureAttributes.MODULATE);
            appearance3.setTextureAttributes(myta);

            appearance3.setTexture(GogglesOn);
        }
        else {

            /*
             * InputStream ist = getClass().getResourceAsStream("/images/textures/hardmetal.png"); String inFileName =
             * StreamUtil.copyStreamToTempFile(ist).getAbsolutePath(); Texture gogglesOn = new
             * TextureLoader(inFileName,"RGBA",null).getTexture();
             * 
             * //scale na textura //gogglesOn.RGBA TextureAttributes myta=new TextureAttributes(); Transform3D
             * texturetrans = new Transform3D(); texturetrans.setScale(new Vector3d(length,width,height));
             * myta.setTextureTransform(texturetrans); myta.setTextureMode(TextureAttributes.MODULATE) ;
             * appearance3.setTextureAttributes(myta);
             * 
             * appearance3.setTexture(gogglesOn);
             */
            // sem textura
        }

        appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        appearance3.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

        Material mat = new Material();
        mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
        mat.setCapability(Material.ALLOW_COMPONENT_READ);
        Color3f c = new Color3f();
        c.set(pipe.getColor());
        // mat.setEmissiveColor(c);
        // mat.setAmbientColor(c);
        mat.setDiffuseColor(c);
        mat.setSpecularColor(c);
        mat.setShininess(pipe.shininess);
        appearance3.setMaterial(mat);
        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setTransparency(((float) pipe.getTransparency()) / 100f);
        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
        // trans.setSrcBlendFunction(TransparencyAttributes.BLEND_ONE);

        trans.setCapability(TransparencyAttributes.ALLOW_BLEND_FUNCTION_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_BLEND_FUNCTION_READ);
        appearance3.setTransparencyAttributes(trans);
        cube.setCapability(Box.ENABLE_APPEARANCE_MODIFY);

        cube.setAppearance(appearance3);

        /*
         * Appearance appearance4 = new Appearance();
         * 
         * Material mat2 = new Material(); Color3f c2=new Color3f(); c2.set(objColor); //mat.setEmissiveColor(c);
         * mat2.setAmbientColor(c); mat2.setDiffuseColor(c); mat2.setSpecularColor(c); //mat.setShininess(1.0f);
         * appearance4.setMaterial(mat); //cube.setCapability(Box.ENABLE_APPEARANCE_MODIFY);
         * 
         * PolygonAttributes pa = new PolygonAttributes(); pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
         * pa.setCullFace(PolygonAttributes.CULL_NONE); appearance4.setPolygonAttributes(pa);
         * //trans.setTransparencyMode(TransparencyAttributes.NONE);
         * 
         * //cube.setCapability(Box.); Box cubew=new Box();
         * 
         * 
         * 
         * cubew.setAppearance(appearance4);
         */
        Appearance appearance = new Appearance();
        appearance.setMaterial(mat);
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
        Point3d myCoords[] = { new Point3d(-1, 1, 1), new Point3d(1, 1, 1), new Point3d(-1, -1, 1),
                new Point3d(1, -1, 1), new Point3d(-1, -1, -1), new Point3d(1, -1, -1), new Point3d(-1, 1, -1),
                new Point3d(1, 1, -1),

                new Point3d(1, 1, -1), new Point3d(1, 1, 1), new Point3d(-1, 1, -1), new Point3d(-1, 1, 1),
                new Point3d(1, -1, -1), new Point3d(1, -1, 1), new Point3d(-1, -1, -1), new Point3d(-1, -1, 1),

                new Point3d(1, -1, 1), new Point3d(1, 1, 1), new Point3d(1, -1, -1), new Point3d(1, 1, -1),
                new Point3d(-1, -1, 1), new Point3d(-1, 1, 1), new Point3d(-1, -1, -1), new Point3d(-1, 1, -1), };

        // NeptusLog.pub().info("<###> "+x);
        // Point3d listp[]=new Point3d[myCoords.length*2];

        LineArray myLines = new LineArray(myCoords.length, LineArray.COORDINATES);
        myLines.setCoordinates(0, myCoords);

        Shape3D shape3D;
        shape3D = new Shape3D(myLines, appearance);

        shape3D.getAppearance();
        shape3D.setAppearance(appearance);

        /*
         * TransparencyAttributes trans=new TransparencyAttributes(); trans.setTransparency(0.3f);
         * trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE); appearance.setTransparencyAttributes(trans);
         */

        model.addChild(shape3D);
        model.addChild(cube);
        // model.addChild(Util3D.makeAxis(true,3.0));

        // model.addChild(cubew);
        model.setTransform(t);
        // if(model.isLive())
        // System.err.println(model+" está vivo");
        // else if(model.isCompiled())
        // System.err.println(model+" está compilado");
        // else
        // System.err.println(model+" nem vivo nem comp");

        // System.err.println("-----------------------");
        // Util3D.enablePicking(model);

        return model;
    }

    public static TransformGroup getPathModel3D(PathElement path) {
        Shape3D shape3D;
        TransformGroup model = new TransformGroup();
        Appearance appearance = new Appearance();

        Material mat = new Material();
        Color3f c = new Color3f(1.0f, 0.0f, 0.0f);
        c.set(path.getMyColor());
        mat.setEmissiveColor(c);
        mat.setAmbientColor(c);
        mat.setDiffuseColor(c);
        // mat.setSpecularColor(c);
        // mat.setShininess(20.0f);
        appearance.setMaterial(mat);
        // appearance.set

        Point3d myCoords[] = path.getPath();

        if (path.getPath().length == 0)
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

    public static TransformGroup getImageModel3D(ImageElement img) {
        Texture3D image3D;
        try {
            image3D = new Texture3D(img.getImage(), img.getImage().getHeight(null) * img.getImageScale(), img
                    .getImage().getWidth(null) * img.getImageScale());
        }
        catch (Exception e) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), "Error loading image",
                    "Image '" + img.getId() + "' was not able to be loaded!\nCheck the path please:\n'"
                            + img.getImageFileName().replace(ConfigFetch.getNeptusTmpDir(), "") + "'");
            return null;
        }
        Image heightImage = img.getImage();
        image3D.setTwoSided(true);
        image3D.setTransparency(((float) img.getTransparency()) / 100f);
        image3D.setShade(false);

        image3D.texturescale = 1.0;
        if (img.isBathymetric()) {
            if (img.getBathymetricImageFileName() != null)
                heightImage = ImageUtils.getImage(img.getBathymetricImageFileName());

            image3D.setBitmap(heightImage);
            image3D.maxvalue = img.getMaxHeight();
            image3D.minvalue = img.getMaxDepth();
            // System.err.println("max"+maxHeight +"min"+maxDepth);
            image3D.setResolution((int)img.getResolution());
        }
        TransformGroup model = image3D.getModel3D();
        // Util3D.enablePicking(model);
        return model;
    }

    public static TransformGroup getTransponderModel3D(TransponderElement transp) {

        InputStream istt = Object3DCreationHelper.class.getResourceAsStream("/models/trans1.3ds");
        // NeptusLog.pub().info("<###>-----------  "+istt);
        String inFileNamet = StreamUtil.copyStreamToTempFile(istt).getAbsolutePath();

        InputStream istb = Object3DCreationHelper.class.getResourceAsStream("/models/boia1.3ds");
        String inFileNameb = StreamUtil.copyStreamToTempFile(istb).getAbsolutePath();

        Inspector3DS loader = new Inspector3DS(inFileNamet); // constructor
        loader.parseIt(); // process the file
        TransformGroup theModel1 = loader.getModel(); // get the resulting 3D

        loader = new Inspector3DS(inFileNameb); // constructor
        loader.parseIt(); // process the file
        TransformGroup theModel2 = loader.getModel(); // get the resulting 3D
        Transform3D t1 = new Transform3D();

        t1.setTranslation(new Vector3f(0f, 0f, -(float) transp.getNEDPosition()[2]));
        theModel2.setTransform(t1);

        TransformGroup theModel = new TransformGroup(); // get the resulting 3D
        theModel.addChild(theModel1);
        // TODO (rjpg) um if para saber se tem boia
        theModel.addChild(theModel2);

        return theModel;
    }

    public static TransformGroup getDynamicElementModel3D(DynamicElement elem) {
        TransformGroup model = new TransformGroup();
        Sphere sphere = new Sphere();
        
        Color3f c = new Color3f(elem.getInnerColor());
        ColoringAttributes coloringAttributes = new ColoringAttributes(c,
                ColoringAttributes.SHADE_GOURAUD);

        coloringAttributes.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
        
        Appearance appearance2 = new Appearance();
        //appearance2.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        //appearance2.setRenderingAttributes(renderingAttributes);
        appearance2.setColoringAttributes(coloringAttributes);
        //TransparencyAttributes trans = new TransparencyAttributes();
        //trans.setTransparency(0.3f);
        //trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
        //appearance2.setTransparencyAttributes(trans);
        sphere.setAppearance(appearance2);
        //sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);

        Transform3D t = new Transform3D();
        t.setScale(0.5);
        model.setTransform(t);
        model.addChild(sphere);
        ChangeColorBehavior tb = new ChangeColorBehavior(1000, coloringAttributes,elem);
        tb.setSchedulingBounds(Util3D.BOUNDS3D);
        model.addChild(tb);
        return model;
    }
    
    private static class ChangeColorBehavior extends Behavior {
        private WakeupCondition timeOut;
        private boolean isStopped = false;
        public ColoringAttributes coloringAttributes;
        public DynamicElement de;
        Color innerColor=Color.gray;
        public ChangeColorBehavior(long timeDelayA, ColoringAttributes coloringAttributesA,
                DynamicElement deA) {
            de=deA;
            timeOut = new WakeupOnElapsedTime(timeDelayA);
            this.coloringAttributes=coloringAttributesA;
            this.coloringAttributes.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
        }

        public void initialize() {
            wakeupOn(timeOut);
        }

        @SuppressWarnings("rawtypes")
        public void processStimulus(Enumeration criteria) { // ignore criteria
            if (!isStopped) {
                 if (de.getIdleTimeSecs() == -1)
                        innerColor = Color.gray;
                    else {
                        
                        innerColor = de.getColorMap().getColor((double)de.getIdleTimeSecs()/(double)de.getConnectionTimeoutSecs());         
                 }
                coloringAttributes.setColor(new Color3f(innerColor));
                //System.err.println(""+innerColor);
                if(de.getIdleTimeSecs()>de.getConnectionTimeoutSecs())
                    this.stopUpdate();
                else
                    wakeupOn(timeOut);
            }
        }
        
        public void stopUpdate() {
            isStopped = true;   
        }
    }

}
