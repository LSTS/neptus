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
 * 17/Fev/2005
 */
package pt.up.fe.dceg.neptus.junit;

import junit.framework.Test;
import junit.framework.TestSuite;
import pt.up.fe.dceg.neptus.junit.plugins.PluginsTest;
import pt.up.fe.dceg.neptus.junit.types.coord.CoordAxisUtilTest;
import pt.up.fe.dceg.neptus.junit.types.coord.CoordinateSystemTest;
import pt.up.fe.dceg.neptus.junit.types.coord.CoordinateUtilTest;
import pt.up.fe.dceg.neptus.junit.types.coord.LocationTypeTest;
import pt.up.fe.dceg.neptus.junit.types.coord.LocationsHolderTest;
import pt.up.fe.dceg.neptus.junit.util.FileUtilTest;
import pt.up.fe.dceg.neptus.junit.util.NameNormalizerTest;
import pt.up.fe.dceg.neptus.junit.util.conf.ConfigFetchTest;

/**
 * @author Paulo
 *
 */
public class AllTests
{

    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(AllTests.class);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Test for pt.up.fe.dceg.neptus.junit");
        //$JUnit-BEGIN$
        suite.addTestSuite(CoordinateSystemTest.class);
        suite.addTestSuite(CoordAxisUtilTest.class);
        suite.addTestSuite(CoordinateUtilTest.class);
        suite.addTestSuite(ConfigFetchTest.class);
        suite.addTestSuite(NameNormalizerTest.class);
        suite.addTestSuite(FileUtilTest.class);
        suite.addTestSuite(LocationTypeTest.class);
        suite.addTestSuite(LocationsHolderTest.class);
        suite.addTestSuite(PluginsTest.class);
        //$JUnit-END$
        return suite;
    }
}
