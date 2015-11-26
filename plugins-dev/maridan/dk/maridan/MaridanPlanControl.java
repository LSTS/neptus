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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Nov 26, 2015
 */
package dk.maridan;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Maridan Plan Control")
public class MaridanPlanControl extends ConsolePanel implements ActionListener {

    private static final long serialVersionUID = -4175142087926209760L;
    private final ImageIcon ICON_START = ImageUtils.getIcon("images/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("images/planning/stop.png");
    private final String startPlanStr = I18n.text("Start Plan");
    private final String stopPlanStr = I18n.text("Stop Plan");
    private ToolbarButton btnStart = new ToolbarButton(ICON_START, startPlanStr, "start");
    private ToolbarButton btnStop = new ToolbarButton(ICON_STOP, stopPlanStr, "stop");
    private int requestId = IMCSendMessageUtils.getNextRequestId();
    
    @NeptusProperty(name="FTP Hostname")
    String ftp_host = "localhost";
    
    @NeptusProperty(name="FTP Port")
    int ftp_port = 21;
    
    @NeptusProperty(name="Username")
    String ftp_username = "";
    
    @NeptusProperty(name="Password")
    String ftp_password = "";
    
    @NeptusProperty(name="File Path")
    String ftp_filepath = "/osv/Plan001.xml";
    
    /**
     * @param console
     */
    public MaridanPlanControl(ConsoleLayout console) {
        super(console);
        btnStart.addActionListener(this);
        btnStop.addActionListener(this);
    }
    
    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));      
        add(btnStart);
        add(btnStop);
    }
    
    @Subscribe
    public void on(PlanControl msg) {
        if (msg.getRequestId() == requestId) {
            
            String title;
            
            if (msg.getOp() == OP.START)
                title = startPlanStr;
            else if (msg.getOp() == OP.STOP)
                title = stopPlanStr;
            else
                return;
            
            if (msg.getType() == TYPE.SUCCESS) {
                getConsole().post(Notification.success(title,
                        I18n.textf("Command was received by %vehicle", msg.getSourceName())));                
            }
            else if (msg.getType() == TYPE.FAILURE) {
                getConsole().post(Notification.error(title,
                        I18n.textf("Error while sending command to %vehicle: %error", msg.getSourceName(), msg.getInfo())));
            }
            else
                return;
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "start":
                PlanType plan = getConsole().getPlan();
                if (plan == null) {
                    getConsole().post(Notification.error(startPlanStr, I18n.text("Please select a plan to execute")));
                    return;
                }
                btnStart.setEnabled(false);
                Thread worker = new Thread() {
                    public void run() {
                        try {
                            
                            ImcSystem system = ImcSystemsHolder.getSystemWithName(getConsole().getMainSystem());
                            LocationType loc = null;
                            
                            if (system != null)
                                loc = system.getLocation();
                                
                            String planStr = MaridanPlanExporter.translate(plan, loc);
                            FtpUploader uploader = new FtpUploader(ftp_host, ftp_port);
                            if (!ftp_username.isEmpty() && !ftp_password.isEmpty())
                                uploader.login(ftp_username, ftp_password);
                            uploader.setRemoteFile(ftp_filepath).upload(planStr).disconnect();
                            getConsole().post(Notification.info(startPlanStr, I18n.textf("Plan stored in %folder", ftp_filepath)));
                            
                            PlanControl req = new PlanControl();
                            requestId = IMCSendMessageUtils.getNextRequestId();
                            req.setRequestId(requestId);
                            req.setType(TYPE.REQUEST);
                            req.setOp(OP.START);
                            req.setPlanId(plan.getId());
                            send(req);
                        }
                        catch (Exception ex) {
                            getConsole().post(Notification.error(startPlanStr, I18n.textf("Error sending the plan: %", ex.getMessage())));
                            ex.printStackTrace();
                        }                        
                        btnStart.setEnabled(true);
                    };                    
                };
                worker.setDaemon(true);
                worker.start();                
                break;
            case "stop":
                PlanControl req = new PlanControl();
                req.setType(TYPE.REQUEST);
                req.setOp(OP.STOP);
                requestId = IMCSendMessageUtils.getNextRequestId();
                req.setRequestId(requestId);
                send(req);                
                break;
            default:
                break;
        }
    }
}
