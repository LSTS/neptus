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
 * Dec 5, 2012
 * $Id:: BathymetryReplay.java 9952 2013-02-19 18:24:10Z jqcorreia              $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.Graphics2D;

import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.ColormapOverlay;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@LayerPriority(priority=-60)
public class BathymetryReplay extends ColormapOverlay implements LogReplayLayer {

    @NeptusProperty(name="Cell width")
    public int cellWidth = 5;
    
    private LsfIndex index;
    private boolean parsed = false, parsing = false; 
    
    public BathymetryReplay() {
        super("Bathymetry", 20, true, 0);              
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().getDefinitions().getVersion().compareTo("5.0.0") >= 0;
    }
    
    @Override
    public void cleanup() {
        generated = scaled = null;
    }

    @Override
    public String getName() {
        return "Bathymetry layer";
    }
    

    @Override
    public String[] getObservedMessages() {
        return new String[0];
    }
    

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }
    
    @Override
    public void onMessage(IMCMessage message) {
        
    }
    
    @Override
    public void parse(IMraLogGroup source) {
        this.index = source.getLsfIndex();
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!parsed) {
            super.cellWidth = cellWidth;
            parsed = true;
            parsing = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (index.getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
                        for (EstimatedState state : index.getIterator(EstimatedState.class)) {
                            if (state.getAlt() < 0 || state.getDepth() < 1 || Math.abs(state.getTheta()) > Math.toDegrees(10))
                                continue;
                            LocationType loc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
                            loc.translatePosition(state.getX(), state.getY(), 0);
                            addSampleUseMax(loc, state.getAlt() + state.getDepth());
                        }
                    }
                    generated = generateImage(ColorMapFactory.createJetColorMap());
                    parsing = false;
                }
            }, "Bathymetry overlay").start();
        }
        
        if (!parsing)
            super.paint(g, renderer);
    }

}
