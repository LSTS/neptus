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
 * Author: tsm
 * 19 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.mapdecomposition;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;

import java.util.ArrayList;
import java.util.List;

public class GridCell {
    private String id;
    private boolean hasObstacle;
    private LocationType centerLocation;

    private double width;
    private double height;
    private double yawRads;

    private GridCell[] neighbours;
    private int nNeighbours;
    private int row;
    private int col;

    /** If this cell is a mega cell */
    private GridCell[] subCells;
    private int nSubCells;

    public GridCell(String id, LocationType center, int row, int col, double w, double h) {
        this.id = id;
        this.hasObstacle = false;
        centerLocation = center;

        this.row = row;
        this.col = col;

        width = w;
        height = h;
        this.yawRads = 0;

        neighbours = new GridCell[4];
        nNeighbours = 0;

        subCells = new GridCell[4];
        nSubCells = 0;
    }

    /**
     * Set this cell's id
     * */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set this cell as having an obstacle
     * */
    public void setHasObstacle(boolean value) {
        this.hasObstacle = true;
    }

    /**
     * Returns if this cell has an obstacle
     * */
    public boolean hasObstacle() {
        return hasObstacle;
    }

    /**
     * Get this cell's id
     * */
    public String getId() {
        return id;
    }

    /**
     * Get this cell's width
     * */
    public double getWidth() {
        return width;
    }

    /**
     * Get this cell's height
     * */
    public double getHeight() {
        return height;
    }

    /**
     * Get this cell's yaw radians
     * */
    public double getYawInRadian() {
        return yawRads;
    }

    /**
     * Split this cell in 4 sub-cells
     * */
    public GridCell[] splitCell() {
        double newCellWidth = width / 2;
        double newCellHeight = height / 2;

        /* Compute 4 new cells' center locations */
        LocationType topLeft = new LocationType(centerLocation);
        topLeft.translatePosition(newCellHeight/2, -newCellWidth/2, 0);

        LocationType topRight = new LocationType(centerLocation);
        topRight.translatePosition(newCellHeight/2, newCellWidth/2, 0);

        LocationType bottomLeft = new LocationType(centerLocation);
        bottomLeft.translatePosition(-newCellHeight/2, -newCellWidth/2, 0);

        LocationType bottomRight = new LocationType(centerLocation);
        bottomRight.translatePosition(-newCellHeight/2, newCellWidth/2, 0);

        GridCell topLeftCell = new GridCell("Top left", topLeft, 2*row, 2*col,newCellWidth, newCellHeight);
        GridCell topRightCell = new GridCell("Top right", topRight, 2*row, 2*col + 1, newCellWidth, newCellHeight);
        GridCell bottomLeftCell = new GridCell("Bottom left", bottomLeft, 2*row + 1, 2*col, newCellWidth, newCellHeight);
        GridCell bottomRightCell = new GridCell("Bottom right", bottomRight, 2*row + 1, 2*col + 1, newCellWidth, newCellHeight);

        /* rotate cells into position */
        topLeftCell.rotate(yawRads, centerLocation);
        topRightCell.rotate(yawRads, centerLocation);
        bottomLeftCell.rotate(yawRads, centerLocation);
        bottomRightCell.rotate(yawRads, centerLocation);

        subCells[nSubCells++] = topLeftCell;
        subCells[nSubCells++] = topRightCell;
        subCells[nSubCells++] = bottomLeftCell;
        subCells[nSubCells++] = bottomRightCell;

        return this.subCells;
    }

    /**
     * Rotate this cell by yaw radians arount the
     * given pivot location
     * */
    public void rotate(double yawRads, LocationType pivot) {
        if(!pivot.isLocationEqual(centerLocation)) {
            this.yawRads += yawRads;
            double offsets[] = pivot.getOffsetFrom(centerLocation);
            double deltas[] = AngleUtils.rotate(yawRads, offsets[0], offsets[1], false);

            centerLocation.translatePosition(offsets[0] - deltas[0], offsets[1] - deltas[1], 0);
        }
    }
    /**
     * Returns this cell's row in
     * its GridArea
     * */
    public int getRow() {
        return row;
    }

    /**
     * Returns this cell's column in
     * its GridArea
     * */
    public int getColumn() {
        return col;
    }

    /**
     * Adds a neighbour of this cell. If it already has
     * 4 neighbours, it will be ignored.
     * */
    public void addNeighbour(GridCell neighCell) {
        if(nNeighbours >= 4) {
            NeptusLog.pub().warn("Trying to add more tha 4 neighbours!");
            return;
        }

        int neighRow = neighCell.getRow();
        int neighCol = neighCell.getColumn();

            /*Left neighbour */
        if(neighRow == this.row && neighCol == this.col - 1)
            neighbours[0] = neighCell;

            /* Down neighbour */
        else if(neighRow == this.row + 1 && neighCol == this.col)
            neighbours[1] = neighCell;

            /* Right neighbour */
        else if(neighRow == this.row && neighCol == this.col + 1)
            neighbours[2] = neighCell;

            /* Up neighbour */
        else if(neighRow == this.row - 1 && neighCol == this.col)
            neighbours[3] = neighCell;

        nNeighbours++;
    }

    /**
     * Get this cell's neighbours as list
     * */
    public List<GridCell> getNeighboursList() {
        List<GridCell> neighboursList = new ArrayList<>();
        for(GridCell n : neighbours) {
            if(n != null)
                neighboursList.add(n);
        }
        return neighboursList;
    }

    /**
     * Returns if this and the given cell are
     * neighbours
     * */
    public boolean isNeighbour(GridCell cell) {
        for(GridCell neighbour : neighbours)
            if(neighbour.getId().equals(cell.getId()))
                return true;
        return false;
    }

    /**
     * Given the position of a cell, in a grid, return if
     * this cell its is neighbour
     * */
    public boolean isNeighbour(int row, int col) {
        return (Math.abs((this.row - row)) <= 1) && (Math.abs((this.col - col)) <= 1);
    }


    /**
     * Returns this cell's center location
     * */
    public LocationType getLocation() {
        return this.centerLocation;
    }
}
