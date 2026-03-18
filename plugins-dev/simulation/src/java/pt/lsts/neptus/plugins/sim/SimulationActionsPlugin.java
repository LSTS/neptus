/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 16, 2012
 */
package pt.lsts.neptus.plugins.sim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import pt.lsts.imc.GpsFix;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SimulationActionsPlugin extends ConsolePanel implements IEditorMenuExtension {

    protected final String menuTools = I18n.text("Tools");
    protected final String menuSimulation = I18n.text("Simulation");
    protected final String menuSendFix = I18n.text("Send GPS Fix");
    protected final String menuChooseHeight = I18n.text("Simulated Height...");

    private Vector<IMapPopup> renderersPopups = new Vector<IMapPopup>();
    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();

    private GpsFixDialog gpsFixDialog;
    
    @NeptusProperty
    private double simulatedHeight = 0;
    
    private static final long serialVersionUID = 1L;

    public SimulationActionsPlugin(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {
        gpsFixDialog = new GpsFixDialog(this);
        addMenuItem(menuTools+">"+menuSimulation+">"+menuSendFix, null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGpsFix();
            }
        });        
        
        addMenuItem(menuTools+">"+menuSimulation+">"+menuChooseHeight, null, new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                selectHeight();
            }
        });

        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);
        for (IMapPopup str2d : renderersPopups) {
            str2d.addMenuExtension(this);
        }
    }

    private void selectHeight() {
        String sel = JOptionPane.showInputDialog(getConsole(), "Select Simulated Height", ""+simulatedHeight);
        
        if (sel == null)
            return;
        
        try {
            simulatedHeight = Double.parseDouble(sel);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }
    
    private void sendGpsFix() {

        Vector<MapPanel> pps = getConsole().getSubPanelsOfClass(MapPanel.class);
        if (pps.isEmpty()) {
            GuiUtils.errorMessage(I18n.text("Cannot send GPS fix"), I18n.text("There must be a planning panel in the console"));
            return;
        }
        else {
            GuiUtils.centerParent(gpsFixDialog, getConsole());
            gpsFixDialog.setVisible(true);
        }
    }

    public void sendGpsFixToCenter() {
        Vector<MapPanel> pps = getConsole().getSubPanelsOfClass(MapPanel.class);
        LocationType loc = pps.firstElement().getRenderer().getCenter();
        sendGpsFix(loc);
    }

    public void sendGpsFix(LocationType loc) {
        loc.convertToAbsoluteLatLonDepth();
        Calendar cal = GregorianCalendar.getInstance();
        GpsFix fix = GpsFix.create(
                "validity", 0xFFFF,
                "type", "MANUAL_INPUT",
                "utc_year", cal.get(Calendar.YEAR),
                "utc_month", cal.get(Calendar.MONTH)+1,
                "utc_day", cal.get(Calendar.DATE),
                "utc_time", cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND),
                "lat", loc.getLatitudeRads(),
                "lon", loc.getLongitudeRads(),
                "height", simulatedHeight,
                "satellites", 4,
                "cog", 0,
                "sog", 0,
                "hdop", 1,
                "vdop", 1,
                "hacc", 2,
                "vacc", 2
        );
        send(fix);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType
     * , pt.lsts.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
        final LocationType l = new LocationType(loc);
        Vector<JMenuItem> menus = new Vector<JMenuItem>();
        JMenu gpsFixMenu = new JMenu(I18n.text("Send GPS fix to..."));
        menus.add(gpsFixMenu);

        AbstractAction sendToCenter = new AbstractAction(I18n.text("Map Center")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGpsFixToCenter();
            }
        };
        gpsFixMenu.add(new JMenuItem(sendToCenter));

        AbstractAction sendToCoord = new AbstractAction(I18n.text("Here")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGpsFix(l);
            }
        };
        gpsFixMenu.add(new JMenuItem(sendToCoord));

        return menus;
    }
}
