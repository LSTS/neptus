/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Jan 4, 2013
 * $Id:: MarkersNode.java 9885 2013-02-07 17:48:58Z mfaria                      $:
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
