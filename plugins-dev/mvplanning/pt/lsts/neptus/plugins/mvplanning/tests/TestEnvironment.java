/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: tsmarques
 * 12 Apr 2016
 */
package pt.lsts.neptus.plugins.mvplanning.tests;

import pt.lsts.neptus.plugins.mvplanning.Environment;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ParallelepipedElement;

/**
 * @author tsmarques
 *
 */
public class TestEnvironment {
    
    public static void testHasObstacle(Environment env) {
        LocationType lt1 = new LocationType(LocationType.FEUP);
        LocationType lt2 = new LocationType(lt1);
        lt2.setOffsetNorth(10000);
        lt2 = lt2.getNewAbsoluteLatLonDepth();
        
        LocationType lt3 = new LocationType(lt1);
        lt2.setOffsetNorth(100);

        ParallelepipedElement obstacle1 = new ParallelepipedElement();
        obstacle1.setCenterLocation(LocationType.FEUP);
        obstacle1.setWidth(250);
        obstacle1.setLength(250);
        
        ParallelepipedElement obstacle2 = new ParallelepipedElement();
        obstacle2.setCenterLocation(LocationType.FEUP);
        obstacle2.setWidth(250);
        obstacle2.setLength(250);
        
        env.addObstacle(obstacle1);
        env.addObstacle(obstacle2);
        
        System.out.println("# Point 1 in obstacle " + env.hasObstacle(lt1));
        System.out.println("# Point 2 in obstacle " + env.hasObstacle(lt2));
        System.out.println("# Point 3 in obstacle " + env.hasObstacle(lt3));
    }
    
    public static void main(String[] args) {
        Environment env = new Environment();
        testHasObstacle(env);
    }
}
