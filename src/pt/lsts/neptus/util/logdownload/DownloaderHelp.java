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
 * 2009/09/19
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.JXDialog;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.lsts.neptus.i18n.I18n;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class DownloaderHelp extends JXPanel {

	//UI
	private JXDialog dialog = null;
	private JXPanel content = null;
	
	private Window parent = null;
	
	/**
	 * 
	 */
	public DownloaderHelp(Window parent) {
		this.parent = parent;
		initialize();
	}
	
	/**
	 * 
	 */
	private void initialize() {
		try {
			dialog = new JXDialog((Frame)parent, this);
		} catch (Exception e) {
			try {
				dialog = new JXDialog((Dialog)parent, this);
			} catch (Exception e1) {
				dialog = new JXDialog(this);
			}
		}
		dialog.setTitle(I18n.text("Download Helper"));
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
		//dialog.setVisible(true);
		
        RectanglePainter rect = new RectanglePainter(5,5,5,5, 10,10);
        Color colorBl = new Color(0xf6, 0xf0, 0xcf); //172, 156, 65);
        rect.setFillPaint(colorBl);
        rect.setBorderPaint(colorBl);
        rect.setStyle(RectanglePainter.Style.BOTH);
        rect.setBorderWidth(5);
        rect.setAntialiasing(true);//RectanglePainter.Antialiasing.On);

		CompoundPainter<JXPanel> cp = new CompoundPainter<JXPanel>(
				//new MattePainter(Color.BLACK), rect,
				new MattePainter(colorBl),
				new GlossPainter());
		
		content = new JXPanel(true);
		BoxLayout boxL = new BoxLayout(content, BoxLayout.Y_AXIS);
		content.setLayout(boxL);
		//content.setBorder(new EmptyBorder(5,5,5,5));
		content.setBackgroundPainter(cp);
		

        JXLabel labelTitle = new JXLabel(I18n.text("Icons Description Help"));
        labelTitle.setFont(labelTitle.getFont().deriveFont(labelTitle.getFont().getSize2D()+5));
        labelTitle.setBackground(Color.WHITE);
//        content.add(labelTitle);

        JXLabel labelFd = new JXLabel(I18n.text("For Log Folders"));
        labelFd.setFont(labelFd.getFont().deriveFont(labelFd.getFont().getSize2D()+1));
        content.add(labelFd);

//        JXHeader header = new JXHeader("Icon Description Help", "", LogFolderInfoList.ICON_DOWN);
//		header.setFont(labelFd.getFont());
		
		content.add(new JLabel(I18n.text("New log folder in the server."), LogFolderInfoList.ICON_NEW, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log folder being downloaded from the server."), LogFolderInfoList.ICON_DOWN, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log folder in sync. with the server."), LogFolderInfoList.ICON_SYNC, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log folder with errors."), LogFolderInfoList.ICON_ERROR, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log folder incomplete."), LogFolderInfoList.ICON_INCOMP, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log folder only local."), LogFolderInfoList.ICON_LOCAL, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log folder on local disk but state unknown."), LogFolderInfoList.ICON_UNKNOWN, JLabel.LEFT));

		JXLabel labelFx = new JXLabel(I18n.text("For Log Files"));
        labelFx.setFont(labelFx.getFont().deriveFont(labelFx.getFont().getSize2D()+1));
        content.add(labelFx);

		content.add(new JLabel(I18n.text("New log file in the server."), LogFileInfoList.ICON_NEW, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log file being downloaded from the server."), LogFileInfoList.ICON_DOWN, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log file in sync. with the server."), LogFileInfoList.ICON_SYNC, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log file with errors."), LogFileInfoList.ICON_ERROR, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log file incomplete."), LogFileInfoList.ICON_INCOMP, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log file only local."), LogFileInfoList.ICON_LOCAL, JLabel.LEFT));
		content.add(new JLabel(I18n.text("Log file on local disk but state unknown."), LogFileInfoList.ICON_UNKNOWN, JLabel.LEFT));

		JXPanel cont = new JXPanel(new BorderLayout());
//		cont.add(header, BorderLayout.NORTH);
		cont.add(labelTitle, BorderLayout.NORTH);
		cont.add(content, BorderLayout.CENTER);
		cont.setBorder(new EmptyBorder(5,5,5,5));
		CompoundPainter<JXPanel> cp1 = new CompoundPainter<JXPanel>(
				new MattePainter(Color.BLACK), 
				rect,new GlossPainter());
		cont.setBackgroundPainter(cp1);

		dialog.add(cont);
		dialog.setSize(320, 410);
	}
	
	/**
	 * @return the dialog
	 */
	public JXDialog getDialog() {
		return dialog;
	}

    public void dispose() {
        if (dialog != null)
            SwingUtilities.invokeLater(new Runnable() {                
                @Override
                public void run() {
                    if (dialog != null)
                        dialog.dispose();
                    dialog = null;
                }
            });            
        if (parent != null)
            parent = null;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DownloaderHelp(new JFrame()).dialog.setVisible(true);
	}
}
