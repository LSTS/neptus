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
 * 2 May 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning.algorithm;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;

/**
 * @author tsmarques
 *
 */

/**
 * Simple implementation of a Minimum Spanning Tree
 * using DFS
 * */
public class MST {
    /* Minimum spanning tree to be generated */
    private List<Pair<MapCell, MapCell>> mst;
    /* Sequence of nodes used to traverse this tree */
    private List<MapCell> nodeSequence;
    private MapCell startCell;

    public MST(MapCell startCell) {
        this.startCell = startCell;
        mst = new ArrayList<>();
        nodeSequence = new ArrayList<>();
        generateMST(startCell);
    }

    /**
     * Generates the spanning tree edges and the node sequence to
     * traverse it
     * */
    private void generateMST(MapCell startCell) {
        /* Nodes already visited */
        List<String> visitedNodes = new ArrayList<>();
        /* When the head node has no more free neighbours, remove it and try the next one */
        List<MapCell> backtrackNodes = new ArrayList<>();

        visitedNodes.add(startCell.id());
        backtrackNodes.add(startCell);

        MapCell currentNode = startCell;
        MapCell parentNode = null;

        while(backtrackNodes.size() != 0) {
            List<MapCell> neighbours = currentNode.getNeighboursAntiClockwise(parentNode);
            boolean freeNode = false;
            int i = 0;

            if(!neighbours.isEmpty()) {
                nodeSequence.add(currentNode);

                while(!freeNode && i < neighbours.size()) {
                    MapCell neighbour = neighbours.get(i);

                    if(!visitedNodes.contains(neighbour.id())) {
                        /* add new edge to the MST */
                        mst.add(new Pair<>(currentNode, neighbour));

                        backtrackNodes.add(0, currentNode);

                        parentNode = currentNode;
                        currentNode = neighbour;
                        visitedNodes.add(currentNode.id());


                        freeNode = true;
                    }
                    else
                        i++;
                }
                /* No free nodes found, then, if possible, backtrack */
                if(!freeNode && !backtrackNodes.isEmpty()) {
                    if(!backtrackNodes.isEmpty()) {
                        parentNode = currentNode;
                        currentNode = backtrackNodes.remove(0);
                    }
                }
            }
            else { /* Graph needs to be connected in order to have an MST*/
                NeptusLog.pub().error("Can't generate a Minimum Spanning Tree because the graph is not connected! Node: " + currentNode.id() + " has no neighbours");
            }
        }
    }

    /**
     * Returns a sequence of nodes to traverse this tree
     * */
    public List<MapCell> getNodeSequence() {
        return this.nodeSequence;
    }

    /**
     * Returns nodes sequence of this mst
     * */
    public List<Pair<MapCell, MapCell>> getEdges() {
        return mst;
    }

    public MapCell startCell() {
        return this.startCell;
    }
}
