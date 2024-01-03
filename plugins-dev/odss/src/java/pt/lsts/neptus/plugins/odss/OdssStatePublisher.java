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
 * Author: José Pinto
 * Oct 26, 2011
 */
package pt.lsts.neptus.plugins.odss;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JCheckBoxMenuItem;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(author = "zp", name = "Email State Publisher", icon = "pt/lsts/neptus/plugins/odss/mail_send.png", category = CATEGORY.WEB_PUBLISHING)
public class OdssStatePublisher extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener {

    private static final long serialVersionUID = -1397794375394456466L;

    @NeptusProperty(name="Update Interval", description="Seconds between updates.", userLevel = LEVEL.REGULAR)
    public int secondsBetweenUpdates = 300;

    @NeptusProperty(name = "Username", category = "Server Settings", userLevel = LEVEL.REGULAR)
    public String username = "";

    @NeptusProperty(name = "Reenter password", description = "The password will be asked again.", category = "Server Settings", userLevel = LEVEL.REGULAR)
    private boolean reenterPassword = false;

//    @NeptusProperty(name = "Password", description = "The password will be stored in plain text.", category = "Server Settings")
    private String password = "";

    @NeptusProperty(name = "Sender name", category = "E-mail content", userLevel = LEVEL.REGULAR)
    public String fromName = "LSTS";

    @NeptusProperty(name = "Sender e-mail", category = "E-mail content", userLevel = LEVEL.REGULAR)
    public String fromEmail = "lstsfeup@gmail.com";

    @NeptusProperty(name = "Receiver name", category = "E-mail content", userLevel = LEVEL.ADVANCED)
    public String toName = "Track";

    @NeptusProperty(name = "Destination e-mail", category = "E-mail content", userLevel = LEVEL.ADVANCED, 
            description = "Can only be auvtrack@mbari.org or lstsfeup+auvtrack@gmail.com")
    public String toEmail = "auvtrack@mbari.org";

    @NeptusProperty(name = "CC name", category = "E-mail content", userLevel = LEVEL.ADVANCED)
    public String ccName = "LSTS";

    @NeptusProperty(name = "CC e-mail", category = "E-mail content", userLevel = LEVEL.ADVANCED)
    public String ccEmail = "lstsfeup@gmail.com";

    @NeptusProperty(name = "Subject", category = "E-mail content", userLevel = LEVEL.ADVANCED)
    public String subject = "$vehicle,$time,$longitude,$latitude";

    @NeptusProperty(name = "Message", category = "E-mail content", userLevel = LEVEL.ADVANCED)
    public String message = "Vehicle: $vehicle ($type)\nPosix time: $time\nPlan: $plan\nPosition: $gmaps";

    @NeptusProperty(name = "Publishing", userLevel = LEVEL.REGULAR)
    public boolean publishOn = false;
    
    @NeptusProperty(name = "Ignore publishing simulated states", userLevel = LEVEL.REGULAR,
            description = "If true any simulated system will not be published.")
    public boolean ignoreSimulatedSystems = true;

    @NeptusProperty(name = "Stmp server", category = "Server Settings", userLevel = LEVEL.REGULAR)
    public String smtpServerName = "smtp.gmail.com";
    
    @NeptusProperty(name = "Stmp port", category = "Server Settings", userLevel = LEVEL.REGULAR)
    public int smtpServerPort = 587;
    
    @NeptusProperty(name = "Use SSL", category = "Server Settings", userLevel = LEVEL.REGULAR)
    public boolean smtpServerSSL = true;
    
    @NeptusProperty(name = "Use TLS", category = "Server Settings", userLevel = LEVEL.REGULAR)
    public boolean smtpServerTLS = true;

    private Timer timer;
    private TimerTask ttask;

    private JCheckBoxMenuItem publishCheckItem = null;

    public OdssStatePublisher(ConsoleLayout console){
        super(console);
        setVisibility(false);

        timer = new Timer(OdssStoqsTrackFetcher.class.getSimpleName() + " [" + OdssStatePublisher.this.hashCode() + "]");
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, secondsBetweenUpdates * 1000L);
    }

    private TimerTask getTimerTask() {
        if (ttask == null) {
            ttask = new TimerTask() {
                @Override
                public void run() {
                    if (!publishOn)
                        return;
                    
                    // String vehicleId = getConsole().getMainVehicle();
                    ImcSystem[] systems = ImcSystemsHolder.lookupSystemVehicles();
                    
                    if (systems.length == 0)
                        return;
                    
                    try {
                        for (ImcSystem sys : systems) {
                            if (ignoreSimulatedSystems && sys.isSimulated())
                                continue;
                            if (System.currentTimeMillis() - sys.getLocationTimeMillis() < secondsBetweenUpdates * 1000L)
                               publishState(sys.getName());
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
            };
        }
        return ttask;
    }

    @Override
    public void propertiesChanged() {
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, secondsBetweenUpdates * 1000L);
        
        if (reenterPassword) {
            password = "";
            reenterPassword = false;
        }
    }

    @Override
    public void initSubPanel() {
        setVisibility(false);
        
        publishCheckItem = addCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass())
                + ">Start/Stop", null,
                new CheckMenuChangeListener() {
                    @Override
                    public void menuUnchecked(ActionEvent e) {
                        publishOn = false;
                    }
                    
                    @Override
                    public void menuChecked(ActionEvent e) {
                        publishOn = true;
                    }
                });

        addMenuItem(
                I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">" + I18n.text("Settings"),
                null,
                e -> PropertiesEditor.editProperties(OdssStatePublisher.this,
                        getConsole(), true));
        
        publishCheckItem.setState(publishOn);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeCheckMenuItem("Tools>" + PluginUtils.getPluginName(this.getClass()) + ">Start/Stop");
        removeMenuItem("Tools>" + PluginUtils.getPluginName(this.getClass()) + ">Settings");
        
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    @Override
    public boolean update() {
        if (publishCheckItem != null)
            publishCheckItem.setState(publishOn);
        
        return true;
    }


    /**
     * Replaces all macros in the String
     * @param original The original String
     * @param vehicleId Vehicle's id
     * @return The original string with all applied replacements
     */
    protected String applyMacros(String original, String vehicleId) {
        ImcSystem system = ImcSystemsHolder.lookupSystemByName(vehicleId);
        if (system == null)
            return original;
        DecimalFormat df = new DecimalFormat("#.00000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        LocationType loc = new LocationType(system.getLocation());
        Date timestamp = new Date(system.getLocationTimeMillis());
        String ip = system.getHostAddress();
        String planId = system.getActivePlan() != null ? system.getActivePlan().getId() : "IDLE";
        String type = system.getVehicle().getType();
        String result = original;
        loc.convertToAbsoluteLatLonDepth();
        result = result.replaceAll("\\$vehicle", IMCUtils.reduceSystemName(vehicleId));
        result = result.replaceAll("\\$latitude", "" + df.format(loc.getLatitudeDegs()));
        result = result.replaceAll("\\$longitude", "" + df.format(loc.getLongitudeDegs()));
        result = result.replaceAll("\\$depth", "" + df.format(loc.getAllZ()));
        result = result.replaceAll("\\$time", "" + timestamp.getTime() / 1000);
        result = result.replaceAll("\\$plan", planId);
        result = result.replaceAll("\\$ip", ip);
        result = result.replaceAll("\\$type", type);
        result = result.replaceAll("\\$gmaps", "http://maps.google.com/maps?q=loc:" + loc.getLatitudeDegs()
                + "," + loc.getLongitudeDegs() + "&z=17");

        return result;
    }

    /**
     * Publish the given state using email and according to the defined message format
     * @param vehicleId Vehicle's id
     * @throws EmailException If an error occurs while sending the email
     */
    public void publishState(String vehicleId) throws EmailException {

        if (username.isEmpty() || password.isEmpty()) {
            String title = I18n.textf("Enter password for %", getName());
            Pair<String, String> userCred = GuiUtils.askCredentials(getConsole(), title , username, password);
            if (userCred == null) {
                return;
            }
            else {
                username = userCred.first();
                password = userCred.second();
            }
        }
        
        Email mail = new SimpleEmail();
        mail.setHostName(smtpServerName); //"smtp.gmail.com"
        mail.setSmtpPort(smtpServerPort); //587
        mail.setAuthenticator(new DefaultAuthenticator(username, password));
        mail.setSSLOnConnect(smtpServerSSL);
        mail.setStartTLSEnabled(smtpServerTLS);
        mail.setFrom(fromEmail, fromName);
        if (ccName != null && ccName.length() > 0)
            mail.addCc(ccEmail, ccName);
        if (toName != null && toName.length() > 0)
            mail.addTo(toEmail, toName);

        String mailSubject = applyMacros(subject, vehicleId);
        String mailMessage = applyMacros(message, vehicleId);

        mail.setSubject(mailSubject);        
        mail.setMsg(mailMessage);

        mail.send();

        NeptusLog.pub().warn("Sent email to "+mail.getToAddresses()+" ["+mailSubject+"] "+mailMessage);
    }
}
