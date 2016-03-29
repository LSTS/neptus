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

import java.util.ArrayList;
import java.util.List;


import pt.lsts.neptus.plugins.mvplanning.Environment;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.plangeneration.MapCell;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Decomposes a given area into a grid
 * @author tsmarques
 *
 */
public class GridArea implements MapDecomposition {
    private final static double CELL_WIDTH = 20;
    private double cellHeight; 

    private int nrows;
    private int ncols;
    private LocationType[] bounds;
    private MapCell[][] decomposedMap;

    /* Used to check for obstacles */
    private Environment env;

    /**
     * Used just for testing
     * */
    public GridArea() {

    }

    public GridArea(Environment env) {
        this.env = env;
    }

    /**
     * Decomposes the given area in a square grid/matrix
     * */
    @Override
    public void decomposeMap(LocationType[] bounds) {
        this.bounds = bounds;

        /* operational area bounds */
        LocationType topLeft = bounds[0];
        LocationType topRight = bounds[1];
        LocationType bottomLeft = bounds[2];

        /* operational area dimensions (rounded to 1 decimal place) */
        double areaWidth = (int) Math.ceil(topRight.getDistanceInMeters(topLeft) * 10) / 10;
        double areaHeight = (int) Math.ceil(bottomLeft.getDistanceInMeters(topLeft) * 10) / 10;

        ncols = (int) (areaWidth / CELL_WIDTH);
        nrows = ncols;

        cellHeight = Math.ceil((areaHeight / nrows) * 10) / 10;

        /* compute grid size */
        decomposedMap = new MapCell[nrows][ncols];

        /* do decomposition */
        for(int i = 0; i < nrows; i ++) {
            for(int j = 0; j < ncols; j++) {
                double horizontalShift = i * CELL_WIDTH;
                double verticalShift = j * cellHeight ;
                
                LocationType cellLoc = new LocationType(topLeft);
                cellLoc.setOffsetWest(horizontalShift);
                cellLoc.setOffsetSouth(verticalShift);
                
                decomposedMap[i][j] = new MapCell(cellLoc, false);
            }
        }
    }

    @Override
    public LocationType[] getBounds() {
        return bounds;
    }

    /* TODO: Check only neighbours of cells
     * that are within the given area, instead of all cells*/
    @Override
    public List<MapCell> getAreaCells(LocationType[] mapArea) {
        List<MapCell> areaCells = new ArrayList<>();

        for(MapCell[] row : decomposedMap)
            for(MapCell cell : row)
                if(cellWithinArea(cell.getLocation(), mapArea))
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

    /**
     * Returns if a given cell is within the bounds of the given area
     * */
    private boolean cellWithinArea(LocationType cellLocation, LocationType[] mapArea) {
        return false;
    }
}
