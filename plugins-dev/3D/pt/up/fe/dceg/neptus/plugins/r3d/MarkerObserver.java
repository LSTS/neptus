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
 * Jan 3, 2013
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
