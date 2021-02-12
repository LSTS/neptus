/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 * 
 */
public class LatLonConv extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final short DECIMAL_DEGREES = 0;
	private static final short DM = 1;
	private static final short DMS = 2;
	private LatLongSelector latLongSelector = null;
	private JButton btnCopy = null;
	private JButton btnPaste = null;
	private LocationType locationType = new LocationType(); 
	private JPanel copyPastePanel = null;
	private JButton okButton = null;
	private JFrame jFrame = null; 
	private JPanel frameContentPane = null;
	private JDialog jDialog = null; 
	private JPanel dialogContentPane = null;
	private JPanel cardsPanel = null;
	private JPanel radsPanel = null;
	private JLabel jLabel = null;
	private JLabel jLabel1 = null;
	private JFormattedTextField latRad = null;
	private NumberFormat df = null;
	private JLabel jLabel7 = null;
	private JLabel jLabel71 = null;
	private JFormattedTextField lonRad = null;
	private JPanel selectorDegRadPanel = null;
	private JRadioButton cardDegRadioButton = null;
	private JRadioButton cardRadRadioButton = null;

	/**
	 * This is the default constructor
	 */
	public LatLonConv() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(419, 190);
		this.setLayout(null);
		this.add(getCopyPastePanel(), null);
		this.add(getOkButton(), null);
		this.add(getCardsPanel(), null);
		this.add(getSelectorDegRadPanel(), null);
	}

	/**
	 * @return the locationType
	 */
	public LocationType getLocationType() {
		if (getLatLongSelector().getErrors() != null)
			return null;

		locationType.setLatitudeStr(getLatLongSelector().getLatitude());
		locationType.setLongitudeStr(getLatLongSelector().getLongitude());

		return locationType;
	}

	/**
	 * @param locationType
	 *            the locationType to set
	 */
	public void setLocationType(LocationType locationType) {
		this.locationType = locationType;
		double[] lld = locationType.getAbsoluteLatLonDepth();
		
		getLatLongSelector().setLatitude(
		        CoordinateUtil.decimalDegreesToDMS(lld[0]));
		getLatLongSelector().setLongitude(
				CoordinateUtil.decimalDegreesToDMS(lld[1]));
	}

	/**
	 * This method initializes latLongSelector
	 * 
	 * @return pt.lsts.neptus.gui.LatLongSelector
	 */
	private LatLongSelector getLatLongSelector() {
		if (latLongSelector == null) {
			latLongSelector = new LatLongSelector();
			latLongSelector.setName("latLongSelector");
		}
		return latLongSelector;
	}

	/**
	 * This method initializes btnCopy
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnCopy() {
		if (btnCopy == null) {
			btnCopy = new JButton();
			btnCopy.setIcon(new ImageIcon(ImageUtils
					.getImage("images/menus/editcopy.png")));
			btnCopy.setMargin(new Insets(0, 0, 0, 0));
			btnCopy.setToolTipText(I18n.text("Copy this location to the clipboard"));
			btnCopy.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					btnPaste.setEnabled(true);
					ClipboardOwner owner = new ClipboardOwner() {
						public void lostOwnership(
								java.awt.datatransfer.Clipboard clipboard,
								java.awt.datatransfer.Transferable contents) {
						}
					};
					Toolkit.getDefaultToolkit().getSystemClipboard()
							.setContents(
									new StringSelection(getLocationType()
											.getClipboardText()), owner);
				}
			});
		}
		return btnCopy;
	}

	/**
	 * This method initializes btnPaste
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBtnPaste() {
		if (btnPaste == null) {
			btnPaste = new JButton();
			btnPaste.setPreferredSize(new Dimension(20, 20));
			btnPaste.setToolTipText(I18n.text("Paste from clipboard"));
			btnPaste.setIcon(new ImageIcon(ImageUtils
					.getImage("images/menus/editpaste.png")));
			btnPaste.setMargin(new Insets(0, 0, 0, 0));
			btnPaste.setEnabled(true);
			btnPaste.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					btnPaste.setEnabled(true);

					Transferable contents = Toolkit.getDefaultToolkit()
							.getSystemClipboard().getContents(null);

					boolean hasTransferableText = (contents != null)
							&& contents
									.isDataFlavorSupported(DataFlavor.stringFlavor);

					if (hasTransferableText) {
						try {
							String text = (String) contents
									.getTransferData(DataFlavor.stringFlavor);
							LocationType lt = new LocationType();
							lt.fromClipboardText(text);
							setLocationType((LocationType) lt.getNewAbsoluteLatLonDepth());
						} catch (Exception e) {
							NeptusLog.pub().error(e);
						}
					}
				}
			});
		}
		return btnPaste;
	}

	/**
	 * This method initializes copyPastePanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getCopyPastePanel() {
		if (copyPastePanel == null) {
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.insets = new Insets(3, 3, 2, 5);
			gridBagConstraints4.gridy = 0;
			gridBagConstraints4.gridx = 1;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.insets = new Insets(2, 5, 1, 2);
			gridBagConstraints3.gridy = 0;
			gridBagConstraints3.gridx = 0;
			copyPastePanel = new JPanel();
			copyPastePanel.setLayout(new GridBagLayout());
			copyPastePanel.setBounds(new Rectangle(12, 147, 135, 35));
			copyPastePanel.setBorder(BorderFactory.createTitledBorder(null, "",
					TitledBorder.DEFAULT_JUSTIFICATION,
					TitledBorder.DEFAULT_POSITION, new Font("Dialog",
							Font.BOLD, 12), new Color(51, 51, 51)));
			copyPastePanel.add(getBtnCopy(), gridBagConstraints3);
			copyPastePanel.add(getBtnPaste(), gridBagConstraints4);
		}
		return copyPastePanel;
	}

	/**
	 * This method initializes okButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setBounds(new Rectangle(317+35, 156, 87, 20));
			okButton.setText(I18n.text("OK"));
			okButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					// NeptusLog.pub().info("<###>actionPerformed()");
					if (jFrame != null) {
						jFrame.setVisible(false);
						jFrame.dispose();
					}
					if (jDialog != null) {
						jDialog.setVisible(false);
						jDialog.dispose();
					}
				}
			});
		}
		return okButton;
	}

	/**
	 * This method initializes jFrame
	 * 
	 * @return javax.swing.JFrame
	 */
	public JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(
					getClass().getResource("/images/neptus-icon.png")));
			jFrame.setSize(new Dimension(419+120, 224));
			jFrame.setTitle(I18n.text("Lat/Lon"));
			jFrame.setContentPane(getFrameContentPane());
			jFrame.add(this);
		}
		return jFrame;
	}

	/**
	 * This method initializes frameContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getFrameContentPane() {
		if (frameContentPane == null) {
			frameContentPane = new JPanel();
			frameContentPane.setLayout(new BorderLayout());
		}
		return frameContentPane;
	}

	/**
	 * This method initializes jDialog
	 * 
	 * @return javax.swing.JDialog
	 */
	public JDialog getJDialog() {
		if (jDialog == null) {
			jDialog = new JDialog(getJFrame());
			jDialog.setSize(new Dimension(419+120, 224));
			jDialog.setContentPane(getDialogContentPane());
			jDialog.add(this);
		}
		return jDialog;
	}

	/**
	 * This method initializes dialogContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getDialogContentPane() {
		if (dialogContentPane == null) {
			dialogContentPane = new JPanel();
			dialogContentPane.setLayout(new BorderLayout());
		}
		return dialogContentPane;
	}

	/**
	 * @param type
	 */
	protected boolean convertLatLonTo(short type) {
		LocationType loc = getLocationType();
		switch (type) {
		case DECIMAL_DEGREES:
			latLongSelector.setLatitude(new double[] {
					MathMiscUtils.round(loc.getLatitudeDegs(), CoordinateUtil.LAT_LON_DDEGREES_DECIMAL_PLACES), 0,
					0 });
			latLongSelector.setLongitude(new double[] {
					MathMiscUtils.round(loc.getLongitudeDegs(), CoordinateUtil.LAT_LON_DDEGREES_DECIMAL_PLACES), 0,
					0 });
			break;

		case DM:
			double[] dmLat = CoordinateUtil.decimalDegreesToDM(loc
					.getLatitudeDegs());
			double[] dmLon = CoordinateUtil.decimalDegreesToDM(loc
					.getLongitudeDegs());
			latLongSelector.setLatitude(new double[] { dmLat[0],
					MathMiscUtils.round(dmLat[1], CoordinateUtil.LAT_LON_DM_DECIMAL_PLACES), 0 });
			latLongSelector.setLongitude(new double[] { dmLon[0],
					MathMiscUtils.round(dmLon[1], CoordinateUtil.LAT_LON_DM_DECIMAL_PLACES), 0 });
			break;

		case DMS:
			double[] dmsLat = CoordinateUtil.decimalDegreesToDMS(loc
					.getLatitudeDegs());
			double[] dmsLon = CoordinateUtil.decimalDegreesToDMS(loc
					.getLongitudeDegs());
			latLongSelector.setLatitude(new double[] { dmsLat[0], dmsLat[1],
					MathMiscUtils.round(dmsLat[2], CoordinateUtil.LAT_LON_DMS_DECIMAL_PLACES) });
			latLongSelector.setLongitude(new double[] { dmsLon[0], dmsLon[1],
					MathMiscUtils.round(dmsLon[2], CoordinateUtil.LAT_LON_DMS_DECIMAL_PLACES) });
			break;

		default:
			break;
		}
		return true;
	}

	/**
	 * This method initializes cardsPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getCardsPanel() {
		if (cardsPanel == null) {
			cardsPanel = new JPanel();
			cardsPanel.setLayout(new CardLayout());
			cardsPanel.setSize(new Dimension(401+33, 134));
			cardsPanel.setLocation(new Point(10, 9));
			/// Angles Degrees
			cardsPanel.add(getLatLongSelector(), I18n.text("degs"));
			/// Angles Radians
			cardsPanel.add(getRadsPanel(), I18n.text("rad"));
		}
		return cardsPanel;
	}

	/**
	 * This method initializes radsPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getRadsPanel() {
		if (radsPanel == null) {
			jLabel71 = new JLabel();
			jLabel71.setHorizontalTextPosition(SwingConstants.CENTER);
            /// Angles Radians
			jLabel71.setText(I18n.text("rad"));
			jLabel71.setLocation(new Point(180, 109));
			jLabel71.setSize(new Dimension(31, 15));
			jLabel71.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel7 = new JLabel();
			jLabel7.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel7.setText(I18n.text("rad"));
			jLabel7.setSize(new Dimension(31, 15));
			jLabel7.setLocation(new Point(180, 49));
			jLabel7.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel1 = new JLabel();
			jLabel1.setText(I18n.text("Longitude:"));
			jLabel1.setLocation(new Point(10, 84));
			jLabel1.setSize(new Dimension(66, 20));
			jLabel = new JLabel();
			jLabel.setText(I18n.text("Latitude:"));
			jLabel.setLocation(new Point(10, 24));
			jLabel.setSize(new Dimension(104, 20));
			radsPanel = new JPanel();
			radsPanel.setLayout(null);
			radsPanel.setSize(new Dimension(401, 134));
			radsPanel.setBorder(BorderFactory.createTitledBorder(null,
					I18n.text("Radians"), TitledBorder.CENTER,
					TitledBorder.DEFAULT_POSITION, new Font("Dialog",
							Font.BOLD, 12), new Color(51, 51, 51)));
			radsPanel.add(jLabel, null);
			radsPanel.add(jLabel1, null);
			radsPanel.add(getLatRad(), null);
			radsPanel.add(jLabel7, null);
			radsPanel.add(jLabel71, null);
			radsPanel.add(getLonRad(), null);
		}
		return radsPanel;
	}

	/**
	 * This method initializes df
	 * 
	 * @return java.text.DecimalFormat
	 */
	private NumberFormat getDf() {
		if (df == null) {
			df = GuiUtils.getNeptusDecimalFormat();
		}
		return df;
	}

	/**
	 * This method initializes latRad
	 * 
	 * @return javax.swing.JFormattedTextField
	 */
	private JFormattedTextField getLatRad() {
		if (latRad == null) {
			latRad = new JFormattedTextField(getDf());
			latRad.setText("0");
			latRad.setLocation(new Point(10, 49));
			latRad.setSize(new Dimension(167, 20));
		}
		return latRad;
	}

	/**
	 * This method initializes df1
	 * 
	 * /** This method initializes lonRad
	 * 
	 * @return javax.swing.JFormattedTextField
	 */
	private JFormattedTextField getLonRad() {
		if (lonRad == null) {
			lonRad = new JFormattedTextField(getDf());
			lonRad.setText("0");
			lonRad.setLocation(new Point(10, 109));
			lonRad.setSize(new Dimension(167, 20));
		}
		return lonRad;
	}

	/**
	 * This method initializes selectorDegRadPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getSelectorDegRadPanel() {
		if (selectorDegRadPanel == null) {
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.insets = new Insets(5, 3, 0, 17);
			gridBagConstraints1.gridy = 0;
			gridBagConstraints1.gridx = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(5, 16, 0, 2);
			gridBagConstraints.gridy = 0;
			gridBagConstraints.gridx = 0;
			selectorDegRadPanel = new JPanel();
			selectorDegRadPanel.setLayout(new GridBagLayout());
            selectorDegRadPanel.setBorder(BorderFactory.createTitledBorder(null, "",
                    TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                    new Font("Dialog", Font.BOLD, 8), new Color(51, 51, 51)));
			selectorDegRadPanel.setLocation(new Point(154, 147));
			selectorDegRadPanel.setSize(new Dimension(151, 35));
			selectorDegRadPanel.setEnabled(true);
			selectorDegRadPanel.add(getCardDegRadioButton(), gridBagConstraints);
			selectorDegRadPanel.add(getCardRadRadioButton(), gridBagConstraints1);
		}
		return selectorDegRadPanel;
	}

	/**
	 * This method initializes cardDegRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getCardDegRadioButton() {
		if (cardDegRadioButton == null) {
			cardDegRadioButton = new JRadioButton();
			cardDegRadioButton.setText(I18n.text("Degrees"));
			cardDegRadioButton.setEnabled(false);
			cardDegRadioButton.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						cardRadRadioButton.setSelected(false);
						CardLayout cl = (CardLayout)(getCardsPanel().getLayout());
		    		    cl.show(getCardsPanel(), "degs");
					}
				}
			});
		}
		return cardDegRadioButton;
	}

	/**
	 * This method initializes cardRadRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getCardRadRadioButton() {
		if (cardRadRadioButton == null) {
			cardRadRadioButton = new JRadioButton();
            /// Angles Radians
			cardRadRadioButton.setText(I18n.text("Rads"));
			cardRadRadioButton.setEnabled(false);
			cardRadRadioButton.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						cardDegRadioButton.setSelected(false);
						CardLayout cl = (CardLayout)(getCardsPanel().getLayout());
		    		    cl.show(getCardsPanel(), "rads");
					}
				}
			});
		}
		return cardRadRadioButton;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LatLonConv latLonConv = new LatLonConv();
		JFrame jf = latLonConv.getJFrame();
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.setVisible(true);

		// latLonConv = new LatLonConv();
		// latLonConv.getJDialog().setVisible(true);
	}

} // @jve:decl-index=0:visual-constraint="10,10"
