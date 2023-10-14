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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridCell;

public class SpanningTree {
    /* Sequence of nodes used to traverse this tree */
    private List<GridCell> nodeSequence;
    private GridCell startCell;
    private SpTreeIterator it;


    public SpanningTree(GridCell startCell) {
        this.startCell = startCell;
        nodeSequence = generateMST(startCell);
        it = iterator();
    }

    public GridCell getStartNode() {
        return startCell;
    }

    /**
     * Generates the spanning tree edges and the node sequence to
     * traverse it
     * */
    private List<GridCell> generateMST(GridCell startCell) {
        List<GridCell> nodeSequence = new ArrayList<>();
        /* Nodes already visited */
        HashSet<String> visitedNodes = new HashSet<>();
        /* When the head node has no more free neighbours, remove it and try the next one */
        Deque<GridCell> backtrackNodes = new ArrayDeque<>();

        visitedNodes.add(startCell.getId());
        backtrackNodes.add(startCell);

        GridCell currentNode = startCell;
        GridCell parentNode = null;
        boolean graphIsConnected = true;

        while(backtrackNodes.size() != 0 && graphIsConnected) {
            List<GridCell> neighbours = shiftNeighbours(currentNode.getNeighboursList(), parentNode);
            boolean freeNode = false;
            int i = 0;

            if(!neighbours.isEmpty()) {
                nodeSequence.add(currentNode);

                while(!freeNode && i < neighbours.size()) {
                    GridCell neighbour = neighbours.get(i);

                    if(!visitedNodes.contains(neighbour.getId())) {
                        backtrackNodes.push(currentNode);

                        parentNode = currentNode;
                        currentNode = neighbour;
                        visitedNodes.add(currentNode.getId());


                        freeNode = true;
                    }
                    else
                        i++;
                }
                /* No free nodes found, then, if possible, backtrack */
                if(!freeNode && !backtrackNodes.isEmpty()) {
                    if(!backtrackNodes.isEmpty()) {
                        parentNode = currentNode;
                        currentNode = backtrackNodes.pop();
                    }
                }
            }
            else { /* Graph needs to be connected in order to have an MST*/
                NeptusLog.pub().warn("Can't generate a Minimum Spanning Tree because the graph is not connected! Node: " + currentNode.getId() + " has no neighbours");
                graphIsConnected = false;

                /* return an empty list */
                nodeSequence = new ArrayList<>(0);
            }
        }
        return nodeSequence;
    }

    private List<GridCell> shiftNeighbours(List<GridCell> neighbours, GridCell startCell) {
        if(startCell == null)
            return neighbours;

        List<GridCell> shifted = new ArrayList<>(neighbours);
        GridCell current = shifted.get(0);

        while(!current.getId().equals(startCell.getId())) {
            shifted.add(shifted.remove(0));
            current = shifted.get(0);
        }

        return shifted;
    }

    public boolean hasNext() {
        return it.hasNext();
    }

    public GridCell next() {
        return it.next();
    }

    public GridCell peekNext() {
        return it.peekNext();
    }

    /**
     * Provide a means to traverse the generated
     * node sequence
     * */
    private SpTreeIterator iterator() {
        return new SpTreeIterator(nodeSequence);
    }

    private class SpTreeIterator implements Iterator<GridCell> {
        private List<GridCell> nodeSequence;
        private Iterator<GridCell> it;
        private int curr = -1;

        public SpTreeIterator(List<GridCell> sequence) {
            nodeSequence = sequence;
            it = nodeSequence.iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public GridCell next() {
            curr++;
            return it.next();
        }

        /**
         * Returns the next element in the iterator,
         * without advancing it
         *
         * Throws NoSuchElementException in case there's
         * no next element (same as next())
         * */
        public GridCell peekNext() {
            if(curr == -1)
                return nodeSequence.get(0);
            else if(!hasNext())
                throw new NoSuchElementException();

            return nodeSequence.get(curr + 1);
        }
    }
}
