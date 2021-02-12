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
 * Author: José Pinto
 * Jan 2, 2013
 */
package pt.lsts.neptus.plugins.sunfish.iridium;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class IridiumHtmlVisualization implements MRAVisualization {

    protected IridiumMessage msg;
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
    {
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    protected JScrollPane scroll;
    
    public IridiumHtmlVisualization(final IridiumMessage message) {
        this.msg = message;
        
        JEditorPane editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/plain");
        editor.setBackground(Color.white);
        editor.setOpaque(true);
        try {
            editor.setText(msg.toString()+"DATA:\n"+new String(Hex.encodeHex(msg.serialize())));
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        editor.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(I18n.text("Copy to clipboard")).addActionListener(
                            new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    StringSelection selection = new StringSelection(msg.toString());
                                    Toolkit.getDefaultToolkit().getSystemClipboard()
                                            .setContents(selection, null);
                                }
                            });

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
        return msg.getClass().getSimpleName()+ " ["+new Date(msg.timestampMillis)+"]";
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
    
    public Type getType() {
        return Type.TABLE;
    }
    
    public void onCleanup() {
        
    }
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

}
