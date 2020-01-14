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
 * Author: 
 * 26/Mai/2005
 */
package pt.lsts.neptus.gui;

import java.awt.FlowLayout;
import java.io.InputStream;

import javax.swing.JPanel;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
/**
 * @author Paulo Dias
 */
public class VehicleDepthFrontDistanceInfo extends JPanel
{
    private static final long serialVersionUID = 1L;
    private ImagePanel beanImage = null;
	private JPanel jPanel = null;
	private ImagePanel upSetaImage = null;
	private ImagePanel vehicleImage = null;
	private ImagePanel downSetaImage = null;
	
	private String vehicleImageFile = null; 

    /**
     * 
     */
    public VehicleDepthFrontDistanceInfo()
    {
        super();
        initialize();
    }

    
    public VehicleDepthFrontDistanceInfo(String vehicleImageFile)
    {
        super();
        this.vehicleImageFile = vehicleImageFile;
        initialize();
    }

	/**
	 * This method initializes imagePanel	
	 * 	
	 * @return pt.lsts.neptus.gui.ImagePanel	
	 */    
	private ImagePanel getBeanImage() {
		if (beanImage == null) {
			beanImage = new ImagePanel();
			beanImage.setLayout(null);
			InputStream ist = getClass().getResourceAsStream("/images/def-bean.png");
		    String inFileName = StreamUtil.copyStreamToTempFile(ist).getAbsolutePath();
			//imagePanel.image = GuiUtils.getImage("images/def-bean.png");
		    beanImage.setImage(inFileName);
			beanImage.setImageWidth(42);
			beanImage.setPreferredSize(new java.awt.Dimension(40,213));
			beanImage.setImageHeight(213);
			beanImage.setBackground(new java.awt.Color(3,113,172));
			//        InputStream ist = getClass().getResourceAsStream("/models/mar.3ds");
		    //    String inFileName = StreamUtil.copyStreamToTempFile(ist).getAbsolutePath();

		}
		return beanImage;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private  void initialize() {
		FlowLayout flowLayout1 = new FlowLayout();
		this.setLayout(flowLayout1);
		this.setPreferredSize(new java.awt.Dimension(150,213));
		this.setSize(150, 213);
		this.setForeground(new java.awt.Color(3,113,172));
		this.setBackground(new java.awt.Color(3,113,172));
		flowLayout1.setHgap(0);
		flowLayout1.setVgap(0);
		this.add(getJPanel(), null);
		this.add(getBeanImage(), null);
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
			jPanel.setPreferredSize(new java.awt.Dimension(108,213));
			flowLayout2.setHgap(0);
			flowLayout2.setVgap(0);
			jPanel.add(getUpSetaImage(), null);
			jPanel.add(getVehicleImage(), null);
			jPanel.add(getDownSetaImage(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes imagePanel	
	 * 	
	 * @return pt.lsts.neptus.gui.ImagePanel	
	 */    
	private ImagePanel getUpSetaImage() {
		if (upSetaImage == null) {
			upSetaImage = new ImagePanel();
			InputStream ist = getClass().getResourceAsStream("/images/def-set.png");
		    String inFileName = StreamUtil.copyStreamToTempFile(ist).getAbsolutePath();
		    upSetaImage.setImage(inFileName);
		    upSetaImage.setPreferredSize(new java.awt.Dimension(108,77));
		    upSetaImage.setBackground(new java.awt.Color(3,113,172));
		    upSetaImage.adjustImageSizeToPreferredSize();
		}
		return upSetaImage;
	}
	/**
	 * This method initializes imagePanel	
	 * 	
	 * @return pt.lsts.neptus.gui.ImagePanel	
	 */    
	private ImagePanel getVehicleImage() {
		if (vehicleImage == null) {
			vehicleImage = new ImagePanel();
			//InputStream ist = getClass().getResourceAsStream("/images/def-veh.png");
			String inFileName;
			if (vehicleImageFile == null)
			{
			    InputStream ist = getClass().getResourceAsStream("/images/def-veh.png");
			    inFileName = StreamUtil.copyStreamToTempFile(ist).getAbsolutePath();
			}
			else
			    inFileName = vehicleImageFile;
		    vehicleImage.setImage(inFileName);
		    //vehicleImage.setImageHeight(46);
			vehicleImage.setPreferredSize(new java.awt.Dimension(108,59));
			vehicleImage.setBackground(new java.awt.Color(3,113,172));
			vehicleImage.adjustImageSizeToPreferredSize();
		}
		return vehicleImage;
	}
	/**
	 * This method initializes imagePanel	
	 * 	
	 * @return pt.lsts.neptus.gui.ImagePanel	
	 */    
	private ImagePanel getDownSetaImage() {
		if (downSetaImage == null) {
			downSetaImage = new ImagePanel();
			InputStream ist = getClass().getResourceAsStream("/images/def-set.png");
		    String inFileName = StreamUtil.copyStreamToTempFile(ist).getAbsolutePath();
		    downSetaImage.setImage(inFileName);
			downSetaImage.setPreferredSize(new java.awt.Dimension(108,77));
			downSetaImage.setBackground(new java.awt.Color(3,113,172));
			downSetaImage.adjustImageSizeToPreferredSize();
		}
		return downSetaImage;
	}
        
	/**
	 * @param vehicleImageFile If null sets the default image.
	 */
	public void changeVehicleImage (String vehicleImageFile)
	{
	    this.vehicleImageFile = vehicleImageFile;
	    vehicleImage.setImage(this.vehicleImageFile);
	    vehicleImage.adjustImageSizeToPreferredSize();
	    this.paintImmediately(0, 0, 300, 300);
	}

	public void substituteVehiclePanel(JPanel vehiclePanel)
	{
		vehiclePanel.setPreferredSize(new java.awt.Dimension(108,59));
		getJPanel().remove(getUpSetaImage());
		getJPanel().remove(getVehicleImage());
		getJPanel().remove(getDownSetaImage());
		jPanel.add(getUpSetaImage(), null);
		jPanel.add(vehiclePanel, null);
		jPanel.add(getDownSetaImage(), null);
		jPanel.paintImmediately(0, 0, 300, 300);
	}

    public void restoreVehiclePanel()
    {
        getJPanel().removeAll();
        jPanel.add(getUpSetaImage(), null);
        jPanel.add(getVehicleImage(), null);
        jPanel.add(getDownSetaImage(), null);
        jPanel.paintImmediately(0, 0, 300, 300);
    }

    
	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException
    {
        ConfigFetch.initialize();
        VehicleDepthFrontDistanceInfo vv = new VehicleDepthFrontDistanceInfo();
        GuiUtils.testFrame(vv, "Unitary Test");
        
        Thread.sleep(2000);
        vv.changeVehicleImage("vehicles-files/rov-kos/conf/images/rov-kos_side.png");
        
        Thread.sleep(2000);
        vv.changeVehicleImage("vehicles-files/isurus/conf/images/isurus_side.png");
        
        Thread.sleep(2000);
        vv.changeVehicleImage("vehicles-files/rov-ies/conf/images/rov-ies_side.png");
        
        Thread.sleep(2000);
        vv.changeVehicleImage("vehicles-files/apv/conf/images/avp-l.png");
        
    }
}
