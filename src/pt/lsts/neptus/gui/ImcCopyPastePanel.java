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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: keila
 * Feb 4, 2020
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

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.InvalidMessageException;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @JPanel with copy and paste buttons for @IMCMessage
 * from @LocationCopyPastePanel
 *
 */
public abstract class ImcCopyPastePanel extends JPanel{

    private static final long serialVersionUID = 789133685584823372L;
    private IMCMessage msg;
    private JButton btnPaste,btnCopy;
    
    public ImcCopyPastePanel() {
        GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        gridBagConstraints4.insets = new Insets(3, 3, 2, 5);
        gridBagConstraints4.gridy = 0;
        gridBagConstraints4.gridx = 1;
        GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
        gridBagConstraints3.insets = new Insets(2, 5, 1, 2);
        gridBagConstraints3.gridy = 0;
        gridBagConstraints3.gridx = 0;
        this.setLayout(new GridBagLayout());
        this.setBounds(new Rectangle(12, 147, 95, 30));//FIXME
        this.setBorder(BorderFactory.createTitledBorder(null, "",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, new Font("Dialog",
                        Font.BOLD, 12), new Color(51, 51, 51)));
        this.add(getBtnCopy(), gridBagConstraints3);
        this.add(getBtnPaste(), gridBagConstraints4);
    }
    
    public IMCMessage pasteImcMessage() {
        return this.msg;

    }
    
    private JButton getBtnCopy() {
        if (btnCopy == null) {
            btnCopy = new JButton();
            btnCopy.setIcon(new ImageIcon(ImageUtils
                    .getImage("images/menus/editcopy.png")));
            btnCopy.setMargin(new Insets(0, 0, 0, 0));
            btnCopy.setToolTipText(I18n.text("Copy this IMC message to the clipboard"));
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
                                    new StringSelection(getMsg().asJSON(true)), owner);
                }
            });
        }
        return btnCopy;
    }
    
    private JButton getBtnPaste() {
        if (btnPaste == null) {
            btnPaste = new JButton();
            btnPaste.setPreferredSize(new Dimension(20, 20));
            btnPaste.setToolTipText(I18n.text("Paste IMC message from clipboard"));
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
                            IMCMessage m = IMCMessage.parseJson(text);
                            try {
                                m.validate();
                                setMsg(m);
                            }
                            catch (InvalidMessageException i) {
                                return;
                            }
                        } catch (Exception e) {
                            NeptusLog.pub().error(e);
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        return btnPaste;
    }

    /**
     * @return the msg
     */
    public IMCMessage getMsg() {
        return this.msg;
    }

    /**
     * @param msg the msg to set
     */
    public void setMsg(IMCMessage msg) {
        this.msg = msg;
    }

}
