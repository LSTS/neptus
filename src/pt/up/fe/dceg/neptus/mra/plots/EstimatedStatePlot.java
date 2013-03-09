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
 * Nov 13, 2012
 */
package pt.up.fe.dceg.neptus.mra.plots;

import org.jfree.data.xy.XYSeries;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class EstimatedStatePlot extends Mra2DPlot implements LogMarkerListener {
    
    /**
     * @param panel
     */
    public EstimatedStatePlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        for (IMCMessage state : source.getIterator("EstimatedState", 0, (long)(timestep*1000))) {
            LocationType loc = new LocationType();
            loc.setLatitudeRads(state.getDouble("lat"));
            loc.setLongitudeRads(state.getDouble("lon"));
            loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
            loc.convertToAbsoluteLatLonDepth();
            addValue(state.getTimestampMillis(), loc.getLatitudeAsDoubleValue(), loc.getLongitudeAsDoubleValue(),
                    state.getSourceName(), "position");
        }
        
        
        IMCMessage estate = source.getMessage(source.getFirstMessageOfType("EstimatedState"));
        LocationType ref = new LocationType(Math.toDegrees(estate.getDouble("lat")), Math.toDegrees(estate.getDouble("lon")));

        for (IMCMessage state : source.getIterator("SimulatedState", 0, (long)(timestep*1000))) {
            LocationType loc = new LocationType();
            if (state.getTypeOf("lat") != null) {
                loc.setLatitudeRads(state.getDouble("lat"));
                loc.setLongitudeRads(state.getDouble("lon"));
            }
            else {
                loc.setLocation(ref);
            }
            loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
            loc.convertToAbsoluteLatLonDepth();
            addValue(state.getTimestampMillis(), loc.getLatitudeAsDoubleValue(), loc.getLongitudeAsDoubleValue(),
                    state.getSourceName(), "simulator");
        }
    }
    
    @Override
    public String getName() {
        return "Position";
    }
    
    @Override
    public String getXAxisName() {
        return "Latitude";
    }
    
    @Override
    public String getYAxisName() {
        return "Longitude";
    }
    
    @Override
    public void addLogMarker(LogMarker marker) {
        XYSeries markerSeries = getMarkerSeries();
        IMCMessage state = mraPanel.getSource().getLog("EstimatedState").getEntryAtOrAfter(new Double(marker.timestamp).longValue());
        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getDouble("lat"));
        loc.setLongitudeRads(state.getDouble("lon"));
        loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
        loc.convertToAbsoluteLatLonDepth();
        
        if(markerSeries != null)
            markerSeries.add(new TimedXYDataItem(loc.getLatitudeAsDoubleValue(), loc.getLongitudeAsDoubleValue(), new Double(marker.timestamp).longValue(), marker.label));
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        
    }

    @Override
    public void GotoMarker(LogMarker marker) {
        
    }

}
