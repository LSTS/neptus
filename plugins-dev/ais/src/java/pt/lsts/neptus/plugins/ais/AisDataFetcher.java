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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * May 12, 2013
 */
package pt.lsts.neptus.plugins.ais;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * This class will create a data set with
 * 
 * @author zp
 * 
 */
public class AisDataFetcher {

    protected LocationType sw, ne;
    protected double gridSizeMeters;
    protected long[][] ships;
    protected AisOverlay overlay = new AisOverlay(null);
    protected LinkedHashMap<String, LocationType> lastShips = new LinkedHashMap<>();
    public AisDataFetcher(double minLat, double maxLat, double minLon, double maxLon, double gridSize) {
        this.gridSizeMeters = gridSize;
        sw = new LocationType(minLat, minLon);
        ne = new LocationType(maxLat, maxLon);

        double[] offsets = ne.getOffsetFrom(sw);

        //System.out.println("width: " + offsets[1] + ", height: " + offsets[0]);
        System.out.println("Latitude,Longitude,Ships");

        ships = new long[(int) (offsets[1] / gridSize)][(int) (offsets[0] / gridSize)];

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                writeFile("output.csv");
                System.out.println("wrote file");
            };
        });
    }

    public void writeFile(String filename) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
            bw.write("latitude,longitude,ships\n");
            for (int i = 0; i < ships.length; i++) {
                for (int j = 0; j < ships[i].length; j++) {
                    if (ships[i][j] == 0)
                        continue;
                    LocationType loc = new LocationType(sw);
                    loc.translatePosition(gridSizeMeters * i, gridSizeMeters * j, 0);
                    loc.convertToAbsoluteLatLonDepth();                    
                    bw.write(loc.getLatitudeDegs()+","+loc.getLongitudeDegs()+","+ships[i][j]+"\n");
                }
            }
            bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }        
    }


    protected void incShip(double lat, double lon) {
        double[] offsets = new LocationType(lat, lon).getOffsetFrom(sw);
        ships[(int) (offsets[1] / gridSizeMeters)][(int) (offsets[0] / gridSizeMeters)]++;
    }

    public void fetchShips() {

        Vector<AisShip> ships = overlay.getShips(sw.getLatitudeDegs(), sw.getLongitudeDegs(),
                ne.getLatitudeDegs(), ne.getLongitudeDegs(), false);
        //System.out.println("fetchShips(): "+ships.size());
        LinkedHashMap<String, LocationType> newShips = new LinkedHashMap<>();
        for (AisShip s : ships) {
            LocationType newLoc = s.getLocation();
            newShips.put(s.getName(), newLoc);
            LocationType prevPosition = lastShips.get(s.getName());
            if (prevPosition == null || prevPosition.getLatitudeDegs() == s.getLatitude())
                continue;
            else {
                double dist = prevPosition.getDistanceInMeters(newLoc);
                double[] offsets = newLoc.getOffsetFrom(prevPosition);
                double points = dist / gridSizeMeters;
                double curDist = 0;

                for (double x = 0, y = 0; curDist <= dist; curDist += gridSizeMeters, x += offsets[0]
                        / points, y += offsets[1] / points) {
                    LocationType loc = new LocationType(prevPosition);
                    loc.translatePosition(x, y, 0);
                    loc.convertToAbsoluteLatLonDepth();
                    incShip(loc.getLatitudeDegs(), loc.getLongitudeDegs());
                    System.out.println(loc.getLatitudeDegs() + "," + loc.getLongitudeDegs()+","+1);
                }

            }
        }
        lastShips = newShips;
    }

    public static void main(String[] args) throws Exception {
        
        //APDL
        //AisDataFetcher fetcher = new AisDataFetcher(41.1210, 41.2295, -8.8951, -8.5175, 10);
        
        //Cadiz
        AisDataFetcher fetcher = new AisDataFetcher(36.4789, 36.5601, -6.3439, -6.1676, 10);
        
        long millisBetweenUpdates = 60 * 1000;
        long count = 0;
        fetcher.fetchShips();
        while (true) {
            Thread.sleep(1000);
            count += 1000;
            if (count > millisBetweenUpdates) {
                fetcher.fetchShips();
                count = 0;
            }
            else {
                // System.out.println((millisBetweenUpdates - count)/1000 +" seconds to a new update");
            }
        }
    }

}
