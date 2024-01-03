/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Dec 5, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public abstract class ValueColorMap extends ColormapOverlay implements LogReplayLayer {

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
    public boolean canBeApplied(IMraLogGroup source, Context context) {
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
