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
 * Author: José Pinto
 * Jul 12, 2012
 */
package pt.up.fe.dceg.neptus.plugins.ais;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.plugins.update.PeriodicUpdatesService;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

import com.google.gson.Gson;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "ZP", name = "AIS Overlay", icon="pt/up/fe/dceg/neptus/plugins/ais/mt.png")
public class AisOverlay extends SimpleRendererInteraction implements IPeriodicUpdates {
    private static final long serialVersionUID = 1L;

    @NeptusProperty
    public boolean showNames = true;

    @NeptusProperty
    public boolean showSpeeds = true;

    @NeptusProperty
    public long updateMillis = 60000;

    @NeptusProperty
    public boolean showOnlyWhenInteractionIsActive = true;

    @NeptusProperty
    public boolean showStoppedShips = false;

    protected boolean active = false;
    protected Vector<AisShip> shipsOnMap = new Vector<AisShip>();
    protected StateRenderer2D renderer = null;

    protected boolean updating = false;
    
    /**
     * @param console
     */
    public AisOverlay(ConsoleLayout console) {
        super(console);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        active = mode;
        if (active)
            update();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }

    protected Thread lastThread = null;

    protected GeneralPath path = new GeneralPath();
    {
        path.moveTo(0, 5);
        path.lineTo(-5, 0);
        path.lineTo(-5, -5);
        path.lineTo(5, -5);
        path.lineTo(5, 0);
        path.lineTo(0, 5);
        path.closePath();
    }
    @Override
    public boolean update() {
        
        if (showOnlyWhenInteractionIsActive && !active)
            return true;

        // don't let more than one thread be running at a time
        if (lastThread != null)
            return true;

        lastThread = new Thread() {
            public void run() {
                updating = true;
                if (renderer == null)
                    return;

                LocationType topLeft = renderer.getTopLeftLocationType();
                LocationType bottomRight = renderer.getBottomRightLocationType();

                shipsOnMap = getShips(bottomRight.getLatitudeAsDoubleValue(),
                        topLeft.getLongitudeAsDoubleValue(), topLeft.getLatitudeAsDoubleValue(),
                        bottomRight.getLongitudeAsDoubleValue(), showStoppedShips);
                lastThread = null;

                updating = false;
                renderer.repaint();
            };
        };
        lastThread.start();

        return true;
    }
    
    protected Vector<AisShip> getShips(double minLat, double minLon, double maxLat, double maxLon, boolean includeStationary) {
        Vector<AisShip> ships = new Vector<>();
        
        try {
            URL url = new URL("http://www.marinetraffic.com/ais/getjson.aspx?sw_x="+minLon+"&sw_y="+minLat+"&ne_x="+maxLon+"&ne_y="+maxLat+"&zoom="+renderer.getLevelOfDetail()+"&fleet=&station=0&id=null");
            
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Referer", "http://www.marinetraffic.com/ais/");
            GetMethod get = new GetMethod(url.toString());
            get.setRequestHeader("Referer", "http://www.marinetraffic.com/ais/");
            
            HttpClient client = new HttpClient();
            client.executeMethod(get);
            Gson gson = new Gson();
            String[][] res = gson.fromJson(get.getResponseBodyAsString(), String[][].class);
            
            for (int i = 0; i < res.length; i++) {
                double knots = Double.parseDouble(res[i][5])/10;
                if (!includeStationary && knots <= 0.1)
                    continue;
                
                AisShip ship = new AisShip();
                ship.setLatitude(Double.parseDouble(res[i][0]));
                ship.setLongitude(Double.parseDouble(res[i][1]));
                ship.setName(res[i][2]);
                ship.setMMSI(Integer.parseInt(res[i][7]));
                ship.setSpeed(knots);
                if (res[i][4] != null)
                    ship.setCourse(Double.parseDouble(res[i][4]));
                if (res[i][6] != null)
                    ship.setCountry(res[i][6]);                
                if (res[i][8] != null)
                    ship.setLength(Double.parseDouble(res[i][8]));
                if (res[i][9] != null)
                    ship.setWidth(Double.parseDouble(res[i][9]));                
                ships.add(ship);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
        }
        return ships;
    }

    @Override
    public void cleanSubPanel() {
        PeriodicUpdatesService.unregister(this);
        shipsOnMap.clear();
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        super.mouseClicked(event, source);

        JPopupMenu popup = new JPopupMenu();
        popup.add("AIS settings").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(AisOverlay.this, getConsole(), true);
            }
        });

        popup.add("Update ships").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });
        
        Vector<AisShip> ships = new Vector<>();
        ships.addAll(shipsOnMap);
        if (ships.size() > 0 && Desktop.isDesktopSupported()) {
            JMenu menu = new JMenu("Ship Info");
            for (final AisShip s : ships) {
                menu.add(s.getName()+" ("+s.getMMSI()+")").addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            URI uri = new URI(s.getShipInfoURL());
                            desktop.browse(uri);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
            
            popup.add(menu);
        }
        

        popup.show(source, event.getX(), event.getY());
    }

//    protected GeneralPath path = new GeneralPath();
//    {
//        path.moveTo(0, 5);
//        path.moveTo(-5, 0);
//        path.lineTo(-5, -5);
//        path.moveTo(5, -5);
//        path.lineTo(5, 0);
//        path.lineTo(0, 5);
//        path.closePath();
//    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        this.renderer = renderer;
        
       

        if (showOnlyWhenInteractionIsActive && !active)
            return;

        if (lastThread != null) {
            g.drawString("Updating AIS layer...", 10, 15);
        }
        else {
            g.drawString(shipsOnMap.size()+" visible ships", 10, 15);
        }

        for (AisShip ship : shipsOnMap) {
            LocationType shipLoc = ship.getLocation();
            Point2D pt = renderer.getScreenPosition(shipLoc);

            g.translate(pt.getX(), pt.getY());

            if (showNames) {
                g.setColor(Color.blue.darker().darker());
                g.drawString(ship.getName(), 5, 5);
            }

            if (showSpeeds) {
                g.setColor(Color.black);
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(ship.getSpeedMps()) + " m/s", 5, 15);
            }

            g.setColor(Color.blue.darker());
            if (ship.getSpeedMps() == 0) {
                g.setColor(Color.gray.darker());
                g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            }
            else {
                
                double scaleX = (renderer.getZoom() / 10) * ship.getWidth();
                double scaleY = (renderer.getZoom() / 10) * ship.getLength();
                
                g.rotate(Math.PI+ship.getHeadingRads());
                g.scale(scaleX, scaleY);
                g.fill(path);
                g.scale(1/scaleX, 1/scaleY);
                g.rotate(-Math.PI-ship.getHeadingRads());
            }

            g.translate(-pt.getX(), -pt.getY());
        }
    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
