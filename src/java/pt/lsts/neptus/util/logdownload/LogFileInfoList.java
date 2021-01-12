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
 * 2009/09/13
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicHTML;

import org.jdesktop.swingx.JXList;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class LogFileInfoList extends JXList {

	public static final Icon ICON_NEW = ImageUtils.getScaledIcon("images/downloader/file_new.png", 20, 20);
	public static final Icon ICON_UNKNOWN = ImageUtils.getScaledIcon("images/downloader/file_question.png", 20, 20);
	public static final Icon ICON_DOWN = ImageUtils.getScaledIcon("images/downloader/file_down.png", 20, 20);
	public static final Icon ICON_ERROR = ImageUtils.getScaledIcon("images/downloader/file_error.png", 20, 20);
	public static final Icon ICON_INCOMP = ImageUtils.getScaledIcon("images/downloader/file_warn.png", 20, 20);
	public static final Icon ICON_SYNC = ImageUtils.getScaledIcon("images/downloader/file_sync.png", 20, 20);
	public static final Icon ICON_LOCAL = ImageUtils.getScaledIcon("images/downloader/file_local.png", 20, 20);
	
	DefaultListModel<LogFileInfo> myModel = new DefaultListModel<LogFileInfo>();	
	
	/**
	 * Creates a new List with no channels
	 *
	 */
	public LogFileInfoList() {
		initialize();
	}
	
	public void initialize() {
		setModel(myModel);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		setCellRenderer(new ListCellRenderer<LogFileInfo>() {
			public Component getListCellRendererComponent(JList<? extends LogFileInfo> list, LogFileInfo value, int index, boolean isSelected, boolean cellHasFocus) {
				try {
					LogFileInfo folder = value;
					
					String infoSize = "";
					if (folder.getSize() >= 0) {
                        infoSize = " ("
                                + MathMiscUtils.parseToEngineeringRadix2Notation(folder.getSize(), 1)
                                + "B"
                                + (!folder.isDirectory() ? "" : (folder.getDirectoryContents() == null
                                        || folder.getDirectoryContents().isEmpty() ? "" : " | "
                                        + I18n.textf("%files files", folder.getDirectoryContents().size()))) + ")";
					}
					
					JLabel lbl = new JLabel(folder.getName() + infoSize, ICON_NEW, JLabel.LEFT) {
                        @Override
                        public void validate() {}
                        @Override
                        public void invalidate() {}
                        @Override
                        public void repaint() {}
                        @Override
                        public void revalidate() {}
                        @Override
                        public void repaint(long tm, int x, int y, int width, int height) {}
                        @Override
                        public void repaint(Rectangle r) {}
                        @Override
                        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
                            // Strings get interned...
                            if (propertyName.equals("text")
                                    || ((propertyName.equals("font") || propertyName.equals("foreground"))
                                            && oldValue != newValue
                                            && getClientProperty(BasicHTML.propertyKey) != null)) {

                                super.firePropertyChange(propertyName, oldValue, newValue);
                            }
                        }
                        @Override
                        public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
                        @Override
                        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
                    };
					
					if (folder.getState() == LogFolderInfo.State.NEW)
						lbl.setIcon(ICON_NEW);
					else if (folder.getState() == LogFolderInfo.State.DOWNLOADING)
						lbl.setIcon(ICON_DOWN);
					else if (folder.getState() == LogFolderInfo.State.ERROR)
						lbl.setIcon(ICON_ERROR);
					else if (folder.getState() == LogFolderInfo.State.INCOMPLETE)
						lbl.setIcon(ICON_INCOMP);
					else if (folder.getState() == LogFolderInfo.State.SYNC)
						lbl.setIcon(ICON_SYNC);
					else if (folder.getState() == LogFolderInfo.State.UNKNOWN)
						lbl.setIcon(ICON_UNKNOWN);
					else if (folder.getState() == LogFolderInfo.State.LOCAL)
						lbl.setIcon(ICON_LOCAL);

					lbl.setBackground(Color.white);
					if (isSelected /*|| cellHasFocus*/) {
						lbl.setForeground(getBackground());
						lbl.setBackground(getForeground());						
					}
					lbl.setOpaque(true);
					lbl.setHorizontalAlignment(JLabel.LEFT);
					return lbl;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return new JLabel("?");		
			}
		});
	}
	
	/**
	 * @param file
	 */
	public void addFile(LogFileInfo file) {
		if (!myModel.contains(file))
			myModel.addElement(file);
	}
	
	/**
	 * @param file
	 */
	public void removeFile(LogFileInfo file) {
		myModel.removeElement(file);
	}
	
	/**
	 * @param file
	 * @return
	 */
	public boolean containsFile (LogFileInfo file) {
		return myModel.contains(file);
	}
	
	/**
	 * @param name
	 * @return
	 */
	public LogFileInfo getFile(String name) {
		if (!containsFile(new LogFileInfo(name)))
			return null;
		
		Enumeration<?> iter = this.myModel.elements();
		LogFileInfo lfx;
		while (iter.hasMoreElements()) {
			try {
				lfx = (LogFileInfo)iter.nextElement();
				if (lfx.getName().equals(name))
					return lfx;
			}
			catch (Exception e) {
			    e.printStackTrace();
			}
		}
		return null;
	}

	public static void main(String[] args) {
		final LogFileInfoList list = new LogFileInfoList();
		list.addFile(new LogFileInfo("22/A"));
		list.addFile(new LogFileInfo("A12/A"));
		list.addFile(new LogFileInfo("12/A"));
		list.addFile(new LogFileInfo("a12/A"));
		list.addFile(new LogFileInfo("22/a"));
		
		list.setSortable(true); 
        list.setAutoCreateRowSorter(true);
        list.setSortOrder(SortOrder.ASCENDING);
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                for (Object sel : list.getSelectedValues()) { // getSelectedValuesList does not give the corrected values
                    NeptusLog.pub().info("<###> "+((LogFileInfo)sel));
                }
            }});
		GuiUtils.testFrame(list);
	}
}
