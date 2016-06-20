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
 * 24 May 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.google.common.collect.MinMaxPriorityQueue;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Implementation of the A-star algorithm.
 * As heuristic it uses the "Manhattan Distance"
 * and looks for the minimum cost D for moving
 * from one cell to the other. In this implementation
 * D is always 1 between adjacent cells.
 * @author tsmarques
 *
 */
public class Astar {
    public Astar() {

    }

    public List<ManeuverLocation> computeShortestPath(MapCell start, MapCell end) {
        MinHeap heap = new MinHeap();
        /* Current cost to each node */
        Map<String, Integer> currentCosts = new HashMap<>();
        Map<String, MapCell> previousNodes = new HashMap<>();

        List<MapCell> path = new ArrayList<>();
        MapCell currCell = start;

        heap.updateNode(currCell, 1);
        currentCosts.put(currCell.id(), 1);
        while(!currCell.id().equals(end.id()) && heap.size() != 0) {
            currCell = heap.pollMin();

            /* search this cell's neighbours */
            int currCellCost = currentCosts.get(currCell.id());
            for(MapCell neighbour : currCell.getNeighbours())
                /* if neighbour has been seen already */
                if(!currentCosts.containsKey(neighbour.id())) {
                    /* add neighbout to the heap */
                    int newCost = currCellCost + 1;
                    heap.updateNode(neighbour, newCost);
                    currentCosts.put(neighbour.id(), newCost);
                    previousNodes.put(neighbour.id(), currCell);
                }
        }

        return reconstructPath(start, end, previousNodes);
    }

    private List<ManeuverLocation> reconstructPath(MapCell startCell, MapCell endCell, Map<String, MapCell> edges) {
        List<ManeuverLocation> path = new ArrayList<>();

        path.add(0, new ManeuverLocation(endCell.getLocation()));

        MapCell currCell = edges.get(endCell.id());
        MapCell prevCell = endCell;
        MapCell nextCell = edges.get(currCell.id());

        while(nextCell != null) {
            boolean areColinear = CoordinateUtil.areColinearLocations(currCell.getLocation(), prevCell.getLocation(), nextCell.getLocation(), 0.1);

            if(!areColinear)
                path.add(0, new ManeuverLocation(currCell.getLocation()));

            prevCell = currCell;
            currCell = edges.get(prevCell.id());
            nextCell = edges.get(currCell.id());
        }

        return path;
    }

    /**
     * Customized implementation of a PriorityQueue
     * for nodes of type MapCell
     * */
    private class MinHeap extends PriorityQueue<MinHeap.Node> {
        public MinHeap() {
            super(new Comparator<Node>() {
              @Override
              public int compare(Node a, Node b) {
                  if(a.value > b.value)
                      return 1;
                  else if(a.value == b.value)
                      return 0;
                  else
                      return -1;
              }
          });
        }

        public MapCell pollMin() {
            MapCell currMin = this.poll().cell;

            return currMin;
        }

        public void updateNode(MapCell cell, int newValue) {
            Iterator<Node> it = this.iterator();
            boolean exists = false;

            while(it.hasNext() && !exists) {
                Node curr = it.next();
                if(curr.cell.id().equals(cell.id())) {
                    this.add(new Node(newValue, cell, curr.previous));
                    exists = true;
                }
            }

            if(!exists)
                this.add(new Node(newValue, cell, null));
        }

        class Node {
            private int value;
            private MapCell cell;
            private MapCell previous;

            public Node(int value, MapCell cell, MapCell prevCell) {
                this.value = value;
                this.cell = cell;
                this.previous = prevCell;
            }
        }
    }
}
