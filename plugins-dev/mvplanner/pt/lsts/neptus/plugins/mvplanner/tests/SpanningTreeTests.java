/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 27 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.tests;

import pt.lsts.neptus.plugins.mvplanner.algorithms.SpanningTree;
import junit.framework.Assert;
import org.junit.Test;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridCell;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;

import java.util.Iterator;

public class SpanningTreeTests {
    private GridArea buildArea() {
        LocationType x1 = new LocationType(LocationType.FEUP)
                .getNewAbsoluteLatLonDepth();
        LocationType x2 = new LocationType(LocationType.FEUP).translatePosition(0, 1000, 0)
                .getNewAbsoluteLatLonDepth();

        LocationType x3 = new LocationType(LocationType.FEUP).translatePosition(-1000, 0, 0)
                .getNewAbsoluteLatLonDepth();
        LocationType x4 = new LocationType(LocationType.FEUP).translatePosition(-1000, 1000, 0)
                .getNewAbsoluteLatLonDepth();

        PolygonType polygon = new PolygonType();
        polygon.addVertex(x1.getLatitudeDegs(), x1.getLongitudeDegs());
        polygon.addVertex(x2.getLatitudeDegs(), x2.getLongitudeDegs());
        polygon.addVertex(x3.getLatitudeDegs(), x3.getLongitudeDegs());
        polygon.addVertex(x4.getLatitudeDegs(), x4.getLongitudeDegs());

        return new GridArea(polygon, 100);
    }

    @Test
    public void spanningTree() {
        GridArea grid = buildArea();

        SpanningTree tree = new SpanningTree(grid.getCellAt(0, 0));
        Assert.assertNotNull("Tree is null", tree);

        Assert.assertEquals("Tree is empty", tree.hasNext(), true);

        // traverse and validate tree
        while(tree.hasNext()) {
            GridCell cell = tree.next();
            Assert.assertNotNull(cell);
        }
    }
}
