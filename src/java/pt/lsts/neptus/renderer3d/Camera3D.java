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
 * Mar 24, 2005
 */
package pt.lsts.neptus.renderer3d;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.exp.swing.JCanvas3D;

import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author RJPG This class is used to create a view point in 3D space using spherical coordinates and setting an pivot
 *         Every method ending by 3D has a close connection with Java3D
 */
public class Camera3D {

    // ---------------------------------------cleanup
    public boolean clean = false;

    /*------------vars da classe------------------------*/
    protected double rho, phi, theta, psi; // coordenadas esfericas
    protected Vector3d pivot, pos; // coordenadasretangulares
    protected double scale, viewangle; // ajustes

    public Renderer3D associatedrender = null;

    public VehicleType lock = null; // aux
    public AbstractElement lockmapobj = null; // aux
    public Obj3D lockobj = null; // aux

    public static final int TOP = 0, BACK = 1, RIGHT = 2, USER = 3, DEFAULT = -1;
    protected short type = DEFAULT;

    /*------------vars do Java3D-------------------------*/
    protected int projection; // tipo: orthonormal ou prespectiva
    public MyCanvas3D canvas; // bitmap de render desta cam para associar a um panel...
    public JPanel jCanvas3DPanel; // painel de render para light swing

    private BoundingSphere bounds;
    private ViewPlatform viewPlatform;
    public View view; // vista(camera)
    private PhysicalBody body; // distancia de olhos se usar 2 monitores...
    private PhysicalEnvironment environment;
    // private Screen3D screen;//isto pode sair: usado para render Full screen

    // roll-phi-X
    // pitch-theta-Y
    // yaw-psi-Z
    // rho
    private TransformGroup Theta = new TransformGroup(); // ang vertical
    private TransformGroup Psi = new TransformGroup(); // ang horizontal
    private TransformGroup Phi = new TransformGroup(); // roll
    private TransformGroup Rho = new TransformGroup(); // dist
    private TransformGroup Pivot = new TransformGroup(); // trasl
    private TransformGroup Scale = new TransformGroup(); // escala(zoom)

    public BranchGroup fullcam = new BranchGroup();
    /*------------vars para aplicação--------------------*/
    public double step = 0.25; // andamento em x,y,z
    public double stepscale = 0.025; // aumento no zoom

    /**
     * Default constructor The viewpoint is (0,0,-10) all angles are null looking to pivot(0,0,0) The roll is null as
     * well (North pointed Right) (looking up 10 meters under wather) Is associated Canvas3D that's public
     */
    public Camera3D() {
        // coordenadas esfericas
        pivot = new Vector3d(0.0, 0.0, 0.0); // para onde olha
        rho = 10.0; // distancia ao Pivot
        phi = 0.0; // "rool" da camera
        theta = 0.0; // angulo vertical
        psi = 0.0; // angulo horizontal

        pos = new Vector3d(0.0, 0.0, 0.0);// ... coordenadas rectangulares(não pronto ainda)

        Transform3D t = new Transform3D();// Matriz aux

        bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.MAX_VALUE);

        Theta.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Theta.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        t.setIdentity();
        t.rotX(theta);
        Theta.setTransform(t);

        Psi.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Psi.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        t.setIdentity();
        t.rotZ(psi);
        Psi.setTransform(t);

        Phi.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Phi.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        t.setIdentity();
        t.rotZ(phi);
        Phi.setTransform(t);

        Rho.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Rho.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        t.setIdentity();
        t.set(new Vector3d(0.0, 0.0, rho));
        Rho.setTransform(t);

        Pivot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Pivot.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        t.setIdentity();
        t.set(pivot);
        Pivot.setTransform(t);

        Pivot.addChild(Psi);
        Psi.addChild(Theta);
        Theta.addChild(Phi);
        Phi.addChild(Rho);

        Scale = new TransformGroup();
        Scale.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Scale.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        // Retirar a configuração do ecra
        GraphicsConfigTemplate3D tmpl = new GraphicsConfigTemplate3D();
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();
        GraphicsConfiguration config = device.getBestConfiguration(tmpl);

        JCanvas3D canJ = new JCanvas3D(device);
        canJ.setSize(400, 400);
        jCanvas3DPanel = new JPanel(new BorderLayout());
        jCanvas3DPanel.add(canJ);

        /*
         * Canvas3D can =(Canvas3D) canJ.getOffscreenCanvas3D(); if(can==null)
         * System.err.println("---------------can é null!!!!!---------"); canvas=(MyCanvas3D) can;
         * 
         * if(canvas==null) System.err.println("---------------canvas é null!!!!!---------");
         * 
         * canvas.stopRenderer(); canvas.setCamera(this); canvas.loadIcons();
         */

        // ou ///////////////////
        canvas = new MyCanvas3D(config, this);
        canvas.stopRenderer();
        // //////////////////////

        // canvas.setBackground(new Color(2, 113, 171));
        canvas.setDoubleBufferEnable(true);

        view = new View();
        viewPlatform = new ViewPlatform();
        viewPlatform.setActivationRadius(Float.MAX_VALUE);
        viewPlatform.setBounds(bounds);
        Scale.addChild(viewPlatform);
        Rho.addChild(Scale);// <<<<<<<<<<<<<---------------------------
        view = new View();
        view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
        projection = View.PERSPECTIVE_PROJECTION;
        view.addCanvas3D(canvas);

        view.setBackClipDistance(800);
        view.setFrontClipDistance(0.1);

        body = new PhysicalBody();
        view.setPhysicalBody(body);
        environment = new PhysicalEnvironment();
        view.setPhysicalEnvironment(environment);
        view.attachViewPlatform(viewPlatform);
        // view.setScreenScalePolicy(View.SCALE_SCREEN_SIZE);
        view.setWindowResizePolicy(View.PHYSICAL_WORLD); //

        // aqui é que entram as tranformações
        // t.lookAt(new Point3d(0.0f,0.0f,-20.0f),new Point3d(0.0f,0.0f,0.0f),new Vector3d(1.0f,0.0f,0.0f));
        // t.invert();
        // canvas.getPixelLocationInImagePlate()

        fullcam.addChild(Pivot);
        fullcam.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        fullcam.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

    }

    /**
     * Constructor
     * 
     * @param style Type of camera 0-TOP 1-RIGHT 2-FRONT 3-USER
     */
    public Camera3D(int style) {
        this();
        switch (style) {
            case TOP: {
                resetTop();
                break;
            }
            case RIGHT: {
                resetRight();
                break;
            }
            case BACK: {
                resetBack();
                break;
            }
            case USER: {
                resetUser();
                break;
            }
            default: {

            }
        }
    }

    public void setWindowResizePolicy(int p) {
        view.setWindowResizePolicy(p);
    }

    /**
     * @param style Type of camera 0-TOP 1-RIGHT 2-FRONT 3-USER
     */
    public void setType(int style) {
        switch (style) {
            case TOP: {
                resetTop();
                break;
            }
            case RIGHT: {
                resetRight();
                break;
            }
            case BACK: {
                resetBack();
                break;
            }
            case USER: {
                resetUser();
                break;
            }
            default: {

            }
        }
    }

    /**
     * @return the camera type as int
     */
    public int getType() {
        return type;
    }

    /**
     * @return the type of camera as String
     */
    public String getStrType() {
        switch (type) {
            case TOP: {
                return "Top";

            }
            case RIGHT: {
                return "Right";

            }
            case BACK: {
                return "Back";

            }
            case USER: {
                return "User";

            }
            default: {
                return "Custom";
            }
        }
    }

    /**
     * reset the camera to top view (ortho)
     * 
     */
    public void resetTop() {
        setPhi(0.0);
        setPsi(Math.PI / 2);
        setScale(0.2);
        setTheta(Math.PI);
        setRho(500.0);
        setPivot(new Vector3d(0.0, 0.0, 0.0));
        setProjection(View.PARALLEL_PROJECTION);
        type = TOP;
    }

    /**
     * reset the camera to back view (ortho)
     * 
     */
    public void resetBack() {
        setPhi(Math.PI);
        setPsi(Math.PI + Math.PI / 2);
        setTheta(0.0);
        setScale(0.2);
        setTheta(Math.PI / 2);
        setRho(500.0);
        setPivot(new Vector3d(0.0, 0.0, 0.0));
        setProjection(View.PARALLEL_PROJECTION);
        type = BACK;
    }

    /**
     * reset the camera to Right view (ortho)
     * 
     */
    public void resetRight() {
        setPhi(Math.PI);
        setPsi(Math.PI);
        setTheta(Math.PI / 2);
        setScale(0.2);
        setRho(500.0);
        setPivot(new Vector3d(0.0, 0.0, 0.0));
        setProjection(View.PARALLEL_PROJECTION);
        type = RIGHT;
    }

    /**
     * reset the camera to "User" view (Prespctive)
     * 
     */
    public void resetUser() {
        setPhi(Math.PI);
        // setPsi(Math.PI-Math.PI/4);
        setPsi(-Math.PI / 2);
        setTheta(Math.PI - Math.PI / 4);
        // setTheta(Math.PI-Math.toRadians(45+27));
        setScale(1.0);
        setRho(30.0);
        setPivot(new Vector3d(0.0, 0.0, 0.0));
        setProjection(View.PERSPECTIVE_PROJECTION);
        type = USER;
    }

    /**
     * all angles and pivot setted null distance of view setted to 30 meters
     */
    public void reset() {
        setPhi(0.0);
        setPsi(0.0);
        setTheta(0.0);
        setScale(0.2);
        setRho(30.0);
        setPivot(new Vector3d(0.0, 0.0, 0.0));
        setProjection(View.PARALLEL_PROJECTION);
        type = DEFAULT;
    }

    /**
     * @param t Vertical angle
     */
    public void setTheta(double t) {
        Transform3D tt = new Transform3D();
        theta = t;
        tt.rotX(theta);
        Theta.setTransform(tt);
    }

    /**
     * @param p Roll of the camera
     */
    public void setPhi(double p) {
        Transform3D t = new Transform3D();
        phi = p;
        t.rotZ(phi);
        Phi.setTransform(t);
    }

    /**
     * @param p horizontal angle
     */
    public void setPsi(double p) {
        Transform3D t = new Transform3D();
        t.setIdentity();
        psi = p;
        t.rotZ(psi);

        // System.err.println("psi:"+psi);
        Psi.setTransform(t);
    }

    /**
     * @param r Distance from Pivot
     */
    public void setRho(double r) {
        Transform3D t = new Transform3D();
        t.setIdentity();
        rho = r;
        t.set(new Vector3d(0.0, 0.0, rho));
        Rho.setTransform(t);
    }

    /**
     * @param p Pivot where camera is looking(pointed)
     */
    public void setPivot(Vector3d p) {
        Transform3D t = new Transform3D();
        pivot = p;
        t.set(pivot);
        Pivot.setTransform(t);
    }

    /**
     * @param s scale of universe
     */
    public void setScale(double s) {
        Transform3D t = new Transform3D();
        scale = s;
        t.setScale(scale);
        t.invert(); // inverte Matriz porque quem fica grande é a camera não os objectos
        Scale.setTransform(t);
    }

    public double getScale() {
        return scale;
    }

    /**
     * @param proj View.PERSPECTIVE_PROJECTION or View.PARALLEL_PROJECTION
     */
    public void setProjection(int proj) {
        view.setProjectionPolicy(proj);
        projection = proj;
    }

    /**
     * @return the full node to be added in Java3D scene graph
     */
    public BranchGroup getCamera3D() {
        return fullcam;
    }

    public void cleanup() {

        if (clean)
            return;

        for (int i = view.numCanvas3Ds() - 1; i >= 0; i--) {
            Canvas3D c = view.getCanvas3D(i);
            if (c.isOffScreen()) {
                c.setOffScreenBuffer(null);
            }
        }

        view.stopView();
        view.stopBehaviorScheduler();
        view.removeAllCanvas3Ds();
        view.attachViewPlatform(null);
        view = null;
        canvas.stopRenderer();
        canvas = null;
        fullcam.detach();
        fullcam = null;
        clean = true;
    }

    public JPanel getCanvas3DPanel() {
        return jCanvas3DPanel;
    }

}
