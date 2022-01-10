/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 2, 2013
 */
package pt.lsts.neptus.util.llf;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class MessageHtmlVisualization implements MRAVisualization {

    protected IMCMessage message;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    {
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected JScrollPane scroll;
    protected JEditorPane editor = new JEditorPane();

    public MessageHtmlVisualization(final IMCMessage message) {
        this.message = message;

        editor.setContentType("text/html");
        editor.setEditable(false);
        editor.setText(IMCUtil.getAsHtml(message));
        editor.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(I18n.text("Copy as HTML")).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            StringSelection selection = new StringSelection(IMCUtil.getAsHtml(message));
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        }
                    });
                    
                    popup.add(I18n.text("Copy as JSON")).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            StringSelection selection = new StringSelection(message.asJSON(true));
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        }
                    });
                    
                    popup.add(I18n.text("Copy as XML")).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            StringSelection selection = new StringSelection(message.asXml(false));
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        }
                    });

                    
                    if (editor.getSelectionStart() < editor.getSelectionEnd()) {
                        popup.add(I18n.text("Copy selection as text")).addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    StringSelection selection = new StringSelection(editor.getSelectedText());
                                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                                }
                                catch (Exception ex) {
                                    NeptusLog.pub().error(ex);
                                }
                            }
                        });
                    }                        
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            };
        });

        scroll = new JScrollPane(editor);
    }

    public void onHide() {

    };

    public void onShow() {
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public String getName() {
        return String.format("%s [%s, %02x]", message.getAbbrev(), fmt.format(message.getDate()), editor.getText().hashCode());
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/view.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        return scroll;
    }
    
    public Component getComponent() {
        return this.scroll;
    }

    public Type getType() {
        return Type.TABLE;
    }

    public void onCleanup() {

    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public int hashCode() {
        return new String(message.getSrc() + "." + message.getSrcEnt() + "." + message.getTimestampMillis()).hashCode();
    }

}
