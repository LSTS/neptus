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
 * Nov 16, 2012
 * $Id:: SimulationActionsPlugin.java 9615 2012-12-30 23:08:28Z pdias           $:
 */
package pt.up.fe.dceg.neptus.plugins.sim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.GpsFix;
import pt.up.fe.dceg.neptus.imc.ManeuverControlState;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.planning.MapPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SimulationActionsPlugin extends SimpleSubPanel {

    protected final String menuTools = I18n.text("Tools");
    protected final String menuSimulation = I18n.text("Simulation");
    protected final String menuSendFix = I18n.text("Send GPS Fix");
    protected final String menuManDone = I18n.text("Flag maneuver completion");

    private static final long serialVersionUID = 1L;

    public SimulationActionsPlugin(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {
        addMenuItem(menuTools+">"+menuSimulation+">"+menuSendFix, null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGpsFix();
            }
        });
        
        addMenuItem(menuTools+">"+menuSimulation+">"+menuManDone, null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendManeuverDone();
            }
        });
    }

    private void sendManeuverDone() {
        ManeuverControlState mcs = new ManeuverControlState();
        mcs.setState(ManeuverControlState.STATE.DONE);
        send(mcs);
    }
    
    private void sendGpsFix() {
        Vector<MapPanel> pps = getConsole().getSubPanelsOfClass(MapPanel.class); 
        if (pps.isEmpty()) {
            GuiUtils.errorMessage(I18n.text("Cannot send GPS fix"), I18n.text("There must be a planning panel in the console"));
            return;
        }
        else {
            LocationType loc = pps.firstElement().getRenderer().getCenter();
            loc.convertToAbsoluteLatLonDepth();
            Calendar cal = GregorianCalendar.getInstance();

            GpsFix fix = new GpsFix( 
                    "validity", 0xFFFF, 
                    "type", "MANUAL_INPUT", 
                    "utc_year", cal.get(Calendar.YEAR),
                    "utc_month", cal.get(Calendar.MONTH)+1,
                    "utc_day", cal.get(Calendar.DATE),
                    "utc_time", cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND),
                    "lat", loc.getLatitudeAsDoubleValueRads(),
                    "lon", loc.getLongitudeAsDoubleValueRads(),
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
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
