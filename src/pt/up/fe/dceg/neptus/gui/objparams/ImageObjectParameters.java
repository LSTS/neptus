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
 * 2005/06/16
 */
package pt.up.fe.dceg.neptus.gui.objparams;

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

import pt.up.fe.dceg.neptus.gui.ImageFileChooser;
import pt.up.fe.dceg.neptus.gui.ImageScaleAndLocationPanel;
import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.ImageElement;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
	private NumberFormat dfI = GuiUtils.getNeptusIntegerFormat();
	
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
			jPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Image", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12), new java.awt.Color(51,51,51)));
			
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
			selectImage.setText("Select image...");
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
			jLabel1.setText("Center:");
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
			changeCenter.setText("Change...");
			changeCenter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LocationType tmp = LocationPanel.showLocationDialog("Set the center location", getCenter(), null);
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
			jLabel3.setText("Scale (m/pixel):");
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
			jButton.setText("2 locations...");
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
						
						/*
						ImageLocatorPanel imgLocator = new ImageLocatorPanel(GuiUtils.getImage(getImageFileName()));
						if (imgLocator.showDialog()) {
							getScale().setText(String.valueOf(imgLocator.getScale()));
							center = imgLocator.getCenter();
						}
						*/
					}
					catch (Exception exception) {
						GuiUtils.errorMessage(SwingUtilities.getRoot((Component)e.getSource()), "Select an image first", "You have to select an image fisrt.");
					}
					
					
					/*
					int imgWidth = 0;
					int imgHeight = 0;
					
					try {
						ImageIcon img = (ImageIcon) getImgLabel().getIcon();
						imgWidth = img.getImage().getWidth(null);
						imgHeight = img.getImage().getHeight(null);
						if ((imgWidth*imgHeight) == 0) 
							throw new Exception("Invalid image size");
					}
					catch (Exception exception) {
						
						return;
					}
					
					LocationType bottomLeft = LocationPanel.showLocationDialog("Location of the bottom-left corner of the image", getCenter(), null);
					if (bottomLeft == null)
						return;
					
					LocationType topRight = LocationPanel.showLocationDialog("Location of the top-right corner of the image", getCenter(), null);
					if (topRight == null)
						return;
					
					LocationType centerPos = new LocationType();
					centerPos.setLocation(bottomLeft);
					double[] offsets = topRight.getOffsetFrom(bottomLeft);
					if (offsets[0]==0 || offsets[1] ==0)
						return;
					centerPos.translatePosition(offsets[0]/2, offsets[1]/2, 0);					
					getCenter().setLocation(centerPos);
					double calcScale = offsets[1] / (double)imgWidth;
					
					getScale().setText(String.valueOf(calcScale));*/
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
            maxHeightLabel.setText("Max height");
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
            batCheckBox.setText("Is bathymetric");
            batCheckBox.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    System.out.println("itemStateChanged()"
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
            maxDepthLabel.setText("Max depth");
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
            resolutionLabel.setText("Resolution");
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
            resolutionFormattedTextField = new JFormattedTextField(dfI);
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

	public int getResolution() {
		return Integer.parseInt(getResolutionFormattedTextField().getText());
	}
	
	public void setResolution(int val) {
		getResolutionFormattedTextField().setText(Integer.toString(val));
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
			bathImageSelect.setText("Bathymetric Image...");
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


	/**
	 * @param args
	 */
	public static void main(String args[]) {
		GuiUtils.testFrame(new ImageObjectParameters(), "test");
	}


	public File getSelectedBathymetricImage() {
		return selectedBathymetricImage;
	}


	public void setSelectedBathymetricImage(File selectedBathymetricImage) {
		this.selectedBathymetricImage = selectedBathymetricImage;
	}

}
