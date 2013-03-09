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
 * 18/Mai/2005
 */
package pt.up.fe.dceg.neptus.junit.types.coord;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.coord.LocationsHolder;

/**
 * @author Paulo
 *
 */
@SuppressWarnings("all")
public class LocationsHolderTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(LocationsHolderTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testLocHolder()
    {
        LocationType loc1 = new LocationType();
        loc1.setId("loc1");
        LocationType loc2 = new LocationType();
        loc2.setId("loc2");
        LocationType loc3 = new LocationType();
        loc3.setId("loc3");
        
        assertTrue(LocationsHolder.putAbstractLocationPoint(loc1));
        assertTrue(LocationsHolder.putAbstractLocationPoint(loc2));
        assertTrue(LocationsHolder.putAbstractLocationPoint(loc3));
        
        System.out.println(LocationsHolder.generateReport());

        assertTrue(LocationsHolder.removeRefToAbstractLocationPoint(loc1.getId(), loc2.getId()));

        System.out.println("_________________________________________\n");
        System.out.println(LocationsHolder.generateReport());

        
        assertTrue(LocationsHolder.removeAbstractLocationPoint(loc1.getId()));

        System.out.println("_________________________________________\n");
        System.out.println(LocationsHolder.generateReport());

    }

}
