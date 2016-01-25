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
 * 5/10/2011
 */
package pt.lsts.neptus.comm;

import java.awt.Component;

import javax.swing.JOptionPane;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.imc.AcousticSystems;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;

/**
 * @author pdias
 *
 */
public class IMCSendMessageUtils {

    private static Integer requestId = 0xFFFF;

    private IMCSendMessageUtils() {
    }
    
    /**
     * @return the next requestId
     */
    public static int getNextRequestId() {
        synchronized (requestId) {
            ++requestId;
            if (requestId > 0xFFFF)
                requestId = 0;
            if (requestId < 0)
                requestId = 0;
            return requestId;
        }
    }

    /**
     * @return requestId
     */
    public static int getCurrentRequestId() {
        synchronized (requestId) {
            return requestId;
        }
    }

    public static boolean sendMessage(IMCMessage msg, String errorTextForDialog,
            String... ids) {
        return sendMessage(msg, errorTextForDialog, false, ids);
    }

    public static boolean sendMessage(IMCMessage msg, String errorTextForDialog,
            boolean ignoreAcousticSending, String... ids) {
        return sendMessage(msg, null, errorTextForDialog, ignoreAcousticSending, "acoustic/operation",
                false, true, ids);
    }

    public static boolean sendMessage(IMCMessage msg, Component parent, String errorTextForDialog,
            boolean ignoreAcousticSending, boolean acousticOpUserAprovedQuestion, String... ids) {
        return sendMessage(msg, null, errorTextForDialog, ignoreAcousticSending, "acoustic/operation",
                false, acousticOpUserAprovedQuestion, ids);
    }
    
    public static boolean sendMessage(IMCMessage msg, Component parent, String errorTextForDialog,
            boolean ignoreAcousticSending, String acousticOpServiceName, boolean acousticOpUseOnlyActive,
            boolean acousticOpUserAprovedQuestion, String... ids) {
        return sendMessage(msg, null, null, parent, errorTextForDialog, ignoreAcousticSending, acousticOpServiceName,
                acousticOpUseOnlyActive, acousticOpUserAprovedQuestion, ids);
    }

    /**
     * @param msg
     * @param sendProperties The same of {@link ImcMsgManager#sendMessage(IMCMessage, pt.lsts.neptus.comm.manager.imc.ImcId16, String, pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener)},
     *              use null if don't care.
     * @param parent The parent component for popup error message
     * @param errorTextForDialog
     * @param ignoreAcousticSending If this is true mean don't use acoustic.
     * @param acousticOpServiceName 
     * @param acousticOpUseOnlyActive
     * @param acousticOpUserAprovedQuestion
     * @param ids
     * @return
     */
    public static boolean sendMessage(IMCMessage msg, String sendProperties, MessageDeliveryListener listener, Component parent, String errorTextForDialog,
            boolean ignoreAcousticSending, String acousticOpServiceName, boolean acousticOpUseOnlyActive,
            boolean acousticOpUserAprovedQuestion, String... ids) {

        ImcSystem[] acousticOpSysLst = !ignoreAcousticSending ? ImcSystemsHolder.lookupSystemByService(
                acousticOpServiceName, SystemTypeEnum.ALL, acousticOpUseOnlyActive)
                : new ImcSystem[0];

        boolean acousticOpUserAprovalRequired = true;
        boolean acousticOpUserAproved = !acousticOpUserAprovedQuestion;
        boolean retAll = true;
        for (String sid : ids) {
            boolean ret;
            ImcSystem sysL = ImcSystemsHolder.lookupSystemByName(sid);
            if (acousticOpSysLst.length != 0 && sysL != null && !sysL.isActive()) {
                if (acousticOpUserAprovalRequired) {
                    acousticOpUserAproved = (GuiUtils.confirmDialog(parent, I18n.text("Send by Acoustic Modem"), 
                            I18n.text("Some systems are not active. Do you want to send by acoustic modem?")) == JOptionPane.YES_OPTION);
                    acousticOpUserAprovalRequired = false;
                }
                if (acousticOpUserAproved)
                    ret = sendMessageByAcousticModem(msg, sid, acousticOpSysLst);
                else
                    ret = false;
            }
            else {
                ret = ImcMsgManager.getManager().sendMessageToSystem(msg, sid, sendProperties, listener);
            }
            retAll = retAll && ret;
            if (!ret) {
                if (parent instanceof ConsolePanel) {
                    ((ConsolePanel) parent).post(Notification.error(I18n.text("Send Message"), errorTextForDialog).src(
                            I18n.text("Console")));
                }
                else {
                    GuiUtils.errorMessage(parent, I18n.text("Send Message"), errorTextForDialog);
                }
            }
        }
        
        return retAll;
    }

    /**
     * @param msg
     * @param system
     * @param acousticOpSysLst
     * @return
     */
    public static boolean sendMessageByAcousticModem(IMCMessage msg, String system,
            ImcSystem[] acousticOpSysLst) {
        // TODO listen for the responses back from the systems with modems
        boolean retAll = false;
        for (ImcSystem acOpSystem : acousticOpSysLst) {
            boolean canReach = doesSystemWithAcousticCanReachSystem(acOpSystem, system);
            if (!canReach)
                continue;
            
            String id = acOpSystem.getName();
            IMCMessage msgAcousticOperation = IMCDefinition.getInstance().create("AcousticOperation");

            msgAcousticOperation.setValue("op", "MSG");
            msgAcousticOperation.setValue("system", system);
            msgAcousticOperation.setValue("msg", msg);

            boolean ret = sendMessage(msgAcousticOperation, I18n.text("Error sending message by acoustic modem!"),
                    true, id);
            retAll = retAll || ret;
        }
        return retAll;
    }

    /**
     * Test if a system with acoustic/operation service can reach a system by acoustics
     * @param acOpSystem
     * @param system
     * @return
     */
    public static boolean doesSystemWithAcousticCanReachSystem(ImcSystem acOpSystem, String system) {
        if (acOpSystem.containsData(ImcSystem.ACOUSTIC_SYSTEMS)) {
            try {
                AcousticSystems msg = (AcousticSystems) acOpSystem.retrieveData(ImcSystem.ACOUSTIC_SYSTEMS);
                String sysLst = msg.getList();
                if (StringUtils.isTokenInList(sysLst, system))
                    return true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return false;
    }
}
