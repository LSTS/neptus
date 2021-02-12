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
 * Nov 9, 2011
 */
package pt.lsts.neptus.renderer2d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * @author pdias
 */
public class FeatureFocuser implements IEditorMenuExtension {

    protected static final ImageIcon markIcon = ImageUtils.getIcon("images/buttons/addpoint.png");
    protected static final ImageIcon homeIcon = ImageUtils.getIcon("images/buttons/homeRef.png");
    protected static final ImageIcon transpIcon = ImageUtils.getIcon("images/transponder.png");
    protected static final ImageIcon myLocIcon = ImageUtils.getScaledIcon("images/myloc.png", 24, 24);

    private final ConsoleLayout console;
    private String mainVeh = "";
    
    protected boolean useMyLocation = true;
    protected boolean useVehiclesAndSystems = true;

    /**
     * @param console
     */
    public FeatureFocuser(ConsoleLayout console) {
        this.console = console;
    }

    /**
     * @param console
     * @param useMyLocation
     * @param useVehiclesAndSystems
     */
    public FeatureFocuser(ConsoleLayout console, boolean useMyLocation, boolean useVehiclesAndSystems) {
        this(console);
        this.useMyLocation = useMyLocation;
        this.useVehiclesAndSystems = useVehiclesAndSystems;
    }

    /**
     * @return the useMyLocation
     */
    public boolean isUseMyLocation() {
        return useMyLocation;
    }

    /**
     * @param useMyLocation the useMyLocation to set
     */
    public void setUseMyLocation(boolean useMyLocation) {
        this.useMyLocation = useMyLocation;
    }

    /**
     * @return the useVehiclesAndSystems
     */
    public boolean isUseVehiclesAndSystems() {
        return useVehiclesAndSystems;
    }

    /**
     * @param useVehiclesAndSystems the useVehiclesAndSystems to set
     */
    public void setUseVehiclesAndSystems(boolean useVehiclesAndSystems) {
        this.useVehiclesAndSystems = useVehiclesAndSystems;
    }

    /**
     * @see pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType, pt.lsts.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
        final StateRenderer2D renderer = source.getRenderer();
        MapGroup mg = renderer.getMapGroup();

        JMenu centerInMenu = new JMenu(I18n.text("Center map in..."));

        if (mg == null)
            return null;

        if (useMyLocation) {
            final LocationType myLoc = MyState.getLocation();
            if (!myLoc.isLocationEqual(LocationType.ABSOLUTE_ZERO)) {
                JMenuItem myLocItem = new JMenuItem(I18n.text("My location"), myLocIcon);
                myLocItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        renderer.focusLocation(myLoc);
                    }
                });
                centerInMenu.add(myLocItem);
                centerInMenu.addSeparator();
            }
        }


        final LocationType location = mg.getHomeRef().getCenterLocation();
        JMenuItem homeItem = new JMenuItem(I18n.text("Home Reference"), homeIcon);
        homeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renderer.focusLocation(location);
            }
        });
        centerInMenu.add(homeItem);

        JMenu marksMenu = new JMenu(I18n.text("Marks"));
        marksMenu.setIcon(markIcon);
        for (MarkElement me : mg.getAllObjectsOfType(MarkElement.class)) {
            final LocationType l = me.getPosition();
            JMenuItem menuItem = new JMenuItem(me.getId(), markIcon);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    renderer.focusLocation(l);
                }
            });
            marksMenu.add(menuItem);
        }
        MenuScroller.setScrollerFor(marksMenu);
        centerInMenu.add(marksMenu);

        for (TransponderElement me : mg.getAllObjectsOfType(TransponderElement.class)) {
            final LocationType l = me.getCenterLocation();
            JMenuItem menuItem = new JMenuItem(me.getDisplayName(), transpIcon);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    renderer.focusLocation(l);
                }
            });
            centerInMenu.add(menuItem);
        }

        if (useVehiclesAndSystems) {
            centerInMenu.addSeparator();

            JMenu vehMenu = new JMenu(I18n.text("Vehicles"));
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
                        renderer.focusLocation(l);
                    }
                });
                vehMenu.add(menuItem);
            }
            MenuScroller.setScrollerFor(vehMenu);
            centerInMenu.add(vehMenu);
            
            JMenu otherMenu = new JMenu(I18n.text("Others"));
            ImcSystem[] other = ImcSystemsHolder.lookupAllSystems();
            Arrays.sort(other, imcComparator);
            List<ImcSystem> vecLst = Arrays.asList(veh);
            for (ImcSystem sys : other) {
                if (vecLst.contains(sys))
                    continue;
                
                final LocationType l = sys.getLocation();
                JMenuItem menuItem = new JMenuItem(sys.getName());
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        renderer.focusLocation(l);
                    }
                });
                otherMenu.add(menuItem);
            }
            MenuScroller.setScrollerFor(otherMenu);
            centerInMenu.add(otherMenu);
            
            JMenu extMenu = new JMenu(I18n.text("External"));
            ExternalSystem[] exts = ExternalSystemsHolder.lookupAllSystems();
            Arrays.sort(exts);
            for (ExternalSystem ext : exts) {
                final LocationType l = ext.getLocation();
                JMenuItem menuItem = new JMenuItem(ext.getName());
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        renderer.focusLocation(l);
                    }
                });
                extMenu.add(menuItem);
            }
            MenuScroller.setScrollerFor(extMenu);
            centerInMenu.add(extMenu);
        }

        JMenuItem centerInMainVeh = null;
        if (useVehiclesAndSystems) {
            mainVeh = (console.getMainSystem() != null) ? console.getMainSystem() : "";
            if (mainVeh != null && !mainVeh.isEmpty()) {
                centerInMainVeh = new JMenuItem();
                centerInMainVeh.setText(I18n.text("Center map in: ") + mainVeh);
                centerInMainVeh.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        if (console.getMainSystem() != null) {
                            String mainVeh = console.getMainSystem();
                            ImcSystem sys = ImcSystemsHolder.getSystemWithName(mainVeh);
                            LocationType lt = sys.getLocation();
                            renderer.focusLocation(lt);
                        }
                    }
                });
            }
        }

        Collection<JMenuItem> listItems = new ArrayList<>();
        listItems.add(centerInMenu);
        if (centerInMainVeh != null)
            listItems.add(centerInMainVeh);

        return listItems;
    }
}
