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
 * Nov 9, 2011
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.gui.MenuScroller;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mystate.MyState;
import pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension;
import pt.up.fe.dceg.neptus.planeditor.IMapPopup;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystem;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystemsHolder;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
public class FeatureFocuser implements IEditorMenuExtension {
    
    protected static final ImageIcon markIcon = ImageUtils.getIcon("images/buttons/addpoint.png");
    protected static final ImageIcon homeIcon = ImageUtils.getIcon("images/buttons/homeRef.png");
    protected static final ImageIcon transpIcon = ImageUtils.getIcon("images/transponder.png");
    protected static final ImageIcon myLocIcon = ImageUtils.getScaledIcon("images/myloc.png", 24, 24);
    
    /**
     * @see pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.up.fe.dceg.neptus.types.coord.LocationType, pt.up.fe.dceg.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
        final StateRenderer2D renderer = source.getRenderer();
        MapGroup mg = renderer.getMapGroup();        
        JMenu menu = new JMenu(I18n.text("Center map in..."));
        
        if (mg == null)
            return null;
        
        final LocationType myLoc = MyState.getLocation();
        if (!myLoc.isLocationEqual(LocationType.ABSOLUTE_ZERO)) {
            JMenuItem myLocItem = new JMenuItem(I18n.text("My location"), myLocIcon);     
            myLocItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    renderer.focusLocation(myLoc);
                }
            });
            menu.add(myLocItem);
            menu.addSeparator();
        }
        
        
        final LocationType location = mg.getHomeRef().getCenterLocation();
        JMenuItem homeItem = new JMenuItem(I18n.text("Home Reference"), homeIcon);     
        homeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                    
                renderer.focusLocation(location);
            }
        });
        menu.add(homeItem);        
        
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
        menu.add(marksMenu);
        
        for (TransponderElement me : mg.getAllObjectsOfType(TransponderElement.class)) {
            final LocationType l = me.getCenterLocation();
            JMenuItem menuItem = new JMenuItem(me.getId(), transpIcon);     
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                    
                    renderer.focusLocation(l);
                }
            });
            menu.add(menuItem);
        }
        menu.addSeparator();
        
//        for (String vt : renderer.vehicleStates.keySet()) {
//            final LocationType l = renderer.getVehicleLocation(vt);
//            final VehicleType veh = VehiclesHolder.getVehicleById(vt);
//            JMenuItem menuItem = veh != null ? new JMenuItem(veh.getId(), veh.getIcon()) : new JMenuItem(vt);     
//            menuItem.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {                    
//                    renderer.focusLocation(l);
//                }
//            });
//            menu.add(menuItem);
//        }

        JMenu vehMenu = new JMenu(I18n.text("Vehicles"));
//        vehMenu.setIcon();
//        mainSystem = ?
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
            JMenuItem menuItem = veh != null ? new JMenuItem(vehS.getId(), vehS.getIcon()) : new JMenuItem(sys.getName());     
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { 
                    renderer.focusLocation(l);
                }
            });
            vehMenu.add(menuItem);
        }
        MenuScroller.setScrollerFor(vehMenu);
        menu.add(vehMenu);

        JMenu otherMenu = new JMenu(I18n.text("Others"));
        //      otherMenu.setIcon();
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
        menu.add(otherMenu);

        JMenu extMenu = new JMenu(I18n.text("External"));
        //      otherMenu.setIcon();
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
        menu.add(extMenu);

        return Arrays.asList((JMenuItem)menu);
    }
}
