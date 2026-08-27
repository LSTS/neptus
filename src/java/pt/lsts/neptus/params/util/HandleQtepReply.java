/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: João Cordeiro
 * Dec 19, 2025
 */
package pt.lsts.neptus.params.util;

import org.dom4j.Element;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.QueryTypedEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemConfigurationEditorPanel;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HandleQtepReply implements NeptusMessageListener, MessageListener<MessageInfo, IMCMessage> {

    private static final long QTEPS_TIMEOUT_MS = 10_000;
    private static final long NOTIFICATION_COOLDOWN_MS = 10_000;

    private volatile boolean isSyncing = false;
    private volatile long syncStartTime = 0;

    private int requestId = -1;
    private String systemId;
    private String currentSystemId;
    private SystemConfigurationEditorPanel owner;
    private ConsoleLayout console;

    private List<String> expectedEntities;
    private final Set<String> receivedEntities =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<String, Map<String, SystemProperty>> pendingQtepsBySystem =
            new ConcurrentHashMap<>();
    private final Map<String, SystemProperty> activeSyncProps =
            new ConcurrentHashMap<>();

    private final Map<String, Long> lastNotificationBySystem =
            new ConcurrentHashMap<>();
    private long lastNotificationTime = 0;
    public HandleQtepReply(ConsoleLayout console, String systemId) {
        this.console = console;
        this.systemId = systemId;
    }

    public void startSync(SystemConfigurationEditorPanel owner, String systemId, List<String> expectedEntities,
                          int requestId, boolean isSyncing) {

        if (!isSyncing)
            return;

        this.owner = owner;
        this.systemId = systemId;
        this.expectedEntities = expectedEntities;
        this.requestId = requestId;
        this.currentSystemId = systemId;

        PeriodicUpdatesService.registerPojo(this);

        this.isSyncing = isSyncing;
        syncStartTime = System.currentTimeMillis();

        receivedEntities.clear();
        activeSyncProps.clear();
    }

    @Override
    public void messageArrived(IMCMessage message) {

        if (!(message instanceof QueryTypedEntityParameters))
            return;

        QueryTypedEntityParameters qtep = (QueryTypedEntityParameters) message;
        if (qtep.getOp() != QueryTypedEntityParameters.OP.REPLY)
            return;

        String srcSystemId = qtep.getSourceName();
        boolean requestedSync = isSyncing && qtep.getRequestId() == requestId;
        
        boolean withinSyncTimeout = syncStartTime > 0 && (System.currentTimeMillis() - syncStartTime) < QTEPS_TIMEOUT_MS;
        
        handleQtepReply(qtep, srcSystemId, requestedSync);

        if ((isSyncing || withinSyncTimeout) && srcSystemId.equals(systemId)) {
        } else if (!requestedSync) {
            showRateLimitedNotification(srcSystemId);
        }
    }

    private void handleQtepReply(QueryTypedEntityParameters qtep, String currentSystemId, boolean requestedSync) {
        try {
            String entityName = qtep.getEntityName();

            Element section = QtepToSectionConverter.convertToSection(qtep);
            if (section == null) {
                NeptusLog.pub().warn("Failed to convert QTEP to section");
                return;
            }

            Map<String, SystemProperty> sectionProps = ConfigurationManager.getInstance().processSection(section, systemId);

            if(requestedSync) {
                receivedEntities.add(entityName);
                synchronized (activeSyncProps) {
                    activeSyncProps.putAll(sectionProps);
                }
            }

            else {
                synchronized (pendingQtepsBySystem) {
                    pendingQtepsBySystem
                            .computeIfAbsent(currentSystemId, k -> new ConcurrentHashMap<>())
                            .putAll(sectionProps);
                }
            }

        }
        catch (Exception e) {
            NeptusLog.pub().error("Error handling QTEP reply", e);
        }
    }

    @Periodic(millisBetweenUpdates = 500)
    public void checkQtepsCompletion() {

        if (!isSyncing)
            return;

        boolean allReceived = expectedEntities == null || receivedEntities.containsAll(expectedEntities);
        boolean timeout =
                System.currentTimeMillis() - syncStartTime > QTEPS_TIMEOUT_MS;

        if (!allReceived && !timeout)
            return;

        isSyncing = false;

        if (timeout) {
            NeptusLog.pub().warn(
                    "QTEP sync timeout for system " + systemId);
        }

        Map<String, SystemProperty> result;
        synchronized (activeSyncProps) {
            result = new LinkedHashMap<>(activeSyncProps);
            activeSyncProps.clear();
        }

        expectedEntities = null;
        receivedEntities.clear();

        SwingUtilities.invokeLater(() ->
                owner.onQtepSyncFinished(result));
    }


    private void showRateLimitedNotification(String srcSystemId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastNotificationBySystem.get(srcSystemId);

        if (lastTime != null &&
                (currentTime - lastTime) < NOTIFICATION_COOLDOWN_MS) {
            return;
        }

        lastNotificationBySystem.put(srcSystemId, currentTime);

        SwingUtilities.invokeLater(() -> {
            Notification notification = Notification.warning(
                    I18n.text("New Configuration Definitions"),
                    I18n.textf("Received configuration definitions for vehicle %s. Click apply to see options.", srcSystemId)
            ).requireHumanAction(true);
            notification.setDismissActionListener(e -> {
                // TODO: Dismiss action listener needs review.
                lastNotificationTime = currentTime;
            });
            notification.setActionListener(e -> {
                askForSync(srcSystemId);
            });

            if(owner.console != null) {
                owner.console.post(notification);
            }
        });
    }

    private void askForSync(String srcSystemId) {
        String[] options = {
                I18n.text("Yes, save and apply definitions"),
                I18n.text("Yes, only apply definitions (don't save)"),
                I18n.text("Cancel")
        };
        int choice = JOptionPane.showOptionDialog(
                owner.console,
                I18n.textf("Do you want to synchronize current configuration definitions for %s?", srcSystemId),
                I18n.text("Synchronize Configuration"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        switch (choice) {
            case 0:
                applyPendingQteps(srcSystemId, true);
                break;
            case 1:
                applyPendingQteps(srcSystemId, false);
                break;
        }
    }

    private void applyPendingQteps(String srcSystemId, boolean save) {

        Map<String, SystemProperty> props = pendingQtepsBySystem.remove(srcSystemId);

        if (props == null || props.isEmpty())
            return;

        SwingUtilities.invokeLater(() -> {
            owner.applyExternalQtepDefinitions(srcSystemId, props, save);
        });
    }


    @Override
    public String[] getObservedMessages() {
        return new String[] {"QueryTypedEntityParameters"};
    }

    @Override
    public void onMessage(MessageInfo messageInfo, IMCMessage imcMessage) {
        messageArrived(imcMessage);
    }

    public void setOwner(SystemConfigurationEditorPanel owner) {
        this.owner = owner;
    }
}
