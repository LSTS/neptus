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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JButton;
import javax.vecmath.Point3d;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.image.TextureLoader;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author RJPG
 * This classe create the object 3D given bitmap's 
 * of depth and texture  
 */
public class Texture3D {
	// 3D space sizes
	private double width=0.;   
	private double height=0.;

	public Image bitmap=null;
	public Image texture=null;
	
	public double texturescale=1.0;

	/**
	 * @param value the tiling scale of the texturing 
	 */
	public void setTextureScale(double value)
	{
		texturescale=1.0;
	}
	public int resolution=10;
	public void setResolution(int value)
	{
		resolution=value;
	}
	public double maxvalue=10;
	/**
	 * @param value
	 * valor de profundidade da cor 0x00000000
	 */
	public void setMaxValue(double value)
	{
		maxvalue=value;
	}
	public double minvalue=0;
	
	/**
	 * @param value
	 * valor de profundidade da cor 0x000000FF
	 */
	public void setMinValue(double value)
	{
		minvalue=value;
	}
	
	public boolean shade=true;
	public void setShade(boolean flag)
	{
		shade=flag;
	}
	public float transparency=0.0f;
	public void setTransparency(float flag)
	{
		transparency=flag;
	}
	
	public boolean twosided;
	public void setTwoSided(boolean flag)
	{
		twosided=flag;
	}
	
	public Texture3D()
	{
		
	}
	
	public Texture3D(Image tex)
	{
		setTexture(tex);
	}
	
	public Texture3D(Image tex,double sizeh,double sizew)
	{
		
		setTexture(tex);

		width=sizew;
		height=sizeh;
	}
	
	public Texture3D(Image tex,Image bit,double sizeh,double sizew)
	{
		setTexture(tex);
		bitmap=bit;
			
		width=sizew;
		height=sizeh;
	}
	
	public void setSize(double sizeh,double sizew)
	{
		width=sizew;
		height=sizeh;
	}
	
	
	private BufferedImage createBufferedImage(Image img)
	{	
  		BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null),BufferedImage.TYPE_INT_RGB);
  		Graphics2D g = bi.createGraphics();
  		g.drawImage(img, 0, 0, null);
        return bi;
	}
	
	
//	private static Image toImage(BufferedImage bufferedImage) 
//	{
//	       return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
//	}
	
	private QuadArray makeTerrain3D (Image img)
	{
		
		
		double diff=maxvalue-minvalue;
		
		double factor=diff/(double)((int)0x000000FF);
		
		QuadArray GFront = new QuadArray (4*resolution*resolution,QuadArray.COORDINATES| QuadArray.NORMALS | QuadArray.TEXTURE_COORDINATE_2 );
		
		
		BufferedImage buffer;
 		buffer=createBufferedImage(img);
    	//	  Get a pixel
	    //int rgb = 0xFF00FF05; // green
	    //rgb=  rgb&0x000000FF;
	    //rgb = buffer.getRGB(10, 10);
	    //System.err.print("cor: "+Integer.toHexString(rgb));
	    //ImagePanel J =new ImagePanel(toImage(buffer));
        //GuiUtils.testFrame(J,"to image,"); // 5
		
		double tamy=(double)buffer.getWidth();
		double tamx=(double)buffer.getHeight();
		double deltax=tamx/resolution;
		double deltay=tamy/resolution;
		double deltau=1.0f/resolution;
		
		Point3d topLeft3d=new Point3d(height/2,-width/2.,0);    				
    	Point3d bottomRight3d=new Point3d(-height/2,width/2,0);
    	Point3d bottomLeft3d=new Point3d(-height/2,-width/2,0);
    	
		//Point3d topLeft3d=new Point3d(topLeft.getAbsoluteNEDInMeters());    				
    	//Point3d bottomRight3d=new Point3d(bottomRight.getAbsoluteNEDInMeters());
    	//Point3d bottomLeft3d=new Point3d(bottomRight3d.x,topLeft3d.y,0);
		double deltaglobalx=(topLeft3d.x-bottomRight3d.x)/resolution;
		double deltaglobaly=(bottomRight3d.y-topLeft3d.y)/resolution;
		
		/*System.err.println("bl"+bottomLeft3d);
		System.err.println("tl->x:"+topLeft3d.x+"tl->y:"+topLeft3d.y);
		System.err.println("br->x:"+bottomRight3d.x+"br->y:"+bottomRight3d.y);
		System.err.println("deltag->x:"+deltaglobalx+"deltag->y:"+deltaglobaly);
		*/
		
		int quad=0;
		/*System.err.println("tamX:"+tamx);
		System.err.println("tamy:"+tamy);*/
		
		int auxx=0;
		int auxy=0;
		for (double x=0;x+(x/100.)<tamx;x+=deltax)
		{
			for(double y=0;y+(y/100.)<tamy;y+=deltay)
			{
				double x1=(bottomLeft3d.x+(deltaglobalx*(double)auxx));
				double y1=(bottomLeft3d.y+(deltaglobaly*(double)auxy));
				
				double x2=(bottomLeft3d.x+(deltaglobalx*(double)auxx)+deltaglobalx);
				double y2=(bottomLeft3d.y+(deltaglobaly*(double)auxy)+deltaglobaly);
				          
			/*	System.err.println("tamX:"+tamx);
				System.err.println("tamy:"+tamy);
				System.err.println("matriz->x:"+x+",y:"+y);
				System.err.println("---------------");
				System.err.println("X:"+(bottomLeft3d.x+(deltaglobalx*(double)auxx))+
				" Y:"+(bottomLeft3d.y+(deltaglobaly*auxy))+" Z:getimg("+y+","+((tamx)-x)+")");
				System.err.println("V:"+(deltau*(double)auxx)+" U:"+(deltau*auxy));
				System.err.println("quad++"+(quad));*/
				
				GFront.setCoordinate (quad,new Point3d(x1,
												y1,
												((double)((int)(buffer.getRGB((int)y,(int)((tamx-1)-x) )&0x000000FF))*factor) +minvalue));
				GFront.setTextureCoordinate (0,quad, new TexCoord2f((float)(deltau*auxy),(float)(deltau*(double)auxx)));
				quad++;
				
				
													
				/*System.err.println("X:"+(bottomLeft3d.x+(deltaglobalx*(double)auxx))+
				" Y:"+(bottomLeft3d.y+(deltaglobaly*auxy)+deltaglobaly)+" Z:getimg("+(y+deltay)+","+(tamx-x)+")");
				System.err.println("V:"+(deltau*(double)auxx)+" U:"+((deltau*auxy)+deltau));
				System.err.println("quad++"+(quad));*/
				
				if(y+deltay<tamy)
				GFront.setCoordinate (quad,new Point3d(x1,
									y2,
									((double)((int)buffer.getRGB((int)(y+(deltay)),(int)((tamx-1)-x))&0x000000FF)*factor) +minvalue ));
				else
				GFront.setCoordinate (quad,new Point3d(x1,
									y2,
									((double)((int)buffer.getRGB((int)(y+(deltay-1)),(int) ((tamx-1)-x))&0x000000FF)*factor) +minvalue ));
				
				GFront.setTextureCoordinate (0,quad,new TexCoord2f((float)((deltau*auxy)+deltau),(float)(deltau*(double)auxx)));
				quad++;
				
				/*System.err.println("X:"+(bottomLeft3d.x+(deltaglobalx*(double)auxx)+deltaglobalx)+
				" Y:"+(bottomLeft3d.y+(deltaglobaly*auxy)+deltaglobaly)+" Z:getimg("+(y+deltay)+","+(tamx-(x+deltax))+")");
				System.err.println("V:"+((deltau*(double)auxx)+deltau)+" U:"+((deltau*auxy)+deltau));
				System.err.println("quad++"+(quad));*/
				
				if(x+deltax<tamx && y+deltay<tamy)
				GFront.setCoordinate (quad,new Point3d(x2,
									y2,
									((double)((int)buffer.getRGB((int)(y+(deltay)),(int)((tamx-1)-(x+(deltax))) )&0x000000FF)*factor) +minvalue ));
				else if(x+deltax==tamx && y+deltay<tamy)
				GFront.setCoordinate (quad,new Point3d(x2,
									y2,
									((double)((int)buffer.getRGB((int)(y+(deltay)),(int)((tamx-1)-(x+(deltax-1))) )&0x000000FF)*factor) +minvalue ));
				else if(x+deltax<tamx && y+deltay==tamy)
				GFront.setCoordinate (quad,new Point3d(x2,
									y2,
									((double)((int)buffer.getRGB((int)(y+(deltay-1)),(int)((tamx-1)-(x+(deltax))) )&0x000000FF)*factor) +minvalue ));
				else //if(x+deltax==tamx && y+deltay==tamy)
				GFront.setCoordinate (quad,new Point3d(x2,
									y2,
									((double)((int)buffer.getRGB((int)(y+(deltay-1)),(int)((tamx-1)-(x+(deltax-1))) )&0x000000FF)*factor) +minvalue ));
				
				
				GFront.setTextureCoordinate (0,quad,new TexCoord2f((float)((deltau*auxy)+deltau),(float)((deltau*(double)auxx)+deltau)));
				
				quad++;
				
				/*System.err.println("X:"+(bottomLeft3d.x+(deltaglobalx*(double)auxx)+deltaglobalx)+
				" Y:"+(bottomLeft3d.y+(deltaglobaly*auxy))+" Z:getimg("+y+","+(tamx-(x+deltax))+")");
				System.err.println("V:"+((deltau*(double)auxx)+deltau)+" U:"+(deltau*auxy));
				System.err.println("quad++"+(quad));*/
				
				if(x+deltax<tamx)
				GFront.setCoordinate (quad,new Point3d(x2,
									y1,
									((double)((int)buffer.getRGB((int)y,(int) ((tamx-1)-(x+deltax)))&0x000000FF)*factor) +minvalue ));
				else
				GFront.setCoordinate (quad,new Point3d(x2,
									y1,
									((double)((int)buffer.getRGB((int)y,(int) ((tamx-1)-(x+deltax-1)))&0x000000FF)*factor) +minvalue ));
				GFront.setTextureCoordinate (0,quad,new TexCoord2f((float)(deltau*auxy),(float)((deltau*(double)auxx)+deltau)));
				quad++;
				
				/*System.err.println("---------------------------------------");*/
				auxy++;
			}
			auxx++;
			auxy=0;
		}

		GFront.setCapability(QuadArray.ALLOW_NORMAL_READ);
		GFront.setCapability(QuadArray.ALLOW_REF_DATA_READ);
		GFront.setCapability(QuadArray.ALLOW_FORMAT_READ);
		GFront.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		GFront.setCapability(QuadArray.ALLOW_NORMAL_WRITE);
		
    	return GFront;
	}
	public TransformGroup getModel3D()
	{
		
		
		//texture = GuiUtils.getImage("c:\\altura.png"); 
		// esperar pela textura 
		MediaTracker tracker = new MediaTracker(new JButton());
		tracker.addImage(texture, 0);
		try {
			tracker.waitForAll();
		}
		catch (Exception e) {
			System.err.println("erro ao esperar pela imagem!");
		}
		// esperar pela imagem de altura 
		tracker = new MediaTracker(new JButton());
		tracker.addImage(bitmap, 0);
		try {
			tracker.waitForAll();
		}
		catch (Exception e) {
			System.err.println("erro ao esperar pela imagem!");
		}
		
		
		
		/*ImagePanel J =new ImagePanel(texture);
        GuiUtils.testFrame(J,"image,");*/
        
	
    	QuadArray GFront = null;
    	
    	// criar o bobjecto 3D
    	 
    	//bitmap = GuiUtils.getImage("images/altura.png"); 
    	 
    	if(bitmap==null)
    	{
    		//System.err.println("terreno é null ");
    		// plano 
    		GFront = new QuadArray (4,QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
    	
    		/*	double double[] pos=new double[3];  topLeft.getAbsoluteNEDInMeters();
    		 bottomRight*/
    		
    		//Point3d topLeft3d=new Point3d(
    		//		(topLeft.getAbsoluteNEDInMeters()[0]-bottomRight.getAbsoluteNEDInMeters()[0])/2
    		//		,0,0);
    		Point3d topLeft3d=new Point3d(height/2,-width/2.,0);    				
        	Point3d bottomRight3d=new Point3d(-height/2,width/2,0);
			
    		GFront.setCoordinate (3, topLeft3d);
    		GFront.setCoordinate (2, new Point3d (topLeft3d.x, bottomRight3d.y, 0));
    		GFront.setCoordinate (1, bottomRight3d);
    		GFront.setCoordinate (0, new Point3d (bottomRight3d.x, topLeft3d.y, 0));
    		
    		GFront.setTextureCoordinate (0,0, new TexCoord2f(0.0f,0.0f));
    		GFront.setTextureCoordinate (0,1, new TexCoord2f(1.0f,0.0f)); 
    		GFront.setTextureCoordinate (0,2, new TexCoord2f(1.0f,1.0f));
    		GFront.setTextureCoordinate (0,3, new TexCoord2f(0.0f,1.0f));
    		//GFront=makeTerrain3D(texture,10);
   		}
    	else
    	{
    		GFront= makeTerrain3D(bitmap);
    		// construir o terreno a partir de imagem
    		//Image Rui;
	    	/* 	BufferedImage buffer = (BufferedImage)fetchImage();
	  		//	  BufferedImage buffer = new BufferedImage( 10, 10, BufferedImage.TYPE_INT_RGB );
	  	
	  		//	  Get a pixel
	    	int rgb = buffer.getRGB(10, 10);
	   
	    	// Get all the pixels
	    	int w = buffer.getWidth(null);
	    	int h = buffer.getHeight(null);
	    	int[] rgbs = new int[w*h];
	    	buffer.getRGB(0, 0, w, h, rgbs, 0, w);
	   
	    	// Set a pixel
	    	rgb = 0xFF00FF00; // green
	    	buffer.setRGB(10, 10, rgb);
	    	*/
	   	
	    	
		   	//	Object obj=GogglesOn.getImage(0).getUserData();   	 
    	}
    	
    	
    	Appearance appGFront = new Appearance();
    	//appGFront.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    	//appGFront.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

    	appGFront.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    	appGFront.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
    	

    	
		
    //	appGFront.setPolygonAttributes(p);
    	
		//appGFront.setColoringAttributes(new ColoringAttributes(1, 1, 1, ColoringAttributes.SHADE_GOURAUD));
		//ColoringAttributes c=new ColoringAttributes();
		//c.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		//appGFront.setColoringAttributes(c);
				
    	
        if(texture!=null)
        {
        	Texture GogglesOn = new TextureLoader( texture, null).getTexture();    	
        	appGFront.setTexture (GogglesOn);
        	TextureAttributes myta=new TextureAttributes();
 	   		Transform3D texturetrans = new Transform3D();
 	   		texturetrans.setScale(new Vector3d(texturescale,texturescale,texturescale));
 	   		myta.setTextureTransform(texturetrans);
 	   		myta.setTextureMode(TextureAttributes.REPLACE);
 	   		/*myta.setCombineRgbMode(TextureAttributes.COMBINE_REPLACE);
 	   		myta.setCombineRgbSource(0,TextureAttributes.COMBINE_TEXTURE_COLOR);
 	   		myta.setCombineRgbFunction(0,TextureAttributes.COMBINE_SRC_COLOR);
 	   		myta.setCombineAlphaMode(TextureAttributes.COMBINE_MODULATE);
 	   		myta.setCombineAlphaSource(0,TextureAttributes.COMBINE_PREVIOUS_TEXTURE_UNIT_STATE);
 	   		myta.setCombineAlphaFunction(0,TextureAttributes.COMBINE_SRC_ALPHA);
 	   		*/
 	   		appGFront.setTextureAttributes(myta);
        }
 	   	
       	if (transparency>=0.001f)
       	{
       		TransparencyAttributes trans=new TransparencyAttributes();
       		trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
            trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
            trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
            trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
            //NeptusLog.pub().info("<###>material con transp:"+transparency);
       		trans.setTransparency(transparency);
       		trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
       		appGFront.setTransparencyAttributes(trans);
       	}
       	else
       	{
       		//System.err.println("material con transp:"+transparency);
       		TransparencyAttributes trans=new TransparencyAttributes();
       		trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
            trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
            trans.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
            trans.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
            trans.setTransparencyMode(TransparencyAttributes.NONE);
       		//trans.setTransparencyMode(TransparencyAttributes.BLENDED);
       		//trans.setSrcBlendFunction(TransparencyAttributes.BLEND_SRC_ALPHA);
       		appGFront.setTransparencyAttributes(trans);
       	}
		
		
	  	   
 	   	
 	   	/*TextureAttributes myta=new TextureAttributes();
 	   		Transform3D texturetrans = new Transform3D();
 	   		texturetrans.setScale(new Vector3d(length,width,height));
 	   		myta.setTextureTransform(texturetrans);
 	   		myta.setTextureMode(TextureAttributes.MODULATE) ;
 	   		appearance3.setTextureAttributes(myta);*/
 	   	  	   	
		
		
  		
		 
	   	GeometryInfo gi=new GeometryInfo(GFront);
		//gi.convertToIndexedTriangles();
	   	
	   	if (shade)
	   	{
	   		Material mat = new Material();
	    	mat.setShininess(0.1f);
	    	appGFront.setMaterial(mat);
	    	
	    	NormalGenerator ng = new NormalGenerator();
	    	//gi.convertToIndexedTriangles();
	    	ng.generateNormals(gi);
	    	gi.recomputeIndices();
	    	gi.unindexify();
	    	gi.compact();
	   	}
	   	if (twosided)
    	{
    		PolygonAttributes p = new PolygonAttributes (PolygonAttributes.POLYGON_FILL,
        			PolygonAttributes.CULL_NONE, 0.0f);
        	appGFront.setPolygonAttributes(p);
    	}
	   	
	   	
		GeometryArray geom=gi.getGeometryArray();
		geom.setCapability(GeometryArray.ALLOW_NORMAL_READ);			   	
	   	Shape3D shape= new Shape3D (geom, appGFront);
	   	
	   	shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
	   	shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
	   	TransformGroup ret=new TransformGroup();
	   	ret.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
	   	ret.addChild(shape);
	   	return ret;
	}

	public static void main (String []arg)
	{
		Renderer3D render=new Renderer3D();
		GuiUtils.testFrame(render,"3D");
	
		Image texture=ImageUtils.getImage("images/batimetria.png");
		Image depth=ImageUtils.getImage("images/batimetria.png");
		
		Texture3D terrain=new Texture3D(texture,depth,49.,49.);
		//terrain.twosided=true;
		terrain.resolution=100;
		terrain.shade=false;
		terrain.transparency=0.3f;
		terrain.minvalue=14;
		terrain.maxvalue=2;
		terrain.texturescale=1.0;
		terrain.twosided=true;
		
		Obj3D obj=new Obj3D();	
		obj.setModel3D(terrain.getModel3D());
		
		render.addObj3D(obj);
	}

	public Image getBitmap() {
		return bitmap;
	}

	public void setBitmap(Image bitmap) {
		this.bitmap = bitmap;
	}

	public Image getTexture() {
		return texture;
	}

	public void setTexture(Image text) {
		this.texture = text;
//		tamanho maximo da textura
		int size=texture.getWidth(null)*texture.getHeight(null);
		if(size>480000)
		{
			
			MediaTracker tracker = new MediaTracker(new JButton());
			tracker.addImage(texture, 0);
			try {
				tracker.waitForAll();
			}
			catch (Exception e) {
				System.err.println("erro ao esperar pela imagem!");
			}
			
			double factor=1.;
			factor=(480000./size);
			
			
			int width=(int)(texture.getWidth(null)*factor);
			int height=(int)(texture.getHeight(null)*factor);
			//NeptusLog.pub().info("<###>-------------\nfactor" +factor
			//		+"\n"+"w:"+texture.getWidth(null)
			//		+"  h:"+texture.getHeight(null)
			//		+"\nx:"+width +" y:"+height);
		    texture=texture.getScaledInstance(width,height, Image.SCALE_SMOOTH);
		    
		}
	}
	
}
