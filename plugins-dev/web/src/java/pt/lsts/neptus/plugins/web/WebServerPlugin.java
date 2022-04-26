/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 2010/06/26
 */
package pt.lsts.neptus.plugins.web;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.Servlet;
import javax.swing.JMenuItem;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.web.IConsoleMenuItemServlet.ConsoleMenuItem;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author ZP
 * @author PDias
 */
@SuppressWarnings("serial")
@PluginDescription(name="Web Server",author="zp",icon="pt/lsts/neptus/plugins/web/internet.png")
public class WebServerPlugin extends ConsolePanel implements ConfigurationListener {

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
		//NeptusLog.pub().info("<###>init WebServer");
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
                                    menuPrefixStr + I18n.text(pathName) + ">" + cmi.getItemSubPath(),
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
	 * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
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
                enabledMenuItem.setText(I18n.text("Disable"));
        }
        else {
        	WebServer.stop();
            if (enabledMenuItem != null)
                enabledMenuItem.setText(I18n.text("Enable"));
        }
    }
}
