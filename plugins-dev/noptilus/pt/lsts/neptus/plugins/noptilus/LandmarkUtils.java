/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Oct 17, 2012
 */
package pt.lsts.neptus.plugins.noptilus;

import java.awt.Color;
import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author noptilus
 */
public class LandmarkUtils {

    public enum LANDMARK {

        UNKNOWN(0, Color.black), VISIBLE(1, Color.red), ACCURATE(2, Color.green);

        private final Color color;
        private final int value;

        LANDMARK(int value, Color color) {
            this.color = color;
            this.value = value;
        }

        public Color getColor() {
            return this.color;
        }

        public int getValue() {
            return this.value;
        }                
    }

    public static LANDMARK getLandmarkState(int value) {
        switch (value) {
            case 0:
                return LANDMARK.UNKNOWN;
            case 1:
                return LANDMARK.VISIBLE;
            default:
                return LANDMARK.ACCURATE;
        }
    }
    
    public static void loadLandmarks(File landmarksFile, Vector<LocationType> positions) throws Exception {
        Vector<double[]> points = PlanUtils.loadWaypoints(landmarksFile);
        positions.clear();
        for (double[] pt : points) {
            LocationType loc = new LocationType(pt[0], pt[1]);
            loc.setAbsoluteDepth(pt[2]);
            positions.add(loc);
        }        
    }

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(null);

    }
}

