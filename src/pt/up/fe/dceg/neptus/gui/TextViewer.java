/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * Jun 1, 2005
 * $Id:: TextViewer.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import pt.up.fe.dceg.neptus.util.FileUtil;
/**
 * @author zp
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TextViewer extends JPanel {
	
    private static final long serialVersionUID = -3285334845813428890L;

    private JPanel jPanel = null;
	private JScrollPane jScrollPane = null;
	private JTextArea textArea = null;
	private JLabel statusText = null;
	/**
	 * This method initializes 
	 * 
	 */
	public TextViewer() {
		super();
		initialize();
	}
	
	public TextViewer(String filename) {
		this();
		showFile(filename);
	}
	
	public boolean showFile(String filename) {
		String fileContent = FileUtil.getFileAsString(filename);
		getTextArea().setText(fileContent);
		
		getStatusText().setText("[viewing "+filename+"]");
		
		Frame frm = JOptionPane.getFrameForComponent(this);
		frm.setTitle(filename+" - TextViewer");
		return true;
	}
	
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		BorderLayout borderLayout2 = new BorderLayout();
		this.setLayout(borderLayout2);
		this.setSize(400, 300);
		this.setPreferredSize(new java.awt.Dimension(400,300));
		borderLayout2.setHgap(0);
		borderLayout2.setVgap(0);
		this.add(getJPanel(), java.awt.BorderLayout.SOUTH);
		this.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
		
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			FlowLayout flowLayout1 = new FlowLayout();
			jPanel = new JPanel();
			jPanel.setLayout(flowLayout1);
			flowLayout1.setAlignment(java.awt.FlowLayout.RIGHT);
			jPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
			jPanel.add(getStatusText(), null);
		}
		return jPanel;
	}
	
	
	private JLabel getStatusText() {
		if (statusText == null) {
			statusText = new JLabel("[No file opened]");
		}
		return statusText;
	}
	
	
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */    
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getTextArea());
			jScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
		}
		return jScrollPane;
	}
	/**
	 * This method initializes textPane	
	 * 	
	 * @return javax.swing.JTextPane	
	 */    
	private JTextArea getTextArea() {
		if (textArea == null) {
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setLineWrap(false);
		}
		return textArea;
	}
	
	
	public static void showFileViewer(String filename) {
		JFrame frm = new JFrame(filename+" - TextViewer");
		frm.setContentPane(new TextViewer(filename));
		frm.setSize(640,500);
		frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frm.setVisible(true);
	}

    /**
     * @param parent Must be a java.awt.Dialog or java.awt.Frame. 
     * @param filename
     */
    public static void showFileViewerDialog(Window parent, String filename) 
    {
        JDialog frm = null;
        if (parent instanceof Dialog)
            frm = new JDialog((Dialog) parent, filename+" - TextViewer");            
        else if (parent instanceof Frame)
            frm = new JDialog((Frame) parent, filename+" - TextViewer");
        else
        {
            frm = new JDialog();
            frm.setTitle(filename+" - TextViewer");
        }
        frm.setContentPane(new TextViewer(filename));
        frm.setSize(640,500);
        frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frm.setModal(true);
        frm.setVisible(true);
    }

	public static void main(String args[]) {
		showFileViewer("/etc/passwd");
	}
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
