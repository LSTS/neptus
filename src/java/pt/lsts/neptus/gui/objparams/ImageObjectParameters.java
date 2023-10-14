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
 * Author: José Pinto
 * 2005/06/16
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
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

	private JPanel imageSelPanel = null;
	private JLabel imgLabel = null;
	private JButton selectImageButton = null;
	private JPanel locAndScalePanel = null;
	private JLabel centerLocLabel = null;
	private JButton changeCenter = null;
	private JPanel imgScalePanel = null;
	private JLabel imgScaleLabel = null;
	private JCheckBox vScaleCheckBox = null;
	private JFormattedTextField scale = null;
    private JFormattedTextField scaleV = null;
	private JButton twoLocationsButton = null;
	private JPanel bathymetryPanel = null;
	private JPanel maxHeightPanel = null;
	private JCheckBox bathCheckBox = null;
	private JLabel maxHeightLabel = null;
	private JFormattedTextField maxHeightFormattedTextField = null;
	private JPanel maxDepthPanel = null;
	private JPanel resolutionPanel = null;
	private JLabel maxDepthLabel = null;
	private JLabel resolutionLabel = null;
	private JFormattedTextField maxDepthFormattedTextField = null;
	private JFormattedTextField resolutionFormattedTextField = null;
	private JButton bathImageSelect = null;
	private JPanel transparencyRotationPanel = null;
	private JSlider transparencySlider = null;
	private JSlider rotationSlider = null;
	private JFormattedTextField rotationFormattedTextField = null;
	
    private NumberFormat df = GuiUtils.getNeptusDecimalFormat();
	
	private String imageFileName = null;
	private LocationType center = new LocationType();
	
	private File selectedBathymetricImage = null;

	/**
	 * This method initializes 
	 * 
	 */
	public ImageObjectParameters() {
		super();
		initialize();
		setPreferredSize(new Dimension(525, 350));
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new MigLayout());
        this.setSize(505, 181);
        this.add(getImageSelPanel(), "wmin pref");
        this.add(getLocAndScalePanel(), "gapleft 0, wmin pref");
        this.add(getBathymetryPanel(), "gapleft 0, wrap, wmin pref");
        this.add(getTransparencyRotationPanel(), "span 3, alignx center");
	}

	public String getErrors() {
		if (getImageFileName() == null) {
			return I18n.text("The selected image is invalid");
		}
		
		if (bathCheckBox.isSelected() && selectedBathymetricImage == null) {
			return I18n.text("You must select a bathymetric image");
		}
		
		if (bathCheckBox.isSelected() && !selectedBathymetricImage.canRead()) {
			return I18n.text("Unable to read the bathymetric image file");
		}
		
		return null;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getImageSelPanel() {
		if (imageSelPanel == null) {
			imageSelPanel = new JPanel();
			imageSelPanel.setLayout(new BorderLayout());
			imageSelPanel.setPreferredSize(new Dimension(150,170));
            imageSelPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Image"),
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION,
                    new java.awt.Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			
			imageSelPanel.add(getImgLabel(), BorderLayout.NORTH);
			imageSelPanel.add(getSelectImageButton(), BorderLayout.SOUTH);
		}
		return imageSelPanel;
	}
	
	/**
	 * This method initializes selectImage	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getSelectImageButton() {
		if (selectImageButton == null) {
			selectImageButton = new JButton();
			selectImageButton.setText(I18n.text("Select image..."));
			selectImageButton.setPreferredSize(new Dimension(90,25));
			selectImageButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File f = ImageFileChooser.showOpenImageDialog();
					if (f != null) {
						setImageFileName(f.getAbsolutePath());
					}
				}
			});
		}
		return selectImageButton;
	}
	
	/**
	 * This method initializes locAndScalePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getLocAndScalePanel() {
		if (locAndScalePanel == null) {
			MigLayout layout = new MigLayout();
			centerLocLabel = new JLabel();
			locAndScalePanel = new JPanel();
			locAndScalePanel.setLayout(layout);
			centerLocLabel.setText(I18n.text("Center"));
			locAndScalePanel.add(centerLocLabel, "push, alignx right");
			locAndScalePanel.add(getChangeCenter(), "gap 2, alignx left, push, wrap 15");
			locAndScalePanel.add(getImgScalePanel(), "span, wrap 15");
			locAndScalePanel.add(getTwoLocationsButton(), "span, alignx center");
		}
		return locAndScalePanel;
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
	 * This method initializes imgScalePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getImgScalePanel() {
		if (imgScalePanel == null) {
		    imgScalePanel = new JPanel(new MigLayout());
			imgScaleLabel = new JLabel();
			imgScaleLabel.setText(I18n.text("Scale (m/pixel)"));
			imgScalePanel.add(imgScaleLabel, "");
			imgScalePanel.add(getScale(), "wmin pref, wrap");
			vScaleCheckBox = new JCheckBox(I18n.text("Vertical scale"));
			vScaleCheckBox.addItemListener(new ItemListener() {
			    public void itemStateChanged(java.awt.event.ItemEvent e) {
			        if (e.getStateChange() == ItemEvent.SELECTED)
			            getScaleV().setEnabled(true);
			        else 
			            getScaleV().setEnabled(false);
			    }
			});
			imgScalePanel.add(vScaleCheckBox);
			imgScalePanel.add(getScaleV(), "wmin pref, wrap");
		}
		return imgScalePanel;
	}
	
	/**
	 * This method initializes scale	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getScale() {
		if (scale == null) {
			scale = new JFormattedTextField(df);
			scale.setPreferredSize(new Dimension(60,20));
			scale.setText("1.0");
		}
		return scale;
	}

	private JFormattedTextField getScaleV() {
	    if (scaleV == null) {
	        scaleV = new JFormattedTextField(df);
	        scaleV.setPreferredSize(new Dimension(60,20));
	        scaleV.setText("1.0");
	    }
	    return scaleV;
	}

	/**
     * @return the transparencyRotationPanel
     */
    public JPanel getTransparencyRotationPanel() {
        if (transparencyRotationPanel == null) {
            JTextField transparencyTextField = new JTextField("0");
            transparencyTextField.setPreferredSize(new Dimension(40,20));
            transparencyTextField.setEnabled(false);

            transparencySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
            transparencySlider.setMajorTickSpacing(20);
            transparencySlider.setMinorTickSpacing(10);
            transparencySlider.setPaintTicks(true);
            transparencySlider.setPaintLabels(true);
            transparencySlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider) e.getSource();
                    int val = slider.getValue();
                    transparencyTextField.setText(String.valueOf(val));
                }
            });

            rotationSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);
            rotationSlider.setMajorTickSpacing(90);
            rotationSlider.setMinorTickSpacing(10);
            rotationSlider.setPaintTicks(true);
            rotationSlider.setPaintLabels(true);
            rotationSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider) e.getSource();
                    int rotI = slider.getValue();
                    double rotDeg = rotI;
                    try {
                        double txtVal = Double.parseDouble(rotationFormattedTextField.getText());
                        if ((int) txtVal != (int) rotDeg) {
                            rotationFormattedTextField.setText(String.valueOf(rotDeg));
                            rotationFormattedTextField.setCaretPosition(0);
                        }
                    }
                    catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    }
                }
            });

            rotationFormattedTextField = new JFormattedTextField(df);
            rotationFormattedTextField.setPreferredSize(new Dimension(40,20));
            rotationFormattedTextField.setText("0.0");
            rotationFormattedTextField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    JFormattedTextField txtF = (JFormattedTextField) e.getSource();
                    double rotDeg = Double.parseDouble(txtF.getText());
                    rotationSlider.setValueIsAdjusting(true);
                    rotationSlider.setValue((int) rotDeg);
                    rotationSlider.setValueIsAdjusting(false);
                }
            });

            transparencyRotationPanel = new JPanel();
            transparencyRotationPanel.setLayout(new MigLayout("center"));

            transparencyRotationPanel.add(new JLabel(I18n.text("Transparency")), "");
            transparencyRotationPanel.add(transparencyTextField, "wmin pref");
            transparencyRotationPanel.add(transparencySlider, "");
            transparencyRotationPanel.add(new JLabel(I18n.text("Rotation")), "");
            transparencyRotationPanel.add(rotationFormattedTextField, "wmin pref");
            transparencyRotationPanel.add(rotationSlider, "");
        }
        return transparencyRotationPanel;
    }
	
    public int getTransparency() {
        return transparencySlider.getValue();
    }

    public void setTransparency(int transparency) {
        transparencySlider.setValue(transparency);
    }
    
    public double getRotationDegs() {
        double rot = Double.parseDouble(rotationFormattedTextField.getText());
        return rot;
    }
    
    public void setRotationDegs(double rotationDegs) {
        rotationSlider.setValue((int) rotationDegs);
        rotationFormattedTextField.setText(String.valueOf(rotationDegs));
        rotationFormattedTextField.setCaretPosition(0);
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
		getScale().setCaretPosition(0);
	}

	public double getImageScaleV() {
	    if (vScaleCheckBox.isSelected())
	        return Double.parseDouble(getScaleV().getText());
	    else
	        return Double.NaN;
	}

	public void setImageScaleV(double scaleV) {
	    if (Double.isNaN(scaleV)) {
	        vScaleCheckBox.setSelected(!false);
	        vScaleCheckBox.doClick();
            getScaleV().setText("1.0");
	    }
	    else {
            vScaleCheckBox.setSelected(!true);
            vScaleCheckBox.doClick();
	        getScaleV().setText(String.valueOf(scaleV));
	        getScaleV().setCaretPosition(0);
	    }
	}

	/**
	 * This method initializes twoLocationsButton	
	 * @return javax.swing.JButton	
	 */    
	private JButton getTwoLocationsButton() {
		if (twoLocationsButton == null) {
			twoLocationsButton = new JButton();
			twoLocationsButton.setPreferredSize(new Dimension(120,26));
			twoLocationsButton.setText(I18n.text("2 locations..."));
			twoLocationsButton.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {  
					try {
						ImageElement tmp = new ImageElement();
						tmp.setImage(ImageUtils.getImage(getImageFileName()));						
                        if (ImageScaleAndLocationPanel.showDialog(tmp,
                                SwingUtilities.getWindowAncestor(ImageObjectParameters.this))) {
							getScale().setText(String.valueOf(tmp.getImageScale()));
							getScale().setCaretPosition(0);
                            getScaleV().setText(String.valueOf(
                                    Double.isNaN(tmp.getImageScaleV()) ? tmp.getImageScale() : tmp.getImageScaleV()));
                            getScaleV().setCaretPosition(0);
                            vScaleCheckBox.setSelected(!Double.isNaN(tmp.getImageScaleV()));
							center = tmp.getCenterLocation();
						}
					}
					catch (Exception exception) {
					    NeptusLog.pub().warn(exception.getMessage());
                        GuiUtils.errorMessage(SwingUtilities.getRoot((Component) e.getSource()),
                                I18n.text("Select an image first"), I18n.text("You have to select an image fisrt."));
					}
				}
			});
		}
		return twoLocationsButton;
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
    private JPanel getBathymetryPanel() {
        if (bathymetryPanel == null) {
            MigLayout layout = new MigLayout();
            bathymetryPanel = new JPanel();
            bathymetryPanel.setLayout(layout);
            bathymetryPanel.add(getBathCheckBox(), "wrap");
            bathymetryPanel.add(getMaxHeightPanel(), "wrap 0");
            bathymetryPanel.add(getMaxDepthPanel(), "wrap 0");
            bathymetryPanel.add(getResolutionPanel(), "wrap");
            bathymetryPanel.add(getBathImageSelect(), null);
        }
        return bathymetryPanel;
    }

    /**
     * This method initializes maxHeightPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getMaxHeightPanel() {
        if (maxHeightPanel == null) {
            maxHeightLabel = new JLabel();
            maxHeightLabel.setText(I18n.text("Max height"));
            maxHeightPanel = new JPanel();
            maxHeightPanel.add(maxHeightLabel, null);
            maxHeightPanel.add(getMaxHeightFormattedTextField(), null);
        }
        return maxHeightPanel;
    }

    /**
     * This method initializes bathCheckBox
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getBathCheckBox() {
        if (bathCheckBox == null) {
            bathCheckBox = new JCheckBox();
            bathCheckBox.setText(I18n.text("Is bathymetric"));
            bathCheckBox.addItemListener(new java.awt.event.ItemListener() {
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
        return bathCheckBox;
    }

    /**
     * This method initializes MaxHeightFormattedTextField
     * 
     * @return javax.swing.JFormattedTextField
     */
    private JFormattedTextField getMaxHeightFormattedTextField() {
        if (maxHeightFormattedTextField == null) {
            maxHeightFormattedTextField = new JFormattedTextField(df);
            maxHeightFormattedTextField.setPreferredSize(new Dimension(50, 20));
            maxHeightFormattedTextField.setText("1.0");
            maxHeightFormattedTextField.setEnabled(false);
        }
        return maxHeightFormattedTextField;
    }

    /**
     * This method initializes maxDepthPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getMaxDepthPanel() {
        if (maxDepthPanel == null) {
            maxDepthLabel = new JLabel();
            maxDepthLabel.setText(I18n.text("Max depth"));
            maxDepthPanel = new JPanel();
            maxDepthPanel.add(maxDepthLabel, null);
            maxDepthPanel.add(getMaxDepthFormattedTextField(), null);
        }
        return maxDepthPanel;
    }

    /**
     * This method initializes resolutionPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getResolutionPanel() {
        if (resolutionPanel == null) {
            resolutionLabel = new JLabel();
            resolutionLabel.setText(I18n.text("Resolution"));
            resolutionPanel = new JPanel();
            resolutionPanel.add(resolutionLabel, null);
            resolutionPanel.add(getResolutionFormattedTextField(), null);
        }
        return resolutionPanel;
    }

    /**
     * This method initializes maxDepthFormattedTextField
     * 
     * @return javax.swing.JFormattedTextField
     */
    private JFormattedTextField getMaxDepthFormattedTextField() {
        if (maxDepthFormattedTextField == null) {
            maxDepthFormattedTextField = new JFormattedTextField(df);
            maxDepthFormattedTextField.setPreferredSize(new Dimension(50, 20));
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
            resolutionFormattedTextField.setPreferredSize(new java.awt.Dimension(50, 20));
            resolutionFormattedTextField.setText("" + ImageElement.DEFAULT_RESOLUTION);
            resolutionFormattedTextField.setEnabled(false);
        }
        return resolutionFormattedTextField;
    }
	
	public boolean getIsBathymetric() {
		return getBathCheckBox().isSelected();
	}
	
	public void setIsBathymetric(boolean val) {
		getBathCheckBox().setSelected(val);
	}

	public double getMaxHeight() {
		return Double.parseDouble(getMaxHeightFormattedTextField().getText());
	}
	
	public void setMaxHeight(double val) {
		getMaxHeightFormattedTextField().setText(Double.toString(val));
		getMaxHeightFormattedTextField().setCaretPosition(0);
	}

	public double getMaxDepth() {
		return Double.parseDouble(getMaxDepthFormattedTextField().getText());
	}
	
	public void setMaxDepth(double val) {
		getMaxDepthFormattedTextField().setText(Double.toString(val));
		getMaxDepthFormattedTextField().setCaretPosition(0);
	}

	public double getResolution() {
		return Double.parseDouble(getResolutionFormattedTextField().getText());
	}
	
	public void setResolution(double val) {
		getResolutionFormattedTextField().setText(Double.toString(val));
		getResolutionFormattedTextField().setCaretPosition(0);
	}
	
	public File getBathimFile() {
		return selectedBathymetricImage;
	}
	
	public void setBathimFile(String filename) {
		if (filename != null)
			this.selectedBathymetricImage = new File(filename);
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
			bathImageSelect.setText(I18n.text("Bathymetric image"));
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
        GuiUtils.setLookAndFeel();
        GuiUtils.testFrame(new ImageObjectParameters(), "test", 505,300);
    }
}
