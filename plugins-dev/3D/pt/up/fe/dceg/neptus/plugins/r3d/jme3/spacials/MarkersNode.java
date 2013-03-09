/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Margarida Faria
 * Jan 4, 2013
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.plugins.r3d.MarkerObserver;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Groups the 3D representation of all the markers of the log.
 * 
 * @author Margarida Faria
 * 
 */
public class MarkersNode extends Element3D implements ToggleVisibility {

    // private final boolean isVisible;
    private final Node markersNode;
    private HashMap<LocationType, Marker> markers;
    private final MarkerObserver markerObserver;



    /**
     * Creates and adds to the scene graph the node that will hold all the markers
     * 
     * @param assetManager
     * @param fatherNode
     * @param worldInfo
     * @param isVisible
     * @param markersNode
     * @param markers
     * @param markerObserver
     */
    public MarkersNode(AssetManager assetManager, Node fatherNode, WorldInformation worldInfo, boolean isVisible,
            MarkerObserver markerObserver) {
        super(assetManager, fatherNode, worldInfo);
        // this.isVisible = isVisible;
        // Node with all the markers
        markersNode = new Node("Markers Node");
        fatherNode.attachChild(markersNode);
        this.markerObserver = markerObserver;
        this.markers = new HashMap<LocationType, Marker>();
    }

    /**
     * Updates the position of the labels of the markers. <br>
     * If a marker has been added or deleted, the 3D markers are rebuilt.
     * 
     * @param guiNode
     * @param cam
     */
    public void updateMarkers(Node guiNode, Camera cam) {
        // marker spatial
        if (markerObserver.hasChanges()) {
            // remove old spatials
            markersNode.detachAllChildren();
            // add new
            this.markers = new HashMap<LocationType, Marker>();
            Vector<LogMarker> markers = markerObserver.consumeNewSetOfMarkers();
            Marker marker;
            LocationType location;
            for (LogMarker logMarker : markers) {
                location = markerObserver.getLatLonDepth(logMarker.timestamp);
                marker = new Marker(assetManager, markersNode, worldInfo, location, logMarker.label);
                this.markers.put(location, marker);
            }
        }
        // update labels
        guiNode.detachAllChildren();
        Set<LocationType> keySet = markers.keySet();
        Marker marker;
        for (LocationType key : keySet) {
            marker = markers.get(key);
            marker.updateLabelGui(guiNode, cam);
        }
    }

    @Override
    public void toggleVisibility() {
        // TODO Auto-generated method stub

    }

}
