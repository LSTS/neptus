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
 * 17 Mar 2016
 */
package pt.lsts.neptus.plugins.mvplanning.tests;

import java.util.List;

import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author tsmarques
 *
 */
public class TestGridDecomposition {
    /* dimension of the area to decompose */
    private double areaWidth;
    private double areaHeight;

    private LocationType center;
    private LocationType topLeft;
    private LocationType topRight;
    private LocationType bottomLeft;
    private LocationType bottomRight;
    private GridArea gridDcmp;

    private List<LocationType> gridBounds;

    public TestGridDecomposition(double areaWidth, double areaHeight, LocationType center) {
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        this.center = center;
        gridDcmp = new GridArea(60, areaWidth, areaHeight, 0, center);

        computeGridBounds();
    }

    public List<LocationType> getGridBounds() {
        return gridBounds;
    }

    /**
     * According to the area's center location,
     * width and height, compute the locations of
     * the top left and right, and bottom left and
     * right locations
     * */
    private void computeGridBounds() {
        topLeft = new LocationType(center);
        topRight = new LocationType(center);
        bottomLeft = new LocationType(center);
        bottomRight = new LocationType(center);

        topLeft.setOffsetWest(areaWidth/2);
        topLeft.setOffsetNorth(areaHeight/2);
        topLeft = topLeft.getNewAbsoluteLatLonDepth();

        topRight.setOffsetEast(areaWidth/2);
        topRight.setOffsetNorth(areaHeight/2);
        topRight = topRight.getNewAbsoluteLatLonDepth();

        bottomLeft.setOffsetWest(areaWidth/2);
        bottomLeft.setOffsetSouth(areaHeight/2);
        bottomLeft = bottomLeft.getNewAbsoluteLatLonDepth();

        bottomRight.setOffsetEast(areaWidth/2);
        bottomRight.setOffsetSouth(areaHeight/2);
        bottomRight = bottomRight.getNewAbsoluteLatLonDepth();

        System.out.println("TopLeft to topRight " + topLeft.getDistanceInMeters(topRight));
        System.out.println("TopLeft to bottomLeft " + topLeft.getDistanceInMeters(bottomLeft));
        System.out.println("TopRight to bottomRight " + topRight.getDistanceInMeters(bottomRight));
        System.out.println("BottomLeft to bottomRight " + bottomLeft.getDistanceInMeters(bottomRight));

        System.out.println("\n--- Coordinates: ---");
        System.out.println("TL: " + topLeft.getLatitudeAsPrettyString() + " " + topLeft.getLongitudeAsPrettyString());
        System.out.println("TR: " + topRight.getLatitudeAsPrettyString() + " " + topRight.getLongitudeAsPrettyString());
        System.out.println("BL: " + bottomLeft.getLatitudeAsPrettyString() + " " + bottomLeft.getLongitudeAsPrettyString());
        System.out.println("BR: " + bottomRight.getLatitudeAsPrettyString() + " " + bottomRight.getLongitudeAsPrettyString());
    }

    public void testDecomposeGrid() {
        if(gridDcmp == null)
            System.out.println("No grid decomposition available");
        else {
            System.out.println("Decomposed grid as [" + gridDcmp.getNumberOfRows() + " x " + gridDcmp.getNumberOfColumns() + "] cells");
            System.out.println("Cells are [" + gridDcmp.getCellWidth() + " x " + gridDcmp.getCellHeight() + "]");
            System.out.println("There are " + gridDcmp.getAreaCells().size() + " cells");
        }
    }

    public void testNumberOfNeighbours() {
        /* count number of cells that don't have 4 neighbours */
        int count = (int) gridDcmp.getAreaCells()
                .stream()
                .filter((s) -> s.getNeighbours().size() < 4)
                .count();

        int nrows = gridDcmp.getNumberOfRows();
        int ncols = gridDcmp.getNumberOfColumns();
        int correctValue = ((nrows + ncols) * 2) - 4;
        System.out.println("\n# There are " + count + " cells with less than 4 neighbours");
        System.out.println("# There should be exactly " + correctValue + " cells");
        System.out.println("# Validated: " + (count == correctValue));
    }

    public void testSplitDecomposition(int n) {
        System.out.println("\n --- Testing splitDecomposition() ---");

        MapDecomposition[] newDcmp = gridDcmp.split(n);
        if(newDcmp == null)
            System.out.println("# Can't split in " + n + " decompositions");
        else {
            System.out.println("# Original decomposition was split in " + newDcmp.length);

            for(int i = 0; i < newDcmp.length; i++) {
                MapCell[][] cells = ((GridArea) newDcmp[i]).getAllCells();
                int nrows = cells.length;
                int ncols = cells[0].length;
                System.out.println("# Decomposition " + i + " : [" + nrows + "x" + ncols + "]");
            }
        }
    }

    public void testSplitCells(int n) {
        System.out.println("\n --- Testing splitCells() ---");
        System.out.println("* Original Grid has " + gridDcmp.getAreaCells().size());
        System.out.println("* Splitting each cell into " + n);
        GridArea newGrid = (GridArea) gridDcmp.splitMegaCells();

        int nCells = newGrid.getAreaCells().size();
        int correctN = gridDcmp.getAreaCells().size() * 4;

        System.out.println("** New Grid has " + nCells + " cells");
        System.out.println("** It should have " + correctN);
        System.out.println("*** Validated: " + (nCells == correctN));
        System.out.println("** Number of rows: " + newGrid.getNumberOfRows());
        System.out.println("** Number of columns: " + newGrid.getNumberOfColumns());
        System.out.println("** Width: " + newGrid.getWidth());
        System.out.println("** Height: " + newGrid.getHeight());
        System.out.println("** Cell width: " + newGrid.getCellWidth());
        System.out.println("** Cell height: " + newGrid.getCellHeight());
    }

    public static void main(String[] args) {
        double[] areasWidths = {1000, 500, 300, 200};
        double[] areasHeights = {1000, 500, 200, 100};

        for(int i = 0; i < areasWidths.length; i++) {
            System.out.println("# Test " + i +  " [" + areasWidths[i] + " x " + areasHeights[i] + "]");
            TestGridDecomposition gridTest = new TestGridDecomposition(areasWidths[i], areasHeights[i], LocationType.FEUP);

            System.out.println();

            gridTest.testDecomposeGrid();
            gridTest.testNumberOfNeighbours();

            int[] splits = {0, 1, 2, 3, 4};
            for(int j = 0; j < splits.length; j++)
                gridTest.testSplitDecomposition(j);
            gridTest.testSplitCells(4);
            System.out.println("\n########\n");
        }
    }
}
