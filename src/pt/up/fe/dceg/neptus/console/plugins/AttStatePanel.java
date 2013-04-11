/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.GridLayout;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;
/**
 * 
 * @author RJPG
 *
 */
public class AttStatePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    protected String varroll;
	
	public boolean viewroll=true;
	private JLabel rolllabel = null;
	public JLabel rolltext = null;
	
	public boolean viewpitch=true;
	private JLabel pitchlabel = null;
	public JLabel pitchtext = null;
	
	public boolean viewyaw=true;
	private JLabel yawlabel = null;
	public JLabel yawtext = null;
	
	public float roll  = 0.f;
	public float pitch = 0.f;
	public float yaw   = 0.f;
	
    public int precision = 2;    
    protected NumberFormat nf = GuiUtils.getNeptusDecimalFormat(precision);
    
	public void refres()
	{
			//NumberFormat nf = GuiUtils.getNeptusDecimalFormat(precision);
			
			this.rolltext.setText(""+nf.format((Math.toDegrees(this.roll)%360)));
			
			this.pitchtext.setText(""+nf.format((Math.toDegrees(this.pitch)%360)));
			
			this.yawtext.setText(""+nf.format((Math.toDegrees(this.yaw)%360)));
			
		
	}
	
	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
        nf = GuiUtils.getNeptusDecimalFormat(this.precision);
		this.refres();
	}

	/**
	 * This is the default constructor
	 */
	public AttStatePanel() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {

		this.setLayout(new GridLayout(3,2));
		this.getAttLabels();
	}

	private void getAttLabels() {
			yawtext = new JLabel("Texto yaw",JLabel.CENTER);
			yawtext.setText("-");
			yawlabel = new JLabel("yaw:",JLabel.CENTER);
			yawlabel.setText("yaw:");
			pitchtext = new JLabel("Texto picth",JLabel.CENTER);
			pitchtext.setText("-");
			pitchlabel = new JLabel("pitch:",JLabel.CENTER);
			pitchlabel.setText("pitch:");
			rolltext = new JLabel("Texto roll",JLabel.CENTER);
			rolltext.setText("-");
	
			rolllabel = new JLabel("roll:",JLabel.CENTER);

			
			this.resetdisplay();
			
		}
		
	public void resetdisplay()
	{
		int x=0;
		this.removeAll();
		if(viewroll)
		{
			this.add(rolllabel);
			this.add(rolltext);
			x++;
		}
		if(viewpitch)
		{
			this.add(pitchlabel);
			this.add(pitchtext);
			x++;
		}
		if(viewyaw)
		{
			this.add(yawlabel);
			this.add(yawtext);
			x++;
		}
		this.setLayout(new GridLayout(x,2));
		this.doLayout();
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
		this.refres();
	}

	public float getRoll() {
		return roll;
	}

	public void setRoll(float roll) {
		this.roll = roll;
		this.refres();
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
		this.refres();
	}
	public static void main (String []arg)
	{
		AttStatePanel buss=new AttStatePanel();
		GuiUtils.testFrame(buss,"Atitude");
		buss.setPrecision(0);
		for(int i=0;;i++)
		{
			buss.setYaw(i/11);
			buss.setPitch(i/12);
			buss.setRoll(i/13);
			try {//nada
				//NeptusLog.pub().info("<###>espera...");
				Thread.sleep(10);
				//NeptusLog.pub().info("<###>esperou");
			}
			catch (Exception e){
			    e.printStackTrace();
			}
		}
	}

	public boolean isViewpitch() {
		return viewpitch;
	}

	public void setViewpitch(boolean viewpitch) {
		this.viewpitch = viewpitch;
		this.resetdisplay();
	}

	public boolean isViewroll() {
		return viewroll;
	}

	public void setViewroll(boolean viewroll) {
		this.viewroll = viewroll;
		this.resetdisplay();
	}

	public boolean isViewyaw() {
		return viewyaw;
	}

	public void setViewyaw(boolean viewyaw) {
		this.viewyaw = viewyaw;
		this.resetdisplay();
	}

	public void variablesChanged(String[] variableNames, Object[] oldValues, Object[] newValues) {
		// TODO Auto-generated method stub
		
	}

	public void variablesUpdated(String[] variableNames, Object[] currentValues) {
		// TODO Auto-generated method stub
		
	}

	
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
