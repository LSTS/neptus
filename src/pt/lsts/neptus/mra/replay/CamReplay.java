/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Jul 5, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.mp.preview.payloads.CameraFOV;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 */
@PluginDescription(name = "Camera footprint replay")
public class CamReplay implements LogReplayLayer {

    private EstimatedState state = null;
    CameraFOV fov = new CameraFOV(Math.toRadians(60), Math.toRadians(45));
    {
        fov.setTilt(Math.toRadians(60));
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (state != null) {
            fov.setState(state);
            Point2D pt = renderer.getScreenPosition(fov.getLookAt());
            g.setColor(Color.yellow);
            g.fill(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10));
            
            GeneralPath path = new GeneralPath();
            Point2D prev = null;
            for (LocationType loc : fov.getFootprintQuad()) {
                Point2D cur = renderer.getScreenPosition(loc);
                if (prev == null)
                    path.moveTo(cur.getX(), cur.getY());
                else
                    path.lineTo(cur.getX(), cur.getY());
                prev = cur;                    
            }
            path.closePath();
            g.setColor(new Color(255,255,0,128));
            g.fill(path);
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return true;
    }

    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }

    @Override
    public void parse(IMraLogGroup source) {
        
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"EstimatedState"};
    }

    @Override
    public void onMessage(IMCMessage message) {
        state = new EstimatedState(message);        
    }

    @Override
    public boolean getVisibleByDefault() {
        return true;
    }

    @Override
    public void cleanup() {
        
    }
}
