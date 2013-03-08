/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 13/Jan/2005
 * $Id:: ConfigFetchTest.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.junit.util.conf;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 *
 */
public class ConfigFetchTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(ConfigFetchTest.class);
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

    /**
     * Constructor for ConfigFetchTest.
     * @param arg0
     */
    public ConfigFetchTest(String arg0)
    {
        super(arg0);
    }

    /*
     * Class under test for void ConfigFetch()
     */
    public void testConfigFetch()
    {
    	ConfigFetch.initialize();
    	String lo = ConfigFetch.getLoggingPropertiesLocation();
    	if (lo == null)
    		TestCase.fail("Fail to read config file!");
    	else
    		System.out.println(lo);
    }

    /*
     * Class under test for void ConfigFetch(String, boolean)
     */
    public void testConfigFetchStringboolean()
    {
    }

    /*
     * Class under test for void ConfigFetch(String)
     */
    public void testConfigFetchString()
    {
    }

    public void testResolvePath()
    {
    }

    public void testGetErrorDescriptionFile()
    {
    }

    public void testGetLoggingPropertiesLocation()
    {
    }

}
