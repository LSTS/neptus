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
 * 2005/08/03
 * $Id:: ConditionEditor.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.env;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import pt.up.fe.dceg.neptus.util.GuiUtils;
/**
 * @author zp
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConditionEditor extends JPanel {

	private static final long serialVersionUID = 1L;
	private JPanel jPanel = null;
	private JPanel jPanel1 = null;
	private JPanel jPanel2 = null;
	private JButton okBtn = null;
	private JButton cancelBtn = null;
	private JTextPane conditionText = null;
	private JLabel jLabel = null;
	private JComboBox<?> varCombo = null;
	private JLabel jLabel1 = null;
	private Environment env = null;
	
	public ConditionEditor(Condition condition) {
		this(condition.getEnv(), condition.getConditionText());
	}
	
	/**
	 * This is the default constructor
	 */
	public ConditionEditor(Environment env, String initialCondition) {
		super();
		this.env = env;
		getConditionText().setText(initialCondition);
		initialize();
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
		this.add(getJPanel1(), java.awt.BorderLayout.CENTER);
		this.add(getJPanel2(), java.awt.BorderLayout.NORTH);
	}
	
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			FlowLayout flowLayout2 = new FlowLayout();
			jPanel = new JPanel();
			jPanel.setLayout(flowLayout2);
			flowLayout2.setHgap(10);
			flowLayout2.setAlignment(java.awt.FlowLayout.RIGHT);
			jPanel.add(getOkBtn(), null);
			jPanel.add(getCancelBtn(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jLabel = new JLabel();
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.Y_AXIS));
			jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(20,20,20,20));
			jLabel.setText("Condition:");
			jPanel1.add(jLabel, null);
			jPanel1.add(getConditionText(), null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jLabel1 = new JLabel();
			FlowLayout flowLayout3 = new FlowLayout();
			jPanel2 = new JPanel();
			jPanel2.setLayout(flowLayout3);
			jLabel1.setText("Variables:");
			flowLayout3.setAlignment(java.awt.FlowLayout.LEFT);
			flowLayout3.setHgap(10);
			jPanel2.add(jLabel1, null);
			jPanel2.add(getVarCombo(), null);
		}
		return jPanel2;
	}
	/**
	 * This method initializes okBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getOkBtn() {
		if (okBtn == null) {
			okBtn = new JButton();
			okBtn.setText("OK");
			okBtn.setPreferredSize(new java.awt.Dimension(75,25));
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Scriptable scope = env.getScope();
					Context cx = Context.enter();
					try {
						Object result = cx.evaluateString(scope, conditionText.getText(), "<condition>", 0, null);
						GuiUtils.errorMessage(SwingUtilities.getRoot((Component)e.getSource()), "Result of condition", "Condition returned "+result);
					}
					catch (Exception exception) {
						GuiUtils.errorMessage(SwingUtilities.getRoot((Component)e.getSource()), "Error parsing condition", exception.getMessage());
					}
				}
			});
		}
		return okBtn;
	}
	/**
	 * This method initializes cancelBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setText("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFrame frame = (JFrame)SwingUtilities.getRoot((Component) e.getSource());
					frame.setVisible(false);
					frame.dispose();
				}
			});

		}
		return cancelBtn;
	}
	/**
	 * This method initializes conditionText	
	 * 	
	 * @return javax.swing.JTextPane	
	 */    
	private JTextPane getConditionText() {
		if (conditionText == null) {
			conditionText = new JTextPane();
			conditionText.setPreferredSize(new java.awt.Dimension(250,40));
		}
		return conditionText;
	}
	/**
	 * This method initializes varCombo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getVarCombo() {
		if (varCombo == null) {
			varCombo = new JComboBox<Object>(env.getVariableNames());
			varCombo.setPreferredSize(new java.awt.Dimension(110,21));
			varCombo.setEditable(false);
			varCombo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getConditionText().setText(getConditionText().getText()+ " "+varCombo.getSelectedItem());
				}
			});
		}
		return varCombo;
	}
	
	
	
	
	public static void main(String args[]) {
		Environment env = new Environment();
		//env.putEnv("x", new Double(0.3));
		//env.putEnv("y", new Double(-12.7));
		//env.putEnv("VinteTres", new Integer(23));
		//env.putEnv("Verdadeiro", new Boolean(true));
		
		GuiUtils.testFrame(new ConditionEditor(env, "vintetres"), "Teste");
	}
}
