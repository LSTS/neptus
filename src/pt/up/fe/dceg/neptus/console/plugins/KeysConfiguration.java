/*
s * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;

public class KeysConfiguration extends JPanel implements KeyListener{


    private static final long serialVersionUID = 1L;
    private JButton Ok = null;
	private JButton Cancel = null;
	private JButton Config = null;
	private JLabel label = null;
	
	private JFrame mainframe=null;

	protected ConsoleLayout console;
	
//	 ------------------------keyboard joy keys code---------------------
	// Axis 0
	protected int axis0up=38;
	protected int axis0down=40;
	protected int axis0left=37;
	protected int axis0right=39;
	
	// Axis 1
	protected int axis1up=104;
	protected int axis1down=98;
	protected int axis1left=100;
	protected int axis1right=102;
	
	//analog_cmd
	protected int analogup=107;
	protected int analogdown=109;
	
	// axis 2
	protected int axis2up=87;
	protected int axis2down=83;
	protected int axis2left=65;
	protected int axis2right=68;

	// axis 2
	protected int butt1=155;
	protected int butt2=36;
	protected int butt3=33;
	protected int butt4=127;
	protected int butt5=35;
	protected int butt6=34;
	
	// -------------------------------------------------------------------

	/**
	 * This is the default constructor
	 */
	public KeysConfiguration(ConsoleLayout cons) {
		super();
		console=cons;
		initialize();
	}
	
	public KeysConfiguration(ConsoleLayout cons,JFrame main) {
		super();
		console=cons;
		mainframe=main;
		initialize();
	}



	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		
		this.addKeyListener(this);
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				requestFocusInWindow();
				
			}
		});
		requestFocusInWindow();
		
		label = new JLabel();
		label.setText("Press Ok to Reset");
		label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		label.setBounds(new java.awt.Rectangle(40,50,130,20));
		this.setLayout(null);
		this.setSize(208, 111);
		this.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Keyboard Configuration", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
		this.add(getOk(), null);
		this.add(getCancel(), null);
		this.add(getConfig(), null);
		this.add(label, null);
	}

	/**
	 * This method initializes Ok	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getOk() {
		if (Ok == null) {
			Ok = new JButton();
			Ok.setBounds(new java.awt.Rectangle(10,80,90,20));
			Ok.setText("Ok");
		}
		return Ok;
	}

	/**
	 * This method initializes Cancel	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getCancel() {
		if (Cancel == null) {
			Cancel = new JButton();
			Cancel.setBounds(new java.awt.Rectangle(110,80,90,20));
			Cancel.setText("Cancel");
			Cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					close();
				}
			});
		}
		return Cancel;
	}

	/**
	 * This method initializes Config	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConfig() {
		if (Config == null) {
			Config = new JButton();
			Config.setBounds(new java.awt.Rectangle(40,20,130,20));
			Config.setText("Config");
			Config.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Ok.setEnabled(false);
					label.setText("Axis 0 UP");
					requestFocusInWindow();
				}
			});
		}
		return Config;
	}

	public void keyTyped(KeyEvent arg0) {
	
		
	}

	public void keyPressed(KeyEvent arg0) {
	
		
	}

	public void keyReleased(KeyEvent e) {
		
		if("Axis 0 UP".equals(label.getText()))
		{
			axis0up = e.getKeyCode();
			label.setText("Axis 0 DOWN");
		
		}
		else if("Axis 0 DOWN".equals(label.getText()))
		{
			axis0down = e.getKeyCode();
			label.setText("Axis 0 LEFT");
			return;
		
		}
		
		if("Axis 0 LEFT".equals(label.getText()))
		{
			axis0left = e.getKeyCode();
			label.setText("Axis 0 RIGHT");
			return;
		}
		
		if("Axis 0 RIGHT".equals(label.getText()))
		{
			axis0right = e.getKeyCode();
			label.setText("Axis 1 UP");
			return;
		}
		
		if("Axis 1 UP".equals(label.getText()))
		{
			axis1up = e.getKeyCode();
			label.setText("Axis 1 DOWN");
			return;
		}
		
		if("Axis 1 DOWN".equals(label.getText()))
		{
			axis1down = e.getKeyCode();
			label.setText("Axis 1 LEFT");
			return;
		}
		
		if("Axis 1 LEFT".equals(label.getText()))
		{
			axis1left = e.getKeyCode();
			label.setText("Axis 1 RIGHT");
			return;
		}
		
		
		if("Axis 1 RIGHT".equals(label.getText()))
		{
			axis1right = e.getKeyCode();
			label.setText("Axis 2 UP");
			return;
		}
		
		if("Axis 2 UP".equals(label.getText()))
		{
			axis2up = e.getKeyCode();
			label.setText("Axis 2 DOWN");
			return;
		}
		
		if("Axis 2 DOWN".equals(label.getText()))
		{
			axis2down = e.getKeyCode();
			label.setText("Axis 2 LEFT");
			return;
		}
		
		if("Axis 2 LEFT".equals(label.getText()))
		{
			axis2left = e.getKeyCode();
			label.setText("Axis 2 RIGHT");
			return;
		}
		
		if("Axis 2 RIGHT".equals(label.getText()))
		{
			axis2right = e.getKeyCode();
			label.setText("Analogic UP");
			return;
		}
		
		if("Analogic UP".equals(label.getText()))
		{
			analogup = e.getKeyCode();
			label.setText("Analogic DOWN");
			return;
		}
		
		if("Analogic DOWN".equals(label.getText()))
		{
			analogdown = e.getKeyCode();
			label.setText("Button 1");
			return;
		}
		
		if("Button 1".equals(label.getText()))
		{
			butt1 = e.getKeyCode();
			label.setText("Button 2");
			return;
		}
		
		if("Button 2".equals(label.getText()))
		{
			butt2 = e.getKeyCode();
			label.setText("Button 3");
			return;
		}
		if("Button 3".equals(label.getText()))
		{
			butt3 = e.getKeyCode();
			label.setText("Button 4");
			return;
		}
		if("Button 4".equals(label.getText()))
		{
			butt4 = e.getKeyCode();
			label.setText("Button 5");
			return;
		}
		if("Button 5".equals(label.getText()))
		{
			butt5 = e.getKeyCode();
			label.setText("Button 6");
			return;
		}
		
		if("Button 6".equals(label.getText()))
		{
			butt6 = e.getKeyCode();
			Ok.setEnabled(true);
			label.setText("Press Ok to set Console");
		}
		
	}
		
	protected void close()
	{
		this.removeKeyListener(this);
		if(mainframe!=null)
			mainframe.dispose();
	}

	protected ConsoleLayout getConsole() {
		return console;
	}

	protected void setConsole(ConsoleLayout console) {
		this.console = console;
	}
	

}  
