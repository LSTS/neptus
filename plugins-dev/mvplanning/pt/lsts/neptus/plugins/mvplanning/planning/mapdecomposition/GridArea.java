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

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.mvplanning.Environment;
import pt.lsts.neptus.plugins.mvplanning.exceptions.SafePathNotFoundException;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.planning.algorithm.Astar;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.GeometryElement;
import pt.lsts.neptus.util.AngleUtils;

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
    private LocationType topLeft;
    private GridCell[][] decomposedMap;

    /* Used to check for obstacles */
    private Environment env;

    /**
     * Used just for testing
     * */
    public GridArea(double cellWidth, double gridWidth, double gridHeight, double yawRad, LocationType center) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellWidth;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.setYaw(yawRad);
        this.center = center;

        /* GeomteryElement's properties */
        setCenterLocation(center);
        setWidth(gridWidth);
        setHeight(gridHeight);

        this.topLeft = computeTopLeftLocation();
        decomposeMap();
    }

    public GridArea(double cellWidth, double gridWidth, double gridHeight, double yawRad, LocationType center, Environment env) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellWidth;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.setYaw(yawRad);
        this.center = center;
        this.env = env;

        /* GeomteryElement's properties */
        setCenterLocation(center);
        setWidth(gridWidth);
        setHeight(gridHeight);

        this.topLeft = computeTopLeftLocation();
        decomposeMap();
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

        /* GeomteryElement's properties */
        setCenterLocation(center);
        setWidth(gridWidth);
        setHeight(gridHeight);
    }

    private LocationType computeTopLeftLocation() {
        LocationType topLeft = new LocationType(center);

        topLeft.setOffsetWest(gridWidth/2);
        topLeft.setOffsetNorth(gridHeight/2);

        /* Rotate this location aroun the area's center */
        double offsets[] = center.getOffsetFrom(topLeft);
        double deltas[] = AngleUtils.rotate(getYaw(), offsets[0], offsets[1], false);

        topLeft.translatePosition(offsets[0] - deltas[0], offsets[1] - deltas[1], 0);

        return topLeft;
    }

    /**
     * Decomposes the given area in a square grid/matrix
     * */
    @Override
    public void decomposeMap() {
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

                decomposedMap[i][j] = new GridCell(cellLoc, i, j);
                decomposedMap[i][j].setId("" + nodeId);
                decomposedMap[i][j].rotate(getYaw(), topLeft);

                /* check for obstacles */
                if(env != null) {
                    boolean hasObstacle = env.areaHasObstacle(this.topLeft, decomposedMap[i][j].getLocation(), cellWidth, cellHeight, getYaw());
                    decomposedMap[i][j].setHasObstacle(hasObstacle);
                }

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
                newGrid[2*i][2*j] = new GridCell(topLeft, 2*i, 2*j);
                newGrid[2*i][2*j + 1] = new GridCell(topRight, 2*i, 2*j+1);
                newGrid[2*i + 1][2*j] = new GridCell(bottomLeft, 2*i + 1, 2*j);
                newGrid[2*i + 1][2*j + 1] = new GridCell(bottomRight, 2*i + 1, 2*j + 1);

                /* rotate cells into position */
                newGrid[2*i][2*j].rotate(getYaw(), cellCenter);
                newGrid[2*i][2*j + 1].rotate(getYaw(), cellCenter);
                newGrid[2*i + 1][2*j].rotate(getYaw(), cellCenter);
                newGrid[2*i + 1][2*j + 1].rotate(getYaw(), cellCenter);

                decomposedMap[i][j].addSubCell(newGrid[2*i][2*j]);
                decomposedMap[i][j].addSubCell(newGrid[2*i][2*j + 1]);
                decomposedMap[i][j].addSubCell(newGrid[2*i + 1][2*j]);
                decomposedMap[i][j].addSubCell(newGrid[2*i + 1][2*j + 1]);
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
        newGridArea.setTopLeftLocation(this.topLeft);

        return newGridArea;
    }

    public void setTopLeftLocation(LocationType topLeft) {
        this.topLeft = topLeft;
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

    /**
     * Given a location, inside this grid, returns the closest
     * cell to it, or null otherwise.
     * */
    public GridCell getClosestCell(LocationType loc) {
        double min = Double.MAX_VALUE;
        GridCell closestCell = null;
        for(GridCell[] row : decomposedMap) {
            for(GridCell cell : row) {
                double dist = cell.getLocation().getDistanceInMeters(loc);

                if(dist < min) {
                    min = dist;
                    closestCell = cell;
                }
            }
        }

        if(min > cellWidth)
            closestCell = null;
        return closestCell;
    }

    /**
     * Returns the shortest path between two given cells
     * @throws SafePathNotFoundException
     * */
    public List<ManeuverLocation> getShortestPath(LocationType startLoc, LocationType endLoc) throws SafePathNotFoundException {
        MapCell start = this.getClosestCell(startLoc);
        MapCell end = this.getClosestCell(endLoc);
        List<ManeuverLocation> path;

        if(start == null || end == null || (path = new Astar().computeShortestPath(start, end)).isEmpty())
            throw new SafePathNotFoundException();

        return path;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        g.transform(new AffineTransform());
        double yaw = getYaw() - renderer.getRotation();

        for(MapCell[] row : decomposedMap) {
            for(MapCell cell : row) {
                Point2D pt = renderer.getScreenPosition(cell.getLocation());
                g.translate(pt.getX(), pt.getY());
                g.rotate(yaw);

                double widthScaled = cellWidth * renderer.getZoom();
                double lengthScaled = cellHeight * renderer.getZoom();

                Rectangle2D.Double cellRec = new Rectangle2D.Double(-widthScaled / 2, -lengthScaled / 2, widthScaled, lengthScaled);

                if(cell.hasObstacle())
                    g.fill(cellRec);
                else
                    g.draw(cellRec);

                g.rotate(-yaw);
                g.translate(-pt.getX(), -pt.getY());
            }
        }
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return AbstractElement.ELEMENT_TYPE.TYPE_PARALLELEPIPED;
    }
}
