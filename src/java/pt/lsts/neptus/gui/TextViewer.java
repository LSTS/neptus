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
 * Author: 
 * Jun 1, 2005
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.util.FileUtil;
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
