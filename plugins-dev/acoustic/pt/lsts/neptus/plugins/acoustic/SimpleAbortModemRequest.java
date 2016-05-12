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
 * 16/12/2010
 */
package pt.lsts.neptus.plugins.acoustic;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.AcousticOperation.OP;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.IAbortSenderProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;

/**
 * @author pdias
 * 
 */
@PluginDescription(author = "Paulo Dias", name = "Abort Request", version = "0.9.0", icon = "pt/lsts/neptus/plugins/acoustic/lbl.png", description = "Simple Abort Modem Request by Manta Gateway", documentation = "abort/abort-button.html#SimpleAbortModemRequest", category = CATEGORY.COMMUNICATIONS)
public class SimpleAbortModemRequest extends ConsolePanel implements IAbortSenderProvider,
        MainVehicleChangeListener, ConfigurationListener {

    private static final long serialVersionUID = 6693361983695124608L;

    @NeptusProperty(name = "Service Name")
    public String serviceName = "acoustic/operation";

    @NeptusProperty(name = "Use only active systems")
    public boolean useOnlyActive = false;

    /**
	 * 
	 */
    public SimpleAbortModemRequest(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
	 * 
	 */
    private void initialize() {
        setVisibility(false);        
    }

    /**
     * React to messages sent by the vehicle in response to previously sent requests
     * @param msg
     */
    @Subscribe
    public void consume(AcousticOperation msg) {
        String source = msg.getSourceName();
        String system = msg.getSystem();
        AcousticOperation.OP op = msg.getOp();
        switch (op) {
            case ABORT_ACKED:
                post(Notification.success(I18n.text("Abort Request"), I18n.textf("%systemName has acknowledged abort command", system)).requireHumanAction(true));
                break;
            case ABORT_IP:
                post(Notification.warning(I18n.text("Abort Request"), I18n.textf("Aborting %systemName acoustically (via %manta)...", system, source)));
                break;
            case ABORT_TIMEOUT:
                post(Notification.error(I18n.text("Abort Request"), I18n.textf("%manta timed out while trying to abort %systemName", source, system)));
                break;
            case UNSUPPORTED:
                post(Notification.error(I18n.text("Abort Request"), I18n.textf("%manta does not support aborting of %systemName", source, system)));
                break;
            case NO_TXD:
                post(Notification.error(I18n.text("Abort Request"), I18n.textf("%manta does not have a tranducer connected", source)));
                break;
            default:
                break;
        }
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Override
    public boolean sendAbortRequest() {
        return sendAbortRequest(getMainVehicleId());
    }

    @Override
    public boolean sendAbortRequest(String system) {
        AcousticOperation acOp = new AcousticOperation();
        acOp.setOp(OP.ABORT);
        acOp.setSystem(system);
        
        boolean ret = IMCSendMessageUtils.sendMessage(acOp,
                null, createDefaultMessageDeliveryListener(),
                this, I18n.text("Error sending ABORT command message!"), false, serviceName,
                useOnlyActive, false, true, system);
        return ret;
    }

    private MessageDeliveryListener createDefaultMessageDeliveryListener() {
        return new MessageDeliveryListener() {

            private String  getDest(IMCMessage message) {
                ImcSystem sys = message != null ? ImcSystemsHolder.lookupSystem(message.getDst()) : null;
                String dest = sys != null ? sys.getName() : I18n.text("unknown destination");
                return dest;
            }

            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery destination unreacheable",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery timeout",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery error. (%error)",
                                message.getAbbrev(), getDest(message), error)));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                NeptusLog.pub().info("Message sent unreliably");
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
                NeptusLog.pub().info("Message was successfully delivered");
            }
        };
    }

    @Override
    public void propertiesChanged() {
    }
}
