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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * Nov 30, 2016
 */
package pt.lsts.neptus.worldwindview;

/**
 * @author Manuel R.
 *
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.Earth.MSVirtualEarthLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Box;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwindx.examples.util.LayerManagerLayer;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

@PluginDescription(name = "WorldWind Renderer Panel")
@Popup(name = "WorldWind Panel", pos = POSITION.CENTER, height = 600, width = 600)
public class WorldWindGlobe extends ConsolePanel implements IPeriodicUpdates {

    private static final long serialVersionUID = -4031214492375928720L;
    private WorldWindowGLCanvas wwd;
    private RenderableLayer vehLayer = null;
    private HashMap<String, Box> systems = new HashMap<>();
    private JPopupMenu menu = new JPopupMenu("Popup");
    private View view = null;
    private ConsoleLayout console = null;
    private Runnable layerUpdateTask;

    @NeptusProperty
    public int updateMillis = 300;

    public WorldWindGlobe(ConsoleLayout console) {
        super(console);
        this.console = console;

    }

    @Override
    public void initSubPanel() {
        
        // Initialize WorldWind
        wwd = new WorldWindowGLCanvas();
        wwd.setModel(new BasicModel());

        // A viewer for the globe
        view = wwd.getView();

        // Setup Layers
        setupLayers();

        // Adjust the view so that it looks at our location
        final LocationType myLoc = MyState.getLocation();
        if (!myLoc.isLocationEqual(LocationType.ABSOLUTE_ZERO))
            view.setEyePosition(Position.fromDegrees(myLoc.getLatitudeDegs(), myLoc.getLongitudeDegs(), 1e3));

        setLayout(new BorderLayout());
        add(wwd, BorderLayout.CENTER);

        // Setup right-click menus
        setupPopupMenu();

        // Setup Listeners
        setupListeners();
        
    }

    private void setupLayers() {
        vehLayer = new RenderableLayer();
        vehLayer.setName("Vehicles");
        vehLayer.setPickEnabled(true);

        LayerManagerLayer mng = new LayerManagerLayer(wwd);

        wwd.getModel().getLayers().addIfAbsent(new MSVirtualEarthLayer());
        wwd.getModel().getLayers().addIfAbsent(mng);

        ImcSystem[] sys = ImcSystemsHolder.lookupActiveSystemVehicles(); 

        for (int i=0; i<sys.length; i++) {
            LocationType vehLoc = sys[i].getLocation();
            Color vehColor = sys[i].getVehicle().getIconColor();
            addVehicleToWorld(sys[i].getName(), vehColor, vehLoc.getLatitudeDegs(), vehLoc.getLongitudeDegs(), vehLoc.getHeight());
        }

        // Add vehicle layer to layer manager
        wwd.getModel().getLayers().add(vehLayer);

        // Update layer manager
        mng.update();

        layerUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (vehLayer != null)
                    vehLayer.firePropertyChange(AVKey.LAYER, null, null);
            }
        };
    }

    private void setupListeners() {
        wwd.addPositionListener(new PositionListener() {
            @Override
            public void moved(PositionEvent arg0) {
                //System.out.println(arg0.getPosition());
            }
        });
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            menu.show(WorldWindGlobe.this, e.getX(), e.getY());
        }
    }

    private void setupPopupMenu() {

        JMenu centerMap = new JMenu(I18n.text("Center map in..."));

        Comparator<ImcSystem> imcComparator = new Comparator<ImcSystem>() {
            @Override
            public int compare(ImcSystem o1, ImcSystem o2) {
                // Comparison if authority option and only one has it
                if ((o1.isWithAuthority() ^ o2.isWithAuthority()))
                    return o1.isWithAuthority() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

                // Comparison if authority option and the levels are different
                if ((o1.getAuthorityState() != o2.getAuthorityState()))
                    return o2.getAuthorityState().ordinal() - o1.getAuthorityState().ordinal();

                return o1.compareTo(o2);
            }
        };
        ImcSystem[] veh = ImcSystemsHolder.lookupSystemVehicles();
        Arrays.sort(veh, imcComparator);
        for (ImcSystem sys : veh) {
            final LocationType l = sys.getLocation();
            final VehicleType vehS = VehiclesHolder.getVehicleById(sys.getName());
            JMenuItem menuItem = vehS != null ? new JMenuItem(vehS.getId(), vehS.getIcon()) : new JMenuItem(sys.getName());
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    focusLocation(l);
                }
            });
            centerMap.add(menuItem);
        }
        MenuScroller.setScrollerFor(centerMap);

        menu.add(centerMap);

        //setup right-click menu
        wwd.addMouseListener(new MouseListener(){

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseClicked(MouseEvent e) {
                checkPopup(e);
            }
        });
    }

    private void focusLocation(LocationType l) {
        view.setEyePosition(Position.fromDegrees(l.getLatitudeDegs(), l.getLongitudeDegs(), 1e3));
    }

    private void addVehicleToWorld(String name, Color color, double lat, double lon, double height) {
        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setInteriorMaterial(new Material(color));
        attrs.setInteriorOpacity(0.7);
        attrs.setEnableLighting(true);
        attrs.setOutlineMaterial(new Material(color));
        attrs.setOutlineWidth(2d);
        attrs.setDrawInterior(true);
        attrs.setDrawOutline(false);

        Box vehBox = new Box(Position.fromDegrees(lat, lon, height), 10, 10, 10);
        vehBox.setAltitudeMode(WorldWind.ABSOLUTE);
        vehBox.setAttributes(attrs);
        vehBox.setVisible(true);
        
        vehLayer.addRenderable(vehBox);
        systems.put(name, vehBox);
    }

    @Override
    public boolean update() {
        updateLayers();
        
        return true;
    }

    private void updateLayers() {
        SwingUtilities.invokeLater(layerUpdateTask);
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        //TODO
    }
    
    @Subscribe
    public void consume(EstimatedState msg) {
        if (msg.getLat() != 0 || msg.getLon() != 0) {
            ImcSystem system = ImcSystemsHolder.getSystemWithName(msg.getSourceName());
            
            if (system.getVehicle() == null)
                return;

            EstimatedState es = IMCUtils.parseState(msg).toEstimatedState();
            if (!systems.containsKey(msg.getSourceName())) {
                Color c = system.getVehicle().getIconColor();
                addVehicleToWorld(msg.getSourceName(), c, Math.toDegrees(es.getLat()), Math.toDegrees(es.getLon()), es.getHeight());
            } 
            else {
                Box vehBox = systems.get(system.getName());
                // Update vehicle color if it's the main vehicle
                Color c = system.getVehicle().getIconColor();
                if (msg.getSourceName().equals(console.getMainSystem()))
                    c = Color.GREEN;
                
                vehBox.getAttributes().setInteriorMaterial(new Material(c));
                vehBox.getAttributes().setOutlineMaterial(new Material(c));
                // Update vehicle position
                vehBox.setCenterPosition(Position.fromDegrees(Math.toDegrees(es.getLat()), Math.toDegrees(es.getLon()), msg.getHeight()));
                
                systems.put(system.getName(), vehBox);
            }
        }
    }

    @Override
    public void cleanSubPanel() {
        wwd.shutdown();
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }
}
