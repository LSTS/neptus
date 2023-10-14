/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * May 25, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBInfo;
import pt.lsts.neptus.gui.MissionBrowser.State;
import pt.lsts.neptus.gui.MissionTreeModel.NodeInfoKey;
import pt.lsts.neptus.gui.tree.ExtendedTreeNode;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.misc.LBLRangesTimer;
import pt.lsts.neptus.types.mission.ChecklistMission;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.VehicleMission;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 * @author pdias
 */
public class MissionTreeCellRenderer extends DefaultTreeCellRenderer {
    // private static final ImageIcon PLAN_EXT_ICON = new ExtendedIcon(ImageUtils.getImage("images/menus/plan.png"));
    // private static final ImageIcon ACOUSTIC_PLAN_ICON = GuiUtils.getLetterIcon('A', Color.BLACK,
    // ColorUtils.setTransparencyToColor(Color.GREEN, 150), 16);
    // private static final ImageIcon PLAN_AC_EXT_ICON = new ExtendedIcon(ImageUtils.getImage("images/menus/plan.png"),
    // ACOUSTIC_PLAN_ICON.getImage());
    // private static final ImageIcon MULTI_PLAN_EXT_ICON = ImageUtils.getScaledIcon("images/buttons/plan_plus.png", 16,
    // 16);
    private final ImageIcon MAP_ICON;
    private final ImageIcon SETTINGS_ICON;
    private final ImageIcon CHECKLIST_ICON;
    private final ImageIcon DIR_ICON;
    private final ImageIcon DIR_CLOSED_ICON;
    private final ImageIcon HOMEREF_ICON;
    // private final ImageIcon TRANSPONDER_ICON;
    private final ImageIcon START_ICON;
    private final ImageIcon PLAN_LOCAL, PLAN_LOCAL_ACOUSTIC, PLAN_REMOTE, PLAN_REMOTE_ACOUSTIC, PLAN_UNSYNC,
            PLAN_UNSYNC_ACOUSTIC, PLAN_SYNC, PLAN_SYNC_ACOUSTIC;
    
    public MissionTreeCellRenderer() {
        MAP_ICON = ImageUtils.createImageIcon("images/menus/mapeditor.png");
        SETTINGS_ICON = ImageUtils.createImageIcon("images/menus/settings.png");
        CHECKLIST_ICON = ImageUtils.createImageIcon("images/buttons/checklist.png");
        DIR_ICON = ImageUtils.createImageIcon("images/menus/open.png");
        DIR_CLOSED_ICON = ImageUtils.createImageIcon("images/menus/folder_closed.png");
        HOMEREF_ICON = ImageUtils.getScaledIcon("images/buttons/homeRef.png", 16, 16);
        // TRANSPONDER_ICON = new ExtendedIcon(ImageUtils.getScaledImage("images/transponder.png", 16, 16));
        START_ICON = ImageUtils.getScaledIcon("images/flag2_green32.png", 16, 16);
        PLAN_LOCAL = ImageUtils.getScaledIcon("images/plans/planLocal.png", 16, 16);
        PLAN_LOCAL_ACOUSTIC = ImageUtils.getScaledIcon("images/plans/planLocalAcoustic.png", 16, 16);
        PLAN_REMOTE = ImageUtils.getScaledIcon("images/plans/planRemote.png", 16, 16);
        PLAN_REMOTE_ACOUSTIC = ImageUtils.getScaledIcon("images/plans/planRemoteAcoustic.png", 16, 16);
        PLAN_UNSYNC = ImageUtils.getScaledIcon("images/plans/planUnsync.png", 16, 16);
        PLAN_UNSYNC_ACOUSTIC = ImageUtils.getScaledIcon("images/plans/planUnsyncAcoustic.png", 16, 16);
        PLAN_SYNC = ImageUtils.getScaledIcon("images/plans/planSync.png", 16, 16);
        PLAN_SYNC_ACOUSTIC = ImageUtils.getScaledIcon("images/plans/planSyncAcoustic.png", 16, 16);
    }
    
    private enum Icons{
        PATH_SOURCE("images/"),
        PLAN("plan"),
        PLAN_PATH("plans/"),
        BEACON("beacon"),
        BEACONS_PATH("beacons/"),
        ACOUSTIC("Acoustic"),
        MULTIPLE_VEHICLES("Multp"),
        EXTENSION(".png");
        private final String name;
        private Icons(String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }


    }

    // Modifiable by properties interface
    public int maxAcceptableElapsedTime;
    public boolean debugOn = false;
    private static final long serialVersionUID = -2666337254439313801L;
    private static HashMap<String, ImageIcon> VEHICLES_ICONS = new HashMap<String, ImageIcon>();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        setToolTipText(null); // no tool tip

        if (leaf) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

            if ((node.getUserObject() instanceof MapType) || (node.getUserObject() instanceof MapMission)) {
                setIcon(MAP_ICON);
                setToolTipText("Edit the map");
            }

            else if (node.getUserObject() instanceof PlanType) {
                State state = State.LOCAL;
                try {
                    if (node instanceof ExtendedTreeNode) {
                        ExtendedTreeNode ptn = (ExtendedTreeNode) node;
                        State sync = (State) ptn.getUserInfo().get(NodeInfoKey.SYNC.name());
                        if (sync != null)
                            state = sync;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                PlanType plan = ((PlanType) node.getUserObject());
                try {
                    setPlanIcon(plan.getId(), state, plan.hasMultipleVehiclesAssociated());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                setToolTipText(plan.toStringWithVehicles());
            }

            else if (node.getUserObject() instanceof PlanDBInfo) {
                PlanDBInfo plan = ((PlanDBInfo) node.getUserObject());
                setPlanIcon(plan.getPlanId(), State.REMOTE, false);
            }

            else if (node.getUserObject() instanceof HomeReference) {
                setIcon(HOMEREF_ICON);
                setText(new LocationType(((HomeReference) node.getUserObject())).toString());
                setToolTipText("View/Edit home reference");
            }
            else if (node.getUserObject() instanceof MarkElement) {
                setIcon(START_ICON);
                setText(((MarkElement) node.getUserObject()).getPosition().toString());
                setToolTipText("View/Edit navigation startup position");
            }

            else if (node.getUserObject() instanceof VehicleMission) {
                ImageIcon vehicleIcon = null;
                try {
                    vehicleIcon = VEHICLES_ICONS.get(((VehicleMission) node.getUserObject()).getVehicle().getId());
                    if (vehicleIcon == null) {
                        Image vehicleImage;
                        if (!((VehicleMission) node.getUserObject()).getVehicle().getPresentationImageHref()
                                .equalsIgnoreCase(""))
                            vehicleImage = ImageUtils.getImage(((VehicleMission) node.getUserObject()).getVehicle()
                                    .getPresentationImageHref());
                        else
                            vehicleImage = ImageUtils.getImage(((VehicleMission) node.getUserObject()).getVehicle()
                                    .getSideImageHref());

                        int desiredWidth = 16, desiredHeight = 16;

                        int height = vehicleImage.getHeight(null);
                        int width = vehicleImage.getWidth(null);

                        if (height > width) {
                            desiredWidth = (int) (16.0 * ((double) width / (double) height));
                        }
                        else {
                            desiredHeight = (int) (16.0 * ((double) height / (double) width));
                        }

                        ImageIcon vIcon = new ImageIcon(vehicleImage.getScaledInstance(desiredWidth, desiredHeight,
                                Image.SCALE_DEFAULT));
                        setIcon(vIcon);
                        VEHICLES_ICONS.put(((VehicleMission) node.getUserObject()).getVehicle().getId(), vIcon);
                    }
                    else
                        setIcon(vehicleIcon);
                }
                catch (Exception e) {
                    NeptusLog.pub().info(((VehicleMission) node.getUserObject()).getId() + " vehicle not found!");
                }

                setToolTipText("View/Edit the vehicle information");
            }

            else if ((node.getUserObject() == "Mission Information") || (node.getUserObject() == "Info")) {
                setIcon(SETTINGS_ICON);
            }

            else if ((node.getUserObject() instanceof ChecklistType)
                    || (node.getUserObject() instanceof ChecklistMission)) {
                setIcon(CHECKLIST_ICON);
                setToolTipText("Edit the checklist");
            }

            else if (node.getUserObject() instanceof TransponderElement) {
                State state = State.LOCAL;
                ExtendedTreeNode ptn = (ExtendedTreeNode) node;
                TransponderElement nodeObj = (TransponderElement) node.getUserObject();
                HashMap<String, Object> info = ptn.getUserInfo();
                ImcSystem imcSystem = ImcSystemsHolder
                        .lookupSystemByName((String) info.get(NodeInfoKey.VEHICLE.name()));
                setBeaconLabel(nodeObj, imcSystem);
                setBeaconIcon(state, ptn);
            }
            else if (node.getUserObject() == "Settings") {
                setIcon(SETTINGS_ICON);
            }
            else {
                setIcon(expanded ? DIR_ICON : DIR_CLOSED_ICON);
            }
        }
        else {
            setIcon(expanded ? DIR_ICON : DIR_CLOSED_ICON);            
        }

        setPreferredSize(new Dimension(200, 120));
        return this;
    }

    private void setBeaconIcon(State state, ExtendedTreeNode ptn) {
        State sync = (State) ptn.getUserInfo().get(NodeInfoKey.SYNC.name());
        if (sync != null)
            state = sync;
        StringBuilder fileName = new StringBuilder(Icons.PATH_SOURCE.getName());
        fileName.append(Icons.BEACONS_PATH.getName());
        fileName.append(Icons.BEACON.getName());
        fileName.append(state.getFileName());
        fileName.append(Icons.EXTENSION.getName());
        setIcon(ImageUtils.getIcon(fileName.toString()));
    }

    private void setBeaconLabel(TransponderElement nodeObj, ImcSystem imcSystem) {
        setText(nodeObj.getDisplayName());
        if (imcSystem != null) {
            LBLRangesTimer timer = (LBLRangesTimer) imcSystem.retrieveData(nodeObj.getIdentification());
            if (timer != null) {
                String color;
                int time = timer.getTime();
                if (time == -1) {
                    setText(nodeObj.getDisplayName());
                }
                else {
                    if (time <= maxAcceptableElapsedTime) {
                        color = "green";
                    }
                    else {
                        color = "red";
                    }
                    int minutes = time / 60;
                    int seconds = time % 60;
                    String formatedTime = (minutes > 0) ? (minutes + "min " + seconds + "s") : (seconds + "s");
                    setText("<html>" + nodeObj.getDisplayName() + " (<span color='" + color + "'>&#916;t "
                            + formatedTime + "</span>)");
                }
            }
        }
    }

    private void setPlanIcon(String planId, State state, boolean hasMultpVehicles) {
        StringBuilder fileNameBuilder = new StringBuilder(Icons.PLAN.getName());
        fileNameBuilder.append(state.getFileName());
        if (planId.length() <= GeneralPreferences.maximumSizePlanNameForAcoustics) {
            fileNameBuilder.append(Icons.ACOUSTIC.getName());
        }

        if (hasMultpVehicles) {
            fileNameBuilder.append(Icons.MULTIPLE_VEHICLES.getName());
        }
        String fileName = fileNameBuilder.toString();
        switch (fileName) {
            case "planLocal":
                setIcon(PLAN_LOCAL);
                break;
            case "planLocalAcoustic":
                setIcon(PLAN_LOCAL_ACOUSTIC);
                break;
            case "planRemote":
                setIcon(PLAN_REMOTE);
                break;
            case "planRemoteAcoustic":
                setIcon(PLAN_REMOTE_ACOUSTIC);
                break;
            case "planUnsync":
                setIcon(PLAN_UNSYNC);
                break;
            case "planUnsyncAcoustic":
                setIcon(PLAN_UNSYNC_ACOUSTIC);
                break;
            case "planSync":
                setIcon(PLAN_SYNC);
                break;
            case "planSyncAcoustic":
                setIcon(PLAN_SYNC_ACOUSTIC);
                break;

            default:
                NeptusLog.pub().error("No match for " + planId + " " + state + " " + fileName+" need to add support for this state.");
                break;
        }
    }
}
