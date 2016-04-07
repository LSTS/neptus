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
 * 14 Mar 2016
 */
package pt.lsts.neptus.plugins.mvplanning.plangeneration.mapdecomposition;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


import pt.lsts.neptus.plugins.mvplanning.Environment;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.plangeneration.MapCell;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.GeometryElement;

/**
 * Decomposes a given area into a grid
 * @author tsmarques
 *
 */
public class GridArea extends GeometryElement implements MapDecomposition {
    private final static double CELL_WIDTH = 20;
    private double cellHeight;
    private double gridWidth;
    private double gridHeight;
    private LocationType center;

    private int nrows;
    private int ncols;
    private LocationType[] bounds;
    private MapCell[][] decomposedMap;

    /* Used to check for obstacles */
    private Environment env;

    /**
     * Used just for testing
     * */
    public GridArea(double gridWidth, double gridHeight, LocationType center) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.center = center;

        this.bounds = computeGridBounds();
    }

    public GridArea(double gridWidth, double gridHeight, LocationType center, Environment env) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.center = center;
        this.env = env;

        this.bounds = computeGridBounds();
    }

    private LocationType[] computeGridBounds() {
        LocationType topLeft = new LocationType(center);
        LocationType topRight = new LocationType(center);
        LocationType bottomLeft = new LocationType(center);
        LocationType bottomRight = new LocationType(center);

        topLeft.setOffsetWest(gridWidth/2);
        topLeft.setOffsetNorth(gridHeight/2);
        topLeft = topLeft.getNewAbsoluteLatLonDepth();

        topRight.setOffsetEast(gridWidth/2);
        topRight.setOffsetNorth(gridHeight/2);
        topRight = topRight.getNewAbsoluteLatLonDepth();

        bottomLeft.setOffsetWest(gridWidth/2);
        bottomLeft.setOffsetSouth(gridHeight/2);
        bottomLeft = bottomLeft.getNewAbsoluteLatLonDepth();

        bottomRight.setOffsetEast(gridWidth/2);
        bottomRight.setOffsetSouth(gridHeight/2);
        bottomRight = bottomRight.getNewAbsoluteLatLonDepth();

        LocationType[] gridBounds = {topLeft, topRight, bottomLeft, bottomRight};

        return gridBounds;
    }

    /**
     * Decomposes the given area in a square grid/matrix
     * */
    @Override
    public void decomposeMap() {
        /* operational area bounds */
        LocationType topLeft = bounds[0];
        LocationType topRight = bounds[1];
        LocationType bottomLeft = bounds[2];

        /* operational area dimensions (rounded to 2 decimal places) */
        gridWidth = (int) Math.ceil(topRight.getDistanceInMeters(topLeft) * 100) / 100;
        gridHeight = (int) Math.ceil(bottomLeft.getDistanceInMeters(topLeft) * 100) / 100;

        ncols = (int) (gridWidth / CELL_WIDTH);
        nrows = ncols;

        cellHeight = Math.ceil((gridHeight / nrows) * 100) / 100;

        /* compute grid size */
        decomposedMap = new MapCell[nrows][ncols];

        /* do decomposition */
        for(int i = 0; i < nrows; i ++) {
            for(int j = 0; j < ncols; j++) {
                double horizontalShift = i * CELL_WIDTH;
                double verticalShift = j * cellHeight ;
                
                LocationType cellLoc = new LocationType(topLeft);
                cellLoc.setOffsetEast(horizontalShift);
                cellLoc.setOffsetSouth(verticalShift);
                cellLoc = cellLoc.getNewAbsoluteLatLonDepth();

                /* TODO check for obstacles, using Environment */
                /* TODO set correct bounds for each map cells (set vertices) */
                decomposedMap[i][j] = new MapCell(cellLoc, false);
            }
        }
    }

    @Override
    public LocationType[] getBounds() {
        return bounds;
    }

    public MapCell[][] getAllCells() {
        return decomposedMap;
    }

    @Override
    public List<MapCell> getAreaCells() {
        List<MapCell> areaCells = new ArrayList<>();

        for(MapCell[] row : decomposedMap)
            for(MapCell cell : row)
                areaCells.add(cell);
        return areaCells;
    }

    public int getNumberOfRows() {
        return nrows;
    }

    public int getNumberOfColumns() {
        return ncols;
    }

    public double getCellWidth() {
        return CELL_WIDTH;
    }

    public double getCellHeight() {
        return cellHeight;
    }

    public double gridWidth() {
        return gridWidth;
    }

    public double gridHeight() {
        return gridHeight;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        g.setTransform(renderer.identity);
        for(int i = 0; i < nrows; i++) {
            for(int j = 0; j < ncols; j++) {
                /* compute cell's center location */
                LocationType cellCenter = new LocationType(decomposedMap[i][j].getLocation());
                cellCenter.setOffsetWest(CELL_WIDTH / 2);
                cellCenter.setOffsetNorth(cellHeight / 2);
                cellCenter = cellCenter.getNewAbsoluteLatLonDepth();

                Point2D cellPos = renderer.getScreenPosition(cellCenter);

                g.setColor(Color.BLACK);
                Rectangle2D.Double cellRec = new Rectangle2D.Double(cellPos.getX(), cellPos.getY(), CELL_WIDTH * renderer.getZoom(), cellHeight * renderer.getZoom());
                g.draw(cellRec);
            }
            g.setTransform(renderer.identity);
        }
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return AbstractElement.ELEMENT_TYPE.TYPE_PARALLELEPIPED;
    }
}
