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
 * 26/09/2006
 * $Id:: DiffFromForXalan.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.util.xsl.xalan;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author pdias
 *
 */
public class DiffFromForXalan 
{
	private org.w3c.dom.Node refPoint;
	
	public void setRefPoint(org.w3c.dom.Node refPoint)
	{
		this.refPoint = refPoint;
	}


	public org.w3c.dom.Node getDiffPoint(org.w3c.dom.Node point) throws Exception
    {
		
		if (refPoint == null)
		{
			throw new Exception("Missing reference point!");
		}

		if (point == null)
		{
			throw new Exception("Missing point!");
		}

		org.w3c.dom.Document doc;
        try
        {
            doc = (new DOMWriter()).write(DocumentHelper.createDocument());
            doc.appendChild(doc.importNode(refPoint, true));
            org.dom4j.Document refPointDOM4J = (new DOMReader()).read(doc);
            
            doc = (new DOMWriter()).write(DocumentHelper.createDocument());
            doc.appendChild(doc.importNode(point, true));
            org.dom4j.Document pointDOM4J = (new DOMReader()).read(doc);
            
            LocationType refPointLoc = new LocationType(refPointDOM4J.asXML());
            LocationType pointLoc = new LocationType(pointDOM4J.asXML());
            
            double[] dists = pointLoc.getOffsetFrom(refPointLoc);
            LocationType mdLoc = refPointLoc.getNewAbsoluteLatLonDepth();
            
            LocationType ptLoc = pointLoc.getNewAbsoluteLatLonDepth();
            
            mdLoc.setOffsetNorth(dists[0]);
            mdLoc.setOffsetEast(dists[1]);
            mdLoc.setOffsetDown(ptLoc.getDepth());
            
            org.dom4j.Document mdDOM4J = mdLoc.asDocument();
            doc = (new DOMWriter()).write(mdDOM4J);
            return doc.getDocumentElement();
        }
        catch (DocumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return refPoint;
        }
    }
}
