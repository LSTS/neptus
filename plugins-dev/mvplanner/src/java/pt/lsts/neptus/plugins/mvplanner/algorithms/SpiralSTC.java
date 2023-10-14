/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 27 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.algorithms;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the offline version of the
 * SpiralSTC's algorithm
 * @author tsmarques
 *
 */
public class SpiralSTC {
    /** Movement direction */
    private static final int UP = 1;
    private static final int DOWN = -1;
    private static final int LEFT = 2;
    private static final int RIGHT = -2;
    private static final int NONE = 0;


    private List<ManeuverLocation> path;
    private GridArea subCells; 

    public SpiralSTC(GridArea areaToCover) {
        subCells = areaToCover.splitMegaCells();
        this.path = generatePath(areaToCover);
    }

    /**
     * Get the path to traverse
     * */
    public List<ManeuverLocation> getPath() {
        return this.path;
    }

    /**
     * Generate a path using SpiralSTC algorithm
     * */
    private List<ManeuverLocation> generatePath(GridArea areaToCover) {
        SpanningTree spanningTree = new SpanningTree(areaToCover.getFirstFreeCell());

        /* Build graph */
        List<ManeuverLocation> path = new ArrayList<>();

        int previousDirection = NONE;
        boolean firstNode = true;

        GridCell currCell =  spanningTree.next();
        GridCell currSubCell = null;
        GridCell newSubCell = null;

        while(spanningTree.hasNext()) {
            if(firstNode) {
                currSubCell = computeStartSubCell(currCell, spanningTree);
                firstNode = false;
            }

            GridCell nextCell = spanningTree.next();
            int nextDir = getNextDirection(currCell, nextCell);
            newSubCell = computeNewDestination(nextDir, nextCell);

            /* can't proceed */
            if(currSubCell == null || newSubCell == null)
                return new ArrayList<>(0);

            generateTransition(path, currSubCell, newSubCell, previousDirection, nextDir);
            previousDirection = nextDir;
            currSubCell = newSubCell;

            currCell = nextCell;
        }
        /* add last subcell */
        addNewNode(path, newSubCell);
        return path;
    }

    private void generateTransition(List<ManeuverLocation> path, GridCell sourceSubCell, GridCell destSubCell, int prevDir, int nextDir) {
        if(prevDir == NONE)
            addNewNode(path, sourceSubCell);

        /* as an optimization, ignore colinear nodes */
        else if(prevDir != nextDir) {
            if(prevDir == -nextDir)
                goAroundLeafNode(path, nextDir, sourceSubCell);
            else /* changing direction */
                changeDirectionTransition(path, sourceSubCell, destSubCell, nextDir);
        }
    }

    /**
     * Generate path, between two cells, to follow spanning tree
     * when it changes direction, e.g., when the vehicle needs
     * to go from a vertical to an horizontal trajectory/path
     * */
    private void changeDirectionTransition(List<ManeuverLocation> path, GridCell sourceSubCell, GridCell destSubCell, int nextDir) {
        if(sourceSubCell.isNeighbour(destSubCell.getRow(), destSubCell.getColumn()))
            addNewNode(path, sourceSubCell);
        else {
            int cornerRow;
            int cornerCol;
            int sourceRow = sourceSubCell.getRow();
            int sourceCol = sourceSubCell.getColumn();

            if(nextDir == UP) {
                /* got to right corner */
                cornerRow = sourceRow;
                cornerCol = sourceCol + 1;
            }
            else if(nextDir == DOWN) {
                /* go to left corner */
                cornerRow = sourceRow;
                cornerCol = sourceCol - 1;
            }
            else if(nextDir == LEFT) {
                /* go to top corner */
                cornerRow = sourceRow - 1;
                cornerCol = sourceCol;
            }
            else {
                /* go to bottom corner */
                cornerRow = sourceRow + 1;
                cornerCol = sourceCol;
            }

            GridCell cornerSubCell = subCells.getCellAt(cornerRow, cornerCol);
            addNewNode(path, cornerSubCell);
        }
    }

    /**
     * Given a graph and GridCell creates a new node and adds it.
     * */
    private void addNewNode(List<ManeuverLocation> path, GridCell cell) {
        path.add(new ManeuverLocation(cell.getLocation()));
    }

    /**
     * Given an initial mega-cell compute the sub-cell inside it
     * where the vehicle will start the plan
     * */
    private GridCell computeStartSubCell(GridCell startCell, SpanningTree spanningTree) {
        int i = startCell.getRow();
        int j = startCell.getColumn();

        GridCell nextCell =  spanningTree.peekNext();
        int movementDirection = getNextDirection(startCell, nextCell);

        int subCellRow;
        int subCellCol;
        /* move to the bottom-right of the start cell */
        if(movementDirection == UP) {
            subCellRow = 2*i + 1;
            subCellCol = 2*j + 1;
        }
        /* move to the top-right of the start cell */
        else if(movementDirection == LEFT) {
            subCellRow = 2*i;
            subCellCol = 2*j + 1;
        }
        /* move to the top-left of the start cell */
        else if(movementDirection == DOWN) {
            subCellRow = 2*i;
            subCellCol = 2*j;
        }
        /* move to the bottom-left of the start cell */
        else if(movementDirection == RIGHT) {
            subCellRow = 2*i + 1;
            subCellCol = 2*j;
        }
        else {
            NeptusLog.pub().error("Couldn't compute first sub-cell");
            return null;
        }

        return subCells.getCellAt(subCellRow, subCellCol);
    }

    /**
     * Computes the next subcell(s) the vehicle is going to move into
     * */
    public GridCell computeNewDestination(int nextDir, GridCell destMegaCell) {
        if(nextDir == 0) {
            NeptusLog.pub().error("Couldn't compute direction to " + destMegaCell.getId());
            return null;
        }
        else {
            int i = destMegaCell.getRow();
            int j = destMegaCell.getColumn();

            int subCellRow;
            int subCellCol;
            /* move to the bottom-right of new subcell*/
            if(nextDir == UP) {
                subCellRow = 2*i + 1;
                subCellCol = 2*j + 1;
            }
            /* move to the top-right of the new subcell */
            else if(nextDir == LEFT) {
                subCellRow = 2*i;
                subCellCol = 2*j + 1;
            }
            /* move to the top-left of the new subcell */
            else if(nextDir == DOWN) {
                subCellRow = 2*i;
                subCellCol = 2*j;
            }
            /* move to the bottom-left of the new subcell */
            else if(nextDir == RIGHT) {
                subCellRow = 2*i + 1;
                subCellCol = 2*j;
            }
            else
                return null;

            return  subCells.getCellAt(subCellRow, subCellCol);
        }
    }

    /**
     * Generates a path that goes around a spanning tree's leaf node
     * */
    private void goAroundLeafNode(List<ManeuverLocation> path, int nextDir, GridCell currSubCell) {
        int currRow = currSubCell.getRow();
        int currCol = currSubCell.getColumn();

        if(nextDir == UP) {
            /* Move one sub-cell down */
            currRow++;
            addNewNode(path, subCells.getCellAt(currRow, currCol));

            /* Then, move one sub-cell right */
            currCol++;
            addNewNode(path, subCells.getCellAt(currRow, currCol));
        }
        else if(nextDir == DOWN) {
            /* Move one sub-cell up */
            currRow--;
            addNewNode(path, subCells.getCellAt(currRow, currCol));

            /* Then, move one sub-cell left */
            currCol--;
            addNewNode(path, subCells.getCellAt(currRow, currCol));
        }
        else if(nextDir == LEFT) {
            /* Move one sub-cell to the right */
            currCol++;
            addNewNode(path, subCells.getCellAt(currRow, currCol));

            /* Then, move one sub-cell up */
            currRow--;
            addNewNode(path, subCells.getCellAt(currRow, currCol));
        }
        else if(nextDir == RIGHT) {
            currCol--;
            addNewNode(path, subCells.getCellAt(currRow, currCol));

            /* Then, move one sub-cell down */
            currRow++;
            addNewNode(path, subCells.getCellAt(currRow, currCol));
        }
        else
            NeptusLog.pub().error("Can't go around leaf node from " + currSubCell.getId() + " sub-cell");
    }

    /**
     * Given 2 cells, the start, A, and goal, B, returns in
     * which direction the vehicle is going to travel in order to
     * go from A to B.
     * 1 is Up
     * -1 is Down
     * 2 is Left
     * -2 is Right
     * */
    private int getNextDirection(GridCell currentMegaCell, GridCell nextMegaCell) {
        int currentRow = currentMegaCell.getRow();
        int nextRow = nextMegaCell.getRow();
        int currentCol = currentMegaCell.getColumn();
        int nextCol = nextMegaCell.getColumn();

        /* Going up */
        if(nextRow == (currentRow - 1) && nextCol == currentCol)
            return UP;

        /* Going down */
        else if(nextRow == (currentRow + 1) && nextCol == currentCol)
            return DOWN;

        /* Going left */
        else if(nextCol == (currentCol - 1) && nextRow == currentRow)
            return LEFT;

        /* Going right */
        else if(nextCol == (currentCol + 1) && nextRow == currentRow)
            return RIGHT;
        else
            return 0;
    }
}
