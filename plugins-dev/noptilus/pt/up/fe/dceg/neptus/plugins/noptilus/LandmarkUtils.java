/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Oct 17, 2012
 * $Id:: LandmarkUtils.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.plugins.noptilus;

import java.awt.Color;
import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

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

