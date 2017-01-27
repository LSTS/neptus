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
 * 19 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.tests;

import org.junit.Assert;
import org.junit.Test;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridCell;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;

public class GridAreaTests {
    @Test
    public void testGridDimensions() {
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

        GridArea area = new GridArea(polygon, 100);
        Assert.assertEquals("Number of rows", 10, area.getNrows());
        Assert.assertEquals("Number of cols", 10, area.getNcols());

        area = new GridArea(polygon, 60);
        Assert.assertEquals("Number of rows", 17, area.getNrows());
        Assert.assertEquals("Number of cols", 17, area.getNcols());
    }

    @Test
    public void testNeighbours() {
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

        GridArea area = new GridArea(polygon, 50);
        int nrows = area.getNrows();
        int ncols = area.getNcols();

        for(GridCell[] row : area.getGrid()) {
            for(GridCell cell : row) {
                int nNeighbours = cell.getNeighboursList().size();

                String msg = "Cell " + cell.getId() + " at " + cell.getRow() + " " + cell.getColumn();
                if(isCornerCell(cell.getRow(), cell.getColumn(), nrows, ncols))
                    Assert.assertEquals(msg, 2, nNeighbours);
                else if(cell.getRow() == 0 || cell.getColumn() == 0 ||
                        cell.getRow() == nrows - 1 || cell.getColumn() == ncols - 1)
                    Assert.assertEquals(msg, 3, nNeighbours);
                else
                    Assert.assertEquals(msg, 4, nNeighbours);

            }
        }
    }

    private boolean isCornerCell(int row, int col, int nrows, int ncols) {
        return  ((row == 0 && col == 0) ||
                (row == nrows -1 && col == ncols - 1) ||
                (row == 0 && col == ncols -1) ||
                (row == nrows - 1 && col == 0));
    }
}
