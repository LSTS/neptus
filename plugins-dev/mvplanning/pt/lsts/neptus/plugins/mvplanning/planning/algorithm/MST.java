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

import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.mvplanning.planning.MapCell;

/**
 * @author tsmarques
 *
 */

/**
 * Simple implementation of a Minimum Spanning Tree
 * using DFS
 * */
public class MST {
    private List<Pair<MapCell, MapCell>> mst;
    private MapCell startCell;

    public MST(MapCell startCell) {
        this.startCell = startCell;
        mst = generateMST(startCell);
    }

    private List<Pair<MapCell, MapCell>> generateMST(MapCell startCell) {
        /* Minimum spanning tree to be generated */
        List<Pair<MapCell, MapCell>> mst = new ArrayList<>();
        /* Nodes already visited */
        List<MapCell> visitedNodes = new ArrayList<>();
        /* When the head node has no more free neighbours, remove it and try the next one */
        List<MapCell> backtrackNodes = new ArrayList<>();

        visitedNodes.add(startCell);
        backtrackNodes.add(startCell);

        MapCell currentNode = startCell;

        while(backtrackNodes.size() != 0) {
            List<MapCell> neighbours = currentNode.getNeighbours();
            boolean freeNode = false;
            int i = 0;

            if(!neighbours.isEmpty()) {
                while(!freeNode && i < neighbours.size()) {
                    MapCell neighbour = neighbours.get(i);

                    if(!visitedNodes.contains(neighbour)) {
                        /* add new edge to the MST */
                        mst.add(new Pair<>(currentNode, neighbour));

                        currentNode = neighbour;
                        visitedNodes.add(currentNode);
                        backtrackNodes.add(0, currentNode);

                        freeNode = true;
                    }
                    else
                        i++;
                }
                /* No free nodes found, then, if possible, backtrack */
                if(!freeNode && !backtrackNodes.isEmpty()) {
                    backtrackNodes.remove(0);
                    if(!backtrackNodes.isEmpty())
                        currentNode = backtrackNodes.get(0);
                }
            }
            else /* Graph needs to be connected in order to have an MST*/
                return null;
        }
        return mst;
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
