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
 * Jan 3, 2013
 * $Id:: MarkerObserver.java 10066 2013-03-04 15:34:31Z mfaria                  $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author Margarida Faria
 *
 */
public class MarkerObserver implements LogMarkerListener {
    private boolean markersChanged;
    private final Vector<LogMarker> markers;
    private IMraLogGroup source;

    public MarkerObserver() {
        markersChanged = false;
        markers = new Vector<LogMarker>();
    }

    public boolean hasChanges() {
        return markersChanged;
    }

    @SuppressWarnings("unchecked")
    public Vector<LogMarker> consumeNewSetOfMarkers() {
        markersChanged = false;
        return (Vector<LogMarker>) markers.clone();
    }

    public LocationType getLatLonDepth(double timestamp) {
        IMCMessage state = source.getLsfIndex().getMessageAtOrAfter("EstimatedState", 0, 0xFF, timestamp / 1000);
        if (state == null)
            return null;
        EstimatedState estimatedState = (EstimatedState) state;

        LocationType location = Bathymetry3DGenerator.getLocationIMC5(estimatedState);
        return location;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener#addLogMarker(pt.up.fe.dceg.neptus.mra.LogMarker)
     */
    @Override
    public void addLogMarker(LogMarker marker) {
        markers.add(marker);
        markersChanged = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener#removeLogMarker(pt.up.fe.dceg.neptus.mra.LogMarker)
     */
    @Override
    public void removeLogMarker(LogMarker marker) {
        markers.remove(marker);
        markersChanged = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener#GotoMarker(pt.up.fe.dceg.neptus.mra.LogMarker)
     */
    @Override
    public void GotoMarker(LogMarker marker) {
        // TODO Auto-generated method stub

    }

    /**
     * @param source the source to set
     */
    public void setSource(IMraLogGroup source) {
        this.source = source;
    }
}
