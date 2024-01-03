/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Paulo Dias
 * 26/10/2023
 */
package pt.lsts.neptus.plugins.remoteactionsextra;

import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EntityState;
import pt.lsts.imc.RemoteActions;
import pt.lsts.imc.RemoteActionsRequest;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.MathMiscUtils;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@PluginDescription(name = "Remote Actions Extra", version = "0.1", category = PluginDescription.CATEGORY.INTERFACE,
    author = "Paulo Dias", icon = "images/control-mode/teleoperation.png",
    description = "This plugin listen for non motion related remote actions and displays its controls.")
@Popup(name = "Remote Actions Extra", width = 300, height = 200, pos = Popup.POSITION.BOTTOM, accelerator = KeyEvent.VK_3)
public class RemoteActionsExtra extends ConsolePanel implements MainVehicleChangeListener, ConfigurationListener {

    static final boolean DEFAULT_AXIS_DECIMAL_VAL = false;
    private static final int DECIMAL_HOUSES_FOR_DECIMAL_AXIS = 6;
    private static final int RANGE_127_AXIS_VAL = 127;

    enum ActionTypeEnum {
        BUTTON,
        AXIS,
        SLIDER,
        HALF_SLIDER
    };

    private final String typeVerbAxis = "Axis";
    private final String typeVerbButton = "Button";
    private final String typeVerbSlider = "Slider";
    private final String typeVerbHalfSlider = "HalfSlider";
    private final String typeVerbHalfSlider2 = "Half-Slider";

    private final String actionVerbRanges = "Ranges";
    private final String typeVerbRangesRange127 = "Range127";
    private final String typeVerbRangesDecimal = "Decimal";

    private final List<String> motionVerbs = new ArrayList<String>() {{
        add("Accelerate");
        add("Decelerate");
        add("Stop");
        add("Heading"); add("turning"); add("yaw"); add("rotate");
        add("Trust"); add("surge"); add("forward"); add("throttle");
        add("Lateral"); add("sway"); add("sideways");
        add("Vertical"); add("heave"); add("up"); add("ascend"); add("verticalrate"); add("depth"); add("z");
        add("Roll"); add("bank");
        add("Pitch");

    }};

    private final List<String> motionRelatedWords = new ArrayList<String>() {{
        add("motor");
        add("motion");
        add("thruster");
        add("thrust");
        add("throttle");
    }};

    private final List<String> actionHorizontalOverrideModifierWords = new ArrayList<String>() {{
        add("pan");
        add("side");
    }};

    private final Map<String, ActionTypeEnum> extraActionsTypesMap = Collections.synchronizedMap(new LinkedHashMap<>());

    private final RemoteActionsState curState = new RemoteActionsState();
    private final RemoteActionsState lastState = new RemoteActionsState();

    private String lastCmdBuilt = "";

    private final TakeControlMonitor takeControlMonitor;

    @NeptusProperty(name = "OBS Entity Name", userLevel = NeptusProperty.LEVEL.ADVANCED,
        description = "Used to check the state of the OBS take control status.")
    public String obsEntityName = "OBS Broker";

    public RemoteActionsExtra(ConsoleLayout console) {
        this(console, false);
    }

    public RemoteActionsExtra(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);
        takeControlMonitor = new TakeControlMonitor(this);
        takeControlMonitor.setEntityName(obsEntityName);
    }

    @Override
    public void initSubPanel() {
        resetUIWithActions();
    }

    @Override
    public void cleanSubPanel() {
        takeControlMonitor.setButton(null);
    }

    @Override
    public void propertiesChanged() {
        takeControlMonitor.setEntityName(obsEntityName);
    }

    private synchronized void resetUIWithActions() {
        takeControlMonitor.setButton(null);

        removeAll();
        setLayout(new MigLayout("insets 10px"));

        if (extraActionsTypesMap.isEmpty()) {
            add(new JLabel("No actions available", SwingConstants.CENTER), "dock center");
            invalidate();
            validate();
            repaint(100);
            return;
        }

        // Let us process the actions list
        List<List<String>> groupedActions = groupActionsBySimilarity(extraActionsTypesMap.keySet(), true);
        groupedActions = processActions(groupedActions, 2, false);

        int grpIdx = 0;
        for (List<String> grp1 : groupedActions) {
            grpIdx++;
            String lastAct = grp1.get(grp1.size() - 1);
            for (String action : grp1) {
                String wrapLay = "";
                if (lastAct.equals(action)) {
                    wrapLay = "wrap";
                }
                switch (extraActionsTypesMap.get(action)) {
                    case BUTTON:
                        JButton button = new JButton(action);
                        button.addActionListener(e -> {
                            curState.changeButtonActionValue(action, 1);
                        });
                        String lay = "dock center, sg grp" + grpIdx;
                        lay += ", " + wrapLay;
                        add(button, lay);
                        if ("Take Control".equalsIgnoreCase(action)) {
                            takeControlMonitor.setButton(button);
                            takeControlMonitor.askedControl();
                        }
                        break;
                    case AXIS:
                        // TODO
                    case SLIDER:
                        // TODO
                    case HALF_SLIDER:
                        // TODO
                        break;
                }
            }
        }

        invalidate();
        validate();
        repaint(100);
    }

    @Subscribe
    public void on(ConsoleEventMainSystemChange evt) {
        configureActions("", DEFAULT_AXIS_DECIMAL_VAL, false);
        takeControlMonitor.on(evt);
    }

    @Subscribe
    public void on(RemoteActionsRequest msg) {
        if (!msg.getSourceName().equals(getMainVehicleId())) {
            return;
        }

        if (msg.getOp() != RemoteActionsRequest.OP.REPORT) return;

        configureActions(msg.getActions(), DEFAULT_AXIS_DECIMAL_VAL, false);
    }

    @Subscribe
    public void on(EntityState msg) {
        takeControlMonitor.on(msg);
    }

    @Subscribe
    public void on(VehicleState msg) {
        takeControlMonitor.on(msg);
    }

    @Periodic(millisBetweenUpdates = 1_000)
    public void sendRemoteActionsToMainSystem() {
        try {
            String actionsStr = "";
            synchronized (extraActionsTypesMap) {
                actionsStr = buildCmdAndReset();
            }
            if (actionsStr.isEmpty()) return;

            RemoteActions remoteActionsMsg = new RemoteActions();
            remoteActionsMsg.setActions(actionsStr);
            send(remoteActionsMsg);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildCmdAndReset() {
        String retCmd = "";
        List<String> ret = new ArrayList<>();

        RemoteActionsState savedCurState = new RemoteActionsState();
        savedCurState.resetWith(curState);

        curState.extraButtonActionsMap.forEach((action, value) ->
                curState.extraButtonActionsMap.put(action, value > 1 ? value : 0));

        if (lastState.isEmpty()) {
            lastState.resetWith(curState);
        }

        // For extra Buttons
        savedCurState.extraButtonActionsMap.forEach((action, value) -> {
          if (isActionMotion(action)) return;

          ret.add(value > 0
              ? action + "=1"
              : (lastState.extraButtonActionsMap.containsKey(action) && lastState.extraButtonActionsMap.get(action) == 1 ? action + "=0" : ""));
        });
        // For extra Axis
        savedCurState.extraAxisActionsMap.forEach((action, value) -> {
            if (isActionMotion(action)) {
                return;
            }

            String val = adjustAxisValue(value);
            ret.add((lastState.extraButtonActionsMap.containsKey(action) || !Objects.equals(lastState.extraAxisActionsMap.get(action), value)
                    ? action + "=" + ("0.0".equalsIgnoreCase(val) ? "0" : val)
                    : ""));
        });

        ret.removeIf(String::isEmpty);
        lastCmdBuilt = retCmd = String.join(";", ret);
        lastState.resetWith(savedCurState);
        return retCmd;
    }

    private boolean isActionMotion(String action) {
        if (action.trim().isEmpty()) {
            return false;
        }
        for (String word : motionVerbs) {
            if (action.equalsIgnoreCase(word.trim())) {
                return true;
            }
        }
        for (String word : motionRelatedWords) {
            if (action.toLowerCase().contains(word.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }

    private String adjustAxisValue(double value) {
        if (curState.decimalAxis) {
            return String.valueOf(MathMiscUtils.round(value, DECIMAL_HOUSES_FOR_DECIMAL_AXIS));
        }
        else {
            return String.valueOf(Math.round(value * RANGE_127_AXIS_VAL));
        }
    }

    private void configureActions(LinkedHashMap<String, String> actions, boolean decimalAxis, boolean resetActionVerbs) {
        String str = actions.keySet().stream().map(s -> s + "=" + actions.get(s)).collect(Collectors.joining(";"));
        configureActions(str, decimalAxis, resetActionVerbs);
    }

    private void configureActions(String actionsString, boolean decimalAxis, boolean resetActionVerbs) {
        synchronized (extraActionsTypesMap) {
            curState.decimalAxis = decimalAxis;

            if (resetActionVerbs) {
                resetActionsVerbs(decimalAxis);
            } else {
                reset();
            }

            if (actionsString == null || actionsString.isEmpty()) {
                resetUIWithActions();
                return;
            }

            try {
                String[] keyPair = actionsString.split(";");
                for (String elem : keyPair) {
                    try {
                        String[] actPair = elem.trim().split("=");
                        String actTxt = actPair[0].trim();
                        String typeTxt = actPair[1].trim();

                        if (actTxt.isEmpty()) continue;

                        if ("Exit".equalsIgnoreCase(actTxt) || isActionMotion(actTxt.toLowerCase())) {
                            // Do nothing, just avoiding going to extra actions
                        } else if (actionVerbRanges.equalsIgnoreCase(actTxt)) {
                            // test if ranges decimal
                            if (typeVerbRangesDecimal.equalsIgnoreCase(typeTxt)) {
                                curState.decimalAxis = true;
                            } else if (typeVerbRangesRange127.equalsIgnoreCase(typeTxt)) {
                                curState.decimalAxis = false;
                            }
                        } else {
                            if (typeVerbButton.equalsIgnoreCase(typeTxt.toLowerCase())) {
                                curState.extraButtonActionsMap.put(actTxt,  0);
                                extraActionsTypesMap.put(actTxt, ActionTypeEnum.BUTTON);
                            } else if (typeVerbAxis.equalsIgnoreCase(typeTxt.toLowerCase())) {
                                curState.extraAxisActionsMap.put(actTxt,0.0);
                                extraActionsTypesMap.put(actTxt, ActionTypeEnum.AXIS);
                            } else if (typeVerbSlider.equalsIgnoreCase(typeTxt.toLowerCase())) {
                                curState.extraAxisActionsMap.put(actTxt,0.0);
                                extraActionsTypesMap.put(actTxt, ActionTypeEnum.SLIDER);
                            } else if (typeVerbHalfSlider.equalsIgnoreCase(typeTxt.toLowerCase()) ||
                                    typeVerbHalfSlider2.equalsIgnoreCase(typeTxt.toLowerCase())) {
                                curState.extraAxisActionsMap.put(actTxt,0.0);
                                extraActionsTypesMap.put(actTxt, ActionTypeEnum.HALF_SLIDER);
                            }
                        }
                    } catch (Exception e) {
                        NeptusLog.pub().warn("Not possible to parse one remote action \"" +
                                elem + "\" with error " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                NeptusLog.pub().warn("'Not possible to parse remote actions \"" +
                        actionsString + "\" with error " + e.getMessage());
            }

            resetUIWithActions();
        }
    }

    private String extractActionGroupKeyword(String actionText) {
        String action = actionText.toLowerCase();
        if (action.equals("arm") || action.equals("disarm")) return "arm";
        if (action.contains("motor")) return "motor";
        if (action.contains("light")) return "light";
        if (action.contains("camera")) return "camera";
        if (action.equals("take control") || action.equals("relinquish control")) {
            return "take control";
        }
        if (action.equals("ready") || action.equals("stopped")) return "mode ready";

        return action;
    }

    private List<List<String>> groupActionsBySimilarity(Set<String> actionList, boolean disableMotionRelatedRemoteActions) {
        List<List<String>> actionGroups = new ArrayList<>();

        for (String action : actionList) {
            if (disableMotionRelatedRemoteActions && isActionMotion(action)) continue;

            if (actionGroups.isEmpty()) {
                List<String> newGroup = new ArrayList<>();
                newGroup.add(action);
                actionGroups.add(newGroup);
                continue;
            }

            String actionGroupKeyword = extractActionGroupKeyword(action);
            boolean added = false;
            for (List<String> group : actionGroups) {
                String groupType = extractActionGroupKeyword(group.get(0));
                if (groupType.equals(actionGroupKeyword)) {
                    group.add(action);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<String> newGroup = new ArrayList<>();
                newGroup.add(action);
                actionGroups.add(newGroup);
            }
        }
        return actionGroups;
    }

    private List<List<String>> processActions(List<List<String>> groupedActions, int maxElemsPerActionGroup, boolean isPortrait) {
        return groupedActions.stream()
                .map(ll -> slices(ll, maxElemsPerActionGroup))
                .flatMap(List::stream)
                .map(ll -> {
                    if (!isPortrait) {
                        List<List<String>> ret = new ArrayList<>();
                        ret.add(ll);
                        return ret;
                    }

                    boolean split = false;
                    for (String action : ll) {
                        ActionTypeEnum at = extraActionsTypesMap.get(action);
                        if (at == null) {
                            at = ActionTypeEnum.SLIDER;
                        }
                        if (at == ActionTypeEnum.SLIDER || at == ActionTypeEnum.HALF_SLIDER) {
                            split = !testIfForVertical(action);
                            if (split) {
                                break;
                            }
                        }
                    }
                    if (split) {
                        return slices(new ArrayList<>(ll), 1);
                    }

                    List<List<String>> ret = new ArrayList<>();
                    ret.add(ll);
                    return ret;
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static List<List<String>> slices(List<String> list, int size) {
        List<List<String>> slices = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            int end = Math.min(i + size, list.size());
            slices.add(list.subList(i, end));
        }
        return slices;
    }
    private boolean testIfForVertical(String action) {
        if (action.trim().isEmpty()) return false;

        boolean candidateForVertical = false;

        List<String> allMotionWords = new ArrayList<>(motionRelatedWords);
        for (String word : allMotionWords) {
            if (action.toLowerCase().contains(word.toLowerCase())) {
                candidateForVertical = true;
                break;
            }
        }

        if (!candidateForVertical) return false;

        for (String word : actionHorizontalOverrideModifierWords) {
            if (action.toLowerCase().contains(word.toLowerCase())) {
                candidateForVertical = false;
                break;
            }
        }

        return candidateForVertical;
    }

    void resetActionsVerbs(boolean decimalAxis) {
        synchronized (extraActionsTypesMap) {
            curState.decimalAxis = decimalAxis;
            curState.extraButtonActionsMap.forEach((action, value) -> curState.extraButtonActionsMap.put(action,0));
            curState.extraAxisActionsMap.forEach((action, value) -> curState.extraAxisActionsMap.put(action, 0.0));
            lastState.reset();
        }
    }

    void reset() {
        synchronized (extraActionsTypesMap) {
            curState.decimalAxis = DEFAULT_AXIS_DECIMAL_VAL;
            lastState.reset();
            extraActionsTypesMap.clear();
            curState.extraButtonActionsMap.clear();
            curState.extraAxisActionsMap.clear();
        }
    }
}
