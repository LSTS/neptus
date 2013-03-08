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
 * $Id:: ValueColorMap.java 9952 2013-02-19 18:24:10Z jqcorreia                 $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.Graphics2D;

import pt.up.fe.dceg.neptus.colormap.ColormapOverlay;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class ValueColorMap extends ColormapOverlay implements LogReplayLayer {

    @NeptusProperty(name="Cell width")
    public int cellWidth = 20;
    
    private LsfIndex index;
    private boolean parsed = false, parsing = false; 
    private String message;
    private String entity;
    private String field;
    
    public ValueColorMap(String message, String entity) {
        super(message, 20, false, 0);       
        this.entity = entity;
        this.message = message;
        this.field = "value";
    }
    
    public ValueColorMap(String message, String entity, String field) {
        super(message,20, false, 0);       
        this.entity = entity;
        this.message = message;
        this.field = field;
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        if (entity != null && source.getLsfIndex().getEntityId(entity) == -1)
            return false;
        return source.getLsfIndex().getFirstMessageOfType(message) != -1;
    }
    
    @Override
    public void cleanup() {
        generated = scaled = null;
    }

    @Override
    public String getName() {
        return message+"."+field;
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
                    int eid = 255;
                    if (entity != null)
                        eid = index.getEntityId(entity);
                    
                    int lastStateIndex = 0;
                    int stateId = index.getDefinitions().getMessageId("EstimatedState");
                    if (index.getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
                        for (IMCMessage m : index.getIterator(message, 0)) {
                            if (eid != 255 && m.getSrcEnt() != eid)
                                continue;
                            
                            int state = index.getMessageAtOrAfer(stateId, 255,  lastStateIndex, m.getTimestamp());
                            
                            if (state != -1) {
                                lastStateIndex = state;
                                IMCMessage mstate = index.getMessage(state);
                                LocationType loc = new LocationType(Math.toDegrees(mstate.getDouble("lat")), Math.toDegrees(mstate.getDouble("lon")));
                                loc.translatePosition(mstate.getDouble("x"), mstate.getDouble("y"), 0);
                                addSample(loc, m.getDouble(field));
                            }
                        }                       
                    }
                    parsing = false;
                }
            }, "Value overlay").start();
        }
        
        if (!parsing)
            super.paint(g, renderer);
    }

}
