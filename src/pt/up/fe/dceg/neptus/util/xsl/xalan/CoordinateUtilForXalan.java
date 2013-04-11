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
 * 4/Jun/2005
 */
package pt.up.fe.dceg.neptus.util.xsl.xalan;

import java.text.NumberFormat;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;

import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

/**
 * @author Paulo Dias
 * @version 1.0   06/2005
 */
/**
 * @author pdias
 *
 */
public class CoordinateUtilForXalan
{

    private static NumberFormat nf6 = GuiUtils.getNeptusDecimalFormat(6);

    /**
     * @param offsetDistance
     * @param azimuth (\u00B0)
     * @param zenith (\u00B0)
     * @param offsetNorth
     * @param offsetSouth
     * @param offsetEast
     * @param offsetWest
     * @param offsetUp
     * @param offsetDown
	 * @return Array with r, theta (rad) and height
     */
    private static double[] getCylOffsetsSum(String offsetDistance,
            String azimuth, String zenith, String offsetNorth,
            String offsetSouth, String offsetEast, String offsetWest,
            String offsetUp, String offsetDown)
    {
        if (offsetDistance.equals(""))
            offsetDistance = "0";
        if (azimuth.equals(""))
            azimuth = "0";
        if (zenith.equals(""))
            zenith = "0";
        if (offsetNorth.equals(""))
            offsetNorth = "0";
        if (offsetSouth.equals(""))
            offsetSouth = "0";
        if (offsetEast.equals(""))
            offsetEast = "0";
        if (offsetWest.equals(""))
            offsetWest = "0";
        if (offsetUp.equals(""))
            offsetUp = "0";
        if (offsetDown.equals(""))
            offsetDown = "0";
        
        //NeptusLog.pub().info("<###>#################"+offsetDistance+"#"+azimuth+"#"+zenith+"#"+offsetNorth+"#"+offsetSouth
        //	+"#"+offsetEast+"#"+offsetWest+"#"+offsetUp+"#"+offsetDown);
        double r = Double.parseDouble(offsetDistance);
        double theta = Double.parseDouble(azimuth);
        double phi = Double.parseDouble(zenith);
        double x = Double.parseDouble(offsetNorth) - Double.parseDouble(offsetSouth);
        double y = Double.parseDouble(offsetEast) -Double.parseDouble(offsetWest);
        double z = -Double.parseDouble(offsetUp) + Double.parseDouble(offsetDown);
        double[] cyl = CoordinateUtil
                .addSphericalToCartesianOffsetsAndGetAsCylindrical(r, theta,
                        phi, x, y, z);
        //printVector("$$$$$$$$$$$$$$$$$$$$$$$", cyl);
        return cyl;
    }

    /**
     * @param offsetDistance
     * @param azimuth
     * @param zenith
     * @param offsetNorth
     * @param offsetSouth
     * @param offsetEast
     * @param offsetWest
     * @param offsetUp
     * @param offsetDown
     * @return
     */
    public static String getRCylOffsetsSumAsString(String offsetDistance,
            String azimuth, String zenith, String offsetNorth,
            String offsetSouth, String offsetEast, String offsetWest,
            String offsetUp, String offsetDown)
    {
        double[] cyl = getCylOffsetsSum(offsetDistance, azimuth, zenith,
                offsetNorth, offsetSouth, offsetEast, offsetWest, offsetUp,
                offsetDown);
        double tmp = MathMiscUtils.round(cyl[0], 3);
        return Double.toString(tmp);
        //return offsetDistance+"#"+azimuth+"#"+zenith+"#"+offsetNorth+"#"+offsetSouth
        //	+"#"+offsetEast+"#"+offsetWest+"#"+offsetUp+"#"+offsetDown;
    }

    /**
     * @param offsetDistance
     * @param azimuth
     * @param zenith
     * @param offsetNorth
     * @param offsetSouth
     * @param offsetEast
     * @param offsetWest
     * @param offsetUp
     * @param offsetDown
     * @return
     */
    public static String getThetaCylOffsetsSumAsString(String offsetDistance,
            String azimuth, String zenith, String offsetNorth,
            String offsetSouth, String offsetEast, String offsetWest,
            String offsetUp, String offsetDown)
    {
        double[] cyl = getCylOffsetsSum(offsetDistance, azimuth, zenith,
                offsetNorth, offsetSouth, offsetEast, offsetWest, offsetUp,
                offsetDown);
        double tmp = MathMiscUtils.round(Math.toDegrees(cyl[1]), 3);
        return Double.toString(tmp);
    }

    /**
     * @param offsetDistance
     * @param azimuth
     * @param zenith
     * @param offsetNorth
     * @param offsetSouth
     * @param offsetEast
     * @param offsetWest
     * @param offsetUp
     * @param offsetDown
     * @return
     */
    public static String getHeightCylOffsetsSumAsString(String offsetDistance,
            String azimuth, String zenith, String offsetNorth,
            String offsetSouth, String offsetEast, String offsetWest,
            String offsetUp, String offsetDown)
    {
        double[] cyl = getCylOffsetsSum(offsetDistance, azimuth, zenith,
                offsetNorth, offsetSouth, offsetEast, offsetWest, offsetUp,
                offsetDown);
        double tmp = MathMiscUtils.round(cyl[2], 3);
        return Double.toString(tmp);
    }

    /**
     * @param param1
     * @param param2
     * @return param1 + param2
     */
    public static String add (String param1, String param2)
    {
        if (param1.equals(""))
            param1 = "0";
        if (param2.equals(""))
            param2 = "0";
        
        double res = Double.parseDouble(param1) + Double.parseDouble(param2);
        return Double.toString(res);
    }
    
    /**
     * @param header
     * @param vec
     */
    public static void printVector (String header, double[] vec)
    {
        String res = header.concat(": ");
        for (int i= 0; i < vec.length; i++)
        {
            res += " # " + vec[i];
        }
        System.out.println(res);
    }
    
    
    
    /**
     * @param p1
     * @param p2
     * @return
     * @deprecated
     */
    public static org.w3c.dom.Node getMiddlePoint(org.w3c.dom.Node p1, org.w3c.dom.Node p2)
    {
    	return p1;
    }
    

    public static org.w3c.dom.Node getLatLonDepthABS(org.w3c.dom.Node point)
    throws Exception
    {
		if (point == null)
		{
			throw new Exception("Missing point!");
		}
		
		org.w3c.dom.Document doc;
        try
        {
            doc = (new DOMWriter()).write(DocumentHelper.createDocument());
            doc.appendChild(doc.importNode(point, true));
            org.dom4j.Document p1DOM4J = (new DOMReader()).read(doc);

            LocationType absLoc = new LocationType(p1DOM4J.asXML()).getNewAbsoluteLatLonDepth();
            absLoc.setLatitude(nf6.format(absLoc.getLatitudeAsDoubleValue()));
            absLoc.setLongitude(nf6.format(absLoc.getLongitudeAsDoubleValue()));
            org.dom4j.Document mdDOM4J = absLoc.asDocument();
            doc = (new DOMWriter()).write(mdDOM4J);
            return doc.getDocumentElement();
        }
        catch (DocumentException e)
        {
            e.printStackTrace();
            return point;
        }
    }

}
