/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Jan 2, 2013
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class MessageHtmlVisualization implements MRAVisualization {

    protected IMCMessage message;
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    protected JScrollPane scroll;
    
    public MessageHtmlVisualization(final IMCMessage message) {
        this.message = message;
        
        JLabel lbl = new JLabel(IMCUtil.getAsHtml(message));
        lbl.setBackground(Color.white);
        lbl.setOpaque(true);
        lbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(I18n.text("Copy HTML to clipboard")).addActionListener(
                            new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    StringSelection selection = new StringSelection(IMCUtil
                                            .getAsHtml(message));
                                    Toolkit.getDefaultToolkit().getSystemClipboard()
                                            .setContents(selection, null);
                                }
                            });

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            };
        });

        scroll = new JScrollPane(lbl);
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
        return message.getAbbrev() + "[" + fmt.format(message.getDate()) + "]";
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
    public JComponent getComponent(IMraLogGroup source, double timestep) {
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
