/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 12/Fev/2005
 */
package pt.lsts.neptus.junit.types.coord;

import java.io.ByteArrayOutputStream;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import junit.framework.TestCase;
import pt.lsts.neptus.types.coord.CoordinateSystem;

/**
 * @author Paulo
 *
 */
public class CoordinateSystemTest extends TestCase
{
    
    static String TCS1 = 
        "<coordinate-system-def>" + "\n" +
		"	<id>ned</id>"  + "\n" +
		"	<name>NED</name>"  + "\n" +
		"	<origin>"  + "\n" +
		"		<latitude>41N11</latitude>"  + "\n" +
		"		<longitude>8W36</longitude>"  + "\n" +
		"		<height>1.0</height>"  + "\n" +
		"		<azimuth>45.0</azimuth>"  + "\n" +
		"		<offset-distance>10.0</offset-distance>"  + "\n" +
		"		<zenith>90.0</zenith>"  + "\n" +
		"		<offset-north>1.0</offset-north>"  + "\n" +
		"		<offset-east>1.0</offset-east>"  + "\n" +
		"		<offset-up>1.0</offset-up>"  + "\n" +
		"	</origin>"  + "\n" +
		"	<axis-attitude>"  + "\n" +
		"		<roll>180.0</roll>"  + "\n" +
		"		<pitch>0.0</pitch>"  + "\n" +
		"		<yaw>0.0</yaw>"  + "\n" +
		"	</axis-attitude>"  + "\n" +
		"</coordinate-system-def>"; 
 
    static String TCS2 = 
        "<coordinate-system-def>" + "\n" +
		"	<id>nwu</id>"  + "\n" +
		"	<name>NWU</name>"  + "\n" +
		"	<origin>"  + "\n" +
		"		<latitude>41N11</latitude>"  + "\n" +
		"		<longitude>8W36</longitude>"  + "\n" +
		"		<depth>1.0</depth>"  + "\n" +
		"	</origin>"  + "\n" +
		"	<axis-attitude>"  + "\n" +
		"		<x-axis-direction>north</x-axis-direction>"  + "\n" +
		"		<y-axis-direction>west</y-axis-direction>"  + "\n" +
		"		<z-axis-direction>up</z-axis-direction>"  + "\n" +
		"	</axis-attitude>"  + "\n" +
		"</coordinate-system-def>"; 
    
	

    
    public void testCoordinateSystemString ()
    {
        CoordinateSystem cst = null;
        
        //TCS1
        cst = new CoordinateSystem(TCS1);
        assertNotNull(cst);
        
        assertEquals("NED", cst.getName());
        assertEquals("ned", cst.getId());
        
        assertEquals("41N11", cst.getLatitudeStr());
        assertEquals("8W36", cst.getLongitudeStr());
        assertEquals(-1d, cst.getDepth(), 0d);
        
        assertEquals(45d, cst.getAzimuth(), 0d);
        assertEquals(10d, cst.getOffsetDistance(), 0d);
        assertEquals(90d, cst.getZenith(), 90d);
        
        assertEquals(1d, cst.getOffsetNorth(), 0d);
        assertEquals(-1d, cst.getOffsetSouth(), 0d);
        assertTrue(cst.isOffsetNorthUsed());

        assertEquals(1d, cst.getOffsetEast(), 0d);
        assertEquals(-1d, cst.getOffsetWest(), 0d);
        assertTrue(cst.isOffsetEastUsed());

        assertEquals(1d, cst.getOffsetUp(), 0d);
        assertEquals(-1d, cst.getOffsetDown(), 0d);
        assertTrue(cst.isOffsetUpUsed());

        assertEquals(180d, cst.getRoll(), 0d);
        assertEquals(0d, cst.getPitch(), 0d);
        assertEquals(0d, cst.getYaw(), 0d);
        assertTrue(cst.isAnglesUsed());

        
        //TCS2
        cst = new CoordinateSystem(TCS2);
        assertNotNull(cst);
        
        assertEquals("NWU", cst.getName());
        assertEquals("nwu", cst.getId());
        
        assertEquals("41N11", cst.getLatitudeStr());
        assertEquals("8W36", cst.getLongitudeStr());
        assertEquals(1d, cst.getDepth(), 0d);
        
        assertEquals(0d, cst.getAzimuth(), 0d);
        assertEquals(0d, cst.getOffsetDistance(), 0d);
        assertEquals(0d, cst.getZenith(), 90d);
        
        assertEquals(0d, cst.getOffsetNorth(), 0d);
        assertEquals(0d, cst.getOffsetSouth(), 0d);
        assertTrue(cst.isOffsetNorthUsed());

        assertEquals(0d, cst.getOffsetEast(), 0d);
        assertEquals(0d, cst.getOffsetWest(), 0d);
        assertTrue(cst.isOffsetEastUsed());

        assertEquals(0d, cst.getOffsetUp(), 0d);
        assertEquals(0d, cst.getOffsetDown(), 0d);
        assertTrue(cst.isOffsetUpUsed());

        assertEquals(180d, cst.getRoll(), 0d);
        assertEquals(0d, cst.getPitch(), 0d);
        assertEquals(0d, cst.getYaw(), 0d);
        assertFalse(cst.isAnglesUsed());

    }
    
    
    public void testAsXMLString()
    {
        try
        {
            CoordinateSystem cst = null;
            
            //TCS1
            Document doc = DocumentHelper.parseText(TCS1);
            cst = new CoordinateSystem(TCS1);
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            //Compact format
            OutputFormat format = OutputFormat.createCompactFormat();
            XMLWriter writer = new XMLWriter( ba, format );
            writer.write( doc );
            //NeptusLog.pub().info("<###> "+ba.toString());
            //NeptusLog.pub().info("<###> "+cst.asXML());
            assertEquals(ba.toString(), cst.asXML());

            //TCS2
            doc = DocumentHelper.parseText(TCS2);
            cst = new CoordinateSystem(TCS2);
            ba = new ByteArrayOutputStream();
            //Compact format
            format = OutputFormat.createCompactFormat();
            writer = new XMLWriter( ba, format );
            writer.write( doc );
            //NeptusLog.pub().info("<###> "+ba.toString());
            //NeptusLog.pub().info("<###> "+cst.asXML());
            assertEquals(ba.toString(), cst.asXML());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }

    }
    
    
    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(CoordinateSystemTest.class);
    }

}
