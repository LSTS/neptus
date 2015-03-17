/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 16/07/2010
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import pt.lsts.imc.CcuEvent;
import pt.lsts.imc.MapFeature;
import pt.lsts.imc.MapFeature.FEATURE_TYPE;
import pt.lsts.imc.MapPoint;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author pdias
 * @author zp
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias, ZP", name = "Contact Marker", version = "1.6.0",
// icon = "pt/lsts/neptus/plugins/acoustic/lbl.png",
description = "Mark a contact on the map from a system location.", documentation = "contact-maker/contact-maker.html")
public class ContactMarker extends ConsolePanel implements IEditorMenuExtension, ConfigurationListener,
        SubPanelChangeListener, MainVehicleChangeListener {

    @NeptusProperty(name = "Use Single Mark Addition Mode", userLevel = LEVEL.ADVANCED, 
            description = "Hability to only add marks by inputing the location or using an active system")
    public boolean useSingleMarkAdditionMode = true;

    private Vector<IMapPopup> renderersPopups = new Vector<IMapPopup>();

    public ContactMarker(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {
        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);
        for (IMapPopup str2d : renderersPopups) {
            str2d.addMenuExtension(this);
        }
    }

    private void placeLocationOnMap(LocationType locContact, String markerName, long tstamp) {
        if (getConsole().getMission() == null)
            return;

        String id = markerName + "_" + DateTimeUtil.timeFormaterNoMillis.format(new Date(tstamp));
        boolean validId = false;
        while (!validId) {
            id = JOptionPane.showInputDialog(getConsole(), I18n.text("Please enter new mark name"), id);
            if (id == null)
                return;
            AbstractElement elems[] = MapGroup.getMapGroupInstance(getConsole().getMission()).getMapObjectsByID(id);
            if (elems.length > 0)
                GuiUtils.errorMessage(getConsole(), I18n.text("Add mark"),
                        I18n.text("The given ID already exists in the map. Please choose a different one"));
            else
                validId = true;
        }

        MissionType mission = getConsole().getMission();
        LinkedHashMap<String, MapMission> mapList = mission.getMapsList();
        if (mapList == null)
            return;
        if (mapList.size() == 0)
            return;
        // MapMission mapMission = mapList.values().iterator().next();
        MapGroup.resetMissionInstance(getConsole().getMission());
        MapType mapType = MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];// mapMission.getMap();
        // NeptusLog.pub().info("<###>MARKER --------------- " + mapType.getId());
        MarkElement contact = new MarkElement(mapType.getMapGroup(), mapType);

        contact.setId(id);
        contact.setCenterLocation(locContact);
        mapType.addObject(contact);
        mission.save(false);

        MapPoint point = new MapPoint();
        point.setLat(locContact.getLatitudeRads());
        point.setLon(locContact.getLongitudeRads());
        point.setAlt(locContact.getHeight());
        MapFeature feature = new MapFeature();
        feature.setFeatureType(FEATURE_TYPE.POI);
        feature.setFeature(Arrays.asList(point));
        CcuEvent event = new CcuEvent();
        event.setType(CcuEvent.TYPE.MAP_FEATURE_ADDED);
        event.setId(id);
        event.setArg(feature);
        ImcMsgManager.getManager().broadcastToCCUs(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType
     * , pt.lsts.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(final LocationType loc, IMapPopup source) {

        Vector<JMenuItem> menus = new Vector<JMenuItem>();

        JMenuItem add = new JMenuItem(I18n.text("Add mark"));
        if (useSingleMarkAdditionMode)
            menus.add(add);

        add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                loc.convertToAbsoluteLatLonDepth();
                LocationType locToAdd = LocationPanel.showLocationDialog(getConsole(), I18n.text("Mark location"), loc,
                        getConsole().getMission(), true);
                if (locToAdd == null)
                    return;
                long tstamp = System.currentTimeMillis();
                placeLocationOnMap(locToAdd, I18n.textc("POI", "Short for Place of Interest. Keep it short because is a prefix for a map marker."), tstamp);
            }
        });

        JMenu myLocMenu = new JMenu(I18n.text("Add mark"));
        if (!useSingleMarkAdditionMode)
            menus.add(myLocMenu);

        AbstractAction addToMap = new AbstractAction(I18n.text("Add a mark at this location")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        long tstamp = System.currentTimeMillis();
                        placeLocationOnMap(loc, I18n.textc("POI", "Short for Place of Interest. Keep it short because is a prefix for a map marker."), tstamp);
                    };
                }.start();
            }
        };
        myLocMenu.add(new JMenuItem(addToMap));

        AbstractAction addToMapEdt = new AbstractAction(I18n.text("Add a mark at...")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                LocationType locToAdd = LocationPanel.showLocationDialog(getConsole(), I18n.text("Add a mark at..."), loc,
                        getConsole().getMission(), true);
                if (locToAdd == null)
                    return;
                long tstamp = System.currentTimeMillis();
                placeLocationOnMap(locToAdd, I18n.textc("POI", "Short for Place of Interest. Keep it short because is a prefix for a map marker."), tstamp);
            }
        };
        myLocMenu.add(new JMenuItem(addToMapEdt));

        Vector<VehicleType> avVehicles = new Vector<VehicleType>();

        ImcSystem[] veh = ImcSystemsHolder.lookupActiveSystemVehicles();
        for (int i = 0; i < veh.length; i++)
            avVehicles.add(VehiclesHolder.getVehicleWithImc(veh[i].getId()));

        if (avVehicles.isEmpty() && getConsole().getMainSystem() != null)
            avVehicles.add(VehiclesHolder.getVehicleById(getConsole().getMainSystem()));

        for (VehicleType v : avVehicles) {
            final ImcSystem sys = ImcSystemsHolder.lookupSystemByName(v.getId());
            final String vid = v.getId();
            if (sys.getLocation() != null) {
                AbstractAction actionSys = new AbstractAction(I18n.textf("Add a mark at %system's location", v.getId())) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        placeLocationOnMap(sys.getLocation(), vid, System.currentTimeMillis());
                    }
                };

                JMenuItem menuItem = new JMenuItem(actionSys);
                menuItem.setIcon(ImageUtils.getScaledIcon(v.getPresentationImageHref(), 20, 16));
                myLocMenu.add(menuItem);
            }
        }

        if (getConsole() != null && getConsole().getMission() != null) {
            Vector<MarkElement> marks = MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(
                    MarkElement.class);
            if (marks.size() > 0) {
                JMenu remove = new JMenu(I18n.text("Remove mark"));
                menus.add(remove);
                for (AbstractElement elem : marks) {
                    final MapType markMap = elem.getParentMap();
                    final String markId = elem.getId();

                    AbstractAction rem = new AbstractAction(markId) {
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            markMap.remove(markId);
                            markMap.getMission().save(false);
                            getConsole().updateMissionListeners();
                        }
                    };
                    remove.add(rem);
                    MenuScroller.setScrollerFor(remove, 25);
                }

                JMenu copy = new JMenu(I18n.text("Copy mark location"));
                menus.add(copy);
                for (final AbstractElement elem : marks) {
                    final String markId = elem.getId();
                    AbstractAction rem = new AbstractAction(markId) {
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            ClipboardOwner owner = new ClipboardOwner() {
                                @Override
                                public void lostOwnership(Clipboard clipboard, Transferable contents) {
                                };
                            };
                            Toolkit.getDefaultToolkit()
                                    .getSystemClipboard()
                                    .setContents(new StringSelection(elem.getCenterLocation().getClipboardText()),
                                            owner);
                        }
                    };
                    copy.add(rem);
                    MenuScroller.setScrollerFor(copy, 25);
                }
            }
        }

        return menus;
    }

    @Override
    public void propertiesChanged() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanelChangeListener#subPanelChanged(pt.lsts.neptus.consolebase.
     * SubPanelChangeEvent)
     */
    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {
        if (panelChange == null)
            return;

        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), IMapPopup.class)) {

            IMapPopup sub = (IMapPopup) panelChange.getPanel();

            if (panelChange.getAction() == SubPanelChangeAction.ADDED) {
                renderersPopups.add(sub);
                IMapPopup str2d = sub;
                if (str2d != null) {
                    str2d.addMenuExtension(this);
                }
            }

            if (panelChange.getAction() == SubPanelChangeAction.REMOVED) {
                renderersPopups.remove(sub);
                IMapPopup str2d = sub;
                if (str2d != null) {
                    str2d.removeMenuExtension(this);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
