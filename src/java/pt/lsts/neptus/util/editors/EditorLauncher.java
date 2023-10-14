/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 5/Mar/2005
 */
package pt.lsts.neptus.util.editors;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.loader.FileHandler;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author Paulo Dias
 *
 */
public class EditorLauncher 
extends JPanel
implements Runnable, FileHandler
{
    private static final long serialVersionUID = 1L;
    public final short TEXT_EDITOR_TYPE = 0;
    public final short XML_EDITOR_TYPE  = 1;
    
    protected final String TEXT_EDITOR_WIN_1 = "notepad";
    protected final String TEXT_EDITOR_WIN_2 = "textpad";
    protected final String TEXT_EDITOR_WIN_3 = "c:\\Program Files\\TextPad 4\\TextPad.exe";
    
    protected final String TEXT_EDITOR_LINUX_1 = "emacs";
    protected final String TEXT_EDITOR_LINUX_2 = "kate";
    protected final String TEXT_EDITOR_LINUX_3 = "jed";
    
    protected Runtime rt;
    protected Process ps;
    protected String exeCmd = "";
    protected String path; 
    protected short type; 
    protected boolean waitForCompletion;

	private JPanel jContentPane = null;
	private JFrame jFrame = null;  //  @jve:decl-index=0:visual-constraint="343,29"
	private JButton jButton = null;
	private JButton jButton1 = null;
	private JPanel jPanel = null;
	private JTextField jTextField = null;
	private boolean exitOnCompletion = false;
	

    /**
     * 
     */
    public EditorLauncher()
    {
        super();
		initialize();
    }

	/**
	 * This method initializes jContentPane	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
		}
		return jContentPane;
	}
	/**
	 * This method initializes jFrame	
	 * 	
	 * @return javax.swing.JFrame	
	 */    
	private JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setContentPane(getJContentPane());
			jFrame.setTitle("Editor Laucher");
			jFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/neptus-icon.png")));
			jFrame.setSize(315, 90);
			jFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
			jFrame.setResizable(false);
			GuiUtils.centerOnScreen(jFrame);
		}
		return jFrame;
	}
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("don't wait");
			jButton.setMnemonic(java.awt.event.KeyEvent.VK_D);
			jButton.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					NeptusLog.pub().info("<###>actionPerformed()");
					getJFrame().dispose();
				}
			});
		}
		return jButton;
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private  void initialize() {
		this.setLayout(new BorderLayout());
		this.setSize(300,200);
		this.add(getJPanel(), java.awt.BorderLayout.SOUTH);
		this.add(getJTextField(), java.awt.BorderLayout.CENTER);
	}
	
	
	public void showFramed()
	{
	    JFrame jf = this.getJFrame();
	    jf.add(this);
	    jf.setVisible(true);
	}
	
	
	protected String getEditorCommand(short type)
	{
        String envEditor = "";
        String envXMLEditor = "";

        try
        {
            envEditor = System.getenv("NEPTUS_EDITOR");
            envXMLEditor = System.getenv("NEPTUS_XMLEDITOR");
        }
        catch (Error e)
        {
            envEditor = System.getProperty("NEPTUS_EDITOR");
            envXMLEditor = System.getProperty("NEPTUS_XMLEDITOR");
        }
                
        String exeCmd = "";

	    if (type == XML_EDITOR_TYPE)
        {
	        if (envXMLEditor != null)
	            exeCmd = envXMLEditor;
	        else if (envEditor != null)
	            exeCmd = envEditor;
	        else if (OsInfo.getName() == OsInfo.Name.WINDOWS)
	            exeCmd = TEXT_EDITOR_WIN_1;
	        else if (OsInfo.getName() == OsInfo.Name.LINUX)
	            exeCmd = TEXT_EDITOR_LINUX_1;
        }
        else
        {
	        if (envEditor != null)
	            exeCmd = envEditor;
	        else if (OsInfo.getName() == OsInfo.Name.WINDOWS)
	            exeCmd = TEXT_EDITOR_WIN_1;
	        else if (OsInfo.getName() == OsInfo.Name.LINUX)
	            exeCmd = TEXT_EDITOR_LINUX_1;
        }
	    return exeCmd;
	}
	
	public boolean editFile(String path, short type, boolean waitForCompletion, boolean exitOnCompletion) {
		this.exitOnCompletion = exitOnCompletion;
		
		if (exitOnCompletion)
			waitForCompletion = true;
		
		return editFile(path,type, waitForCompletion);
	}
	
	/**
	 * @param path
	 * @param type
	 * @param waitForCompletion
	 * @return
	 */
	public boolean editFile(String path, short type, boolean waitForCompletion)
	{
	    this.path = path;
	    this.type = type;
	    this.waitForCompletion = waitForCompletion;
	    exeCmd = getEditorCommand(type);
	    if (exeCmd.equals(""))
	    {
	        JOptionPane.showMessageDialog(this, "Editor not found!\n" +
	        		"Set environment variable NEPTUS_EDITOR and \n" +
	        		"optionaly also NEPTUS_XMLEDITOR to the editor \n" +
	        		"of choice.");
	        return false;
	    }
	    rt = Runtime.getRuntime();
	    new Thread(this).start();
	    return true;
	}

	/**
	 * @param path
	 * @return
	 */
	public boolean editFile(String path)
	{
		return editFile(path, TEXT_EDITOR_TYPE, false);	    
	}

	/**
	 * @param path
	 * @return
	 */
	public boolean editFileWait(String path)
	{
		return editFile(path, TEXT_EDITOR_TYPE, true);	    
	}

	/**
	 * @param path
	 * @return
	 */
	public boolean editXMLFile(String path)
	{
		return editFile(path, XML_EDITOR_TYPE, false);	    
	}

	/**
	 * @param path
	 * @return
	 */
	public boolean editXMLFileWait(String path)
	{
		return editFile(path, XML_EDITOR_TYPE, true);	    
	}
	
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        try
        {
            String[] cmdArray = {exeCmd, path};
            ps = rt.exec(cmdArray);
            
            if (exitOnCompletion) {
            	ps.waitFor();
            	System.exit(ps.exitValue());
            }
            
            if (waitForCompletion)
            {
                jTextField.setText(exeCmd + " " + path);
                showFramed();
                ps.waitFor();                
                NeptusLog.pub().info("<###> "+ps.exitValue());
                jFrame.dispose();
                
            }
            else
            {
                ps = null;
                rt = null;
            }
        }
        catch (IOException e)
        {
            NeptusLog.pub().error(this, e);
        }
        catch (InterruptedException e)
        {
            NeptusLog.pub().error(this, e);
        }
    }

    
	/**
	 * This method initializes jButton1	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton1() {
		if (jButton1 == null) {
			jButton1 = new JButton();
			jButton1.setText("terminate editor");
			jButton1.setMnemonic(java.awt.event.KeyEvent.VK_T);
			jButton1.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					NeptusLog.pub().info("<###>actionPerformed()");
					ps.destroy();
					jFrame.dispose();
				}
			});
		}
		return jButton1;
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.add(getJButton(), null);
			jPanel.add(getJButton1(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getJTextField() {
		if (jTextField == null) {
			jTextField = new JTextField();
			jTextField.setForeground(java.awt.Color.gray);
			jTextField.setEditable(false);
			jTextField.setBackground(new java.awt.Color(255,255,244));
		}
		return jTextField;
	}
	
	
	public static void main(String[] args)
    {
        EditorLauncher ed = new EditorLauncher();
        boolean rsb = ed.editFile("/home/zp/teste.txt",
                ed.TEXT_EDITOR_TYPE, true);
        NeptusLog.pub().info("<###>>" + rsb);
    }

	@Override
	public Window handleFile(File f) {
		editFile(f.getAbsolutePath(), this.TEXT_EDITOR_TYPE, true, true);
        return null; // This is an InternalFrame
	}
}
