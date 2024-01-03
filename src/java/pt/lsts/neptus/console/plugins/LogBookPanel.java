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
 * 2009/06/09
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import pt.lsts.imc.LogBookEntry;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 *
 */
@PluginDescription(name="Log Book Panel", description="Allows sending entries to the logbook", icon="images/buttons/logbook.png",
documentation="logging/logbook.html")
public class LogBookPanel extends ConsolePanel implements ActionListener {

	private static final long serialVersionUID = -7765372596110306525L;

	//@NeptusProperty(name="Operator name", description="The name of the operator to be attached to the entries")
	public String name = System.getProperty("user.name");
	
	protected JTextField textField = new JTextField(10);
	protected JButton button = new JButton("Log");
	protected LogBookEntry logMsg = new LogBookEntry();
	protected Vector<String> msgHistory = new Vector<String>();
	protected int msgHistoryIndex = 0;
	private static DateFormat df1 = new SimpleDateFormat("yyyyMMdd");
	private static DateFormat df2 = new SimpleDateFormat("HH:mm:ss");
	public static String filename = "log/logbook_"+df1.format(new Date())+".html";
	
	public LogBookPanel(ConsoleLayout console) {
	    super(console);
		setLayout(new BorderLayout());
		button.setMargin(new Insets(0,2,0,2));
		button.setEnabled(false);
		
		textField.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if (textField.getText().length() == 0)
					button.setEnabled(false);
				else
					button.setEnabled(true);
			}
		});
				
		
		textField.addKeyListener(new KeyAdapter() {
			
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10)
					button.doClick();				
				else if (e.getKeyCode() == 38) {					
					if (msgHistory.size() > 0)
						textField.setText(msgHistory.get(msgHistoryIndex));
					
					if (msgHistoryIndex > 0)
						msgHistoryIndex--;			
				}
				else if (e.getKeyCode() == 40) {
					if (msgHistory.size() > 0)
						textField.setText(msgHistory.get(msgHistoryIndex));
					
					if (msgHistoryIndex < msgHistory.size()-1)
						msgHistoryIndex++;					
				}
			}
		});
	
		textField.setHorizontalAlignment(JTextField.CENTER);
		
		
		add(textField, BorderLayout.CENTER);
		add(button, BorderLayout.EAST);
		
		button.addActionListener(this);
		
		setSize(200, 31);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    logMsg.setContext(name);
	    logMsg.setHtime(DateTimeUtil.timeStampSeconds());
	    logMsg.setType(LogBookEntry.TYPE.INFO);
	    logMsg.setText(textField.getText());
		
		logPlain(textField.getText()+"\n");
		
		try {
//		    LsfMessageLogger.log(logMsg);
//			NeptusMessageLogger.logMessage(ImcMsgManager.getManager().getLocalId().toString(), "", logMsg);
			LsfMessageLogger.log(logMsg);
			
			if (msgHistory.isEmpty() || !msgHistory.lastElement().equals(textField.getText())) {					
				msgHistory.add(textField.getText());			
			}	
			if (!msgHistory.isEmpty())
				msgHistoryIndex = msgHistory.size()-1;
			
			textField.setText("");	
		}
		catch (Exception ex) {
			NeptusLog.pub().error(ex);
		}
	}
	
	
	public static void logPlain(String text) {
		try {
			BufferedWriter lb = getLogBookWriter();
			if (lb != null) {				
				lb.write("<tr><td><b>"+df2.format(new Date())+"</b></td><td>"+text+"</td></tr>");
				lb.flush();
			}			
		}
		catch (Exception e) {
			NeptusLog.pub().error(e);
		}
	}
	
	private static BufferedWriter lbWriter = null;
	
	private static BufferedWriter getLogBookWriter() throws Exception {
		
		if (lbWriter == null) {
			boolean fileExists = new File(filename).exists();
			lbWriter = new BufferedWriter(new FileWriter(new File(filename), true));		
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						logPlain("Neptus shutdown");
						//lbWriter.write("</table></body></html>");
						lbWriter.close();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			if(!fileExists)
				lbWriter.write("<html><head><title>Neptus Log Book</title></head><body><table>");			
		}		
		return lbWriter;
	}
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		GuiUtils.testFrame(new LogBookPanel(null));
	}

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
