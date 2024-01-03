/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.util;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.swing.ImageIcon;
import javax.vecmath.Color3f;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;

/**
 * 
 * @author RJPG
 * read .X3D files and return TranformGroup of Java3D
 */
@SuppressWarnings("rawtypes")
public class X3dParse {
	
	public String fileX3d;
	public String fileSchema;
	
	public X3dParse()
	{
		
	}

	public String getFileX3d() {
		return fileX3d;
	}

	public void setFileX3d(String file) {
		this.fileX3d = file;
	}
	
	public String getFileSchema() {
		return fileSchema;
	}

	public void setFileSchema(String fileschema) {
		this.fileSchema = fileschema;
		
	}

    public Document initparse(String url) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(url);
        return document;
    }
	
    
    public String resolvePathWithParent (String parentPath, String filePath)
	{
	    File fx = new File (filePath);
	    File fxParent = new File (parentPath);
	    
	  
	    if (fx.isAbsolute())
	    {
	       
	        return fx.getAbsolutePath();
	    }
	    else
	    {
	       
	        if (fxParent.exists())
	        {
	            String parent = "";
	            if (fxParent.isDirectory())
	                parent = fxParent.getAbsolutePath();
	            else
	                parent = fxParent.getAbsoluteFile().getParent();
	        
	            File fx1 = new File(parent + "/" + filePath).getAbsoluteFile();
	            if (fx1.isAbsolute())
	            {
	                try
                    {
	        	  
                        return fx1.getCanonicalPath();
                    }
                    catch (IOException e)
                    {
            	    
                        return fx1.getAbsolutePath();
                    }
	            }
	        }
	    }
     
	    return filePath;
	}

    
    public Point2f[] stringToPoint2f (String strin)
    {
    	
    	String aux=strin.replace("  "," ");
    	String[] str=aux.split(", ");
    	Point2f[] ret=new Point2f[str.length];
    	for(int i=0;i<str.length;i++)
    	{
    		//NeptusLog.pub().info("<###>parsing"+i+":"+str[i]);
    		
    		String[] result = str[i].split(" ");
    		ret[i]=new Point2f();
   		 	ret[i].x=Float.parseFloat(result[0]);
   		 	ret[i].y=Float.parseFloat(result[1]);
   		 
    	}
    	return ret;
    }
    
    public Point3f[] stringToPoint3f (String strin)
    {
    	
    	String aux=strin.replace("  "," ");
    	String[] str=aux.split(", ");
    	
    	Point3f[] ret=new Point3f[str.length];
    	for(int i=0;i<str.length;i++)
    	{
    		String[] result = str[i].split(" ");
    	//	//NeptusLog.pub().info("<###>parsing"+i+":"+str[i]);
    		ret[i]=new Point3f();
    		 ret[i].x=Float.parseFloat(result[0]);
    	  	 ret[i].z=-Float.parseFloat(result[1]);
    	  	 ret[i].y=Float.parseFloat(result[2]);
    	}
    	return ret;
    }   
    
    public int[] stringToInt3 (String strin)
    {
    	
    	String aux=strin.replace(" -1","");
    	aux=aux.replace("  "," ");
    	int auxiniciospace=0;
    	if(aux.charAt(0)==' ')
    		auxiniciospace++;
    	//NeptusLog.pub().info("<###>edwefwefwefwefwef:"+aux);
    	String[] str=aux.split(" ");
    	int[] ret=new int[str.length-auxiniciospace];
    	for(int i=0;i<str.length-auxiniciospace;i++)
    	{
    			if(str[i].equals(""))
    				auxiniciospace++;
    			
    			ret[i]=Integer.parseInt(str[i+auxiniciospace]);
    			//NeptusLog.pub().info("<###>parsing"+i+":"+str[i+auxiniciospace]);
    	}
    	return ret;
    }
    
 
    
    
    public Transform3D parseRotation(String text)
    {
    	Transform3D ret = new Transform3D();
    	return ret;
    }
    
    public Transform3D parseTranslation(String text)
    {
    	Transform3D ret=new Transform3D();
    	//ret.setIdentity();
    	float x,y,z;
    	
  	    String[] result = text.split(" ");
  	    x=Float.parseFloat(result[0]);
  	    z=-Float.parseFloat(result[1]);
  	    y=Float.parseFloat(result[2]);
  	    
  	    ret.set(new Vector3d(x,y, z));
  	    
    	return ret;
    }
    
    public Transform3D parseScale(String text)
    {
    	Transform3D ret=new Transform3D();
    	//ret.setIdentity();
    	float x,y,z;
    	
  	    String[] result = text.split(" ");
  	    x=Float.parseFloat(result[0]);
  	    z=-Float.parseFloat(result[1]);
  	    y=Float.parseFloat(result[2]);
  	    
  	    ret.setScale(new Vector3d(x,y, z));
  	    
    	return ret;
    }
    
    public Transform3D parseTransform(org.dom4j.Node node)
    {
    	
    	Transform3D ret = new Transform3D();
    	//ret.setIdentity();
    	//
    	
    	
    	List list = node.selectNodes( "@*" );

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            //System.err.println(attribute.getValue());
            if("rotation".equals(attribute.getName()))
  			{
            	  ret.mul(parseRotation(attribute.getValue()));
  			}

            if("translation".equals(attribute.getName()))
  			{
            	//NeptusLog.pub().info("<###>Entrei no translation");
            	  ret.mul(parseTranslation(attribute.getValue()));
  			}

            if("scale".equals(attribute.getName()))
  			{
            	  ret.mul(parseScale(attribute.getValue()));
  			}

        }
    	
    	return ret;
    	//rotx.setTransform(rot);
    }
    
    public TransformGroup parseScene(org.dom4j.Node node)
    {
    	TransformGroup ret=new TransformGroup();
    	
    	
    	
    	  List list = node.selectNodes( "*" );
	      for (Iterator i = list.iterator(); i.hasNext(); ) {
	            
            Element element = (Element) i.next();
            //NeptusLog.pub().info("<###> "+element.getName());
            
            if("StaticGroup".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseScene((org.dom4j.Node)element));
			}
            
            
			if("Group".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseScene((org.dom4j.Node)element));
			}
			
			if("Shape".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseShape((org.dom4j.Node)element));
			}
			
			if("Transform".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 TransformGroup trans=new TransformGroup();
			 Transform3D tr=parseTransform((org.dom4j.Node)element);
			 trans.setTransform(tr);
			 trans.addChild(parseScene((org.dom4j.Node)element));
			 ret.addChild(trans);
			 
			}
		}
    	return ret;
    }
    
    public Color3f parseColor(String strin)
    {
    	Color3f ret=new Color3f();
    	float r,g,b;
    	String color=strin.replace(",","");
  	  String[] result = color.split(" ");
  	  r=Float.parseFloat(result[0]);
  	   g=Float.parseFloat(result[1]);
  	  b=Float.parseFloat(result[2]);
  	  
  	  ret.set(r,g,b);   
    	return ret;
    }
    
    public Vector3f parseSize(String size)
    {
    	Vector3f ret=new Vector3f();
    	float x,y,z;
    	
  	  String[] result = size.split("\\s");
  	  x=Float.parseFloat(result[0]);
  	  y=Float.parseFloat(result[1]);
  	  z=Float.parseFloat(result[2]);
  	  
  	  ret.set(x,y,z);   
    	return ret;
    }
    
    public Sphere parseSphere(org.dom4j.Node node,Appearance app)
    {
    	
    	float radius=2f;
    	
    	List list = node.selectNodes( "@*" );

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            //System.err.println(attribute.getValue());
            if("radius".equals(attribute.getName()))
  			{
            	radius=Float.parseFloat(attribute.getValue());
  			}
        }
    	
    	Sphere ret= new com.sun.j3d.utils.geometry.Sphere(radius,com.sun.j3d.utils.geometry.Sphere.GENERATE_TEXTURE_COORDS|
    			com.sun.j3d.utils.geometry.Cylinder.GENERATE_NORMALS,null);
    	ret.setAppearance(app);
    	
    	return ret;
    }
    
    public Cylinder parseCylinder(org.dom4j.Node node,Appearance app)
    {
    	float height=1f;
    	float radius=2f;
    	
    	List list = node.selectNodes( "@*" );

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            //System.err.println(attribute.getValue());
            if("radius".equals(attribute.getName()))
  			{
            	radius=Float.parseFloat(attribute.getValue());
  			}
            if("height".equals(attribute.getName()))
  			{
            	height=Float.parseFloat(attribute.getValue());
  			}
            
        }

    	Cylinder ret= new com.sun.j3d.utils.geometry.Cylinder(radius,height,com.sun.j3d.utils.geometry.Cylinder.GENERATE_TEXTURE_COORDS|
    			com.sun.j3d.utils.geometry.Cylinder.GENERATE_NORMALS,null);
    	ret.setAppearance(app);
    	
    	return ret;
    }
    
    public Cone parseCone(org.dom4j.Node node,Appearance app)
    {
    	float height=1f;
    	float radius=2f;
    	
    	List list = node.selectNodes( "@*" );

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            //System.err.println(attribute.getValue());
            if("radius".equals(attribute.getName()))
  			{
            	radius=Float.parseFloat(attribute.getValue());
  			}
            if("height".equals(attribute.getName()))
  			{
            	height=Float.parseFloat(attribute.getValue());
  			}
            
        }

    	Cone ret= new com.sun.j3d.utils.geometry.Cone(radius,height,com.sun.j3d.utils.geometry.Cone.GENERATE_TEXTURE_COORDS|
    			com.sun.j3d.utils.geometry.Cone.GENERATE_NORMALS,null);
    	ret.setAppearance(app);
    	
    	return ret;
    }
    
    
    public Box parseBox(org.dom4j.Node node,Appearance app)
    {
    	float x=1f;
    	float y=1f;
    	float z=1f;
    	
    	List list = node.selectNodes( "@*" );

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            //System.err.println(attribute.getValue());
            if("size".equals(attribute.getName()))
  			{
            	 Vector3f v=parseSize(attribute.getValue());
            	 x=v.x;
            	 y=v.y;
            	 z=v.z;
  			}
            
        }

    	Box ret= new com.sun.j3d.utils.geometry.Box(x,y,z,com.sun.j3d.utils.geometry.Box.GENERATE_TEXTURE_COORDS|
    			com.sun.j3d.utils.geometry.Box.GENERATE_NORMALS,null);
    	ret.setAppearance(app);
    	
    	return ret;
    }
    
    public Material parseMaterial(org.dom4j.Node node)
    {
    	Material mat = new Material();
    	Color3f dc=new Color3f(0.5f,0.5f,0.5f);
    	Color3f sc=new Color3f(0.5f,0.5f,0.5f);
    	Color3f ec=new Color3f(0.0f,0.0f,0.0f);
    	float shininess=0.5f;
    	mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
    	mat.setCapability(Material.ALLOW_COMPONENT_READ);
    	
    	List list = node.selectNodes( "@*" );

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Attribute attribute = (Attribute) iter.next();
            //System.err.println(attribute.getValue());
            if("diffuseColor".equals(attribute.getName()))
  			{
            	  dc=parseColor(attribute.getValue());
            	/*StringTokenizer st = new StringTokenizer(attribute.getValue());
                while (st.hasMoreTokens()) {
                    st.nextToken());
                }*/

  			}
            if("specularColor".equals(attribute.getName()))
  			{
            	  sc=parseColor(attribute.getValue());
            	/*StringTokenizer st = new StringTokenizer(attribute.getValue());
                while (st.hasMoreTokens()) {
                    st.nextToken());
                }*/

  			}
            if("emissiveColor".equals(attribute.getName()))
  			{
            	  ec=parseColor(attribute.getValue());
            	/*StringTokenizer st = new StringTokenizer(attribute.getValue());
                while (st.hasMoreTokens()) {
                    st.nextToken());
                }*/

  			}
            
            if("shininess".equals(attribute.getName()))
  			{
            	shininess=Float.parseFloat(attribute.getValue());
  			}
            
        }
        
        
    	mat.setDiffuseColor(dc);
	    mat.setSpecularColor(sc);
	    mat.setEmissiveColor(ec);
	    mat.setShininess(shininess);
	    return mat; 
    	
    }
    public TransparencyAttributes parseGetTrnspMat(org.dom4j.Node node)
    {
    	float value=0f;
    	
    	List list = node.selectNodes( "@*" );
    	  for (Iterator iter = list.iterator(); iter.hasNext(); ) {
              Attribute attribute = (Attribute) iter.next();
              //System.err.println(attribute.getValue());
              if("transparency".equals(attribute.getName()))
    			{
              	  value=Float.parseFloat(attribute.getValue());
              	/*StringTokenizer st = new StringTokenizer(attribute.getValue());
                  while (st.hasMoreTokens()) {
                      st.nextToken());
                  }*/

    			}
              }
    	  if(value==0f)
    		  return null;
    	  else
    	  {
         	 TransparencyAttributes trans=new TransparencyAttributes();
          	 trans.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
          	 trans.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
           	 trans.setTransparency(value);
           	 trans.setTransparencyMode(TransparencyAttributes.BLEND_ONE);
           	 return trans;
    	  }
    }
    
    public Appearance parseAppearance(org.dom4j.Node node)
    {
    	Appearance app=new Appearance();

    	app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    	app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);	
    	List list = node.selectNodes( "*" );
	      for (Iterator i = list.iterator(); i.hasNext(); ) {
	    	    Element element = (Element) i.next();
	            //NeptusLog.pub().info("<###> "+element.getName());
	           
	            if("Material".equals(element.getName()))
	  			{
	            	app.setMaterial(parseMaterial((org.dom4j.Node)element));
	            		
	            	TransparencyAttributes trans=parseGetTrnspMat((org.dom4j.Node)element);
	            	if(trans!=null)
	            		app.setTransparencyAttributes(trans);
	  			}
	            if("ImageTexture".equals(element.getName()))
	  			{
	            	//NeptusLog.pub().info("<###>!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	            	app.setTexture (parseImageTexture((org.dom4j.Node)element));
	            	
	  			}
	      }
	
	      
	      
	    PolygonAttributes p = new PolygonAttributes ();
      	p.setCullFace (PolygonAttributes.CULL_NONE);
      	app.setPolygonAttributes(p);

	    return app;
    }
    
    public Texture parseImageTexture(org.dom4j.Node node)
    {
    	Texture ret=null;
    	List list = node.selectNodes( "@*" );
    	for (Iterator iter = list.iterator(); iter.hasNext(); ) 
    	{
    		Attribute attribute = (Attribute) iter.next();
    		//System.err.println(attribute.getValue());
    		if("url".equals(attribute.getName()))
    		{
    			String pathtext=resolvePathWithParent(fileX3d,attribute.getValue());
    			//NeptusLog.pub().info("<###>File:"+fileX3d);
    			//NeptusLog.pub().info("<###>attrib:"+attribute.getValue());
    			//NeptusLog.pub().info("<###>total:"+pathtext);
    			Image texture= new ImageIcon(pathtext).getImage();
    			ret= new TextureLoader( texture, null).getTexture(); 

     			 //System.err.println("Encontrei:"+attribute.getName());
    		}
    	}
    	return ret;
    }
    
    public Point3f[] parseCoordinate(org.dom4j.Node node)
    {	
    	Point3f[] ret=null;
    	List list = node.selectNodes( "@*" );
    	for (Iterator iter = list.iterator(); iter.hasNext(); ) 
    	{
    		Attribute attribute = (Attribute) iter.next();
    		//System.err.println(attribute.getValue());
    		if("point".equals(attribute.getName()))
    		{
    			 ret=stringToPoint3f(attribute.getValue());
     			 //System.err.println("Encontrei:"+attribute.getName());
    		}
    	}
    	return ret;
    }
    
    public Point3f[] parseColor(org.dom4j.Node node)
    {	
    	Point3f[] ret=null;
    	List list = node.selectNodes( "@*" );
    	for (Iterator iter = list.iterator(); iter.hasNext(); ) 
    	{
    		Attribute attribute = (Attribute) iter.next();
    		//System.err.println(attribute.getValue());
    		if("color".equals(attribute.getName()))
    		{
    			 ret=stringToPoint3f(attribute.getValue());
     			 //System.err.println("Encontrei:"+attribute.getName());
    		}
    	}
    	return ret;
    }
    
    public Point2f[] parseTextureCoordinate(org.dom4j.Node node)
    {	
    	Point2f[] ret=null;
    	List list = node.selectNodes( "@*" );
    	for (Iterator iter = list.iterator(); iter.hasNext(); ) 
    	{
    		Attribute attribute = (Attribute) iter.next();
    		//System.err.println(attribute.getValue());
    		if("point".equals(attribute.getName()))
    		{
    			 ret=stringToPoint2f(attribute.getValue());
     			 //System.err.println("Encontrei:"+attribute.getName());
    		}
    	}
    	return ret;
    }
    
    @SuppressWarnings("deprecation")
    public Shape3D parseIndexedFaceSet(org.dom4j.Node node,Appearance app)
    {
	Shape3D shape=null;
    
	int propreties=0;
	
   	Point3f[] pointsvector=null;
   	Point2f[] pointstextvector=null;
   	int[] indexpoints=null;
   	int[] indextextpoints=null;
   	List list = node.selectNodes( "@*" );
  	  for (Iterator iter = list.iterator(); iter.hasNext(); ) 
  	  {
  		  Attribute attribute = (Attribute) iter.next();
  		  //System.err.println(attribute.getValue());
  		  if("coordIndex".equals(attribute.getName()))
  		  {
  			  //NeptusLog.pub().info("<###>Encontrei:"+attribute.getName());
  			  propreties=propreties|TriangleArray.COORDINATES;
  			  indexpoints=stringToInt3(attribute.getValue());
  			  
  		  }
  		  if("texCoordIndex".equals(attribute.getName()))
		  {
			  propreties=propreties|TriangleArray.TEXTURE_COORDINATE_2;
			  indextextpoints=stringToInt3(attribute.getValue());
			  //NeptusLog.pub().info("<###>Encontrei:"+attribute.getName());
		  }
  		 if("colorIndex".equals(attribute.getName()))
		  {
  			 //NeptusLog.pub().info("<###>Encontrei:"+attribute.getValue());
			  propreties=propreties|TriangleArray.COLOR_3;
			  stringToInt3(attribute.getValue());
			 
		  }
  	  }
       
  	  list = node.selectNodes( "*" );
      for (Iterator i = list.iterator(); i.hasNext(); ) 
      {      
    	  Element element = (Element) i.next();
    	  //NeptusLog.pub().info("<###> "+element.getName());
     
    	  if("Coordinate".equals(element.getName()))
    	  {
    		  //NeptusLog.pub().info("<###> "+element.getName());
    		  pointsvector=parseCoordinate((org.dom4j.Node)element);
    	  }
    	  
    	  if("TextureCoordinate".equals(element.getName()))
    	  {
    		  //NeptusLog.pub().info("<###> "+element.getName());
    		  pointstextvector=parseTextureCoordinate((org.dom4j.Node)element);
    	  }
    	  
    	  if("Color".equals(element.getName()))
    	  {
    		  parseColor((org.dom4j.Node)element);
    	  }
    	  
    	  
      }
  	  
  	  int numvertices=indexpoints.length;
      //NeptusLog.pub().info("<###>numvertices:"+indexpoints.length);
  	  TriangleArray GFront=new TriangleArray(numvertices,propreties);
  	 
  	  for (int i=0;i<numvertices;i++)
  	  {
  			GFront.setCoordinate (i, pointsvector[indexpoints[i]] );
  			if(pointstextvector!=null)
  				GFront.setTextureCoordinate (i, pointstextvector[indextextpoints[i]]);
  	  }
  	  GeometryInfo gi=new GeometryInfo(GFront);
  	  NormalGenerator ng = new NormalGenerator();
  	  //gi.convertToIndexedTriangles();
  	  ng.generateNormals(gi);
  	  gi.recomputeIndices();
  	  gi.unindexify();
  	  gi.compact();
  	  GeometryArray geom=gi.getGeometryArray();
  	  geom.setCapability(GeometryArray.ALLOW_NORMAL_READ);			   	
  	  shape= new Shape3D (geom, app);
  	  return shape;
    }
    
    public TransformGroup parseShape(org.dom4j.Node node)
    {
    	TransformGroup ret=new TransformGroup();
	    Appearance app=new Appearance();  
       
       	
	    List list = node.selectNodes( "*" );
	      for (Iterator i = list.iterator(); i.hasNext(); ) {
	            
          Element element = (Element) i.next();
          //NeptusLog.pub().info("<###> "+element.getName());
         
          if("Appearance".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 app=parseAppearance((org.dom4j.Node)element);
			 
			}
          
	      }
	    
         list = node.selectNodes( "*" );
	      for (Iterator i = list.iterator(); i.hasNext(); ) {
	            
            Element element = (Element) i.next();
            //NeptusLog.pub().info("<###> "+element.getName());
           
            if("Cylinder".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseCylinder((org.dom4j.Node)element,app));
			}     
            
            if("Box".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseBox((org.dom4j.Node)element,app));
			}     
            
            if("Sphere".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseSphere((org.dom4j.Node)element,app));
			}     
            
            if("Cone".equals(element.getName()))
			{
			 //NeptusLog.pub().info("<###> "+element.getName());
			 ret.addChild(parseCone((org.dom4j.Node)element,app));
			}
            
          
           if("IndexedFaceSet".equals(element.getName()))
            {
            	//NeptusLog.pub().info("<###> "+element.getName());
   			 	ret.addChild(parseIndexedFaceSet((org.dom4j.Node)element,app));
            }
            
	      }
    	
		 
    	return ret;
    }
    
	public TransformGroup parse() throws DocumentException 
	{
		TransformGroup tr=new TransformGroup();
		 
		Document doc=null;
		try {
			  doc = this.initparse(fileX3d);
		} catch (DocumentException e) {
			  e.printStackTrace();
				//NeptusLog.pub().info("<###>Erro em parse:DocumentHelper.parseText(fileX3d)");
				return null;  
		}
		//NeptusLog.pub().info("<###> "+doc.asXML());
		
		//Element root = doc.getRootElement();
		//org.dom4j.Node shape= doc.selectSingleNode("//X3D/Scene/Shape");
		
		  List list = doc.selectNodes( "//X3D/*" );
	      for (Iterator i = list.iterator(); i.hasNext(); ) {
	            
	            Element element = (Element) i.next();
	            //NeptusLog.pub().info("<###> "+element.getName());
	            
	           if("Scene".equals(element.getName()))
				 {
					 //NeptusLog.pub().info("<###> "+element.getName());
					 tr.addChild(parseScene((org.dom4j.Node)element));
				 }
	           /* 
	            if("Group".equals(element.getName()))
				 {
					 //NeptusLog.pub().info("<###> "+element.getName());
					 tr.addChild(parseScene((org.dom4j.Node)element));
				 }
			*/	 
				 
	        }
	      
	      
		
		return tr;
	}

	
	public boolean validateDtd() {
		boolean flag;
		 try
	        {
	           
	            XMLValidator xmlVal = new XMLValidator(fileX3d.toString(), fileSchema);
	            xmlVal.validate();
	            flag = true;
	        }
	        catch (Exception e1)
	        {
	        	//System.err.println(this.toString()+e1.toString());
	        	flag= false;
	            //NeptusLog.pub().error(this, e1);
	        }
		return flag;
	}
}
