/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/06/06
 */
package pt.lsts.neptus.plugins.alarms;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author ZP
 * 
 */
@PluginDescription(icon="pt/lsts/neptus/plugins/alarms/icon.png", name="Error monitor", description="This panel shows software exceptions that may occur")
public class ExceptionAlarm extends SimpleAlarm {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private boolean added = false;
	private static final String ok_message = "No errors found";
	private String messageToShow = ok_message;
	private int currentLevel = 0;
	
	@Override
	protected String getTextToDisplay() {
		return "SW Errors";
	}
	
	public ExceptionAlarm(ConsoleLayout console) {
	    super(console);
		
		display.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getButton() == MouseEvent.BUTTON3 && currentLevel == 4) {
					JPopupMenu popup = new JPopupMenu();
					popup.add(new AbstractAction("Clear errors") {
						/**
                         * 
                         */
                        private static final long serialVersionUID = 1L;

                        @Override
						public void actionPerformed(ActionEvent e) {
							currentLevel = 0;
							messageToShow = ok_message;
						}
					});
					popup.show(display, e.getX(), e.getY());
				}
			}
		});
	}
	
	@Override
	public void initSubPanel() {	
		if (!added)
			NeptusLog.pubRoot().addAppender(appender);
		added = true;
	}
	
	@Override
	public void cleanSubPanel() {
		if (added)
		    NeptusLog.pubRoot().removeAppender(appender);
		added = false;
	}
	
	@Override
	public String getAlarmMessage() {
		return messageToShow;
	}
	
	@Override
	public int getAlarmState() {
		return currentLevel;
	}
	
	private AppenderSkeleton appender = new AppenderSkeleton() {

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent arg0) {
			if (arg0.getLevel().isGreaterOrEqual(Level.ERROR)) {				
				if (arg0.getMessage() instanceof Exception) {
					Exception e = (Exception)  arg0.getMessage();
					messageToShow = e.getClass().getSimpleName()+": "+e.getMessage();
				}
				else
					messageToShow = arg0.getRenderedMessage();
				
				currentLevel = LEVEL_4;
			}
		}

		@Override
		public void close() {

		}
	};
}
