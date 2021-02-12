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
 * 2008/10/12
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

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
					.getImage("images/menus/copyLocation.png")));
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
			btnPaste.setToolTipText(I18n.text("Paste location from clipboard"));
			btnPaste.setIcon(new ImageIcon(ImageUtils
					.getImage("images/menus/pasteLocation.png")));
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
