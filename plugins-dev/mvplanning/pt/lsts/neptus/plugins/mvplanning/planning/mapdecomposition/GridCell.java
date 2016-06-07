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
 * 9 May 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;

/**
 * @author tsmarques
 *
 */
public class GridCell extends MapCell {
    private LocationType centerLoc;
    private GridCell[] neighbours;
    private int row;
    private int col;
    private int nNeighbours;

    public GridCell(LocationType centerLocation, int i, int j) {
        super(false);

        nNeighbours = 0;
        this.centerLoc = centerLocation;
        this.neighbours = new GridCell[4];
        setPosition(i, j);
    }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return col;
    }

    @Override
    public void rotate(double yaw, LocationType pivot) {
        if(!pivot.isLocationEqual(this.centerLoc)) {
            double offsets[] = pivot.getOffsetFrom(centerLoc);
            double deltas[] = AngleUtils.rotate(yaw, offsets[0], offsets[1], false);

            this.centerLoc.translatePosition(offsets[0] - deltas[0], offsets[1] - deltas[1], 0);
        }
    }

    @Override
    public void addNeighbour(MapCell neighCell) {
        if(nNeighbours < 4 && !neighCell.hasObstacle() && !this.hasObstacle()) {
            int neighRow = ((GridCell) neighCell).getRow();
            int neighCol = ((GridCell) neighCell).getColumn();

            /*Left neighbour */
            if(neighRow == this.row && neighCol == this.col - 1)
                neighbours[0] = (GridCell) neighCell;

            /* Down neighbour */
            else if(neighRow == this.row + 1 && neighCol == this.col)
                neighbours[1] = (GridCell) neighCell;

            /* Right neighbour */
            else if(neighRow == this.row && neighCol == this.col + 1)
                neighbours[2] = (GridCell) neighCell;

            /* Up neighbour */
            else if(neighRow == this.row - 1 && neighCol == this.col)
                neighbours[3] = (GridCell) neighCell;

            nNeighbours++;
        }
        else if(nNeighbours >= 4)
            NeptusLog.pub().warn("Can't add more neighbours to " + id() + " grid cell");
    }

    @Override
    public List<MapCell> getNeighbours() {
        List<MapCell> neighboursList = new ArrayList<>();

        for(MapCell neighbour : neighbours)
            if(neighbour != null)
                neighboursList.add(neighbour);

        return neighboursList;
    }

    @Override
    public List<MapCell> getNeighboursAntiClockwise(MapCell firstNeighbour) {
        List<MapCell> neighbours = getNeighbours();

        if(firstNeighbour == null)
            return neighbours;
        else {
            /* Find given node and build list starting at that position */
            int index = neighbours.indexOf(firstNeighbour);
            List<MapCell> neighboursAclk;

            if(index == -1)
                return null;
            else {
                int seenNeighbours = 0;
                neighboursAclk = new ArrayList<>();

                while(seenNeighbours < nNeighbours) {
                    neighboursAclk.add(neighbours.get(index));

                    index = (index + 1) % nNeighbours;
                    seenNeighbours++;
                }

                return neighboursAclk;
            }
        }
    }

    @Override
    public boolean isNeighbour(MapCell cell) {
        for(MapCell neighbour : neighbours)
            if(neighbour.id().equals(cell.id()))
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

    @Override
    public LocationType getLocation() {
        return this.centerLoc;
    }
}
