/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 19, 2013
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventSystemAuthorityStateChanged;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.up.fe.dceg.neptus.console.notifications.Notification;
import pt.up.fe.dceg.neptus.imc.LogBookControl;
import pt.up.fe.dceg.neptus.imc.LogBookControl.COMMAND;
import pt.up.fe.dceg.neptus.imc.LogBookEntry;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem.IMCAuthorityState;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Logbook History")
@Popup(accelerator = 'H', height = 400, width = 600, name = "Logbook History", pos = POSITION.CENTER)
public class LogBookSubPanel extends SimpleSubPanel implements IPeriodicUpdates {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Seconds between history update requests")
    public int secondsBetweenRequests = 60;

    protected MultiSystemHistory history = new MultiSystemHistory();
    protected LinkedHashMap<String, Double> lastMessagesUpdate = new LinkedHashMap<>();

    public LogBookSubPanel(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        add(history, BorderLayout.CENTER);
    }

    @Subscribe
    public void consume(LogBookControl logControl) {

        ImcSystem sys = ImcSystemsHolder.lookupSystem(logControl.getSrc());
        if (sys == null)
            return;

        if(sys.getAuthorityState().ordinal() <= IMCAuthorityState.NONE.ordinal())
            return;

        HashSet<HistoryMessage> msgs = new HashSet<>();

        if (logControl.getCommand() == COMMAND.REPLY) {
            for (LogBookEntry entry : logControl.getMsg()) {
                msgs.add(generate(logControl.getSourceName(), entry, false));
            }

            Collection<HistoryMessage> newMessages = history.add(msgs, logControl.getSourceName());
            Vector<HistoryMessage> msgsVec = new Vector<>();
            msgsVec.addAll(newMessages);
            Collections.reverse(msgsVec);
            lastMessagesUpdate.put(logControl.getSourceName(), logControl.getHtime());
            if (!newMessages.isEmpty()) {

                String text = "";//Received "+newMessages.size()+" previous entries from "+logControl.getSourceName()+": <br/>";
                boolean error = false;
                int count = 0;
                for (HistoryMessage m : msgsVec) {
                    if (m.type == msg_type.critical || m.type == msg_type.error) {
                        error = true;                                    
                        text = "<i>"+m.toString()+"</i><br/>"+text;
                        if (++count == 3)
                            break;
                    }
                    
                }
                if (!error) {
                    for (HistoryMessage m : msgsVec) {
                        if (m.type != msg_type.critical && m.type != msg_type.error) {          
                            text = "<i>"+m.toString()+"</i><br/>" + text;
                            if (++count == 3)
                                break;
                        }                        
                    }
                }
                if (count < newMessages.size())
                    text = "..."+"<br/>"+text;
                
                text = "Received "+newMessages.size()+" previous entries from "+logControl.getSourceName()+" including: <br/>"+text;
                
                if (error)
                    post(Notification.error("LogBook", text));
                else
                    post(Notification.info("LogBook", text));
            }
        }
    }

    @Subscribe
    public void consume(LogBookEntry logEntry) {
        if (!isShowing())
            return;

        ImcSystem sys = ImcSystemsHolder.lookupSystem(logEntry.getSrc());
        if (sys == null)
            return;

        if(sys.getAuthorityState().ordinal() <= IMCAuthorityState.NONE.ordinal())
            return;

        history.add(generate(logEntry.getSourceName(), logEntry, true), logEntry.getSourceName());
    }

    @Subscribe
    public void consume(ConsoleEventSystemAuthorityStateChanged authChanged) {

        if (ImcSystemsHolder.getSystemWithName(authChanged.getSystem()).getType() != SystemTypeEnum.VEHICLE)
            return;

        if (authChanged.getPreviousAuthorityState().ordinal() <= IMCAuthorityState.NONE.ordinal()
                && authChanged.getAuthorityState().ordinal() > IMCAuthorityState.NONE.ordinal()) {
            history.createHistory(authChanged.getSystem());
        }
        else if (authChanged.getPreviousAuthorityState().ordinal() > IMCAuthorityState.NONE.ordinal()
                && authChanged.getAuthorityState().ordinal() <= IMCAuthorityState.NONE.ordinal()) {
            history.removeHistory(authChanged.getSystem());
        }
    }

    @Subscribe
    public void consume(ConsoleEventVehicleStateChanged stateChanged) {
        if (ImcSystemsHolder.getSystemWithName(stateChanged.getVehicle()).getAuthorityState().ordinal() < IMCAuthorityState.NONE
                .ordinal()) {
            return;
        }

        if (stateChanged.getState() != STATE.DISCONNECTED) {
            if (lastMessagesUpdate.get(stateChanged.getVehicle()) == null
                    || System.currentTimeMillis() / 1000 - lastMessagesUpdate.get(stateChanged.getVehicle()) > secondsBetweenRequests)
                update();
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return secondsBetweenRequests * 1000;
    }

    protected HistoryMessage generate(String src, LogBookEntry entry, boolean async) {
        msg_type type = msg_type.info;

        switch (entry.getType()) {
            case ERROR:
                type = msg_type.error;
                break;
            case WARNING:
                type = msg_type.warning;
                break;
            case CRITICAL:
                type = msg_type.critical;
                break;
            default:
                break;
        }

        return new HistoryMessage((long) (entry.getHtime() * 1000), entry.getText(), entry.getContext(), async, type);
    }

    protected void requestMessages() {
        for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            double timestamp = 0;
            if (lastMessagesUpdate.containsKey(s.getName()) && lastMessagesUpdate.get(s.getName()) != 0)
                timestamp = lastMessagesUpdate.get(s.getName());

            // If already got an update recently (maybe requested by someone else) do nothing
            if (System.currentTimeMillis() / 1000 - timestamp < secondsBetweenRequests/2) {
                return;
            }

            LogBookControl msg = new LogBookControl();
            msg.setCommand(COMMAND.GET);
            msg.setHtime(timestamp);
            send(s.getName(), msg);

            msg = new LogBookControl();
            msg.setCommand(COMMAND.GET_ERR);
            msg.setHtime(timestamp);
            send(s.getName(), msg);
        }
    }

    @Override
    public boolean update() {
        requestMessages();
        return true;
    }

    @Override
    public void initSubPanel() {
        for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            if (s.getAuthorityState().ordinal() > IMCAuthorityState.NONE.ordinal())
                history.createHistory(s.getName());
        }
        requestMessages();
    }

    @Override
    public void cleanSubPanel() {

    }

}
