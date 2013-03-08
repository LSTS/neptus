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
 * 2009/06/06
 * $Id:: ExceptionAlarm.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.plugins.alarms;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;

/**
 * @author ZP
 * 
 */
@PluginDescription(icon="pt/up/fe/dceg/neptus/plugins/alarms/icon.png", name="Error monitor", description="This panel shows software exceptions that may occur")
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
