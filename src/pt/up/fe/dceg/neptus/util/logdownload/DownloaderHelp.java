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
 * 2009/09/19
 * $Id:: DownloaderHelp.java 9778 2013-01-28 14:44:18Z pdias              $:
 */
package pt.up.fe.dceg.neptus.util.logdownload;

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

import pt.up.fe.dceg.neptus.i18n.I18n;

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
