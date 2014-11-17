/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 16/11/2014
 */
package org.acme.myplugin;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Date;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;

import com.google.common.eventbus.Subscribe;

/**
 * @author You
 *
 */
@PluginDescription(name = "My Console Layer")
@LayerPriority(priority = 66)
public class MyConsoleLayer extends ConsoleLayer implements MainVehicleChangeListener {
    
    @NeptusProperty(name = "Show Time", userLevel = LEVEL.REGULAR, 
            category="Visibility", editable = true)
    public boolean showTime = true;

    private LocationType location = null;
    private String positionStr = null;
    private String dateTimeStr = null;

    public MyConsoleLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }
    
    @Subscribe
    public void on(EstimatedState msg) {
        if (!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;
        
        LocationType loc = new LocationType();
        loc.setLatitudeRads(msg.getLat());
        loc.setLongitudeRads(msg.getLon());
        loc.setOffsetNorth(msg.getX());
        loc.setOffsetEast(msg.getY());
        loc.convertToAbsoluteLatLonDepth();
        positionStr = I18n.text("Position:") + " " + loc.getLatitudeAsPrettyString() +
                " " + loc.getLongitudeAsPrettyString();
        dateTimeStr = I18n.text("Age:") + " " + 
                DateTimeUtil.dateFormaterXMLNoMillisUTC.format(new Date(msg.getTimestampMillis()));
        
        location = loc;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.MainVehicleChangeListener#mainVehicleChange(java.lang.String)
     */
    @Override
    public void mainVehicleChange(String id) {
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(getConsole().getMainSystem());
        if (sys != null && sys.getLocation() != null) {
            LocationType loc = new LocationType(sys.getLocation());
            loc.convertToAbsoluteLatLonDepth();
            positionStr = I18n.text("Position:") + " " + loc.getLatitudeAsPrettyString() +
                    " " + loc.getLongitudeAsPrettyString();
            dateTimeStr = I18n.text("Age:") + " " + 
                    DateTimeUtil.dateFormaterXMLNoMillisUTC.format(new Date(sys.getLocationTimeMillis()));

            location = loc;
        }
        else {
            positionStr = I18n.text("Position:") + " ?";
            dateTimeStr = I18n.text("Age:") + " ?";
            location = null;
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        
        if (location == null)
            return;
        
        Graphics2D g2 = (Graphics2D) g.create();
        
        Point2D pt = renderer.getScreenPosition(location);
        g2.translate(pt.getX(), pt.getY());
        g2.translate(20, 20);
        g2.setColor(Color.BLACK);
        g2.drawString(positionStr, 1, 1);
        g2.setColor(Color.WHITE);
        g2.drawString(positionStr, 0, 0);

        if (showTime) {
            g2.setColor(Color.BLACK);
            g2.drawString(dateTimeStr, 1, 16);
            g2.setColor(Color.WHITE);
            g2.drawString(dateTimeStr, 0, 15);
        }
        g2.dispose();
    }
}
