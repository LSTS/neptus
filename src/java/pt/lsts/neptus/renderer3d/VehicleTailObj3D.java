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

import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.conf.GeneralPreferences;

public class VehicleTailObj3D {
	public VehicleType vehicle;
	
	private SystemPositionAndAttitude lastPoint=null;
	private int NPoints=0;
	private Vector <BranchGroup>shapesList=new Vector<BranchGroup>();
	private TransformGroup move =new TransformGroup(); // trasl
	protected double[] pos=new double[3];      //posição
	private BranchGroup fullobj=new BranchGroup();   //node de tudo
	//protected LocationType location=new LocationType();
	private LocationType lt;
	public double sideSize=0.5;
	
	public boolean activated=true;
	
	
	VehicleTailObj3D(LocationType loc)
	{
		fullobj.setCapability(BranchGroup.ALLOW_DETACH);
		fullobj.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		fullobj.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		fullobj.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		
		
		move.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    	move.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    	move.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
    	move.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		move.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		
    	fullobj.addChild(move);
		lt=loc;
		//location=loc;
	}
	
	public void addNewVehicleState(SystemPositionAndAttitude state)
	{
		NPoints++;
		//points.add(state);
		//VehicleState[] pointsArray=points.toArray(new VehicleState[0]);
		BranchGroup shapeSeg;
		//VehicleState stateaux=state.getPosition().getOffsetFrom(location);
				
		if(lastPoint!=null)
		{
			shapeSeg=createShapeSegment(lastPoint,state);
			shapeSeg.setCapability(BranchGroup.ALLOW_DETACH);
		
		
		
		
		shapesList.add(shapeSeg);
		move.addChild(shapeSeg);
		
		
		int numberOfPoints = GeneralPreferences.numberOfShownPoints;
		int diference=NPoints-numberOfPoints;
		//System.err.println("------------------------\nnumber of poins mmax:"+numberOfPoints);
		//System.err.println("number of poins current:"+NPoints);
		//System.err.println("diff:"+diference);
		
		if(diference>0)
		{	
			BranchGroup[] shapesListArray=shapesList.toArray(new BranchGroup[0]);
	    	
	    	for(int i=0;i<diference;i++)
	    	{
	    		
	    		move.removeChild(shapesListArray[i]);
	    		shapesList.remove(shapesListArray[i]);
	    		
	    	}
	    	NPoints-=diference;
		}
			
		}
		lastPoint=state;
	}
	
	private BranchGroup createShapeSegment(SystemPositionAndAttitude a,SystemPositionAndAttitude b)
	{
		//System.err.println("Criando segmento");
		if(a==null || b==null) return null;
		
		QuadArray gFront = new QuadArray (4,QuadArray.COORDINATES);
    	
		//Point3d center=new Point3d(this.getCenterLocation().getOffsetFrom(location));
		
		Point3d aPoint=new Point3d(a.getPosition().getOffsetFrom(lt));
		Point3d bPoint=new Point3d(b.getPosition().getOffsetFrom(lt));
		
		/*aPoint.x-=center.x;
		aPoint.y-=center.y;
		aPoint.z-=center.z;
		
		bPoint.x-=center.x;
		bPoint.y-=center.y;
		bPoint.z-=center.z;*/
		aPoint.x-=pos[0];
		aPoint.y-=pos[1];
		aPoint.z-=pos[2];
		
		bPoint.x-=pos[0];
		bPoint.y-=pos[1];
		bPoint.z-=pos[2];
		
		
		Point3d left=new Point3d(0,sideSize,0);    				
    	Point3d right=new Point3d(0,-sideSize,0);
		
    	Point3d bottomLeft=Util3D.setTransform(left, a.getRoll(), a.getPitch(), a.getYaw());
    	Point3d bottomRight=Util3D.setTransform(right, a.getRoll(), a.getPitch(), a.getYaw());
    	Point3d topLeft=Util3D.setTransform(left, b.getRoll(), b.getPitch(), b.getYaw());
    	Point3d topRight=Util3D.setTransform(right, b.getRoll(), b.getPitch(), b.getYaw());
    	
    	bottomLeft.x+=aPoint.x;
    	bottomLeft.y+=aPoint.y;
    	bottomLeft.z+=aPoint.z;
    	
    	bottomRight.x+=aPoint.x;
    	bottomRight.y+=aPoint.y;
    	bottomRight.z+=aPoint.z;
    	
    	topLeft.x+=bPoint.x;
    	topLeft.y+=bPoint.y;
    	topLeft.z+=bPoint.z;
    	
    	topRight.x+=bPoint.x;
    	topRight.y+=bPoint.y;
    	topRight.z+=bPoint.z;
    	
		gFront.setCoordinate (3, topLeft);
		gFront.setCoordinate (2, topRight);
		gFront.setCoordinate (1, bottomRight);
		gFront.setCoordinate (0, bottomLeft);
    			
		Appearance appGFront = new Appearance();
       	TransparencyAttributes trans=new TransparencyAttributes();
       	trans.setTransparency(0.5f);
       	trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
       	appGFront.setTransparencyAttributes(trans);

       	GeometryInfo gi=new GeometryInfo(gFront);
   		Material mat = new Material();
   		Color3f c=new Color3f();
   		c.set(vehicle.getIconColor());

   		mat.setDiffuseColor(c);
   		mat.setSpecularColor(c);
    	mat.setShininess(0.1f);
    	appGFront.setMaterial(mat);
    	
    	NormalGenerator ng = new NormalGenerator();
    	//gi.convertToIndexedTriangles();
    	ng.generateNormals(gi);
    	gi.recomputeIndices();
    	gi.unindexify();
    	gi.compact();

   		PolygonAttributes p = new PolygonAttributes (PolygonAttributes.POLYGON_FILL,PolygonAttributes.CULL_NONE, 0.0f);
        appGFront.setPolygonAttributes(p);
	   	
		GeometryArray geom=gi.getGeometryArray();
		geom.setCapability(GeometryArray.ALLOW_NORMAL_READ);			   	
	   	Shape3D shape= new Shape3D (geom, appGFront);
	   	
	   	BranchGroup ret=new BranchGroup();
	   	ret.setCapability(BranchGroup.ALLOW_DETACH);
	   	ret.addChild(shape);
	   	return ret;
	}
	
	public void clearTail()
	{
		shapesList.clear();
		move.removeAllChildren();
		lastPoint=null;
		NPoints=0;
	}
	
	

	public BranchGroup getFullObj3D() {
		return fullobj;
	}
	
	public void clean()
	{
		clearTail();
	}

	public VehicleType getVehicle() {
		return vehicle;
	}

	public void setVehicle(VehicleType vehicle) {
		this.vehicle = vehicle;
	}
	
	 public void setPos(double[] p){
	    	Transform3D m = new Transform3D();
	    	m.set(new Vector3d(p[0], p[1], p[2]));
	    	move.setTransform(m);
	    	pos[0]=p[0];//
	    	pos[1]=p[1];//
	    	pos[2]=p[2];//
	    }
	    
	 public void setPos(Point3d p){
	    	Transform3D m = new Transform3D();
	    	m.set(new Vector3d(p.x, p.y, p.z));
	    	move.setTransform(m);
	    	pos[0]=p.x;//
	    	pos[1]=p.y;//
	    	pos[2]=p.z;//
	    }
	    
	  public Point3d getPos(){
	    	Point3d ptn=new Point3d();
	    	ptn.x=pos[0];//
	    	ptn.y=pos[1];//
	    	ptn.z=pos[2];//
	    	return ptn;
	  }
	  
	 
		public LocationType getCenterLocation() {
			return lt;
		}

	
		public void setCenterLocation(LocationType l) {
			lt.setLocation(l);	
		}

	    
}
