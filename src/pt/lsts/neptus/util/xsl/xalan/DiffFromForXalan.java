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
 * 26/09/2006
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
