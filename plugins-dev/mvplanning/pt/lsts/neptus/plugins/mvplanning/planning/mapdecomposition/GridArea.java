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
package pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition;


import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.plugins.mvplanning.Environment;
import pt.lsts.neptus.plugins.mvplanning.planning.GridCell;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.GeometryElement;

/**
 * Decomposes a given area into a grid
 * @author tsmarques
 *
 */
/**
 * @author tsmarques
 *
 */
public class GridArea extends GeometryElement implements MapDecomposition {
    private double cellWidth;
    private double cellHeight;
    private double gridWidth;
    private double gridHeight;
    private LocationType center;

    private int nrows;
    private int ncols;
    private LocationType[] bounds;
    private GridCell[][] decomposedMap;

    /* Used to check for obstacles */
    private Environment env;

    /**
     * Used just for testing
     * */
    public GridArea(double cellWidth, double gridWidth, double gridHeight, LocationType center) {
        this.cellWidth = cellWidth;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.center = center;

        setWidth(gridWidth);
        setHeight(gridHeight);

        this.bounds = computeGridBounds();
    }

    public GridArea(double cellWidth, double gridWidth, double gridHeight, LocationType center, Environment env) {
        this.cellWidth = cellWidth;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.center = center;
        this.env = env;

        setWidth(gridWidth);
        setHeight(gridHeight);
    }

    /**
     * Used when splitting a GridArea or its cells
     * */
    public GridArea(GridCell[][] cells, double cellWidth, double cellHeight, int nrows, int ncols, LocationType center, Environment env) {
        this.decomposedMap = cells;
        this.nrows = nrows;
        this.ncols = ncols;
        this.center = center;

        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.gridWidth = cellWidth * ncols;
        this.gridHeight = cellHeight * nrows;

        setWidth(gridWidth);
        setHeight(gridHeight);
    }

    /**
     * From the center location and grid's height and width
     * compute its vertices' locations
     * */
    private LocationType[] computeGridBounds() {
        LocationType topLeft = new LocationType(center);
        LocationType topRight = new LocationType(center);
        LocationType bottomLeft = new LocationType(center);
        LocationType bottomRight = new LocationType(center);

        topLeft.setOffsetWest(gridWidth/2);
        topLeft.setOffsetNorth(gridHeight/2);

        topRight.setOffsetEast(gridWidth/2);
        topRight.setOffsetNorth(gridHeight/2);

        bottomLeft.setOffsetWest(gridWidth/2);
        bottomLeft.setOffsetSouth(gridHeight/2);

        bottomRight.setOffsetEast(gridWidth/2);
        bottomRight.setOffsetSouth(gridHeight/2);

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
        cellHeight = (int) Math.ceil(cellWidth * (gridHeight / gridWidth));

        ncols = (int) Math.ceil(gridWidth / cellWidth);
        nrows = (int) Math.ceil(gridHeight / cellHeight);

        /* compute grid size */
        decomposedMap = new GridCell[nrows][ncols];

        int nodeId = 0;
        /* do decomposition */
        for(int i = 0; i < nrows; i ++) {
            for(int j = 0; j < ncols; j++) {
                double horizontalShift = (j * cellWidth) + cellWidth / 2;
                double verticalShift = (i * cellHeight) + cellHeight / 2;

                LocationType cellLoc = new LocationType(topLeft);
                cellLoc.translatePosition(-verticalShift, horizontalShift, 0);

                /* TODO check for obstacles, using Environment */
                /* TODO set correct bounds for each map cells (set vertices) */
                decomposedMap[i][j] = new GridCell(cellLoc, i, j, false);
                decomposedMap[i][j].setId("" + nodeId);
                nodeId++;

                /* neighbour cells */

                if(i != 0) {
                    /* cell above me is my neighbour,
                     * and I'm neighbour its */
                    decomposedMap[i][j].addNeighbour(decomposedMap[i-1][j]);
                    decomposedMap[i-1][j].addNeighbour(decomposedMap[i][j]);
                }

                if(j != 0) {
                    /* the same for the cell on my left */
                    decomposedMap[i][j].addNeighbour(decomposedMap[i][j-1]);
                    decomposedMap[i][j-1].addNeighbour(decomposedMap[i][j]);
                }
            }
        }
    }

    @Override
    public MapDecomposition[] split(int n) {
        if((n == 0 || n == 1) || n > nrows)
            return null;
        else {
            MapDecomposition[] parts = new GridArea[n];

            for(int i = 0; i < n; i++) {
                int newRows = nrows / n;

                if((i == n-1)) /* in case matrix is not square, last part is bigger*/
                    newRows += (nrows % n);

                GridCell[][] newCells = subsetGrid(i * newRows, newRows, ncols);
                /* TODO set new center location */
                parts[i] = new GridArea(newCells, cellWidth, cellHeight, newRows, ncols, center, env);
            }
            return parts;
        }
    }

    /**
     * Get a nrows by ncols submatrix of the current matrix,
     * starting at startRow
     * */
    private GridCell[][] subsetGrid(int startRow, int nrows, int ncols) {
        GridCell[][] newGrid = new GridCell[nrows][ncols];
        for(int i = startRow; i < nrows; i++)
            newGrid[i] = decomposedMap[i].clone();
        return newGrid;
    }

    /**
     * Makes a new grid where each (old) cell is
     * split into 4 cells, i.e. increases grid's
     * resolution.
     * */
    public GridArea splitMegaCells() {
        int newRows = 2 * nrows;
        int newCols = 2 * ncols;
        double newCellWidth = cellWidth / 2;
        double newCellHeight = cellHeight / 2;

        GridCell[][] newGrid = new GridCell[newRows][newCols];

        for(int i = 0; i < nrows; i++) {
            for(int j = 0; j < ncols; j++) {
                LocationType cellCenter = decomposedMap[i][j].getLocation();

                /* Compute 4 new cells' center locations */
                LocationType topLeft = new LocationType(cellCenter);
                topLeft.translatePosition(newCellHeight/2, -newCellWidth/2, 0);

                LocationType topRight = new LocationType(cellCenter);
                topRight.translatePosition(newCellHeight/2, newCellWidth/2, 0);

                LocationType bottomLeft = new LocationType(cellCenter);
                bottomLeft.translatePosition(-newCellHeight/2, -newCellWidth/2, 0);

                LocationType bottomRight = new LocationType(cellCenter);
                bottomRight.translatePosition(-newCellHeight/2, newCellWidth/2, 0);

                /* TODO check if cells have an obstacle */
                newGrid[2*i][2*j] = new GridCell(topLeft, 2*i, 2*j, false);
                newGrid[2*i][2*j + 1] = new GridCell(topRight, 2*i, 2*j+1, false);
                newGrid[2*i + 1][2*j] = new GridCell(bottomLeft, 2*i + 1, 2*j, false);
                newGrid[2*i + 1][2*j + 1] = new GridCell(bottomRight, 2*i + 1, 2*j + 1, false);
            }
        }
        /* Set cells' id */
        /* TODO improve */
        int id = 0;
        for(int i = 0; i < newRows; i++)
            for(int j = 0; j < newCols; j++) {
                newGrid[i][j].setId("" + id);
                id++;
            }

        GridArea newGridArea = new GridArea(newGrid, newCellWidth, newCellHeight, newRows, newCols, this.center, this.env);
        newGridArea.setBounds(this.bounds);

        return newGridArea;
    }

    public void setBounds(LocationType[] bounds) {
        this.bounds = bounds;
    }

    @Override
    public LocationType[] getBounds() {
        return bounds;
    }

    /**
     * Returns the matrix representation of this grid
     * */
    public GridCell[][] getAllCells() {
        return decomposedMap;
    }

    @Override
    public List<MapCell> getAreaCells() {
        List<MapCell> areaCells = new ArrayList<>();

        for(GridCell[] row : decomposedMap)
            for(GridCell cell : row)
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
        return cellWidth;
    }

    public double getCellHeight() {
        return cellHeight;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        g.setTransform(new AffineTransform());
        double scaledWidth = cellWidth * renderer.getZoom();
        double scaledHeight = cellHeight * renderer.getZoom();
        Point2D topLeftP = renderer.getScreenPosition(bounds[0]);

        Rectangle2D.Double cellRec = new Rectangle2D.Double(0, 0, scaledWidth, scaledHeight);

        for(int i = 0; i < nrows; i++) {
            for(int j = 0; j < ncols; j++) {
                Graphics2D g2 = (Graphics2D) g.create();
                double verticalShift = scaledHeight * i;
                double horizontalShift = scaledWidth * j;

                g2.translate(topLeftP.getX() + horizontalShift, topLeftP.getY() + verticalShift);

                if(decomposedMap[i][j].hasObstacle())
                    g2.fill(cellRec);
                else
                    g2.draw(cellRec);

                g2.dispose();
            }
            g.setTransform(new AffineTransform());
        }
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return AbstractElement.ELEMENT_TYPE.TYPE_PARALLELEPIPED;
    }
}
