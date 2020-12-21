/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 18/Mar/2002
 */
package pt.lsts.neptus.renderer3d;

import java.awt.Color;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;

/**
 * @author RJPG
 * 
 *         This class makes the conection between obj from neptus and Java3D aplying rotations and translations
 * 
 *         every method ending by "3D" is related with Java3D
 */
public class Obj3D {

    private Vector<Obj3D> obj3dChilds = new Vector<Obj3D>(); // childs

    private TransformGroup rotx = new TransformGroup(); // em x
    private TransformGroup roty = new TransformGroup(); // em y
    // private BranchGroup brroty =new BranchGroup(); //node de tudo
    private TransformGroup rotz = new TransformGroup(); // em z
    private TransformGroup move = new TransformGroup(); // trasl
    private TransformGroup model; // "desenho"
    private BranchGroup fullobj = new BranchGroup(); // node de tudo

    // connect childs
    TransformGroup connectTG = new TransformGroup();
    BranchGroup connect = new BranchGroup();
    private Shape3D connectShape = new Shape3D();
    private GeometryArray geometry = null;
    private QuadArray GFront = null;
    public Appearance app = new Appearance();
    public float connectTransp = 0.5f;
    public Color3f connectColor = new Color3f(Color.RED);
    // private BranchGroup axisobj=new BranchGroup(); //node de tudo

    protected double roll = 0., pitch = 0., yaw = 0.; // angulos
    protected double[] pos = new double[3]; // posição

    public BranchGroup Axis = new BranchGroup();
    public boolean drawaxis = false;
    public boolean drawlabel = false;
    public boolean drawinfo = false;
    public boolean hide = false;

    /**
     * constructor all vars goes 0 and no model3D
     */
    public Obj3D() {
        // hierarquia
        fullobj.addChild(move);
        fullobj.setCapability(BranchGroup.ALLOW_DETACH);
        // model.addChild(move);
        move.addChild(rotz); // translação
        rotz.addChild(rotx);
        rotx.addChild(roty); // rotações
        roty.addChild(model); // filho: desenho...

        // permitir escritas e leituras nos nós
        move.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        move.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        move.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

        rotx.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotx.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        roty.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        roty.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        roty.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        roty.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        roty.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

        rotz.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotz.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        rotz.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        rotz.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        rotz.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

        // Eixos
        Axis.addChild(Util3D.makeAxis(false, 10.0));
        Axis.setCapability(BranchGroup.ALLOW_DETACH);

        TransformGroup connectTG = new TransformGroup();
        BranchGroup connect = new BranchGroup();
        connectTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        connectTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        connectTG.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        connectTG.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        connectTG.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        connect.setCapability(BranchGroup.ALLOW_DETACH);
        connect.addChild(connectTG);
        GFront = new QuadArray(100 * 4, QuadArray.COORDINATES);
        GeometryInfo gi = new GeometryInfo(GFront);
        geometry = gi.getGeometryArray();
        geometry.setCapability(GeometryArray.ALLOW_NORMAL_READ);
        geometry.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
        geometry.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        geometry.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
        geometry.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

        // geometry.setCapability();
        // System.err.println("format:"+geometry.getVertexFormat()+"\n count:"+geometry.getVertexCount());

        createAppearance();
        // cube.setAppearance(app);
        connectShape = new Shape3D(geometry, app);

        // connectShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        // connectShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        connectShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        connectShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        connectShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        connectShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        // move.setPickable(true);// se se implementar o clic em objs
        connectTG.addChild(connectShape);
    }

    public void createAppearance() {
        app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

        Material mat = new Material();
        mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
        mat.setCapability(Material.ALLOW_COMPONENT_READ);
        mat.setDiffuseColor(connectColor);
        mat.setSpecularColor(connectColor);
        mat.setShininess(0.3f);
        app.setMaterial(mat);
        TransparencyAttributes trans = new TransparencyAttributes();
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setTransparency(connectTransp);
        trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
        trans.setCapability(TransparencyAttributes.ALLOW_BLEND_FUNCTION_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_BLEND_FUNCTION_READ);
        app.setTransparencyAttributes(trans);
    }

    /**
     * @param obj Model 3D to be associated
     */
    public Obj3D(TransformGroup obj) {
        this();
        this.setModel3D(obj);

    }

    public void DrawAxis(boolean flag) {
        if (flag != drawaxis)
            if (!flag) {
                drawaxis = flag;
                roty.removeChild(Axis); // filho: desenho...
            }
            else {
                drawaxis = flag;
                roty.addChild(Axis); // filho: desenho...
            }
    }

    /**
     * @param m modelo3D a seleccionar
     */
    public void setModel3D(TransformGroup m) {
        roty.removeChild(model);
        model = null;

        model = m;
        roty.addChild(model);
        // fullobj.compile();
    }

    /**
     * @return the model associated
     */
    public TransformGroup getModel3D() {
        return model;
    }

    /**
     * @return the full node to be added in Java3D scene
     */
    public BranchGroup getFullObj3D() {
        return fullobj;
    }

    /**
     * @param p set p as node parent of this obj3D
     */
    public void setParent3D(TransformGroup p) {
        p.addChild(fullobj);
    }

    /**
     * @param ang roll angle of object
     */
    public void setRoll(double ang) {
        // float rad=(float) ((float)( Math.PI*(ang-roll) )/180.0);//graus to rad
        // double rad=Math.toRadians(ang);
        Transform3D rot = new Transform3D();
        rot.rotX(ang);
        rotx.setTransform(rot);
        roll = ang;//
    }

    /**
     * @param ang picth angle of object
     */
    public void setPitch(double ang) {
        // double rad=Math.toRadians(ang);
        Transform3D rot = new Transform3D();
        rot.rotY(ang);
        roty.setTransform(rot);
        pitch = ang;//
    }

    /**
     * @param zz angle of object
     */
    public void setYaw(double ang) {
        // double rad=Math.toRadians(ang);
        Transform3D rot = new Transform3D();
        rot.rotZ(ang);
        rotz.setTransform(rot);
        yaw = ang;//
    }

    /**
     * @param p vector position p[0]->X->North p[1]->Y->East p[2]->Z->Depth
     */
    public void setPos(double[] p) {
        Transform3D m = new Transform3D();
        m.set(new Vector3d(p[0], p[1], p[2]));
        move.setTransform(m);
        pos[0] = p[0];//
        pos[1] = p[1];//
        pos[2] = p[2];//
    }

    public void setPos(Point3d p) {
        Transform3D m = new Transform3D();
        m.set(new Vector3d(p.x, p.y, p.z));
        move.setTransform(m);
        pos[0] = p.x;//
        pos[1] = p.y;//
        pos[2] = p.z;//
    }

    public Point3d getPos() {
        Point3d ptn = new Point3d();
        ptn.x = pos[0];//
        ptn.y = pos[1];//
        ptn.z = pos[2];//
        return ptn;
    }

    /**
     * @param c Camera3D is locked on this object
     */
    public void addCamera3D(Camera3D c) {
        // TransformGroup x;
        // x=(TransformGroup)c.getCamera3D().getParent();
        // x.removeChild(c.getCamera3D());
        rotz.addChild(c.getCamera3D());
    }

    /**
     * @param c Camera3D is removed
     */
    public void removeCamera3D(Camera3D c) {
        // TransformGroup x;
        // x=(TransformGroup)c.getCamera3D().getParent();
        // x.removeChild(c.getCamera3D());
        rotz.removeChild(c.getCamera3D());
        // roty.addChild(c.getCamera3D());
    }

    public void addObj3D(Obj3D obj) {
        obj3dChilds.add(obj);
        roty.addChild(obj.getFullObj3D());
    }

    public void removeObj3D(Obj3D obj) {
        roty.removeChild(obj.getFullObj3D());
        obj3dChilds.remove(obj);
    }

    public void removeObj3DByRelativeLocation(Point3d p3d) {
        if (p3d == null)
            return;
        for (Obj3D j : obj3dChilds) {
            if (j.pos[0] == p3d.x && j.pos[1] == p3d.y && j.pos[2] == p3d.z) {
                removeObj3D(j);
            }
        }

    }

    // public void connectChilds()
    // {
    // Obj3D[] obj3DArray=obj3dChilds.toArray(new Obj3D[0]);
    // for(int i=0;i<obj3DArray.length;i++)
    // {
    //
    // }
    //
    //
    // }

    public void removeLastNObj3D(int N) {
        // System.err.println("N:"+N);
        Obj3D[] obj3DArray = obj3dChilds.toArray(new Obj3D[0]);

        for (int i = 0; i < N; i++) {
            roty.removeChild(obj3DArray[i].getFullObj3D());
            obj3dChilds.remove(obj3DArray[i]);
        }
    }

    public int getObj3DChildsLength() {
        return obj3dChilds.size();
    }

    public void setPickble() {
        // System.err.println("enablePicking foi chamado");
        // Util3D.enablePicking(this.getModel3D());

    }
}
