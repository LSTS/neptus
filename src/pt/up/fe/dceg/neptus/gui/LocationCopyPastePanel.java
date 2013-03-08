/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2008/10/12
 * $Id:: LocationCopyPastePanel.java 9736 2013-01-18 15:29:09Z pdias      $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author pdias
 *
 */
public class LocationCopyPastePanel extends JPanel {

	private static final long serialVersionUID = 6057200519930411065L;

	private LocationType locationType = new LocationType();

	private JButton btnCopy = null;
	private JButton btnPaste = null;

	public LocationCopyPastePanel() {
		initialize();
	}

	private void initialize() {
		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.insets = new Insets(3, 3, 2, 5);
		gridBagConstraints4.gridy = 0;
		gridBagConstraints4.gridx = 1;
		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.insets = new Insets(2, 5, 1, 2);
		gridBagConstraints3.gridy = 0;
		gridBagConstraints3.gridx = 0;
		this.setLayout(new GridBagLayout());
		this.setBounds(new Rectangle(12, 147, 135, 35));
		this.setBorder(BorderFactory.createTitledBorder(null, "",
				TitledBorder.DEFAULT_JUSTIFICATION,
				TitledBorder.DEFAULT_POSITION, new Font("Dialog",
						Font.BOLD, 12), new Color(51, 51, 51)));
		this.add(getBtnCopy(), gridBagConstraints3);
		this.add(getBtnPaste(), gridBagConstraints4);
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
					@SuppressWarnings("unused")
					ClipboardOwner owner = new ClipboardOwner() {
						public void lostOwnership(
								java.awt.datatransfer.Clipboard clipboard,
								java.awt.datatransfer.Transferable contents) {
						}
					};

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
							setLocationType(lt);
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
	 * @return the locationType
	 */
	public LocationType getLocationType() {
		return locationType;
	}

	/**
	 * @param locationType
	 *            the locationType to set
	 */
	public void setLocationType(LocationType locationType) {
		this.locationType = locationType;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LocationCopyPastePanel locPanel = new LocationCopyPastePanel();
		GuiUtils.testFrame(locPanel, "", 100, 100);
	}
}
