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
 * Author: José Pinto
 * 2009/09/21
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;

import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.console.IConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.ConsoleVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.FeatureFocuser;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.renderer2d.VehicleStateListener;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.ImageUtils;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * @author Paulo Dias
 */
@PluginDescription(name = "Map Panel", icon = "images/planning/planning.png", author = "ZP, Paulo Dias", documentation = "planning/planning_panel.html", category = CATEGORY.INTERFACE)
public class MapPanel extends ConsolePanel implements MainVehicleChangeListener, MissionChangeListener,
PlanChangeListener, IPeriodicUpdates, ILayerPainter, ConfigurationListener, IMapPopup,
CustomInteractionSupport, VehicleStateListener, ConsoleVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    private final ImageIcon TAIL_ICON = ImageUtils.getScaledIcon(
            "images/planning/tailOnOff.png", 16, 16);

    @NeptusProperty(name = "Show world map")
    public boolean worldMapShown = true;

    @NeptusProperty
    public int updateMillis = 100;

    @NeptusProperty(name = "Save mission states", description = "Save the mission states whenever the mission is changed")
    public boolean saveMissionStates = false;

    @NeptusProperty(name = "Smooth image resizing")
    public boolean smoothResize = false;

    @NeptusProperty(name = "Antialiasing")
    public boolean antialias = true;

    @NeptusProperty(name = "Interpolate States")
    public boolean interpolate = false;

    @NeptusProperty(name = "Fixed Vehicle Size", description = "Vehicle icon size (0 for real size)")
    public int fixedSize = 0;

    public enum PlacementEnum {
        Left,
        Right,
        Top,
        Bottom
    }

    @NeptusProperty(name = "Toolbar placement", description = "Where to place the toolbar")
    public PlacementEnum toolbarPlacement = PlacementEnum.Bottom;

    protected StateRenderer2D renderer = new StateRenderer2D();
    protected String planId = null;
    protected boolean editing = false;
    protected ToolbarSwitch tailSwitch, dummySwitch;
    protected AbstractAction tailMode, addPlan;

    protected JLabel status = new JLabel();
    protected PlanElement mainPlanPainter = null;
    protected ButtonGroup bg = new ButtonGroup();
    protected JToolBar bottom = new JToolBar(JToolBar.VERTICAL);

    protected LinkedHashMap<VehicleType, SystemPositionAndAttitude> vehicles = new LinkedHashMap<VehicleType, SystemPositionAndAttitude>();

    // Interaction variables
    protected LinkedHashMap<String, StateRendererInteraction> interactionModes = new LinkedHashMap<String, StateRendererInteraction>();
    protected LinkedHashMap<String, ToolbarSwitch> interactionButtons = new LinkedHashMap<String, ToolbarSwitch>();

    private MissionType mission = null;
    private PlanElement planElem;
    private MapGroup mapGroup = null;

    public MapPanel(ConsoleLayout console) {
        super(console);
        removeAll();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder()); //        editor.setEditable(false);

        renderer.setMinDelay(0);
        renderer.setShowWorldMapOnScreenControls(false);
        add(renderer, BorderLayout.CENTER);
        bottom.setFloatable(false);
        bottom.setAlignmentX(JToolBar.CENTER_ALIGNMENT);
        renderer.addMenuExtension(new FeatureFocuser(console));
        AbstractAction tmp = new AbstractAction(I18n.text("Translate"), null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

            }

        };
        dummySwitch= new ToolbarSwitch(tmp);
        bg.add(dummySwitch);
        dummySwitch.setSelected(true);

        tailMode = new AbstractAction(I18n.text("Show tail"), TAIL_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    String mainVehicle = getConsole().getMainSystem();
                    if (mainVehicle != null)
                        renderer.setVehicleTailOn(null);
                }
                else {
                    renderer.setVehicleTailOff(null);
                }
            }
        };
        tailSwitch = new ToolbarSwitch(tailMode);
        tailSwitch.setSelected(false);

        bottom.add(new JSeparator(JSeparator.HORIZONTAL));
        bottom.add(tailSwitch);

        status.setForeground(Color.red);
        bottom.add(status);

        setToolbarPlacement();

    }

    @Override
    public void initSubPanel() {
        setSize(500, 500);

        setMission(getConsole().getMission());

        getConsole().addConsoleVehicleListener(this);

        for (ConsoleSystem v : getConsole().getSystems().values()) {
            v.addRenderFeed(this);
        }
    }

    @Override
    public void cleanSubPanel() {
        ImcMsgManager.getManager().removeListenerFromAllSystems(this);
        getConsole().removeRenderAll(this);
    }

    private void setToolbarPlacement() {
        // Orient and position the tab based on placement property (toolbarPlacement)
        String position = BorderLayout.SOUTH;
        switch (toolbarPlacement) {
            case Left:
                position = BorderLayout.WEST;
                bottom.setOrientation(JToolBar.VERTICAL);
                break;
            case Right:
                position = BorderLayout.EAST;
                bottom.setOrientation(JToolBar.VERTICAL);
                break;
            case Top:
                position = BorderLayout.NORTH;
                bottom.setOrientation(JToolBar.HORIZONTAL);
                break;
            case Bottom:
                position = BorderLayout.SOUTH;
                bottom.setOrientation(JToolBar.HORIZONTAL);
                break;
        }
        add(bottom, position);
    }

    @Subscribe
    public void consume(PlanControlState message) {

        if(getConsole().getMainSystem() != null)
            if (!message.getSourceName().equals(getConsole().getMainSystem()))
                return;

        String planId = message.getPlanId();
        String manId = message.getManId();

        if (getConsole().getPlan() != null && getConsole().getPlan().getId().equals(planId)) {
            if (mainPlanPainter != null)
                mainPlanPainter.setActiveManeuver(manId);
        }
    }

    @Override
    public void missionReplaced(MissionType mission) {
        // editor.setMission(mission);
        setMission(mission);
        if (addPlan != null)
            addPlan.setEnabled(mission != null);
    }

    @Override
    public void missionUpdated(MissionType mission) {
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        if (mainPlanPainter != null)
            mainPlanPainter.setActiveManeuver(null);
    }

    @Override
    public void planChange(PlanType plan) {
        StateRenderer2D r2d = renderer;
        if (mainPlanPainter != null)
            r2d.removePostRenderPainter(mainPlanPainter);

        if (plan != null) {
            PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
            po.setMissionType(plan.getMissionType());
            po.setPlan(plan);
            po.setRenderer(r2d);
            po.setColor(new Color(255, 255, 255, 128));
            po.setShowDistances(false);
            po.setShowManNames(false);
            r2d.addPostRenderPainter(po, I18n.text("Plan Painter"));
            mainPlanPainter = po;
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }

    @Override
    public boolean update() {
        try {
            for (VehicleType vt : vehicles.keySet()) {
                renderer.vehicleStateChanged(vt.getId(), vehicles.get(vt), false);
            }
            renderer.repaint();
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e);
        }
        return true;
    }


    public void setMission(MissionType mission) {
        if (mission == null)
            return;

        mapGroup = MapGroup.getMapGroupInstance(mission);
        renderer.setMapGroup(mapGroup);
        this.mission = mission;
    }

    public void setPlan(PlanType plan) {
        if (plan == null) {
            renderer.removePostRenderPainter(planElem);
            return;
        }
        if (plan.getMissionType() != mission)
            setMission(plan.getMissionType());

        renderer.removePostRenderPainter(planElem);

        planElem = new PlanElement(mapGroup, new MapType());
        planElem.setTransp2d(1.0);
        renderer.addPostRenderPainter(planElem, "Plan Layer");
        planElem.setRenderer(renderer);
        planElem.setPlan(plan);

        renderer.repaint();
    }

    @Override
    public void propertiesChanged() {
        renderer.setSmoothResizing(smoothResize);
        renderer.setAntialiasing(antialias);
        renderer.setFixedVehicleWidth(fixedSize);
        renderer.setWorldMapShown(worldMapShown);
        setToolbarPlacement(); // Refresh toolbar position
    }

    public void addLayer(final IConsoleLayer layer) {
        final String name = layer.getName();
        AbstractAction custom = new AbstractAction(layer.getName(), ImageUtils.getScaledIcon(
                layer.getIcon(), 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    renderer.addPostRenderPainter(layer, name);
                }
                else {
                    renderer.removePostRenderPainter(layer);
                }
            }
        };
        ToolbarSwitch tswitch = new ToolbarSwitch(I18n.text(name), custom);
        tswitch.setSelected(false);
        bottom.add(tswitch, bottom.getComponentCount());
        invalidate();
        revalidate();
    }

    public void removeLayer(final IConsoleLayer layer) {
        final String name = layer.getName();
        renderer.removePostRenderPainter(layer);
        for (int i = 0; i < bottom.getComponentCount(); i++) {
            Component c = bottom.getComponent(i);
            if (c instanceof ToolbarSwitch) {
                ToolbarSwitch ts = (ToolbarSwitch)c;
                if (ts.getToolTipText().equals(name)) {
                    bottom.remove(ts);
                    break;
                }
            }
        }
        invalidate();
        revalidate();
    }

    @Override
    public void addInteraction(StateRendererInteraction interaction) {

        try {
            final String name = interaction.getName();
            if (!interactionModes.containsKey(name))
                interactionModes.put(name, interaction);
            else
                return;

            AbstractAction custom = new AbstractAction(interaction.getName(), ImageUtils.getScaledIcon(
                    interaction.getIconImage(), 16, 16)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (((ToolbarSwitch) e.getSource()).isSelected()) {
                        StateRendererInteraction ri = interactionModes.get(name);

                        if (ri.isExclusive()) {
                            if (renderer.getActiveInteraction() != null) {
                                renderer.getActiveInteraction().setActive(false, renderer);
                                if (renderer.getActiveInteraction() == ri) {
                                    renderer.setActiveInteraction(null);
                                    dummySwitch.setSelected(true);
                                    return;
                                }
                            }
                        }
                        renderer.setActiveInteraction(ri);
                        ri.setActive(true, renderer);
                    }
                }
            };
            ToolbarSwitch tswitch = new ToolbarSwitch(I18n.text(name), custom);
            if (tswitch.isEnabled())
                bg.add(tswitch);
            bottom.add(tswitch, 0);
            interactionButtons.put(interaction.getName(), tswitch);
            tswitch.setSelected(false);
            interaction.setAssociatedSwitch(tswitch);
            invalidate();
            revalidate();
        }
        catch (AbstractMethodError e) {
            AbstractMethodError e1 = new AbstractMethodError(e.getMessage() + " :: " + interaction.getClass().getName()
                    + " missed the complete implementation of " + StateRendererInteraction.class.getName()
                    + "!!! Recompiling the plugin jar might resolve " + "this issue.");
            e1.setStackTrace(e.getStackTrace());
            e1.printStackTrace();
        }
    }

    @Override
    public Collection<StateRendererInteraction> getInteractionModes() {
        return interactionModes.values();
    }

    @Override
    public void removeInteraction(StateRendererInteraction interaction) {
        renderer.removeInteraction(interaction);
        ToolbarSwitch sw = interactionButtons.get(interaction.getName());
        if (sw != null)
            bottom.remove(sw);
        doLayout();
    }

    @Override
    public void setActiveInteraction(StateRendererInteraction interaction) {
        if (interaction == null) {
            renderer.setActiveInteraction(null);
            return;
        }
        ToolbarSwitch sw = interactionButtons.get(interaction.getName());
        if (sw != null)
            sw.doClick();
        else
            renderer.setActiveInteraction(interaction);
    }

    @Override
    public StateRendererInteraction getActiveInteraction() {
        return renderer.getActiveInteraction();
    }

    @Override
    public void setVehicleState(VehicleType vehicle, SystemPositionAndAttitude state) {
        vehicles.put(vehicle, state);
    }

    @Override
    public void consoleVehicleChange(VehicleType v, int status) {
        if (status != ConsoleVehicleChangeListener.VEHICLE_REMOVED) {
            getConsole().addRender(v.getId(), this);
            ImcMsgManager.getManager().addListener(this, v.getId());
        }
    }

    @Override
    public void addPreRenderPainter(Renderer2DPainter painter) {
        renderer.addPreRenderPainter(painter);
    }

    @Override
    public void removePreRenderPainter(Renderer2DPainter painter) {
        renderer.removePreRenderPainter(painter);
    }


    @Override
    public boolean addPostRenderPainter(Renderer2DPainter painter, String name) {
        // NeptusLog.pub().info("<###>Adding a post render painter: "+name);
        return renderer.addPostRenderPainter(painter, name);
    }

    @Override
    public boolean removePostRenderPainter(Renderer2DPainter painter) {
        return renderer.removePostRenderPainter(painter);
    }

    @Override
    public StateRenderer2D getRenderer() {
        return renderer;
    }

    @Override
    public boolean addMenuExtension(IEditorMenuExtension extension) {
        return renderer.addMenuExtension(extension);
    }

    @Override
    public boolean removeMenuExtension(IEditorMenuExtension extension) {
        return renderer.removeMenuExtension(extension);
    }

    @Override
    public Collection<IEditorMenuExtension> getMenuExtensions() {
        return renderer.getMenuExtensions();
    }
}
