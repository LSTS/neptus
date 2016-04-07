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

import pt.lsts.neptus.plugins.mvplanning.Environment;
import pt.lsts.neptus.plugins.mvplanning.plangeneration.mapdecomposition.GridArea;
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
        gridDcmp = new GridArea(areaWidth, areaHeight, center);

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

    private void testComputeGridBounds(double gridWidth, double gridHeight) {
        if(gridDcmp == null)
            System.out.println("No grid decomposition available");
        else {
            LocationType[] bounds = gridDcmp.getBounds();

            LocationType tl = bounds[0];
            LocationType tr = bounds[1];
            LocationType bl = bounds[2];
            LocationType br = bounds[3];

            System.out.println("\n# Using GridArea.computeGridBounds()");
            System.out.println("TopLeft to topRight " + tl.getDistanceInMeters(tr));
            System.out.println("TopLeft to bottomLeft " + tl.getDistanceInMeters(bl));
            System.out.println("TopRight to bottomRight " + tr.getDistanceInMeters(br));
            System.out.println("BottomLeft to bottomRight " + bl.getDistanceInMeters(br));

            System.out.println("\n--- Coordinates: ---");
            System.out.println("TL: " + tl.getLatitudeAsPrettyString() + " " + tl.getLongitudeAsPrettyString());
            System.out.println("TR: " + tr.getLatitudeAsPrettyString() + " " + tr.getLongitudeAsPrettyString());
            System.out.println("BL: " + bl.getLatitudeAsPrettyString() + " " + bl.getLongitudeAsPrettyString());
            System.out.println("BR: " + br.getLatitudeAsPrettyString() + " " + br.getLongitudeAsPrettyString());
        }
    }

    public void testDecomposeGrid() {
        if(gridDcmp == null)
            System.out.println("No grid decomposition available");
        else {
            LocationType[] bounds = {topLeft, topRight, bottomLeft, bottomRight}; 
            gridDcmp.decomposeMap(bounds);

            System.out.println("Decomposed grid as [" + gridDcmp.getNumberOfRows() + " x " + gridDcmp.getNumberOfColumns() + "] cells");
            System.out.println("Cells are [" + gridDcmp.getCellWidth() + " x " + gridDcmp.getCellHeight() + "]");
        }
    }

    public static void main(String[] args) {
        double[] areasWidths = {1000, 500, 300, 200};
        double[] areasHeights = {1000, 500, 200, 50};

        for(int i = 0; i < areasWidths.length; i++) {
            System.out.println("# Test " + i +  " [" + areasWidths[i] + " x " + areasHeights[i] + "]");
            TestGridDecomposition gridTest = new TestGridDecomposition(areasWidths[i], areasHeights[i], LocationType.FEUP);

            System.out.println();

            gridTest.testDecomposeGrid();
            gridTest.testComputeGridBounds(areasWidths[i], areasHeights[i]);
            System.out.println("\n########\n");
        }
    }
}
