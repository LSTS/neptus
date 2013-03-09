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
 * Oct 26, 2011
 */
package pt.up.fe.dceg.neptus.plugins.odss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.CheckMenuChangeListener;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
@PluginDescription(author = "zp", name = "Email State Publisher", icon = "pt/up/fe/dceg/neptus/plugins/odss/mail_send.png", category = CATEGORY.WEB_PUBLISHING)
public class OdssStatePublisher extends SimpleSubPanel implements IPeriodicUpdates, ConfigurationListener {

    private static final long serialVersionUID = -1397794375394456466L;

    @NeptusProperty(name="Update Interval", description="Seconds between updates.")
    public int secondsBetweenUpdates = 300;

    @NeptusProperty(name = "Username", category = "Server Settings")
    public String username = "lstsfeup";

    @NeptusProperty(name = "Password", description = "The password will be stored in plain text.", category = "Server Settings")
    public String password = "XtremeSeacon";

    @NeptusProperty(name = "Sender name", category = "E-mail content")
    public String fromName = "LSTS";

    @NeptusProperty(name = "Sender e-mail", category = "E-mail content")
    public String fromEmail = "lstsfeup@gmail.com";

    @NeptusProperty(name = "Receiver name", category = "E-mail content")
    public String toName = "Track";

    @NeptusProperty(name = "Sender e-mail", category = "E-mail content", description = "Can only be auvtrack@mbari.org or lstsfeup+auvtrack@gmail.com")
    public String toEmail = "auvtrack@mbari.org";

    @NeptusProperty(name = "CC name", category = "E-mail content")
    public String ccName = "LSTS";

    @NeptusProperty(name = "CC e-mail", category = "E-mail content")
    public String ccEmail = "lstsfeup@gmail.com";

    @NeptusProperty(name = "Subject", category = "E-mail content")
    public String subject = "$vehicle,$time,$longitude,$latitude";

    @NeptusProperty(name = "Message", category = "E-mail content")
    public String message = "Vehicle: $vehicle ($type)\nPosix time: $time\nPlan: $plan\nPosition: $gmaps";

    @NeptusProperty(name = "Publishing")
    public boolean publishOn = false;
    
    @NeptusProperty(name = "Publishing simulated states", description = "If true any simulated system will not be published.")
    public boolean ignoreSimulatedSystems = true;

    @NeptusProperty(name = "Stmp server", category = "Server Settings")
    public String smtpServerName = "smtp.gmail.com";
    
    @NeptusProperty(name = "Stmp port", category = "Server Settings")
    public int smtpServerPort = 587;
    
    @NeptusProperty(name = "Use SSL", category = "Server Settings")
    public boolean smtpServerSSL = true;
    
    @NeptusProperty(name = "Use TLS", category = "Server Settings")
    public boolean smtpServerTLS = true;

    private Timer timer = null;
    private TimerTask ttask = null;

    private JCheckBoxMenuItem publishCheckItem = null;

    public OdssStatePublisher(ConsoleLayout console){
        super(console);
        setVisibility(false);

        timer = new Timer(OdssStoqsTrackFetcher.class.getSimpleName() + " [" + OdssStatePublisher.this.hashCode() + "]");
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, secondsBetweenUpdates * 1000);
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
                            if (System.currentTimeMillis() - sys.getLocationTimeMillis() < secondsBetweenUpdates * 1000)
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
        timer.scheduleAtFixedRate(ttask, 500, secondsBetweenUpdates * 1000);
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
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PropertiesEditor.editProperties(OdssStatePublisher.this,
                                getConsole(), true);
                    }
                });
        
        publishCheckItem.setState(publishOn);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
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
     * @param loc The location of the vehicle
     * @param timestamp The timestamp of last update
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
        result = result.replaceAll("\\$latitude", "" + df.format(loc.getLatitudeAsDoubleValue()));
        result = result.replaceAll("\\$longitude", "" + df.format(loc.getLongitudeAsDoubleValue()));
        result = result.replaceAll("\\$depth", "" + df.format(loc.getAllZ()));
        result = result.replaceAll("\\$time", "" + timestamp.getTime() / 1000);
        result = result.replaceAll("\\$plan", planId);
        result = result.replaceAll("\\$ip", ip);
        result = result.replaceAll("\\$type", type);
        result = result.replaceAll("\\$gmaps", "http://maps.google.com/maps?q=loc:" + loc.getLatitudeAsDoubleValue()
                + "," + loc.getLongitudeAsDoubleValue() + "&z=17");

        return result;
    }

    /**
     * Publish the given state using email and according to the defined message format
     * @param vehicleId Vehicle's id
     * @param loc The location of the vehicle
     * @param timestamp The timestamp of last update
     * @throws EmailException If an error occurs while sending the email
     */
    public void publishState(String vehicleId) throws EmailException {

        Email mail = new SimpleEmail();
        mail.setHostName(smtpServerName); //"smtp.gmail.com"
        mail.setSmtpPort(smtpServerPort); //587
        mail.setAuthenticator(new DefaultAuthenticator(username, password));
        mail.setSSL(smtpServerSSL);
        mail.setTLS(smtpServerTLS);
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
