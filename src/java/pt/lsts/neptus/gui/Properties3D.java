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
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.renderer3d.Obj3D;
import pt.lsts.neptus.renderer3d.Renderer3D;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.CylinderElement;
import pt.lsts.neptus.types.map.EllipsoidElement;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.util.GuiUtils;

public class Properties3D extends JDialog  {
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JPanel jPanel = null;
	//private JLabel jLabel = null;
	private Renderer3D renderer = null;
	private AbstractElement obj=null;
	private JLabel jLabel2 = null;
	private JTextArea jTextArea = null;
	private JButton okBtn = null;
	private JButton cancelBtn = null;
	private JCheckBox checktrans = null;
	private JCheckBox checkaxis = null;
	private JCheckBox checkhide = null;
	private JCheckBox checklabel = null;
	private JCheckBox checkinfo = null;
	private JCheckBox checkfocus = null;
	private JButton buttonfocus=null;
	
	JScrollPane scroll=null;
	private JButton jButton = null;
	
	
	public void setRenderer3D(Renderer3D r3d) {
		this.renderer = r3d;
	}
	
	public void setMapObject(AbstractElement mobj)
	{
		obj=mobj;
		jLabel2.setText("Map Object "+obj.getId()); //nome
		//NeptusLog.pub().info("<###> "+renderer.istrans(obj));
		
		checktrans.setSelected(renderer.isTrans(obj));//transp
		if (!(obj instanceof EllipsoidElement) && !(obj instanceof CylinderElement) &&!(obj instanceof ParallelepipedElement) &&!(obj instanceof ImageElement))
			checktrans.setEnabled(false);  //se não dá para mudar 
		
		//axis
		checkaxis.setSelected(((Obj3D) renderer.objects.get(obj)).drawaxis);
		checkinfo.setSelected(((Obj3D) renderer.objects.get(obj)).drawinfo);
		checklabel.setSelected(((Obj3D) renderer.objects.get(obj)).drawlabel);
		
		checkhide.setSelected(((Obj3D) renderer.objects.get(obj)).hide);
		// se estiver escondino não pode prender a cam ... (?)
		if (((Obj3D) renderer.objects.get(obj)).hide)
			checkfocus.setEnabled(false);  //se não dá para mudar 
		
		
		if (renderer.cams[renderer.panel_op].lockmapobj==obj)
		{
			checkfocus.setSelected(true);
		}
		
		
		
		//info
		jTextArea.setText("Name:"+obj.getId()+
				"\nId:"+obj.getId()+"\nPosition:\nN:"+obj.getNEDPosition()[0]+"\nE:"+obj.getNEDPosition()[1]+
				"\nD:"+obj.getNEDPosition()[2]);
		
		//scroll.setViewport(0,0);
	}
	
	private void initialize() {
        this.setContentPane(getJPanel());
        this.setSize(303, 309);
        this.setResizable(false);
        this.setTitle("Object Render3D");
			
	}
	
	public Properties3D(Frame parent) {
		super(parent);
		initialize();
	}
	
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel2 = new JLabel();
			jPanel = new JPanel();
			jPanel.setLayout(null);
			jLabel2.setBounds(10, 10, 265, 20);
			jPanel.add(getOkBtn(), null);
			jPanel.add(getCancelBtn(), null);
			jPanel.add(getCheckTrans(), null);
			jPanel.add(getCheckHide(),null);
			jPanel.add(getCheckAxis(), null);
			jPanel.add(getCheckLabel(), null);
			jPanel.add(getCheckInfo(), null);
			jPanel.add(getCheckFocus(),null);
			jPanel.add(getJTextArea(),null);
			jPanel.add(getFocusBtn(),null);
			jPanel.add(jLabel2, null);
			jPanel.add(getJButton(), null);
		}
		return jPanel;
	}
	
	private JCheckBox getCheckTrans() {
		if (checktrans == null) {
			checktrans = new JCheckBox();
			checktrans.setText("Tranparency");
			checktrans.setBounds(97, 74, 108, 20);
		}
		return checktrans;
	}
	
	private JCheckBox getCheckHide() {
		if (checkhide == null) {
			checkhide = new JCheckBox();
			checkhide.setText("Hide");
			checkhide.setBounds(8, 53, 55, 20);
		}
		return checkhide;
	}

	
	private JCheckBox getCheckAxis() {
		if (checkaxis == null) {
			checkaxis = new JCheckBox();
			checkaxis.setText("Show Axis");
			checkaxis.setBounds(8, 34, 80, 20);
		}
		return checkaxis;
	}

	private JCheckBox getCheckLabel() {
		if (checklabel == null) {
			checklabel = new JCheckBox();
			checklabel.setText("Labeling");
			checklabel.setBounds(97, 34, 90, 20);
		}
		return checklabel;
	}
	
	private JCheckBox getCheckInfo() {
		if (checkinfo == null) {
			checkinfo = new JCheckBox();
			checkinfo.setText("Show Info");
			checkinfo.setBounds(8, 73, 80, 20);
		}
		return checkinfo;
	}
	
	private JCheckBox getCheckFocus() {
		if (checkfocus == null) {
			checkfocus = new JCheckBox();
			checkfocus.setText("Lock view on");
			checkfocus.setBounds(97, 55, 105, 20);
		}
		return checkfocus;
	}
	
	private JScrollPane getJTextArea() {
		if (jTextArea == null) {
			//jPanel1.setBounds(10, 130, 170, 80);
			
			jTextArea = new JTextArea();
			//jTextArea.setBounds(10, 130, 170, 80);
			
			jTextArea.setAutoscrolls(true);
			jTextArea.setEditable(false);
			jTextArea.setBackground(new Color(225,225,225));
			
			
			scroll = new JScrollPane(jTextArea);
			scroll.setName("Information");
			scroll.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Information", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			scroll.setBounds(10, 110, 272, 103);
			
		}
		return scroll;
	}
	
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setBounds(115, 225, 70, 30);
			cancelBtn.setText("Cancel");
			cancelBtn.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					setVisible(false);
					dispose();
				}
			});
		}
		return cancelBtn;
	}
	
	
	
	private void setRenderer() {
	
		if (checktrans.isSelected())//transparencia
			renderer.setTrans(obj);
		else
			renderer.setNoTrans(obj);
	
		if (checkaxis.isSelected())// axis
			((Obj3D) renderer.objects.get(obj)).DrawAxis(true);
		else
			((Obj3D) renderer.objects.get(obj)).DrawAxis(false);
		
		if (checklabel.isSelected())// axis
			((Obj3D) renderer.objects.get(obj)).drawlabel=true;
		else
			((Obj3D) renderer.objects.get(obj)).drawlabel=false;
		
		if (checkinfo.isSelected())// axis
			((Obj3D) renderer.objects.get(obj)).drawinfo=true;
		else
			((Obj3D) renderer.objects.get(obj)).drawinfo=false;
		
		if (checkhide.isSelected())// axis
			renderer.hide(obj);
		else
			renderer.unHide(obj);
		
		
		
		if (renderer.cams[renderer.panel_op].lockmapobj==obj) // focus
		{
			if (!checkfocus.isSelected()) //focus
				renderer.lockView(renderer.panel_op,null);
		}
		else
		{
			if(checkfocus.isSelected())
				renderer.lockView(renderer.panel_op,((Obj3D) renderer.objects.get(obj)));
		}
	}

	
	
	private JButton getOkBtn() {
		if (okBtn == null) {
			okBtn = new JButton();
			okBtn.setBounds(30, 225, 70, 30);
			okBtn.setText("OK");
			okBtn.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					setRenderer();
					setVisible(false);
					dispose();
				}
			});
		}
		return okBtn;
	}
	
	private JButton getFocusBtn()
	{
		if(buttonfocus==null)
		{
			buttonfocus=new JButton();
			buttonfocus.setBounds(208, 38, 70, 55);
			buttonfocus.setText("Focus");
			buttonfocus.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					renderer.focusLocation(obj.getCenterLocation());
				}
			});
		}
			
		return buttonfocus;
	}
	
	public static void showPropreties3DDialog(Renderer3D r3d,AbstractElement mobj) {
		Frame parentFrame = (Frame) SwingUtilities.getRoot(r3d);
		
		Properties3D p3d = new Properties3D(parentFrame);
		p3d.setRenderer3D(r3d);
		p3d.setVisible(true);
		p3d.setModal(true);
		p3d.setMapObject(mobj);
		GuiUtils.centerOnScreen(p3d);
		
	}

	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setBounds(new java.awt.Rectangle(200,225,70,30));
			jButton.setText("Apply");
			jButton.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					setRenderer();
				}
			});
		}
		
		return jButton;
	}
}  //  @jve:decl-index=0:visual-constraint="83,10"
