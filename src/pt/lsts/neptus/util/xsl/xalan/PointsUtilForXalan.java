/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 30/Mar/2006
 */
package pt.lsts.neptus.util.xsl.xalan;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleCalc;

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
