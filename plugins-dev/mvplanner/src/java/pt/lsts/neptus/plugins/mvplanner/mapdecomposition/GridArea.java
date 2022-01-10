/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.plugins.mvplanner.mapdecomposition;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

public class GridArea {
    private PolygonType polygon;

    private int cellWidth;
    private int cellHeight;

    private double gridWidth;
    private double gridHeight;
    private double yawRads;

    private LocationType center;

    private int nrows;
    private int ncols;
    private GridCell[][] grid;

    // rectangle bounds
    private LocationType topLeft = null;
    private LocationType topRight = null;
    private LocationType bottomLeft = null;
    private LocationType bottomRight = null;

    public GridArea(PolygonType mapObject) {
        polygon = mapObject;
        this.cellWidth = 0;
        this.cellHeight = this.cellWidth;

        computeAreaDimensions();
    }

    public GridArea(PolygonType mapObject, int cellWidth) {
        polygon = mapObject;
        this.cellWidth = cellWidth;
        this.cellHeight = this.cellWidth;

        computeAreaDimensions();

        this.nrows = (int) Math.ceil(gridHeight / cellHeight);
        this.ncols = (int) Math.ceil(gridWidth / cellWidth);
        grid = new GridCell[nrows][ncols];
        decompose();
    }

    /**
     * Copy constructor.
     * Build a GridArea based on another's grid
     * */
    private GridArea(GridCell[][] cells, int cellW, int cellH, LocationType center) {
        this.grid = cells;
        this.cellWidth = cellW;
        this.cellHeight = cellH;
        this.nrows = cells.length;
        this.ncols = cells[0].length;
        this.center = new LocationType(center);
    }

    public GridCell getCellAt(int row, int col) {
        return grid[row][col];
    }

    public GridCell[][] getGrid() {
        return grid;
    }

    public int getNrows() {
        return nrows;
    }

    public int getNcols() {
        return  ncols;
    }

    public double getWidth() {
        return gridWidth;
    }

    public double getHeight() {
        return gridHeight;
    }

    public double getYawRads() {
        return yawRads;
    }

    public void setYawRads(double yawRads) {
        this.yawRads = yawRads;
    }

    /**
     * Set if the given cell has an obstacle or not
     * */
    public void setObstacleAt(boolean value, int row, int col) {
        grid[row][col].setHasObstacle(value);
    }


    /**
     * Get the first cell without an obstacle,
     * starting the search at (0,0)
     * */
    public GridCell getFirstFreeCell() {
        for(GridCell[] row : grid)
            for(GridCell cell : row)
                if(!cell.hasObstacle())
                    return cell;

        return null;
    }

    /**
     * Decompose area in a grid with cells' size
     * of cellWidth x cellWidth.
     * If cells' width is <= 0 then nothing happens
     * */
    private void decompose() {
        if(this.cellWidth <= 0)
            return;

        int id = 0;
        for(int i = 0; i < nrows; i++) {
            for(int j = 0; j < ncols; j++) {
                double horizontalShift = (j * cellWidth) + cellWidth / 2;
                double verticalShift = (i * cellHeight) + cellHeight / 2;

                LocationType cellLoc = new LocationType(topLeft);
                cellLoc.translatePosition(-verticalShift, horizontalShift, 0);

                grid[i][j] = new GridCell(Integer.toString(id), cellLoc, i, j, cellWidth, cellHeight);
                grid[i][j].rotate(getYawRads(), topLeft);

                // set neighbour relation
                if(i != 0) {
                    grid[i][j].addNeighbour(grid[i-1][j]);
                    grid[i-1][j].addNeighbour(grid[i][j]);
                }

                if(j != 0) {
                    grid[i][j].addNeighbour(grid[i][j-1]);
                    grid[i][j-1].addNeighbour(grid[i][j]);
                }
                id++;
            }
        }
    }

    public void recomputeDimensions(PolygonType polygon) {
        this.polygon = polygon;
        computeAreaDimensions();
    }

    /**
     * Computes the bounding rectangle' paramaters
     * of the polygon that represents this area.
     *
     * TODO work with non-convex polygons
     * */
    private void computeAreaDimensions() {
        List<PolygonType.Vertex> vertices = polygon.getVertices();

        PolygonType.Vertex minLatV = vertices.get(0);
        PolygonType.Vertex maxLatV = vertices.get(0);
        PolygonType.Vertex minLonV = vertices.get(0);
        PolygonType.Vertex maxLonV = vertices.get(0);

        for(int i = 1; i < vertices.size(); i++) {
            PolygonType.Vertex currentV = vertices.get(i);
            if(currentV.getLatitudeDegs() < minLatV.getLatitudeDegs())
                minLatV = currentV;
            else if(currentV.getLatitudeDegs() > maxLatV.getLatitudeDegs())
                maxLatV = currentV;

            if(currentV.getLongitudeDegs() < minLonV.getLongitudeDegs())
                minLonV = currentV;
            else if(currentV.getLongitudeDegs() > maxLonV.getLongitudeDegs())
                maxLonV = currentV;
        }

        // area vertices
        topLeft = new LocationType(maxLatV.getLatitudeDegs(), minLonV.getLongitudeDegs());
        topRight = new LocationType(maxLatV.getLatitudeDegs(), maxLonV.getLongitudeDegs());

        bottomLeft = new LocationType(minLatV.getLatitudeDegs(), minLonV.getLongitudeDegs());
        bottomRight = new LocationType(minLatV.getLatitudeDegs(), maxLonV.getLongitudeDegs());

        // area dimensions
        gridWidth = topRight.getDistanceInMeters(topLeft);
        gridHeight = topRight.getDistanceInMeters(bottomRight);

        center = CoordinateUtil.computeLocationsCentroid(Arrays.asList(
                new LocationType[]{topLeft, topRight, bottomLeft, bottomRight}));
    }

    /**
     * Makes a new grid where each (old) cell is
     * split into 4 cells, i.e. increases grid's
     * resolution.
     * */
    public GridArea splitMegaCells() {
        int newRows = 2 * nrows;
        int newCols = 2 * ncols;
        int newCellWidth = Math.round(cellWidth / 2);
        int newCellHeight = Math.round(cellHeight / 2);

        GridCell[][] newGrid = new GridCell[newRows][newCols];

        for(int i = 0; i < nrows; i++) {
            for(int j = 0; j < ncols; j++) {
                GridCell[] subCells = grid[i][j].splitCell();

                newGrid[2*i][2*j] = subCells[0];
                newGrid[2*i][2*j + 1] = subCells[1];
                newGrid[2*i + 1][2*j] = subCells[2];
                newGrid[2*i + 1][2*j + 1] = subCells[3];
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

        return new GridArea(newGrid, newCellWidth, newCellHeight, this.center);
    }


    public LocationType getCenterLocation() {
        return center;
    }

    public void paint(Graphics2D g, StateRenderer2D source, Color color, boolean paintGrid, boolean filled) {
        if(paintGrid)
            displayGrid(g, source, color);
        else
            displayArea(g, source, color, filled);
    }

    private void displayArea(Graphics2D g, StateRenderer2D source, Color color, boolean filled) {
        Graphics2D g2 = (Graphics2D) g.create();
        Point2D p = source.getScreenPosition(this.center);
        double scale = source.getZoom();
        int w = (int) (this.gridWidth * scale);
        int h = (int) (this.gridHeight * scale);

        AffineTransform transform = new AffineTransform();
        transform.translate(p.getX(), p.getY());
        transform.rotate(getYawRads() - source.getRotation());
        g2.transform(transform);

        g2.setColor(color);
        if(filled) {
            g2.fillRect(-w / 2, -h / 2, w, h);
            g2.setColor(Color.BLACK);
        }

        g2.drawRect(-w / 2, -h / 2, w, h);

        int radius = 10;
        g2.setColor(Color.green);
        g2.fillOval(-w/2, -h/2,  radius, radius);
    }

    private void displayGrid(Graphics2D g, StateRenderer2D source, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);

        g2.transform(new AffineTransform());
        double yaw = getYawRads() - source.getRotation();

        for(GridCell[] row : grid) {
            for(GridCell cell : row) {
                Point2D pt = source.getScreenPosition(cell.getLocation());
                g.translate(pt.getX(), pt.getY());
                g.rotate(yaw);

                double widthScaled = cellWidth * source.getZoom();
                double lengthScaled = cellHeight * source.getZoom();

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
}
