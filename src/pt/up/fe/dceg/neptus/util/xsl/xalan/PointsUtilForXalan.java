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
 * 30/Mar/2006
 * $Id:: PointsUtilForXalan.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.util.xsl.xalan;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;

import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.AngleCalc;

/**
 * @author Paulo Dias
 *
 */
public class PointsUtilForXalan
{

	private org.w3c.dom.Node p1, p2;
	
	public void setPoint1(org.w3c.dom.Node p1)
	{
		this.p1 = p1;
	}

	public void setPoint2(org.w3c.dom.Node p2)
	{
		this.p2 = p2;
	}

	public org.w3c.dom.Node getMiddlePoint(String name) throws Exception
    {
		final int maxDive = 20;
		
		if (p1 == null)
		{
			throw new Exception("Missing start point!");
		}
		if (p2 == null)
		{
			throw new Exception("Missing end point!");
		}
		
		org.w3c.dom.Document doc;
        try
        {
            doc = (new DOMWriter()).write(DocumentHelper.createDocument());
            doc.appendChild(doc.importNode(p1, true));
            org.dom4j.Document p1DOM4J = (new DOMReader()).read(doc);
            
            doc = (new DOMWriter()).write(DocumentHelper.createDocument());
            doc.appendChild(doc.importNode(p2, true));
            org.dom4j.Document p2DOM4J = (new DOMReader()).read(doc);
            
            LocationType p1Loc = new LocationType(p1DOM4J.asXML());
            LocationType p2Loc = new LocationType(p2DOM4J.asXML());
            
            double[] dists = p2Loc.getOffsetFrom(p1Loc);
            LocationType mdLoc = new LocationType(p1Loc);
            mdLoc.setId(name);
            mdLoc.setName(name);
            
            double x = 0, y = 0, z = 0, d = 0;
            
            x = dists[0] / 2.0;
            y = dists[1] / 2.0;
            z = dists[2] / 2.0;
            
            d = x*x + y*y + z*z;
            if(d > maxDive)
            {
            	double angXY = AngleCalc.calcAngle(0, 0, x, y);
            	double angXZ = AngleCalc.calcAngle(0, 0, x, z);
            	d = maxDive;
            	x = d * Math.sin(angXY);
            	y = d * Math.cos(angXY);
            	z = d * Math.cos(angXZ);
            }
            
            mdLoc.setOffsetNorth(mdLoc.getOffsetNorth() + x);
            mdLoc.setOffsetEast(mdLoc.getOffsetEast()   + y);
            mdLoc.setOffsetDown(mdLoc.getOffsetDown()   + z);
            
            
            
            org.dom4j.Document mdDOM4J = mdLoc.asDocument();
            doc = (new DOMWriter()).write(mdDOM4J);
            return doc.getDocumentElement();
        }
        catch (DocumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return p2;
        }
    }
}
