/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Feb 19, 2019
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.UsblConfig;
import pt.lsts.imc.UsblFixExtended;
import pt.lsts.imc.UsblModem;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "USBL Fixes Layer")
public class UsblLayer extends ConsoleLayer {

    private ArrayList<UsblFixExtended> fixes = new ArrayList<>();

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {

    }

    @Override
    @NeptusMenuItem("Tools>USBL>Clear Fixes")
    public void cleanLayer() {
        synchronized (fixes) {
            fixes.clear();
        }
    }

    @NeptusMenuItem("Tools>USBL>Send USBL configuration")
    public void sendConfig() {
        Vector<TransponderElement> transponders = MapGroup.getMapGroupInstance(getConsole().getMission())
                .getAllObjectsOfType(TransponderElement.class);
        if (transponders.isEmpty())
            GuiUtils.errorMessage(getConsole(), "Send USBL Config",
                    "You need to add at least one transponder to the mission");
        else {
            ArrayList<String> names = new ArrayList<>();
            for (TransponderElement t : transponders)
                names.add(t.getId());
            
            String str = (String) JOptionPane.showInputDialog(getConsole(),
                    "Select USBL transponder", "Send USBL Config", JOptionPane.QUESTION_MESSAGE, null,
                    names.toArray(), names.get(0));
            
            if (str != null) {
                TransponderElement obj = null;
                
                for (TransponderElement e : transponders)
                    if (e.getId().equals(str)) {
                        obj = e;
                        break;
                    }
                
                UsblConfig config = new UsblConfig();
                LocationType loc = new LocationType(obj.getCenterLocation());
                loc.convertToAbsoluteLatLonDepth();
                UsblModem modem = new UsblModem();
                modem.setName(obj.getId());
                modem.setLat(loc.getLatitudeRads());
                modem.setLon(loc.getLongitudeRads());
                modem.setZ(loc.getDepth());
                modem.setZUnits(ZUnits.DEPTH);
                config.setModems(Collections.singletonList(modem));
                config.setOp(UsblConfig.OP.SET_CFG);
                NeptusLog.pub().info("Sending the following USBL config:\n" + config);
                ImcMsgManager.getManager().sendMessageToSystem(config, getConsole().getMainSystem());
            }
        }
    }

    @NeptusMenuItem("Tools>USBL>Clear USBL configuration")
    public void clearConfig() {
        UsblConfig config = new UsblConfig();
        config.setOp(UsblConfig.OP.SET_CFG);
        NeptusLog.pub().info("Sending the following USBL config:\n" + config);
        ImcMsgManager.getManager().sendMessageToSystem(config, getConsole().getMainSystem());
    }

    @NeptusMenuItem("Tools>USBL>Get USBL configuration")
    public void getConfig() {
        UsblConfig config = new UsblConfig();
        config.setOp(UsblConfig.OP.GET_CFG);
        NeptusLog.pub().info("Sending the following USBL config:\n" + config);
        ImcMsgManager.getManager().sendMessageToSystem(config, getConsole().getMainSystem());
    }

    @Subscribe
    public void on(UsblConfig msg) {
        NeptusLog.pub().info("Received the following USBL config:\n" + msg);
    }

    @Subscribe
    public void on(UsblFixExtended msg) {
        NeptusLog.pub().info("Received USBL fix to " + msg.getTarget() + " from " + msg.getSourceName());
        synchronized (fixes) {
            fixes.add(msg);
        }
    }
    
    

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        synchronized (fixes) {
            for (UsblFixExtended fix : fixes) {
                LocationType loc = new LocationType();
                loc.setLatitudeRads(fix.getLat());
                loc.setLongitudeRads(fix.getLon());
                Point2D pt = renderer.getScreenPosition(loc);
                ImcSystem target = ImcSystemsHolder.getSystemWithName(fix.getTarget());
                Color c1 = Color.white;

                if (target != null && target.getVehicle() != null)
                    c1 = target.getVehicle().getIconColor();

                g.setColor(c1);
                g.draw(new Line2D.Double(pt.getX(), pt.getY() - 6, pt.getX(), pt.getY() + 6));
                g.draw(new Line2D.Double(pt.getX() - 6, pt.getY(), pt.getX() + 6, pt.getY()));
            }
        }
    }

}
