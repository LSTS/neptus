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
 * Author:
 * 20??/??/??
 */
package pt.lsts.neptus.renderer3d;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.View;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.DynamicElement;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author Rui
 * this class is extended to canvas3D of Java3Dm, the difference
 * is the rewrited method PostRenderer.
 * Is used to display 2d graphiscs over the 3D rederer scene.
 */
class MyCanvas3D extends Canvas3D {


    //private GraphicsContext3D gc;
    private static final long serialVersionUID = 1L;
    public J3DGraphics2D g2 = null;

    private Dimension d = new Dimension(0, 0);

    protected Camera3D camera;

    //private Obj3D obj = null;

    public Point2d p1 = new Point2d();

    public Point2d p2 = new Point2d();

    public boolean caminfo = false;

    public boolean axisinfo = false;

    public boolean objinfo = false;

    public boolean transpinfo = false;

    public boolean veicleinfo = false;

    public boolean reguainfo = false;

    public boolean selected = false;

    public boolean vehicleicons = false;

    public boolean objsicons = false;

    public boolean gradback = false;

    public Image trans;
    public Image mark;
    public Image home;

    /**
     * @param point3d point in 3D space
     * @return the 2d coodinates in canvas form point3d in space
     * it as to be an camera3D assiciated to this canvas
     */
    public Point2d get3DTo2DPoint(Point3d point3d) {

        Transform3D temp = new Transform3D();
        this.getVworldToImagePlate(temp);
        temp.transform(point3d);
        //NeptusLog.pub().info("<###> "+point3d.z);

        Point2d point2d = new Point2d();
        if (point3d.z > 0.0) {
            return point2d;
        }
        this.getPixelLocationFromImagePlate(point3d, point2d);

        return point2d;
    }

    public Point2d get3DTo2DPoint(Point3d point3d, double dist) {

        Transform3D temp = new Transform3D();
        this.getVworldToImagePlate(temp);
        temp.transform(point3d);
        //NeptusLog.pub().info("<###> "+point3d.z);

        Point2d point2d = new Point2d();
        if (camera.projection == View.PARALLEL_PROJECTION) {
            dist = 0.0;
        }
        if (point3d.z > -dist) {
            return point2d;
        }
        this.getPixelLocationFromImagePlate(point3d, point2d);

        return point2d;
    }

    /**
     * Constructor
     *
     * @param gcfg screen configuration
     * @param cam  Camera3D to be associated
     */
    public MyCanvas3D(GraphicsConfiguration gcfg, Camera3D cam) {
        super(gcfg);
        camera = cam;
        loadIcons();
    }


    public void loadIcons() {
        trans = ImageUtils.getImage("images/transponder.png");
        mark = ImageUtils.getImage("images/mark.png");
        home = ImageUtils.getImage("images/home.png");
    }
	
	/*@Override
	public void preRender() {
		
		super.preRender();
		
		if (g2 == null) {
			g2 = this.getGraphics2D();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	
		}
		
		this.getSize(d);
		
		if(gradback)
			drawSky(d.width, d.height, g2);
		
		g2.flush(true);
	}
	*/

    /**
     * rewrited method from Canvas3D (of Java3D)
     */
    public void postRender() {
        //	super.postRender();

        g2 = this.getGraphics2D();
			/*g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			 RenderingHints.VALUE_ANTIALIAS_ON);*/
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			/*float strokeThickness = 3.0f;
			 BasicStroke stroke = new BasicStroke(strokeThickness);
			 g2.setStroke(stroke);*/


        this.getSize(d);

        //    if(selected) selected(d.width, d.height, g2);  //ver esta parte
        if (vehicleicons) {
            vehicleIcons(d.width, d.height, g2);
        }
        if (objsicons) {
            objsIcons(d.width, d.height, g2);
        }
        if (caminfo) {
            camInfo(d.width, d.height, g2);
        }
        if (reguainfo) {
            reguaInfo(d.width, d.height, g2);
        }
        if (axisinfo) {
            axisInfo(d.width, d.height, g2);
        }
        if (objinfo) {
            objInfo(d.width, d.height, g2);
        }
        if (veicleinfo) {
            veicleInfo(d.width, d.height, g2);
        }
        if (camera.associatedrender.gtext) {
            gridInfo(d.width, d.height, g2);
        }

        //drawDemo(d.width, d.height, g2);

        defaultDisplay(d.width, d.height, g2);

        if (isVisible()) {
            try {
                g2.flush(true);
            }
            catch (IllegalStateException e) {
                //e.printStackTrace();
                NeptusLog.waste().warn("MyCanvas3D error", e);
            }
        }
    }


    public void defaultDisplay(int w, int h, J3DGraphics2D g2) {
        g2.setColor(Color.BLACK);
        Point2d orig = get3DTo2DPoint(new Point3d(0.0, 0.0, 0.0));
        //g2.drawString("O",(int)orig.x,(int)orig.y);
        //camera.associatedrender.objects
        for (Enumeration<AbstractElement> enuma = camera.associatedrender.objects.keys(); enuma.hasMoreElements(); ) {
            AbstractElement objs = enuma.nextElement();
            Obj3D obj2 = camera.associatedrender.objects.get(objs);

            if (obj2.drawlabel) {
                orig = get3DTo2DPoint(new Point3d(obj2.pos));
                if (orig.y != 0 && orig.x != 0) {
                    Rectangle2D stringBounds = g2.getFontMetrics()
                            .getStringBounds(objs.getId(), g2);
                    window((int) orig.x - 2, (int) orig.y - 12, (int) orig.x
                            + (int) stringBounds.getWidth(), (int) orig.y + 2);
                    g2.drawString(objs.getId(), (int) orig.x, (int) orig.y);
                }
            }
            if (obj2.drawinfo) {
                orig = get3DTo2DPoint(new Point3d(obj2.pos));
                if (orig.y != 0 && orig.x != 0) {
                    if (objs instanceof DynamicElement) {
                        DynamicElement de = (DynamicElement) objs;
                        Rectangle2D stringBounds = g2.getFontMetrics()
                                .getStringBounds(objs.getCenterLocation().toString(), g2);
                        Rectangle2D stringBounds2 = g2.getFontMetrics()
                                .getStringBounds(objs.getType(), g2);
                        Rectangle2D stringBounds3 = g2.getFontMetrics()
                                .getStringBounds("Idle Time: " + de.getIdleTimeSecs(), g2);
                        int max = (int) stringBounds.getWidth();
                        if (max < stringBounds2.getWidth()) {
                            max = (int) stringBounds2.getWidth();
                        }
                        if (max < stringBounds3.getWidth()) {
                            max = (int) stringBounds3.getWidth();
                        }
                        window((int) orig.x - 2, (int) orig.y + 2, (int) orig.x
                                + max, (int) orig.y + 38);
                        g2.drawString("Idle Time: " + de.getIdleTimeSecs(), (int) orig.x, (int) orig.y + 36);
                        g2.drawString(objs.getCenterLocation().toString(), (int) orig.x, (int) orig.y + 24);
                        g2.drawString(objs.getType(), (int) orig.x, (int) orig.y + 12);


                    }
                    else {
                        Rectangle2D stringBounds = g2.getFontMetrics()
                                .getStringBounds(objs.getCenterLocation().toString(), g2);
                        Rectangle2D stringBounds2 = g2.getFontMetrics()
                                .getStringBounds(objs.getType(), g2);
                        int max = (int) stringBounds.getWidth();
                        if (max < stringBounds2.getWidth()) {
                            max = (int) stringBounds2.getWidth();
                        }
                        window((int) orig.x - 2, (int) orig.y + 2, (int) orig.x
                                + max, (int) orig.y + 28);
                        g2.drawString(objs.getCenterLocation().toString(), (int) orig.x, (int) orig.y + 24);
                        g2.drawString(objs.getType(), (int) orig.x, (int) orig.y + 12);
                    }
                }
            }
        }

    }

    /**
     * draw objects info on their position
     *
     * @param w  width of bitmap
     * @param h  heigth of bitmap
     * @param g2 draw obj
     */
    public void objInfo(int w, int h, J3DGraphics2D g2) {
        g2.setColor(Color.BLACK);
        Point2d orig = get3DTo2DPoint(new Point3d(0.0, 0.0, 0.0));
        //g2.drawString("O",(int)orig.x,(int)orig.y);
        //camera.associatedrender.objects
        for (Enumeration<AbstractElement> enuma = camera.associatedrender.objects.keys(); enuma.hasMoreElements(); ) {
            AbstractElement objs = enuma.nextElement();
            Obj3D obj2 = camera.associatedrender.objects.get(objs);

            orig = get3DTo2DPoint(new Point3d(obj2.pos));
            if (orig.y != 0 && orig.x != 0) {
                Rectangle2D stringBounds = g2.getFontMetrics()
                        .getStringBounds(objs.getId(), g2);
                window((int) orig.x - 2, (int) orig.y - 12, (int) orig.x
                        + (int) stringBounds.getWidth(), (int) orig.y + 2);
                g2.drawString(objs.getId(), (int) orig.x, (int) orig.y);
            }
        }

    }

    public void gridInfo(int w, int h, J3DGraphics2D g2) {
        double scale = camera.associatedrender.gspacing;

        String units = " m";

        if (scale < 1) {
            units = "cm";
            scale *= 100;
        }
        else {
            if (scale > 1000) {
                units = "Km";
                scale /= 1000;
            }
        }
        int scaleint = (int) (scale * 100);
        scale = scaleint;
        scale /= 100;
        String text = scale + units;
        window(w - 70, h - 35, w - 9, h - 10);

        g2.drawString(text, w - 60, h - 12);
        g2.drawString("Grid Unit", w - 60, h - 24);
    }

    public void veicleInfo(int w, int h, J3DGraphics2D g2) {
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g2.setColor(Color.BLACK);
        Point2d orig = get3DTo2DPoint(new Point3d(0.0, 0.0, 0.0));
        //g2.drawString("O",(int)orig.x,(int)orig.y);
        //camera.associatedrender.objects
        for (Enumeration<VehicleType> enuma = camera.associatedrender.vehicles.keys(); enuma
                .hasMoreElements(); ) {
            VehicleType vt = enuma.nextElement();
            Obj3D obj2 = camera.associatedrender.vehicles.get(vt);

            orig = get3DTo2DPoint(new Point3d(obj2.pos));
            if (orig.y != 0 && orig.x != 0) {
                Rectangle2D stringBounds = g2.getFontMetrics()
                        .getStringBounds(vt.getName(), g2);
                window((int) orig.x - 2, (int) orig.y - 12, (int) orig.x
                        + (int) stringBounds.getWidth(), (int) orig.y + 2);
                g2.drawString(vt.getName(), (int) orig.x, (int) orig.y);
            }
        }
    }


    public void vehicleIcons(int w, int h, J3DGraphics2D g2) {
        g2.setColor(Color.BLACK);


        //g2.get
        //RenderedImage
        Point2d orig = get3DTo2DPoint(new Point3d(0.0, 0.0, 0.0));
        //g2.drawString("O",(int)orig.x,(int)orig.y);
        //camera.associatedrender.objects
        for (Enumeration<VehicleType> enuma = camera.associatedrender.vehicles.keys(); enuma
                .hasMoreElements(); ) {
            VehicleType vt = enuma.nextElement();
            Obj3D obj2 = camera.associatedrender.vehicles.get(vt);
            Cam c = new Cam();
            if (camera.lockobj == null) {
                c.camdef(20, (camera.psi - obj2.yaw) + Math.PI / 2,
                        (camera.theta + obj2.pitch),
                        -(camera.phi - Math.PI));
            }
            else {
                c.camdef(20,
                        ((camera.psi + camera.lockobj.yaw) - obj2.yaw) + Math.PI / 2,
                        ((camera.theta/*-camera.lockobj.pitch*/) + obj2.pitch),
                        -((camera.phi/*+camera.lockobj.roll*/) - Math.PI));
            }


            Vector3f vec3a = new Vector3f(8, 0, 0);
            Vector3f vec3b = new Vector3f(-1, 0, 0);
            Vector3f vec3c = new Vector3f(-3, 3.5f, 0); // para a cetinha
            Vector3f vec3d = new Vector3f(-3, -3.5f, 0);

            Vector2f vec2a = c.to2d(vec3a);
            Vector2f vec2b = c.to2d(vec3b);
            Vector2f vec2c = c.to2d(vec3c);
            Vector2f vec2d = c.to2d(vec3d);

            //vec2a.x=(float) ((vec2a.x*Math.cos(camera.phi))-(vec2a.y*Math.sin(camera.phi)));
            //vec2a.y=(float) ((vec2a.x*Math.sin(camera.phi))-(vec2a.y*Math.cos(camera.phi)));

            int x1 = (int) (3 * vec2a.x) - 15;
            int y1 = (int) (3 * vec2a.y) - 15;
            int x2 = (int) (3 * vec2b.x) - 15;
            int y2 = (int) (3 * vec2b.y) - 15;
            int x3 = (int) (3 * vec2c.x) - 15;
            int y3 = (int) (3 * vec2c.y) - 15;
            int x4 = (int) (3 * vec2d.x) - 15;
            int y4 = (int) (3 * vec2d.y) - 15;

            orig = get3DTo2DPoint(new Point3d(obj2.pos), 4);
            x1 = (int) (x1 + orig.x); // translate
            y1 = (int) (y1 + orig.y);
            x2 = (int) (x2 + orig.x);
            y2 = (int) (y2 + orig.y);
            x3 = (int) (x3 + orig.x); // translate
            y3 = (int) (y3 + orig.y);
            x4 = (int) (x4 + orig.x);
            y4 = (int) (y4 + orig.y);

            int[] px = {x1, x3, x2, x4};
            int[] py = {y1, y3, y2, y4};

            if (orig.y != 0 && orig.x != 0) {
                g2.setColor(vt.getIconColor());
                g2.fillPolygon(px, py, 4);
                //g2.drawLine(x1, y1, x2, y2); // desenhar
                g2.setColor(Color.BLACK);
            }
        }
    }

    public void drawSky(int w, int h, J3DGraphics2D g2) {
        //g2.scale(4,4);
        //g2.drawImage(sky,0,-128,null);

        //g2.scale(0.25,0.25);

        //(camera.psi + camera.lockobj.yaw) + Math.PI / 2,
        //(camera.theta/*-camera.lockobj.pitch*/),
        //-((camera.phi/*+camera.lockobj.roll*/) - Math.PI));

        //vetor2d camdir=

        double horang;
        double vertang;
        if (camera.lockobj == null) {
            horang = camera.psi;
            vertang = camera.theta;
        }
        else {
            horang = camera.psi + camera.lockobj.yaw;
            vertang = camera.theta;
        }


        Vector2d camdir = new Vector2d();
        camdir.y = Math.cos(horang);
        camdir.x = -Math.sin(horang);

        if (Math.sin(vertang) < 0) {
            camdir.y = -camdir.y;
            camdir.x = -camdir.x;
        }


        Rectangle2D e = new Rectangle2D.Float(0, 0, w, h);


        Point2d light = get3DTo2DPoint(new Point3d(camdir.x * 100000, camdir.y * 100000, 0), -20);
        Point2d dark = get3DTo2DPoint(new Point3d(camdir.x * 100000, camdir.y * 100000, 10), 0);

        Point2d horline = get3DTo2DPoint(new Point3d(camdir.x * 100000, camdir.y * 100000, 0), 0);
        Vector2d v = new Vector2d();
        v.x = (light.x - dark.x);
        v.y = (light.y - dark.y);
        v.normalize();


        Point2d up = new Point2d();
        up.x = v.x * 10;
        up.y = v.y * 10;

        //Color3f bgColor = new Color3f(0.007843137254901961f,
        //			0.4431372549019608f, 0.6705882352941176f);
        GradientPaint gp = new GradientPaint((float) horline.x, (float) horline.y, new Color(0.007843137254901961f,
                0.4431372549019608f, 0.6705882352941176f),
                (float) (horline.x + up.x * 10), (float) (horline.y + up.y * 10), new Color(0.0431372549019608f,
                0.5431372549019608f, 0.8705882352941176f), false);
        g2.setPaint(gp);
        g2.fill(e);

        //g2.setColor(Color.RED);

        //g2.drawLine((int)horline.x,(int)horline.y,(int)(horline.x+up.x*10),(int)(horline.y+up.y*10));


        //g2.setColor(Color.RED);


        //g2.drawString("hor:"+Math.toDegrees(horang)+" vert:"+Math.toDegrees(vertang), 0, 400);
        //g2.drawString("x:"+camdir.x+" y:"+camdir.y, 0, 420);
    }

    public void objsIcons(int w, int h, J3DGraphics2D g2) {
        g2.setColor(Color.BLACK);
        Stroke strokebase = g2.getStroke();
        Point2d orig = get3DTo2DPoint(new Point3d(0.0, 0.0, 0.0));
        //g2.drawString("O",(int)orig.x,(int)orig.y);
        //camera.associatedrender.objects
        for (Enumeration<AbstractElement> enuma = camera.associatedrender.objects.keys(); enuma.hasMoreElements(); ) {
            AbstractElement objs = enuma.nextElement();
            Obj3D obj2 = camera.associatedrender.objects.get(objs);

            orig = get3DTo2DPoint(new Point3d(obj2.pos), 3);
            if (orig.y != 0 && orig.x != 0) {
                if (objs instanceof TransponderElement) {
                    g2.drawImage(trans, (int) orig.x - (trans.getWidth(null) / 2), (int) orig.y - (trans.getWidth(null) / 2), null);
                }

                if (objs instanceof MarkElement) {
                    g2.drawImage(mark, (int) orig.x - (mark.getWidth(null) / 2), (int) orig.y - (mark.getWidth(null) / 2), null);
                }

                if (objs instanceof HomeReferenceElement) {
                    g2.drawImage(home, (int) orig.x - (home.getWidth(null) / 2), (int) orig.y - (home.getWidth(null) / 2), null);
                }

            }
			/*if(objs instanceof ScatterPointsElement)
			{
				ScatterPointsElement obj = (ScatterPointsElement) objs;

				int curPoint = 0;
				int size = obj.getPoints().size();

				Point3d offset = new Point3d();
				offset.x = obj.getCenterLocation().getOffsetFrom(
						camera.associatedrender.location)[0];
				offset.y = obj.getCenterLocation().getOffsetFrom(
						camera.associatedrender.location)[1];
				offset.z = obj.getCenterLocation().getOffsetFrom(
						camera.associatedrender.location)[2];

				if (size > 0) {
					Point3d a = obj.getPoints().firstElement();
					a.x += offset.x;
					a.y += offset.y;
					a.z += offset.z;
					Point3d b = new Point3d();

					Point2d aux2d1;
					Point2d aux2d2;
					// System.err.println(" desenhando o caminho");
					Stroke stroke = g2.getStroke();
					g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND,
							BasicStroke.JOIN_ROUND));
					for (Point3d j : obj.getPoints()) {
						g2.setColor(cmap.getColor((double) curPoint++
								/ (double) size));

						// System.err.println("j.x:"+j.x+" j.y:"+j.y+"
						// j.z:"+j.z);

						b.x = j.x + offset.x;
						b.y = j.y + offset.y;
						b.z = j.z + offset.z;

						aux2d1 = get3DTo2DPoint(new Point3d(a), 1);
						aux2d2 = get3DTo2DPoint(new Point3d(b), 1);

						if ((aux2d1.y != 0 && aux2d1.x != 0)
								&& (aux2d2.y != 0 && aux2d2.x != 0)) {

							g2.drawLine((int) aux2d1.x, (int) aux2d1.y,
									(int) aux2d1.x, (int) aux2d1.y);
							// g2.drawLine((int)aux2d1.x,(int)aux2d1.y,(int)aux2d2.x,(int)aux2d2.y);
						}

						a = new Point3d(b);

					}
					
					g2.setStroke(stroke);
				}
			}*/

        }
        g2.setStroke(strokebase);

    }


    /***
     *
     * draw information about the distance between p1 and p2
     */
    public void reguaInfo(int w, int h, J3DGraphics2D g2) {
        g2.setColor(Color.BLACK);

        g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);

        Point3d point3d1 = new Point3d();
        Point3d point3d2 = new Point3d();
        camera.canvas.getPixelLocationInImagePlate(p1, point3d1);
        Transform3D temp = new Transform3D();
        camera.canvas.getImagePlateToVworld(temp);
        temp.transform(point3d1);
        camera.canvas.getPixelLocationInImagePlate(p2, point3d2);
        temp.transform(point3d2);
        double scale = point3d2.distance(point3d1);

        String units = " m";

        if (scale < 1) {
            units = "cm";
            scale *= 100;
        }
        else {
            if (scale > 1000) {
                units = "Km";
                scale /= 1000;
            }
        }
        int scaleint = (int) (scale * 100);
        scale = scaleint;
        scale /= 100;
        String text = scale + units;
//		Point2d paux=new Point2d(p1.x-p2.x,p1.y-p2.y);
        double angle = AngleUtils.calcAngle(p1.x, p1.y, p2.x, p2.y);
        //double ang=Math.atan(m);
        angle = Math.toDegrees(-angle + Math.PI);

        while (angle < 0) {
            angle += 360;
        }

        while (angle > 360) {
            angle -= 360;
        }
        int angleint = (int) (angle * 100);
        angle = angleint;
        angle /= 100;
        text = text + " " + angle + "\u00B0"; //º

        if (camera.projection == View.PERSPECTIVE_PROJECTION) {
            text = "(Prespective view)";
        }

        Rectangle2D stringBounds = g2.getFontMetrics()
                .getStringBounds(text, g2);

        window((int) p2.x - 2 + 2, (int) p2.y - 14, (int) p2.x
                + (int) stringBounds.getWidth(), (int) p2.y);
        g2.drawString(text, (int) p2.x + 2, (int) p2.y - 2);

    }

    /**
     * draw axis information
     */
    public void axisInfo(int w, int h, J3DGraphics2D g2) {
        g2.setColor(Color.BLACK);
        Cam c = new Cam();
        if (camera.lockobj == null) {
            c.camdef(20, camera.psi + Math.PI / 2, camera.theta,
                    -(camera.phi - Math.PI));
        }
        else {
            c.camdef(20,
                    (camera.psi + camera.lockobj.yaw) + Math.PI / 2,
                    (camera.theta/*-camera.lockobj.pitch*/),
                    -((camera.phi/*+camera.lockobj.roll*/) - Math.PI));
        }
        //NeptusLog.pub().info("<###>psi="+camera.psi+" theta="+camera.theta+" phi="+camera.phi);//ok...
        int xr1 = w - 78, yr1 = 8, xr2 = w - 8, yr2 = 89;
        window(xr1, yr1, xr2, yr2);
        int xc = w - 58, yc = 51;
        g2.drawLine(xr1 + 10, yr1 + 10, xr2 - 10, yr1 + 10); // desenhar
        g2.drawLine(xr1 + 10, yr1 + 5, xr1 + 10, yr1 + 15); // desenhar
        g2.drawLine(xr2 - 10, yr1 + 5, xr2 - 10, yr1 + 15); // desenhar
        if (camera.projection == View.PERSPECTIVE_PROJECTION) {
            g2.drawLine(xr1 + 22, yr1 + 7, xr1 + 22, yr1 + 13); // desenhar
            g2.drawLine(xr2 - 22, yr1 + 7, xr2 - 22, yr1 + 13); // desenhar
            g2.drawLine(xr1 + 31, yr1 + 9, xr1 + 31, yr1 + 11); // desenhar
            g2.drawLine(xr2 - 31, yr1 + 9, xr2 - 31, yr1 + 11); // desenhar
            //g2.drawString("Presp",xr1+15,yr1+25);
        }
        else {
            int x1 = xr1 + 10, y1 = yr1 + 10;
            int x2 = xr2 - 10, y2 = yr1 + 10;
            Point3d point3d1 = new Point3d();
            camera.canvas.getPixelLocationInImagePlate(x1, y1, point3d1);
            Transform3D temp = new Transform3D();
            camera.canvas.getImagePlateToVworld(temp);
            temp.transform(point3d1);
            //NeptusLog.pub().info("<###>x1:"+point3d1.x+"y1:"+point3d1.y+"z1:"+point3d1.z);

            Point3d point3d2 = new Point3d();
            camera.canvas.getPixelLocationInImagePlate(x2, y2, point3d2);

            temp.transform(point3d2);
            // NeptusLog.pub().info("<###>x2:"+point3d2.x+"y2:"+point3d2.y+"z2:"+point3d2.z);

            double scale = point3d2.distance(point3d1);
            // NeptusLog.pub().info("<###>dist:"+scale);

            String units = " m";

            if (scale < 1) {
                units = "cm";
                scale *= 100;
            }
            else {
                if (scale > 1000) {
                    units = "Km";
                    scale /= 1000;
                }
            }
            int scaleint = (int) (scale * 100);
            scale = scaleint;
            scale /= 100;

            g2.drawString(scale + "", xr1 + 15, yr1 + 25);
            g2.drawString(units, xr1 + 45, yr1 + 25);
        }

        Vector2f vec2a = null;
        Vector2f vec2b = null;
        //-----------------zz
        Vector3f vec3a = new Vector3f(0, 0, -8);
        Vector3f vec3b = new Vector3f(0, 0, 0);
        vec2a = c.to2d(vec3a);
        vec2b = c.to2d(vec3b);

        //vec2a.x=(float) ((vec2a.x*Math.cos(camera.phi))-(vec2a.y*Math.sin(camera.phi)));
        //vec2a.y=(float) ((vec2a.x*Math.sin(camera.phi))-(vec2a.y*Math.cos(camera.phi)));

        int x1 = (int) (3 * vec2a.x);
        int y1 = (int) (3 * vec2a.y);
        int x2 = (int) (3 * vec2b.x);
        int y2 = (int) (3 * vec2b.y);

        x1 += xc; // translate
        y1 += yc;
        x2 += xc;
        y2 += yc;

        g2.drawLine(x1, y1, x2, y2); // desenhar
        g2.drawString("D", x1 - 5, y1);

        //---------------------------------yy
        vec3a = new Vector3f(0, 8, 0);
        vec3b = new Vector3f(0, 0, 0);
        vec2a = c.to2d(vec3a);
        vec2b = c.to2d(vec3b);
        x1 = (int) (3 * vec2a.x);
        y1 = (int) (3 * vec2a.y);
        x2 = (int) (3 * vec2b.x);
        y2 = (int) (3 * vec2b.y);

        x1 += xc; // translate
        y1 += yc;
        x2 += xc;
        y2 += yc;

        g2.drawLine(x1, y1, x2, y2); //cima
        g2.drawString("E", x1 - 5, y1);
        //------------------------xx
        vec3a = new Vector3f(8, 0, 0);
        vec3b = new Vector3f(-1, 0, 0);
        Vector3f vec3c = new Vector3f(-3, 3.5f, 0); // para a cetinha
        Vector3f vec3d = new Vector3f(-3, -3.5f, 0);

        vec2a = c.to2d(vec3a);
        vec2b = c.to2d(vec3b);
        Vector2f vec2c = c.to2d(vec3c);
        Vector2f vec2d = c.to2d(vec3d);

        x1 = (int) (3 * vec2a.x);
        y1 = (int) (3 * vec2a.y);
        x2 = (int) (3 * vec2b.x);
        y2 = (int) (3 * vec2b.y);

        int x3 = (int) (3 * vec2c.x);
        int y3 = (int) (3 * vec2c.y);
        int x4 = (int) (3 * vec2d.x);
        int y4 = (int) (3 * vec2d.y);


        x1 += xc; // translate
        y1 += yc;
        x2 += xc;
        y2 += yc;

        x3 += xc; // translate
        y3 += yc;
        x4 += xc;
        y4 += yc;

        int[] px = {x1, x3, x2, x4};
        int[] py = {y1, y3, y2, y4};

        g2.fillPolygon(px, py, 4);
        g2.drawLine(x1, y1, x2, y2); //cima
        g2.drawString("N", x1 - 5, y1);
        //g2.drawLine(0,40,40,40);  //cima

    }

    /**
     * draw associated camera information
     */
    public void camInfo(int w, int h, J3DGraphics2D g2) {

        String text = "";
        LocationType curLocation = null;
        if (camera.lockobj == null) {
            double[] location = {0, 0, 0};
            Point3d vec = new Point3d(location[0], location[1], location[2]);
            vec.x += camera.pivot.x;
            vec.y += camera.pivot.y;
            vec.z += camera.pivot.z;
            text = "Target:(" + (double) ((int) (vec.x * 100)) / 100 + ","
                    + (double) ((int) (vec.y * 100)) / 100 + ","
                    + (double) ((int) (vec.z * 100)) / 100 + ")";
            //text="Target:("+(double)((int)(camera.pivot.x*100))/100+","+(double)((int)(camera.pivot.y*100))/100+","+(double)((int)(camera.pivot.z*100))/100+")";
            curLocation = new LocationType(camera.associatedrender.location);
            curLocation.translatePosition(vec.x, vec.y, vec.z);
        }
        else if (camera.lock != null) {
            double[] location = {0, 0, 0};
            Point3d vec2 = new Point3d(location[0], location[1], location[2]);

            Vector3d vec = new Vector3d(camera.lockobj.pos[0],
                    camera.lockobj.pos[1], camera.lockobj.pos[2]);
            vec.x += camera.pivot.x;
            vec.y += camera.pivot.y;
            vec.z += camera.pivot.z;
            vec.x += vec2.x;
            vec.y += vec2.y;
            vec.z += vec2.z;
            text = "Target:(" + (double) ((int) (vec.x * 100)) / 100 + ","
                    + (double) ((int) (vec.y * 100)) / 100 + ","
                    + (double) ((int) (vec.z * 100)) / 100 + ")";
            curLocation = new LocationType(camera.associatedrender.location);
            curLocation.translatePosition(vec.x, vec.y, vec.z);
        }
        else if (camera.lockmapobj != null) {
            double[] location = {0, 0, 0};
            Point3d vec2 = new Point3d(location[0], location[1], location[2]);

            Vector3d vec = new Vector3d(camera.lockobj.pos[0],
                    camera.lockobj.pos[1], camera.lockobj.pos[2]);
            vec.x += camera.pivot.x;
            vec.y += camera.pivot.y;
            vec.z += camera.pivot.z;
            vec.x += vec2.x;
            vec.y += vec2.y;
            vec.z += vec2.z;
            text = "Target:(" + (double) ((int) (vec.x * 100)) / 100 + ","
                    + (double) ((int) (vec.y * 100)) / 100 + ","
                    + (double) ((int) (vec.z * 100)) / 100 + ")";
            curLocation = new LocationType(camera.associatedrender.location);
            curLocation.translatePosition(vec.x, vec.y, vec.z);
        }

        if (curLocation != null && !(curLocation.getLatitudeDegs() == 0 && curLocation.getLongitudeDegs() == 0)) {
            double[] latLonDepth = curLocation.getAbsoluteLatLonDepth();
            StringBuilder loc = new StringBuilder();
            loc.append("Target:(");
            loc.append(CoordinateUtil.latitudeAsString(latLonDepth[0], true));
            loc.append(" / ").append(CoordinateUtil.longitudeAsString(latLonDepth[1], true));
            loc.append(" / ").append(MathMiscUtils.round(latLonDepth[2], 1)).append("m)");
            text = loc.toString();
        }

        //FontRenderContext frc = g2.getFontRenderContext();
        // Font font = new Font("Lucida Sans", 120,  Font.BOLD | Font.ITALIC);
        //g2.getFont();
        //TextLayout tl = new TextLayout(text, font, frc);
        //tl.draw(g2, 5, 10);
        //g2.setFont( new Font("areal", 130,  Font.BOLD));
        Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(text, g2);
        if (stringBounds.getWidth() > 210) {
            window(8, 8, (int) stringBounds.getWidth() + 15, 37);
        }
        else {
            window(8, 8, 225, 37);
        }

        g2.drawString(text, 15, 20);
        if (camera.lockobj == null) {
            text = "Locked: off";
        }
        else if (camera.lock != null) {
            text = "Locked: " + camera.lock.getName();
        }
        else if (camera.lockmapobj != null) {
            text = "Locked: " + camera.lockmapobj.getId();
        }

        g2.drawString(text, 15, 33);
        text = "View type: " + camera.getStrType();
        g2.drawString(text, 118, 33);
        //alpha = 0.0f;
        //g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        //g2.drawRoundRect(1,1,130,50,5,5);
        //Shape asda=new Shape();

        //g2.setColor(Color.MAGENTA);

        //g2.drawLine(130,1,130,40);
    }


    public void selected(int w, int h, J3DGraphics2D g2) {
        //String text;
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(2, 2, w - 3, 2); //cima
        g2.drawLine(2, h - 3, w - 3, h - 3); //dir
        g2.drawLine(2, 2, 2, h - 3); //esq
        g2.drawLine(w - 3, 2, w - 3, h - 3); // baixo
        //this.doLayout();

    }

    /**
     * call the original postRender of Canvas3D
     */
    public void truePostRender() {
        super.postRender();
    }

    /**
     * draw a transparent rectangle over 3d scene
     *
     * @param xr1 x1 corner coordinate
     * @param yr1 x1 corner coordinate
     * @param xr2 x2 corner coordinate
     * @param yr2 y2 corner coordinate
     */
    private void window(int xr1, int yr1, int xr2, int yr2) {
        //---------------------------------caixa
        float alpha = 0.5f;
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                alpha));
        g2.setPaint(new Color(225, 225, 220));
        g2.fillRect(xr1, yr1, xr2 - xr1, yr2 - yr1);
        alpha = 1.0f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                alpha));
        g2.setColor(Color.BLACK);
        g2.drawLine(xr1, yr1, xr2, yr1);
        g2.drawLine(xr1, yr1, xr1, yr2);
        g2.drawLine(xr1, yr2, xr2, yr2);
        g2.drawLine(xr2, yr1, xr2, yr2);
        //----------------------------------fim caixa
    }

    public Camera3D getCamera() {
        return camera;
    }

    public void setCamera(Camera3D camera) {
        this.camera = camera;
    }

    /**
     * @author RJPG
     * <p>
     * this class simulate a camera looking to (0,0,0)
     * and its use to convert Vector3d -> Vector2d
     * based on spherical cooredinates and roll of viewpoint.
     * (this class doesnt use Java3D or any other 3D engine)
     */
    private class Cam {
        private double v11, v12, v13, v21, v22, v23, v32, v33, v43;

        protected double rho, theta, phi, psi;
        public float dist;

        public Cam() {
            rho = 40;
            theta = Math.PI / 4;
            phi = Math.PI / 4;

            //theta*=pidiv180;
            //phi*=pidiv180;

            coeff();
            dist = 1;
            //	target = new Vector3f(0, 0, 0);
        }

        public void camdef(double rhod, double thetad, double phid, double psid) {
            rho = rhod;
            theta = thetad;
            phi = phid;
            psi = psid;
            //theta*=pidiv180;
            //phi*=pidiv180;
            coeff();
        }

        private void coeff() {
            double costh, sinth, cosph, sinph;
            costh = Math.cos(theta);
            sinth = Math.sin(theta);
            cosph = Math.cos(phi);
            sinph = Math.sin(phi);

            v11 = -sinth;
            v12 = -cosph * costh;
            v13 = -sinph * costh;
            v21 = costh;
            v22 = -cosph * sinth;
            v23 = -sinph * sinth;
            v32 = sinph;
            v33 = -cosph;
            v43 = rho;
        }

        private Vector3f eyecoord(Vector3f pw) {
            Vector3f pe = new Vector3f();
            pe.x = (float) (v11 * pw.x + v21 * pw.y);
            pe.y = (float) (v12 * pw.x + v22 * pw.y + v32 * pw.z);
            pe.z = (float) (v13 * pw.x + v23 * pw.y + v33 * pw.z + v43);

            return pe;
        }

        private Vector2f perspective(Vector3f p) {
            Vector3f pe = null;
            pe = eyecoord(p);
            Vector2f pxy = new Vector2f();
            Vector3f prot = new Vector3f();
            prot.x = (float) ((pe.x * Math.cos(psi)) - (pe.y * Math.sin(psi)));
            prot.y = (float) ((pe.x * Math.sin(psi)) + (pe.y * Math.cos(psi)));
            prot.z = pe.z;
            //prot.x=pe.x;
            //prot.y=pe.y;
            pxy.x = prot.x;///prot.z;
            pxy.y = prot.y;///prot.z;
            return pxy;
        }

        public Vector2f to2d(Vector3f p) {
            Vector2f aux = null;
            aux = perspective(p);
            Vector2f rt = new Vector2f();
            rt.x = (float) (dist * aux.x + 5.);
            rt.y = (float) (-dist * aux.y + 5.);
            return rt;
        }
    }


}
