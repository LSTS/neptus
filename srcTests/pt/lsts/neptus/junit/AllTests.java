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
 * 17/Fev/2005
 */
package pt.lsts.neptus.junit;

import junit.framework.Test;
import junit.framework.TestSuite;
import pt.lsts.neptus.junit.plugins.PluginsTest;
import pt.lsts.neptus.junit.types.coord.CoordAxisUtilTest;
import pt.lsts.neptus.junit.types.coord.CoordinateSystemTest;
import pt.lsts.neptus.junit.types.coord.CoordinateUtilTest;
import pt.lsts.neptus.junit.types.coord.LocationTypeTest;
import pt.lsts.neptus.junit.types.coord.LocationsHolderTest;
import pt.lsts.neptus.junit.util.FileUtilTest;
import pt.lsts.neptus.junit.util.NameNormalizerTest;
import pt.lsts.neptus.junit.util.conf.ConfigFetchTest;

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
        TestSuite suite = new TestSuite("Test for pt.lsts.neptus.junit");
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
