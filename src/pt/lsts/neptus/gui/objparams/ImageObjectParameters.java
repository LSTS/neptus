/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 2005/06/16
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ImageFileChooser;
import pt.lsts.neptus.gui.ImageScaleAndLocationPanel;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * @author Paulo Dias
 */
public class ImageObjectParameters extends ParametersPanel {

	private static final long serialVersionUID = 1L;

	private JPanel jPanel = null;
	private JLabel imgLabel = null;
	private JButton selectImage = null;
	private JPanel lat = null;
	private JPanel jPanel2 = null;
	private JLabel jLabel1 = null;
	private JButton changeCenter = null;
	private JPanel jPanel4 = null;
	private JLabel jLabel3 = null;
	private JFormattedTextField scale = null;
	private NumberFormat df = GuiUtils.getNeptusDecimalFormat();
	
	private String imageFileName = null;
	private LocationType center = new LocationType();
	
	private JPanel jPanel1 = null;
	
	private JPanel jPanel3 = null;
	private JButton jButton = null;

	private JPanel batPanel = null;

	private JPanel jPanel5 = null;

	private JCheckBox batCheckBox = null;

	private JLabel maxHeightLabel = null;

	private JFormattedTextField maxHeightFormattedTextField = null;

	private JPanel jPanel6 = null;

	private JPanel jPanel7 = null;

	private JLabel maxDepthLabel = null;

	private JLabel resolutionLabel = null;

	private JFormattedTextField maxDepthFormattedTextField = null;

	private JFormattedTextField resolutionFormattedTextField = null;

	private JPanel jPanel8 = null;

	private JButton bathImageSelect = null;
	
	private File selectedBathymetricImage = null;

	/**
	 * This method initializes 
	 * 
	 */
	public ImageObjectParameters() {
		super();
		initialize();
		setPreferredSize(new java.awt.Dimension(505,300));
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        GridLayout gridLayout6 = new GridLayout();
        this.setLayout(gridLayout6);
        //this.setSize(396, 181);
        this.setSize(505, 181);
        gridLayout6.setRows(1);
        gridLayout6.setColumns(3);
        this.add(getJPanel1(), null);
        this.add(getLat(), null);
        this.add(getBatPanel(), null);
	}

	public String getErrors() {
		if (getImageFileName() == null) {
			return "The selected image is invalid";
		}
		
		if (batCheckBox.isSelected() && selectedBathymetricImage == null) {
			return "You must select a bathymetric image";
		}
		
		if (batCheckBox.isSelected() && !selectedBathymetricImage.canRead()) {
			return "Unable to read the bathymetric image file";
		}
		
		return null;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			//jLabel = new JLabel();
			jPanel = new JPanel();
			jPanel.setLayout(new BorderLayout());
			jPanel.setPreferredSize(new java.awt.Dimension(150,170));
			jPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, I18n.text("Image"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12), new java.awt.Color(51,51,51)));
			
			jPanel.add(getImgLabel(), java.awt.BorderLayout.NORTH);
			jPanel.add(getSelectImage(), java.awt.BorderLayout.SOUTH);
		}
		return jPanel;
	}
	
	/**
	 * This method initializes selectImage	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getSelectImage() {
		if (selectImage == null) {
			selectImage = new JButton();
			selectImage.setText(I18n.text("Select image..."));
			selectImage.setPreferredSize(new java.awt.Dimension(90,25));
			selectImage.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File f = ImageFileChooser.showOpenImageDialog();
					if (f != null) {
						setImageFileName(f.getAbsolutePath());
					}
				}
			});
		}
		return selectImage;
	}
	
	/**
	 * This method initializes lat	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getLat() {
		if (lat == null) {
			lat = new JPanel();
			lat.setLayout(new BorderLayout());
			lat.add(getJPanel3(), java.awt.BorderLayout.NORTH);
			lat.add(getJPanel2(), java.awt.BorderLayout.CENTER);
		}
		return lat;
	}
	
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			FlowLayout flowLayout1 = new FlowLayout();
			jLabel1 = new JLabel();
			jPanel2 = new JPanel();
			jPanel2.setLayout(flowLayout1);
			jLabel1.setText(I18n.text("Center:"));
			flowLayout1.setVgap(30);
			jPanel2.add(jLabel1, null);
			jPanel2.add(getChangeCenter(), null);
			jPanel2.add(getJPanel4(), null);
			jPanel2.add(getJButton(), null);
		}
		return jPanel2;
	}
	
	/**
	 * This method initializes changeCenter	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getChangeCenter() {
		if (changeCenter == null) {
			changeCenter = new JButton();
			changeCenter.setText(I18n.text("Change..."));
			changeCenter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LocationType tmp = LocationPanel.showLocationDialog(I18n.text("Set the center location"), getCenter(), null);
					if (tmp != null)
						getCenter().setLocation(tmp);
					
				}
			});
		}
		return changeCenter;
	}
	
	/**
	 * This method initializes jPanel4	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			jLabel3 = new JLabel();
			jPanel4 = new JPanel();
			jLabel3.setText(I18n.text("Scale (m/pixel):"));
			jPanel4.add(jLabel3, null);
			jPanel4.add(getScale(), null);
		}
		return jPanel4;
	}
	
	/**
	 * This method initializes scale	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getScale() {
		if (scale == null) {
			scale = new JFormattedTextField(df);
			scale.setPreferredSize(new java.awt.Dimension(40,20));
			scale.setText("1.0");
		}
		return scale;
	}
	
	public LocationType getCenter() {
		return center;
	}
	
	public void setCenter(LocationType center) {
		this.center.setLocation(center);
	}
	
	public String getImageFileName() {
		return imageFileName;
	}
	
	public void setImageFileName(String imageFileName) {
		this.imageFileName = imageFileName;
		Image origImage = ImageUtils.getImage(getImageFileName());
		if (origImage != null)
			getImgLabel().setIcon(new ImageIcon(ImageUtils.getScaledImage(origImage, 100,100,false)));
	}
	
	public double getImageScale() {
		return Double.parseDouble(getScale().getText());
	}
	
	public void setImageScale(double scale) {
		getScale().setText(String.valueOf(scale));
	}
	
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.add(getJPanel(), null);
		}
		return jPanel1;
	}
	
	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			jPanel3 = new JPanel();
			jPanel3.setLayout(new BorderLayout());
			jPanel3.setSize(192, 122);
		}
		return jPanel3;
	}

	/**
	 * This method initializes jButton	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setPreferredSize(new java.awt.Dimension(120,26));
			jButton.setText(I18n.text("2 locations..."));
			jButton.addActionListener(new java.awt.event.ActionListener() { 
				
				public void actionPerformed(java.awt.event.ActionEvent e) {  
					try {
						ImageElement tmp = new ImageElement();
						tmp.setImage(ImageUtils.getImage(getImageFileName()));						
                        if (ImageScaleAndLocationPanel.showDialog(tmp,
                                SwingUtilities.getWindowAncestor(ImageObjectParameters.this))) {
							getScale().setText(String.valueOf(tmp.getImageScale()));
							center = tmp.getCenterLocation();
						}
					}
					catch (Exception exception) {
					    NeptusLog.pub().warn(exception.getMessage());
						GuiUtils.errorMessage(SwingUtilities.getRoot((Component)e.getSource()), I18n.text("Select an image first"), I18n.text("You have to select an image fisrt."));
					}
				}
			});
		}
		return jButton;
	}
	
	private JLabel getImgLabel() {
		if (imgLabel == null) {
			imgLabel = new JLabel();
			imgLabel.setPreferredSize(new Dimension(120,120));
			imgLabel.setHorizontalAlignment(JLabel.CENTER);
			imgLabel.setVerticalAlignment(JLabel.CENTER);
		}
		return imgLabel;
	}

    /**
     * This method initializes batPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getBatPanel() {
        if (batPanel == null) {
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.LEFT);
            batPanel = new JPanel();
            batPanel.setLayout(flowLayout);
            batPanel.add(getBatCheckBox(), null);
            batPanel.add(getJPanel5(), null);
            batPanel.add(getJPanel6(), null);
            batPanel.add(getJPanel7(), null);
            batPanel.add(getJPanel8(), null);
        }
        return batPanel;
    }

    /**
     * This method initializes jPanel5
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel5() {
        if (jPanel5 == null) {
            maxHeightLabel = new JLabel();
            maxHeightLabel.setText(I18n.text("Max height"));
            jPanel5 = new JPanel();
            jPanel5.add(maxHeightLabel, null);
            jPanel5.add(getMaxHeightFormattedTextField(), null);
        }
        return jPanel5;
    }

    /**
     * This method initializes batCheckBox
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getBatCheckBox() {
        if (batCheckBox == null) {
            batCheckBox = new JCheckBox();
            batCheckBox.setText(I18n.text("Is bathymetric"));
            batCheckBox.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    NeptusLog.pub().info("<###>itemStateChanged()"
                            + ((e.getStateChange() == ItemEvent.SELECTED) ? "Sel." : "Desel."));
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        getMaxHeightFormattedTextField().setEnabled(true);
                        getMaxDepthFormattedTextField().setEnabled(true);
                        getResolutionFormattedTextField().setEnabled(true);
                        getBathImageSelect().setEnabled(true);
                    }
                    else {
                        getMaxHeightFormattedTextField().setEnabled(false);
                        getMaxDepthFormattedTextField().setEnabled(false);
                        getResolutionFormattedTextField().setEnabled(false);
                        getBathImageSelect().setEnabled(false);
                    }
                }
            });
        }
        return batCheckBox;
    }

    /**
     * This method initializes MaxHeightFormattedTextField
     * 
     * @return javax.swing.JFormattedTextField
     */
    private JFormattedTextField getMaxHeightFormattedTextField() {
        if (maxHeightFormattedTextField == null) {
            maxHeightFormattedTextField = new JFormattedTextField(df);
            maxHeightFormattedTextField.setPreferredSize(new java.awt.Dimension(70, 20));
            maxHeightFormattedTextField.setText("1.0");
            maxHeightFormattedTextField.setEnabled(false);
        }
        return maxHeightFormattedTextField;
    }

    /**
     * This method initializes jPanel6
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel6() {
        if (jPanel6 == null) {
            maxDepthLabel = new JLabel();
            maxDepthLabel.setText(I18n.text("Max depth"));
            jPanel6 = new JPanel();
            jPanel6.add(maxDepthLabel, null);
            jPanel6.add(getMaxDepthFormattedTextField(), null);
        }
        return jPanel6;
    }

    /**
     * This method initializes jPanel7
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel7() {
        if (jPanel7 == null) {
            resolutionLabel = new JLabel();
            resolutionLabel.setText(I18n.text("Resolution"));
            jPanel7 = new JPanel();
            jPanel7.add(resolutionLabel, null);
            jPanel7.add(getResolutionFormattedTextField(), null);
        }
        return jPanel7;
    }

    /**
     * This method initializes maxDepthFormattedTextField
     * 
     * @return javax.swing.JFormattedTextField
     */
    private JFormattedTextField getMaxDepthFormattedTextField() {
        if (maxDepthFormattedTextField == null) {
            maxDepthFormattedTextField = new JFormattedTextField(df);
            maxDepthFormattedTextField.setPreferredSize(new java.awt.Dimension(70, 20));
            maxDepthFormattedTextField.setText("0.0");
            maxDepthFormattedTextField.setEnabled(false);
        }
        return maxDepthFormattedTextField;
    }

    /**
     * This method initializes resolutionFormattedTextField
     * 
     * @return javax.swing.JFormattedTextField
     */
    private JFormattedTextField getResolutionFormattedTextField() {
        if (resolutionFormattedTextField == null) {
            resolutionFormattedTextField = new JFormattedTextField(df);
            resolutionFormattedTextField.setPreferredSize(new java.awt.Dimension(70, 20));
            resolutionFormattedTextField.setText("" + ImageElement.DEFAULT_RESOLUTION);
            resolutionFormattedTextField.setEnabled(false);
        }
        return resolutionFormattedTextField;
    }
	
	public boolean getIsBathymetric() {
		return getBatCheckBox().isSelected();
	}
	
	public void setIsBathymetric(boolean val) {
		getBatCheckBox().setSelected(val);
	}

	public double getMaxHeight() {
		return Double.parseDouble(getMaxHeightFormattedTextField().getText());
	}
	
	public void setMaxHeight(double val) {
		getMaxHeightFormattedTextField().setText(Double.toString(val));
	}

	public double getMaxDepth() {
		return Double.parseDouble(getMaxDepthFormattedTextField().getText());
	}
	
	public void setMaxDepth(double val) {
		getMaxDepthFormattedTextField().setText(Double.toString(val));
	}

	public double getResolution() {
		return Double.parseDouble(getResolutionFormattedTextField().getText());
	}
	
	public void setResolution(double val) {
		getResolutionFormattedTextField().setText(Double.toString(val));
	}
	
	public File getBathimFile() {
		return selectedBathymetricImage;
	}
	
	public void setBathimFile(String filename) {
		if (filename != null)
			this.selectedBathymetricImage = new File(filename);
	}

	/**
	 * This method initializes jPanel8	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel8() {
		if (jPanel8 == null) {
			FlowLayout flowLayout2 = new FlowLayout();
			flowLayout2.setHgap(0);
			flowLayout2.setVgap(0);
			jPanel8 = new JPanel();
			jPanel8.setLayout(flowLayout2);
			jPanel8.setPreferredSize(new Dimension(145, 30));
			jPanel8.add(getBathImageSelect(), null);
		}
		return jPanel8;
	}


	/**
	 * This method initializes bathImageSelect	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBathImageSelect() {
		if (bathImageSelect == null) {
			bathImageSelect = new JButton();
			bathImageSelect.setPreferredSize(new Dimension(145, 26));
			bathImageSelect.setText(I18n.text("Bathymetric Image..."));
			bathImageSelect.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					File imgFile = ImageFileChooser.showOpenImageDialog();
					if (imgFile != null)
						selectedBathymetricImage = imgFile;
				}
			});
			bathImageSelect.setEnabled(false);
		}
		return bathImageSelect;
	}

	public File getSelectedBathymetricImage() {
		return selectedBathymetricImage;
	}

	public void setSelectedBathymetricImage(File selectedBathymetricImage) {
		this.selectedBathymetricImage = selectedBathymetricImage;
	}

    public static void main(String args[]) {
        GuiUtils.testFrame(new ImageObjectParameters(), "test");
    }
}
