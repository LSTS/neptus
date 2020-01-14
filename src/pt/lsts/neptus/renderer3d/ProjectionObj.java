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
 * 20??/??/??
 */
package pt.lsts.neptus.renderer3d;

import java.awt.image.BufferedImage;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point2f;
import javax.vecmath.Point3d;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.JVideoPanelConsole;
import pt.lsts.neptus.types.vehicle.VehicleType;

public class ProjectionObj {

	Point3d lastvpositio; 
	double lastroll; 
	double lastpitch;
	double lastyaw;
	// projection
	
	int ptop=0,pdown=0,pleft=0,pright=0;
	
	public double viewAngleVert = 36.52577988438826;

	public double viewAngleHor = 49.97812801910477;

	public int gridResolutionV = 20;

	public int gridResolutionH = 20; 

	public double xOffSet = 0, yOffSet = 0, zOffSet = 0;

	public boolean activated = false;

	public double pan = 0, tilt = 0;
	
	public double distanceWall =0.2;

	// Neptus
	public Renderer3D render;

	public JVideoPanelConsole videoSource = null;

	public VehicleType vehicle;

	// Java3D
	public BranchGroup bg = null;

	public TransformGroup tg = null;

	public TimeBehavior tb = null;

	public Appearance app = new Appearance();

	public Texture2D texture;

	public ImageComponent2D ic;
	public BufferedImage imGlobal;
	private Shape3D projShape = new Shape3D();
	private GeometryArray geometry=null; 

	QuadArray GFront = null;

	//private Box cube = new Box(1.0f, 1.0f, 1.0f, Box.GENERATE_NORMALS
	//		| Box.GENERATE_TEXTURE_COORDS, null);

	/*Point3d topLeft3d=new Point3d(1,-0.5,-0.5); 
	Point3d topRight3d=new Point3d(1,0.5,-0.5); 
	Point3d bottomLeft3d=new Point3d(1,-0.5,0.5);
	Point3d bottomRight3d=new Point3d(1,0.5,0.5);
	*/
	
	Vector3d[][] pointArray=null;//{topLeft3d,topRight3d,bottomLeft3d,bottomRight3d};
	
	Point3d[] Points=new Point3d[gridResolutionH*gridResolutionV*4];
	
	public ProjectionObj(Renderer3D R, VehicleType v) {
		render = R;
		vehicle = v;
		//rebuildPointArray();
		
		GFront = new QuadArray (gridResolutionH*gridResolutionV*4,QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 );
		
		/*	
		GFront.setCoordinate (3, bottomLeft3d);
		GFront.setCoordinate (2, bottomRight3d);
		GFront.setCoordinate (1, topRight3d);
		GFront.setCoordinate (0, topLeft3d);
		*/
		/*
		GFront.setTextureCoordinate (0,0, new TexCoord2f(0.0f,0.0f));
		GFront.setTextureCoordinate (0,1, new TexCoord2f(1.0f,0.0f)); 
		GFront.setTextureCoordinate (0,2, new TexCoord2f(1.0f,1.0f));
		GFront.setTextureCoordinate (0,3, new TexCoord2f(0.0f,1.0f));
		*/
		
		
		GeometryInfo gi=new GeometryInfo(GFront);
		geometry=gi.getGeometryArray();
		geometry.setCapability(GeometryArray.ALLOW_NORMAL_READ);
		geometry.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		geometry.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		geometry.setCapability(GeometryArray.ALLOW_TEXCOORD_WRITE);
		geometry.setCapability(GeometryArray.ALLOW_TEXCOORD_READ);
		geometry.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		geometry.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		
		//geometry.setCapability();
		//System.err.println("format:"+geometry.getVertexFormat()+"\n count:"+geometry.getVertexCount());
	   	
		createAppearance();
		//cube.setAppearance(app);
		projShape= new Shape3D (geometry, app);
		
	   	projShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
	   	projShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
	   	projShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
	   	projShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
	   	
	   	//geometry =projShape.getGeometry();
 	
		bg = new BranchGroup();
		tg = new TransformGroup();
		bg.addChild(tg);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.addChild(projShape);

		//tb = new TimeBehavior(40, this);
		//tb.setSchedulingBounds(getRender().getBoundsSphere());
		//bg.addChild(tb);
	}
	@SuppressWarnings("deprecation")
	public void reInit() {
		if(bg.getParent()!=null)
			getRender().contentsNoPickTransGr.removeChild(bg);

		
		GFront = new QuadArray (gridResolutionH*gridResolutionV*4,QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
			
		/*GFront.setCoordinate (3, bottomLeft3d);
		GFront.setCoordinate (2, bottomRight3d);
		GFront.setCoordinate (1, topRight3d);
		GFront.setCoordinate (0, topLeft3d);
		*/
		/*
		GFront.setTextureCoordinate (0,0, new TexCoord2f(0.0f,0.0f));
		GFront.setTextureCoordinate (0,1, new TexCoord2f(1.0f,0.0f)); 
		GFront.setTextureCoordinate (0,2, new TexCoord2f(1.0f,1.0f));
		GFront.setTextureCoordinate (0,3, new TexCoord2f(0.0f,1.0f));
		*/
		//GFront.setCoordinates(0,pointArray);
		Point2f[] tpoint=new Point2f[gridResolutionH*gridResolutionV*4];
		for(int i=0;i<gridResolutionH*gridResolutionV*4;i++)
			tpoint[i]=new Point2f();
		GFront.setTextureCoordinates(0, tpoint);
		GeometryInfo gi=new GeometryInfo(GFront);
		geometry=gi.getGeometryArray();
		geometry.setCapability(GeometryArray.ALLOW_NORMAL_READ);
		geometry.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		geometry.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		geometry.setCapability(GeometryArray.ALLOW_TEXCOORD_WRITE);
		//System.err.println("format:"+geometry.getVertexFormat()+"\n count:"+geometry.getVertexCount());
	   	
		createAppearance();
		//cube.setAppearance(app);
		projShape= new Shape3D (geometry, app);
		
	   	projShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
	   	projShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
	   	projShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
	   	projShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
	   	
	   	//geometry =projShape.getGeometry();
	  
	   	
		bg = new BranchGroup();
		tg = new TransformGroup();
		bg.addChild(tg);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.addChild(projShape);

		//tb = new TimeBehavior(40, this);
		//tb.setSchedulingBounds(getRender().getBoundsSphere());
		//bg.addChild(tb);
	}

	private void setPointArray(int i,int j,Vector3d p)
	{
		//NeptusLog.pub().info("<###>Length:"+pointArray.length+" index:"+i*(gridResolutionH+1)+j);
		//NeptusLog.pub().info("<###>i:"+i+" j:"+j);
		//NeptusLog.pub().info("<###>gridH:"+gridResolutionH);
		
		//pointArray[i*(gridResolutionH+1)+j]=p;
		pointArray[i][j]=p;
	}
	
//	private Vector3d getPointArray(int i,int j)
//	{
//		return pointArray[i][j];
//		//return pointArray[i*(gridResolutionH+1)+j];
//	}
	
	public void rebuildPointArray()
	{
		/*geometry.setTextureCoordinate (0,0, new TexCoord2f(0.0f,0.0f));
		geometry.setTextureCoordinate (0,1, new TexCoord2f(1.0f,0.0f)); 
		geometry.setTextureCoordinate (0,2, new TexCoord2f(1.0f,1.0f));
		geometry.setTextureCoordinate (0,3, new TexCoord2f(0.0f,1.0f));
		*/
		
		//NeptusLog.pub().info("<###>H:"+gridResolutionH+" V"+gridResolutionV);
		pointArray=new Vector3d[(gridResolutionH+1)][(gridResolutionV+1)];
		//pointArray[0]=topLeft3d;
		//pointArray[1]=topRight3d;
		//pointArray[2]=bottomLeft3d;
		//pointArray[3]=bottomRight3d;
		/*
		Point3d aux=new Point3d(1,0,0);
		
		double angAuxH=-Math.toRadians(getViewAngleHor())/2;
		//System.err.println("angAuxH:"+Math.toDegrees(angAuxH));
		double angAuxV=-Math.toRadians(getViewAngleHor())/2;
		//System.err.println("angAuxV:"+Math.toDegrees(angAuxV));
		
		double stepH=Math.toRadians(getViewAngleHor())/gridResolutionH;
		//System.err.println("stepH:"+Math.toDegrees(stepH));
		double stepV=Math.toRadians(getViewAngleVert())/gridResolutionV;
		//System.err.println("stepV:"+Math.toDegrees(stepV));
		*/
		//System.err.println("------------------------------------");
		
		double baseHor=Math.tan(Math.toRadians(viewAngleHor/2));
		double baseVert=Math.tan(Math.toRadians(viewAngleVert/2));
		
		double baseHorD=baseHor*2;
		double baseVertD=baseVert*2;
		
		//NeptusLog.pub().info("<###>baseHor:"+baseHor);
		//NeptusLog.pub().info("<###>baseVert:"+baseVert);
		
		for(int j=0;j<gridResolutionV+1;j++)
		{
			for(int i=0;i<gridResolutionH+1;i++)
			{
				//setPointArray(i,j,Util3D.setTransform(aux, 0, angAuxV, angAuxH));
				//System.err.println("angAuxH:"+Math.toDegrees(angAuxH)+"  angAuxV:"+Math.toDegrees(angAuxV));
				//System.err.println("pointo:"+Util3D.setTransform(aux, 0, angAuxV, angAuxH));
				setPointArray(i,j,new Vector3d(1.0,((baseHorD/(double)gridResolutionH)*(double)i)-baseHor,baseVert-((baseVertD/(double)gridResolutionV)*(double)j)));
				//System.err.println("angAuxH:"+Math.toDegrees(angAuxH)+"  angAuxV:"+Math.toDegrees(angAuxV));
				//System.err.println("--------------------------------------");
				//System.err.println("pointo("+i+","+j+"):"+getPointArray(i, j));
				//System.err.println("pointo:"+new Point3d(0.5,((1.0/(double)gridResolutionV)*(double)i)-0.5,0.5-((double)(1.0/(double)gridResolutionH)*(double)j)));
				//angAuxH+=stepH;
			}
			//angAuxV+=stepV;
			//angAuxH=-Math.toRadians(getViewAngleHor()/2);		
		}

		
		TexCoord2f[] tPoints=new TexCoord2f[gridResolutionH*gridResolutionV*4];
		int x=0,y=0;
		for(int i=0;i<tPoints.length;i+=4)
		{
			//(x, y)
			tPoints[i]=new TexCoord2f((float)x/(float)(gridResolutionH),1f-((float)y/(float)(gridResolutionV)));
			//System.err.println("Index"+i+"->("+x+","+y+")=("+(float)x/(float)(gridResolutionH)+","+(1f-((float)y/(float)(gridResolutionV)))+")");
			//(x+1,y)
			tPoints[i+1]=new TexCoord2f((float)(x+1)/(float)(gridResolutionH),1f-((float)y/(float)(gridResolutionV)));
			//System.err.println("Index"+(i+1)+"->("+(x+1)+","+y+")=("+(float)(x+1)/(float)(gridResolutionH)+","+(1f-((float)y/(float)(gridResolutionV)))+")");
			//(x+1,y+1)
			tPoints[i+2]=new TexCoord2f((float)(x+1)/(float)(gridResolutionH),1f-((float)(y+1)/(float)(gridResolutionV)));
			//System.err.println("Index"+(i+2)+"->("+(x+1)+","+(y+1)+")=("+(float)(x+1)/(float)(gridResolutionH)+","+(1f-((float)(y+1)/(float)(gridResolutionV)))+")");
			//(x,y+1)
			tPoints[i+3]=new TexCoord2f((float)x/(float)(gridResolutionH),1f-((float)(y+1)/(float)(gridResolutionV)));
			//System.err.println("Index"+(i+3)+"->("+x+","+(y+1)+")=("+(float)x/(float)(gridResolutionH)+","+(1f-((float)(y+1)/(float)(gridResolutionV)))+")");
			
			
			x++;
			if(x>gridResolutionH-1)
			{
				x=0;
				y++;
			}
		}
		geometry.setValidVertexCount(tPoints.length);
		
		geometry.setTextureCoordinates (0,0, tPoints);
		
		Points=new Point3d[gridResolutionH*gridResolutionV*4];

		NeptusLog.pub().info("<###>chamado o rebuild");
		//geometry.setInitialCoordIndex(0);


		//	reInit();
	}
	
	public void printMatrix()
	{
		for(int i=0;i<gridResolutionH+1;i++)
			for(int j=0;j<gridResolutionV+1;j++)
				System.err.println("("+i+","+j+"):"+pointArray[i][j]);
	}
	
	private void createAppearance() {

		// see the texture from both sides
		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		app.setPolygonAttributes(pa);

		// Create a two dimensional texture with magnification filtering
		texture = new Texture2D(Texture2D.BASE_LEVEL, Texture.RGBA,
				Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE);
		texture.setCapability(Texture.ALLOW_IMAGE_WRITE); // texture can change
		texture.setCapability(Texture.ALLOW_IMAGE_READ);
		texture.setMagFilter(Texture2D.BASE_LEVEL_LINEAR);
		// this setting noticably reduces pixilation on the screen

		// set the texture from the retrieved movie frame
		BufferedImage im=new BufferedImage(Util3D.FORMAT_SIZE,
				Util3D.FORMAT_SIZE, BufferedImage.TYPE_INT_RGB);
		ic = new ImageComponent2D(ImageComponent2D.FORMAT_RGB,
						Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE, true, true);
		/*if (videoSource != null) {
			im = (BufferedImage) videoSource.grabFrameImage();
			if (im != null) {
				ic.set(im);
				texture.setImage(0, ic);
			}	
		}*/
		

		ic.setCapability(ImageComponent2D.ALLOW_IMAGE_WRITE);
		ic.set(im);
		texture.setImage(0, ic);

		PolygonAttributes p = new PolygonAttributes (PolygonAttributes.POLYGON_FILL,
    			PolygonAttributes.CULL_NONE, 0.0f);
    	app.setPolygonAttributes(p);
		app.setTexture(texture);
		TransparencyAttributes trans=new TransparencyAttributes();
   		trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        trans.setTransparency(0.8f);
   		trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
   		app.setTransparencyAttributes(trans);
		//setAppearance(app);
	} // end of createAppearance()

	//double i=0.5;
	public void refreshVideoMap() {
		//NeptusLog.pub().info("<###>refresh video");
		//geometry.setCoordinate(0, new Point3d(1,0.5,i+=0.2));		  
		if (videoSource != null) {
			imGlobal = (BufferedImage) videoSource.grabFrameImage(ptop,pdown,pleft,pright);
			if (imGlobal != null) {
				ic.set(imGlobal);
				texture.setImage(0, ic);
			} else
				NeptusLog.pub().info("<###>Null BufferedImage");
		}
	}

	public static void main(String args[]) {
		//Vector3d v = new Vector3d(10, 0, 0);
		//NeptusLog.pub().info("<###>orig:" + v);
		//NeptusLog.pub().info("<###>rodado:"
		//		+ Util3D.setTransform(v, 0,  Math.PI/2/*+Math.PI / 2*/, 0));
		
		NeptusLog.pub().info("<###>Angulo vertical:"+Math.toDegrees(Math.atan(0.33))*2);
		NeptusLog.pub().info("<###>Angulo vertical:"+Math.toDegrees(Math.atan(0.445))*2);
		//ConfigFetch.initialize();
		//ProjectionObj pj=new ProjectionObj(new Renderer3D(),VehiclesHolder.getVehicleById("isurus"));
		//pj.setGridResolutionH(1);
		//pj.setGridResolutionV(1);
		//pj.rebuildPointArray();
		//pj.rebuildPointArray();
		//pj.printMatrix();
	}

	//Point3d[] Points=null;
	public void refreshObj(Point3d vpositio, double roll, double pitch,
			double yaw) {
		
		lastvpositio=vpositio; 
		lastroll=roll; 
		lastpitch=pitch;
		lastyaw=yaw;
		
		
		if (activated) {
			refreshVideoMap();
			int x=0,y=0;
			Point3d point;
			Vector3d v=new Vector3d();
			double tiltCalc=pitch+Math.toRadians(tilt);
			double panCalc=yaw+Math.toRadians(pan);
			
			//-------------------------
			//----------------
			Transform3D ret = new Transform3D();// return transform
			Transform3D xrot = new Transform3D();
			Transform3D yrot = new Transform3D();
			Transform3D zrot = new Transform3D();
			
			xrot.rotX(roll);
			yrot.rotY(tiltCalc);
			zrot.rotZ(panCalc);
			
			
			ret.mul(zrot);
			ret.mul(xrot);
			ret.mul(yrot);
			//----------------
			//-------------------------
			for(int i=0;i<Points.length;i+=4)
			{
				//System.err.println("BASE:pointo("+x+","+y+"):"+getPointArray(x, y));
				//NeptusLog.pub().info("<###>ciclo");
				//----------------------------Top Left
				//v=Util3D
				//.setTransform(new Vector3d(getPointArray(x,y)), roll,tiltCalc,panCalc);
				ret.transform(pointArray[x][y],v);
				point = getRender().fireRay(
						vpositio,	v		);

				v.normalize();
				v.negate();
				v.x*=distanceWall;
				v.y*=distanceWall;
				v.z*=distanceWall;
				point.add(v);
				
				//Points[i]=getPointArray(x,y);
				Points[i]=point;
				//geometry.setCoordinate(i, point);
				
				//----------------------------Top Right
				//v=Util3D
				//.setTransform(new Vector3d(getPointArray(x+1,y)), roll,tiltCalc,panCalc);
				ret.transform(pointArray[x+1][y],v);
				point = getRender().fireRay(
						vpositio,	v		);

				v.normalize();
				v.negate();
				v.x*=distanceWall;
				v.y*=distanceWall;
				v.z*=distanceWall;
				point.add(v);
				
				//Points[i+1]=getPointArray(x+1,y);
				Points[i+1]=point;
				//geometry.setCoordinate(i+1, point);
				
				//----------------------------Bottom Right
				//v=Util3D
				//.setTransform(new Vector3d(getPointArray(x+1,y+1)), roll,tiltCalc,panCalc);
				ret.transform(pointArray[x+1][y+1],v);
				point = getRender().fireRay(
						vpositio,	v		);

				v.normalize();
				v.negate();
				v.x*=distanceWall;
				v.y*=distanceWall;
				v.z*=distanceWall;
				point.add(v);
				
				//Points[i+2]=getPointArray(x+1,y+1);
				Points[i+2]=point;
				//geometry.setCoordinate(i+2, point);
				
				//----------------------------Bottom Left
				//v=Util3D
				//.setTransform(new Vector3d(getPointArray(x,y+1)),roll,tiltCalc,panCalc);
				ret.transform(pointArray[x][y+1],v);
				point = getRender().fireRay(
						vpositio,	v		);

				v.normalize();
				v.negate();
				v.x*=distanceWall;
				v.y*=distanceWall;
				v.z*=distanceWall;
				point.add(v);
				
				//Points[i+3]=getPointArray(x,y+1);
				Points[i+3]=point;
				//geometry.setCoordinate(i+3, point);
				
				x++;
				if(x>gridResolutionH-1)
				{
					x=0;
					y++;
				}
				
				
			}
			/*GFront = new QuadArray (4,QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
			
			GFront.setCoordinates (3, bottomLeft3d);
			GFront.setCoordinate (2, bottomRight3d);
			GFront.setCoordinate (1, topRight3d);
			GFront.setCoordinate (0, topLeft3d);
			
			GFront.setTextureCoordinate (0,0, new TexCoord2f(0.0f,0.0f));
			GFront.setTextureCoordinate (0,1, new TexCoord2f(1.0f,0.0f)); 
			GFront.setTextureCoordinate (0,2, new TexCoord2f(1.0f,1.0f));
			GFront.setTextureCoordinate (0,3, new TexCoord2f(0.0f,1.0f));
			
			GeometryInfo gi=new GeometryInfo(GFront);*/
			//geometry.setValidVertexCount(Points.length);
			//geometry.setInitialCoordIndex(0);
			//geometry.setValidVertexCount(Points.length+4);
			
			geometry.setCoordinates(0, Points);
			//array.setCoordinates(0, data.coordinates);
			
			/*Transform3D t = new Transform3D();
			//Matrix3d a = new Matrix3d();
			//NeptusLog.pub().info("<###>Ponto:" + point);
			if (point == null) {
				tg.setTransform(t);
			} else {
				t.setTranslation(new Vector3d(point));
				tg.setTransform(t);
			}*/

			//NeptusLog.pub().info("<###>construir objecto de projecção (Activo)");
		} else {
			//NeptusLog.pub().info("<###>construção inactiva..");
		}
	}

	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
		if (this.activated)
			if(bg.getParent()==null)
				getRender().contentsNoPickTransGr.addChild(bg);
			else
			{
				getRender().contentsNoPickTransGr.removeChild(bg);
				getRender().contentsNoPickTransGr.addChild(bg);
			}
		else
			if(bg.getParent()!=null)
				getRender().contentsNoPickTransGr.removeChild(bg);
		
	}

	public double getPan() {
		return pan;
	}

	public void setPan(double pan) {
		this.pan = pan;
	}

	public Renderer3D getRender() {
		return render;
	}

	public void setRender(Renderer3D render) {
		this.render = render;
	}

	public double getTilt() {
		return tilt;
	}

	public void setTilt(double tilt) {
		this.tilt = tilt;
	}

	public JVideoPanelConsole getVideoSource() {
		return videoSource;
	}

	public void setVideoSource(JVideoPanelConsole videoSource) {
		this.videoSource = videoSource;
		//System.err.println("video escolhido:"+videoSource);
	}

	public double getXOffSet() {
		return xOffSet;
	}

	public void setXOffSet(double offSet) {
		xOffSet = offSet;
	}

	public double getYOffSet() {
		return yOffSet;
	}

	public void setYOffSet(double offSet) {
		yOffSet = offSet;
	}

	public double getZOffSet() {
		return zOffSet;
	}

	public void setZOffSet(double offSet) {
		zOffSet = offSet;
	}

	public double getViewAngleHor() {
		return viewAngleHor;
	}

	public void setViewAngleHor(double viewAngleHor) {
		this.viewAngleHor = viewAngleHor;
	}

	public double getViewAngleVert() {
		return viewAngleVert;
	}

	public void setViewAngleVert(double viewAngleVert) {
		this.viewAngleVert = viewAngleVert;
	}

	public int getGridResolutionH() {
		return gridResolutionH;
	}

	public void setGridResolutionH(int gridResolutionH) {
		this.gridResolutionH = gridResolutionH;
	}

	public int getGridResolutionV() {
		return gridResolutionV;
	}

	public void setGridResolutionV(int gridResolutionV) {
		this.gridResolutionV = gridResolutionV;
	}

	public VehicleType getVehicle() {
		return vehicle;
	}

	public void setVehicle(VehicleType vehicle) {
		this.vehicle = vehicle;
	}

	public double getDistanceWall() {
		return distanceWall;
	}

	public void setDistanceWall(double distanceWall) {
		this.distanceWall = distanceWall;
	}

	public void leaveProjection()
	{
		
		int RESOLUTION=10;
		//BranchGroup bgcopy=(BranchGroup) bg.cloneTree();
		Shape3D projShapeCopy;
		
		Texture2D textureCopy;
		Appearance appCopy = new Appearance();
		
		// see the texture from both sides
		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		appCopy.setPolygonAttributes(pa);

		// Create a two dimensional texture with magnification filtering
		textureCopy = new Texture2D(Texture2D.BASE_LEVEL, Texture.RGBA,
				Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE);
		textureCopy.setMagFilter(Texture2D.BASE_LEVEL_LINEAR);
		// this setting noticably reduces pixilation on the screen

		ImageComponent icaux = new ImageComponent2D(ImageComponent2D.FORMAT_RGB,
				imGlobal);

		textureCopy.setImage(0, icaux);

		PolygonAttributes p = new PolygonAttributes (PolygonAttributes.POLYGON_FILL,
    			PolygonAttributes.CULL_NONE, 0.0f);
    	appCopy.setPolygonAttributes(p);
		appCopy.setTexture(textureCopy);
		TransparencyAttributes trans=new TransparencyAttributes();
   		trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
        trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
        trans.setTransparency(0.8f);
   		trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
   		appCopy.setTransparencyAttributes(trans);
		//setAppearance(app);

		//------------------------------------------------
		QuadArray GFrontCopy = new QuadArray (RESOLUTION*RESOLUTION*4,QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 );

		Vector3d[][] pointArrayCopy=new Vector3d[(RESOLUTION+1)][(RESOLUTION+1)];

		double baseHor=Math.tan(Math.toRadians(viewAngleHor/2));
		double baseVert=Math.tan(Math.toRadians(viewAngleVert/2));
		
		double baseHorD=baseHor*2;
		double baseVertD=baseVert*2;
		
		for(int j=0;j<RESOLUTION+1;j++)
		{
			for(int i=0;i<RESOLUTION+1;i++)
			{
				pointArrayCopy[i][j]=new Vector3d(1.0,((baseHorD/(double)RESOLUTION)*(double)i)-baseHor,baseVert-((baseVertD/(double)RESOLUTION)*(double)j));
				//pointArrayCopy[i][j]=new Vector3d(1.56,((1.0/(double)RESOLUTION)*(double)i)-0.5,0.5-((double)(1.0/(double)RESOLUTION)*(double)j));
				//setPointArray(i,j,new Point3d(0.5,((1.0/(double)gridResolutionH)*(double)i)-0.5,0.5-((double)(1.0/(double)gridResolutionV)*(double)j)));
			}
		}
		
		Vector3d v=new Vector3d();
		Point3d point; 
		int x=0,y=0;
		
		
		
		
		double tiltCalc=lastpitch+Math.toRadians(tilt);
		double panCalc=lastyaw+Math.toRadians(pan);
		
		
		//----------------
		Transform3D ret = new Transform3D();// return transform
		Transform3D xrot = new Transform3D();
		Transform3D yrot = new Transform3D();
		Transform3D zrot = new Transform3D();
		
		
		
		xrot.rotX(lastroll);
		yrot.rotY(tiltCalc);
		zrot.rotZ(panCalc);
		
		
		ret.mul(zrot);
		ret.mul(xrot);
		ret.mul(yrot);
		
		
		
		//----------------
		
		for(int i=0;i<RESOLUTION*RESOLUTION*4;i+=4)
		{
			//System.err.println("BASE:pointo("+x+","+y+"):"+getPointArray(x, y));
			//NeptusLog.pub().info("<###>ciclo");
			//----------------------------Top Left
			//v=Util3D
			//.setTransform(new Vector3d(pointArrayCopy[x][y]), lastroll,tiltCalc,panCalc);
			ret.transform(pointArrayCopy[x][y],v);
			point = getRender().fireRay(
					lastvpositio,	v		);

			v.normalize();
			v.negate();
			v.x*=distanceWall;
			v.y*=distanceWall;
			v.z*=distanceWall;
			point.add(v);
			
			//Points[i]=getPointArray(x,y);
			//Points[i]=point;
			GFrontCopy.setCoordinate(i, point);
			
			//----------------------------Top Right
			//v=Util3D
			//.setTransform(new Vector3d(pointArrayCopy[x+1][y]), lastroll,lastpitch+Math.toRadians(tilt),
			//		lastyaw+Math.toRadians(pan));
			ret.transform(pointArrayCopy[x+1][y],v);
			point = getRender().fireRay(
					lastvpositio,	v		);

			v.normalize();
			v.negate();
			v.x*=distanceWall;
			v.y*=distanceWall;
			v.z*=distanceWall;
			point.add(v);
			
			//Points[i+1]=getPointArray(x+1,y);
			//Points[i+1]=point;
			GFrontCopy.setCoordinate(i+1, point);
			
			//----------------------------Bottom Right
			//v=Util3D
			//.setTransform(new Vector3d(pointArrayCopy[x+1][y+1]), lastroll,lastpitch+Math.toRadians(tilt),
			//		lastyaw+Math.toRadians(pan));
			ret.transform(pointArrayCopy[x+1][y+1],v);
			point = getRender().fireRay(
					lastvpositio,	v		);

			v.normalize();
			v.negate();
			v.x*=distanceWall;
			v.y*=distanceWall;
			v.z*=distanceWall;
			point.add(v);
			
			//Points[i+2]=getPointArray(x+1,y+1);
			//Points[i+2]=point;
			GFrontCopy.setCoordinate(i+2, point);
			
			//----------------------------Bottom Left
			//v=Util3D
			//.setTransform(new Vector3d(pointArrayCopy[x][y+1]), lastroll,lastpitch+Math.toRadians(tilt),
			//		lastyaw+Math.toRadians(pan));
			ret.transform(pointArrayCopy[x][y+1],v);
			point = getRender().fireRay(
					lastvpositio,	v		);

			v.normalize();
			v.negate();
			v.x*=distanceWall;
			v.y*=distanceWall;
			v.z*=distanceWall;
			point.add(v);
			
			//Points[i+3]=getPointArray(x,y+1);
			//Points[i+3]=point;
			GFrontCopy.setCoordinate(i+3, point);
			
			x++;
			if(x>RESOLUTION-1)
			{
				x=0;
				y++;
			}
			
			
		}
		
		
		//TexCoord2f[] tPoints=new TexCoord2f[gridResolutionH*gridResolutionV*4];
		x=0;y=0;
		for(int i=0;i<RESOLUTION*RESOLUTION*4;i+=4)
		{
			//(x, y)
			GFrontCopy.setTextureCoordinate (0,i, new TexCoord2f((float)x/(float)(RESOLUTION),((float)y/(float)(RESOLUTION))));
			//(x+1,y)
			GFrontCopy.setTextureCoordinate (0,i+1, new TexCoord2f((float)(x+1)/(float)(RESOLUTION),((float)y/(float)(RESOLUTION)))); 
			//(x+1,y+1)
			GFrontCopy.setTextureCoordinate (0,i+2, new TexCoord2f((float)(x+1)/(float)(RESOLUTION),((float)(y+1)/(float)(RESOLUTION))));
			//(x,y+1)
			GFrontCopy.setTextureCoordinate (0,i+3, new TexCoord2f((float)x/(float)(RESOLUTION),((float)(y+1)/(float)(RESOLUTION))));

			x++;
			if(x>RESOLUTION-1)
			{
				x=0;
				y++;
			}
		}
		
		projShapeCopy= new Shape3D (GFrontCopy, appCopy);
		BranchGroup bgaux=new BranchGroup();
		bgaux.addChild(projShapeCopy);
		getRender().contentsNoPickTransGr.addChild(bgaux);
		
	}

	public int getPdown() {
		return pdown;
	}

	public void setPdown(int pdown) {
		this.pdown = pdown;
	}

	public int getPleft() {
		return pleft;
	}

	public void setPleft(int pleft) {
		this.pleft = pleft;
	}

	public int getPright() {
		return pright;
	}

	public void setPright(int pright) {
		this.pright = pright;
	}

	public int getPtop() {
		return ptop;
	}

	public void setPtop(int ptop) {
		this.ptop = ptop;
	}
}
