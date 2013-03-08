/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2010/06/26
 * $Id:: WebServerPlugin.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.plugins.web;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.Servlet;
import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.web.IConsoleMenuItemServlet.ConsoleMenuItem;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * @author ZP
 * @author PDias
 */
@SuppressWarnings("serial")
@PluginDescription(name="Web Server",author="zp",icon="pt/up/fe/dceg/neptus/plugins/web/internet.png")
public class WebServerPlugin extends SimpleSubPanel implements ConfigurationListener {

	@NeptusProperty(name="Web Server Port")
	public int port = WebServer.port;
	
	@NeptusProperty(name="Web Server Enabled")
	public boolean enabled = true;
	
	JMenuItem enabledMenuItem = null;
	
	protected boolean alreadyStarted = false;
	@Override
	public void initSubPanel() {
		if (alreadyStarted)
			return;
		
		alreadyStarted = true;
		
        String menuPrefixStr = I18n.text("Settings") + ">" + I18n.text("Web Server") + ">";
		//System.out.println("init WebServer");
		try {
            enabledMenuItem = addMenuItem(menuPrefixStr + I18n.text("Disable"),
                    ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
                            if (enabledMenuItem.getText().equals(I18n.text("Disable"))) {
						WebServer.stop();
                                enabledMenuItem.setText(I18n.text("Enable"));
					}
					else {
						WebServer.restart();
                                enabledMenuItem.setText(I18n.text("Disable"));
					}
				}
			});		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		Vector<String> alreadyLoaded = new Vector<String>();
		
		try {
			for (Class<?> c : ReflectionUtil.getClassesForPackage(WebServer.class.getPackage().getName())) {
				if (c.getAnnotation(NeptusServlet.class) != null) {
				    
				    if (alreadyLoaded.contains(c.getCanonicalName()))
				        continue;
				    alreadyLoaded.add(c.getCanonicalName());
				    
					Servlet s = WebServer.install(c);
					if (s instanceof IConsoleServlet) {
						((IConsoleServlet)s).setConsole(getConsole());
					}
					if (s instanceof IConsoleMenuItemServlet) {
					    String pathName = c.getSimpleName();
				        NeptusServlet sa = c.getAnnotation(NeptusServlet.class);
				        if (sa != null && !sa.name().equals("")) {
				            pathName = sa.name();
				        }
                        Hashtable<String, JMenuItem> mht = new Hashtable<String, JMenuItem>();
                        for (ConsoleMenuItem cmi : ((IConsoleMenuItemServlet) s)
                                .getConsoleMenuItems()) {
                            JMenuItem jmi = addMenuItem(
                                    menuPrefixStr + pathName + ">" + cmi.getItemSubPath(),
                                    cmi.getIcon(), cmi.getActionListener());
                            if (jmi != null)
                                mht.put(cmi.getItemSubPath(), jmi);
                        }
                        ((IConsoleMenuItemServlet) s).informCreatedConsoleMenuItem(mht);
                    }
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		launchServer();
	}
	
	public WebServerPlugin(ConsoleLayout console) {
	    super(console);
		setVisibility(false);
	}
	
	@Override
	public void propertiesChanged() {
		new Thread(WebServerPlugin.this.getClass().getSimpleName() + "[" + Integer.toHexString(WebServerPlugin.this.hashCode()) + "]") {
			@Override
            public void run() {
				try { Thread.sleep(3000); } catch (Exception e) { e.printStackTrace(); }
				launchServer();
			};
		}.start();
		
	}
	
	@Override
	public DefaultProperty[] getProperties() {
	    port = WebServer.port;
	    return super.getProperties();
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#clean()
	 */
	@Override
	public void cleanSubPanel() {
	    WebServer.stop();
	}

    private void launchServer() {
        WebServer.setPort(port);

        if (enabled) {
            if (WebServer.isRunning()) {
                WebServer.restart();
            }
            else if (!WebServer.isRunning())
                    WebServer.start(port);
            
            if (enabledMenuItem != null)
                enabledMenuItem.setText("Disable");
        }
        else {
        	WebServer.stop();
            if (enabledMenuItem != null)
                enabledMenuItem.setText("Enable");
        }
    }
}
