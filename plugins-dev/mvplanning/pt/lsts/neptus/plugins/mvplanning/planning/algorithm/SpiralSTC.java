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
 * 6 May 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning.algorithm;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.planning.GridCell;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;

/**
 * Implementation of the offline version of the
 * SpiralSTC's algorithm
 * @author tsmarques
 *
 */
public class SpiralSTC {
    private static final int UP = 1;
    private static final int DOWN = -1;
    private static final int LEFT = 2;
    private static final int RIGHT = -2;
    private static final int NONE = 0;

    private MST minSpanningTree;
    private List<ManeuverLocation> path;

    public SpiralSTC(GridArea areaToCover) {
        this.minSpanningTree = new MST(areaToCover.getAreaCells().get(0));
        this.path = generatePath(areaToCover);
    }

    public List<ManeuverLocation> getPath() {
        return this.path;
    }

    private List<ManeuverLocation> generatePath(GridArea areaToCover) {
        /* Build graph */
        List<ManeuverLocation> path = new ArrayList<>();
        GridArea subCells = areaToCover.splitMegaCells();

        GridCell previousSubCell = null;
        GridCell previousCell = null;
        int previousDirection = 0;
        boolean firstNode = true;
        for(MapCell node : minSpanningTree.getNodeSequence()) {
            GridCell newSubCell;
            if(firstNode) {
                newSubCell = computeStartSubCell((GridCell) node, minSpanningTree, subCells);

                addNewNode(path, newSubCell);
                firstNode = false;
            }
            else {
                /* Direction from previous mega-cell to the current one */
                int nextDir = getNextDirection(previousCell, (GridCell) node);
                newSubCell = computeNewDestination(path, nextDir, previousDirection, previousSubCell, previousCell, (GridCell) node, subCells);

                generateTransition(path, previousSubCell, newSubCell, subCells, previousDirection, nextDir);
                previousDirection = nextDir;
            }
            previousSubCell = newSubCell;
            previousCell = (GridCell) node;
        }
        return path;
    }

    private void generateTransition(List<ManeuverLocation> path, GridCell sourceSubCell, GridCell destSubCell, GridArea subCells, int prevDir, int nextDir) {
        /* Maintaining same direction from previous movement (Normal, linear, transition) */
        if((prevDir == nextDir) || (prevDir == NONE)) {
            addNewNode(path, sourceSubCell);
            addNewNode(path, destSubCell);
        }
        else if(prevDir == -nextDir) {
            goAroundLeafNode(path, nextDir, sourceSubCell, subCells);
        }
        else /* changing direction */
            changeDirectionTransition(path, sourceSubCell, destSubCell, subCells, prevDir, nextDir);
    }

    /**
     * Generate path, between two cells, to follow spanning tree
     * when it changes direction, e.g., when the vehicle needs
     * to go from a vertical to an horizontal trajectory/path
     * */
    private void changeDirectionTransition(List<ManeuverLocation> path, GridCell sourceSubCell, GridCell destSubCell, GridArea subCells, int prevDir, int nextDir) {
        if(sourceSubCell.isNeighbour(destSubCell.getRow(), destSubCell.getColumn())) {
            addNewNode(path, sourceSubCell);
            addNewNode(path, destSubCell);
        }
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

            GridCell cornerSubCell = subCells.getAllCells()[cornerRow][cornerCol];
            addNewNode(path, sourceSubCell);
            addNewNode(path, cornerSubCell);
            addNewNode(path, destSubCell);
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
    private GridCell computeStartSubCell(GridCell startCell, MST minSpanningTree, GridArea subCells) {
        int i = startCell.getRow();
        int j = startCell.getColumn();

        GridCell nextCell = (GridCell) minSpanningTree.getNodeSequence().get(1);
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

        return subCells.getAllCells()[subCellRow][subCellCol];
    }

    /**
     * Computes the next subcell(s) the vehicle is going to move into
     * */
    public GridCell computeNewDestination(List<ManeuverLocation> path, int nextDir, int prevDir, GridCell currSubCell, GridCell sourceMegaCell, GridCell destMegaCell, GridArea subCells) {
        if(nextDir == 0) {
            NeptusLog.pub().error("Couldn't compute direction from " + sourceMegaCell.id() + " to " + destMegaCell.id());

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

            return (GridCell) subCells.getAllCells()[subCellRow][subCellCol];
        }
    }

    /**
     * Generates a path that goes around a spanning tree's leaf node
     * */
    private void goAroundLeafNode(List<ManeuverLocation> path, int nextDir, GridCell currSubCell, GridArea subCells) {
        if(nextDir == UP)
            goAroundDown(path, currSubCell, subCells);
        else if(nextDir == DOWN)
            goAroundUp(path, currSubCell, subCells);
        else if(nextDir == LEFT)
            goAroundRight(path, currSubCell, subCells);
        else if(nextDir == RIGHT)
            goAroundLeft(path, currSubCell, subCells);
        else
            NeptusLog.pub().error("Can't go around leaf node from " + currSubCell.id() + " sub-cell");
    }

    private void goAroundUp(List<ManeuverLocation> path, GridCell currSubCell, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        int currRow = currSubCell.getRow();
        int currCol = currSubCell.getColumn();

        /* Move one sub-cell to the up */
        currRow--;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move one sub-cell left */
        currCol--;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move 2 sub-cells down */
        currRow++;
        addNewNode(path, cells[currRow][currCol]);
        currRow++;
        addNewNode(path, cells[currRow][currCol]);
    }

    private void goAroundDown(List<ManeuverLocation> path, GridCell currSubCell, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        int currRow = currSubCell.getRow();
        int currCol = currSubCell.getColumn();

        /* Move one sub-cell to the down */
        currRow++;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move one sub-cell right */
        currCol++;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move 2 sub-cells up */
        currRow--;
        addNewNode(path, cells[currRow][currCol]);
        currRow--;
        addNewNode(path, cells[currRow][currCol]);
    }

    private void goAroundLeft(List<ManeuverLocation> path, GridCell currSubCell, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        int currRow = currSubCell.getRow();
        int currCol = currSubCell.getColumn();

        /* Move one sub-cell to the left */
        currCol--;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move one sub-cell down */
        currRow++;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move 2 sub-cells right */
        currCol++;
        addNewNode(path, cells[currRow][currCol]);
        currCol++;
        addNewNode(path, cells[currRow][currCol]);
    }

    private void goAroundRight(List<ManeuverLocation> path, GridCell currSubCell, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        int currRow = currSubCell.getRow();
        int currCol = currSubCell.getColumn();

        /* Move one sub-cell to the right */
        currCol++;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move one sub-cell up */
        currRow--;
        addNewNode(path, cells[currRow][currCol]);

        /* Then, move 2 sub-cells left */
        currCol--;
        addNewNode(path, cells[currRow][currCol]);

        currCol--;
        addNewNode(path, cells[currRow][currCol]);
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
