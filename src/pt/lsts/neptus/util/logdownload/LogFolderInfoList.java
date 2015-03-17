/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;

import org.jdesktop.swingx.JXList;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class LogFolderInfoList extends JXList {

	/*
VER
Sorting and Filtering
JXList supports sorting and filtering. Changed to use core support. Usage is very similar to J/X/Table. It provides api to apply a specific sort order, to toggle the sort order and to reset a sort. Sort sequence can be configured by setting a custom comparator.


 list.setAutoCreateRowSorter(true);
 list.setComparator(myComparator);
 list.setSortOrder(SortOrder.DESCENDING);
 list.toggleSortOder();
 list.resetSortOrder();
 

JXList provides api to access items of the underlying model in view coordinates and to convert from/to model coordinates. Note: JXList needs a specific ui-delegate - BasicXListUI and subclasses - which is aware of model vs. view coordiate systems and which controls the synchronization of selection/dataModel and sorter state. SwingX comes with a subclass for Synth. 	 
*/
	
	public static final Icon ICON_NEW = ImageUtils.getScaledIcon("images/downloader/folder_grey3.png", 20, 20);
	public static final Icon ICON_UNKNOWN = ImageUtils.getScaledIcon("images/downloader/folder_grey2.png", 20, 20);
	public static final Icon ICON_DOWN = ImageUtils.getScaledIcon("images/downloader/folder_download.png", 20, 20);
	public static final Icon ICON_ERROR = ImageUtils.getScaledIcon("images/downloader/folder_red1.png", 20, 20);
	public static final Icon ICON_INCOMP = ImageUtils.getScaledIcon("images/downloader/folder_yellow1.png", 20, 20);
	public static final Icon ICON_SYNC = ImageUtils.getScaledIcon("images/downloader/folder_green1.png", 20, 20);
	public static final Icon ICON_LOCAL = ImageUtils.getScaledIcon("images/downloader/folder_green2.png", 20, 20);
	
	
	DefaultListModel<LogFolderInfo> myModel = new DefaultListModel<LogFolderInfo>();	
	
	/**
	 * Creates a new List with no channels
	 *
	 */
	public LogFolderInfoList() {
		initialize();
	}
	
	/**
	 * 
	 */
	public void initialize() {
		setModel(myModel);
		//setVisibleRowCount(25);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//setDragEnabled(true);
		setCellRenderer(new ListCellRenderer<LogFolderInfo>() {
			
			public Component getListCellRendererComponent(JList<? extends LogFolderInfo> list, 
			        LogFolderInfo value, int index, boolean isSelected, 
					boolean cellHasFocus) {
				try {
					LogFolderInfo folder = value;
					
					JLabel lbl = new JLabel(folder.getName(), ICON_NEW, JLabel.LEFT) {
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
					                    && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {

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
					// TODO: handle exception
				}
				return new JLabel("?");		
			}
		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
//				NeptusLog.pub().info("<###>"+((LogFolder)getSelectedValue()).logFiles.size());
				if (e.getButton() == MouseEvent.BUTTON1
						&& e.getModifiers() == MouseEvent.CTRL_DOWN_MASK
						&& e.getClickCount() == 1) {
				    
				}
				
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
//					if (getNeptusIM() != null)
//						getNeptusIM().setSelectedChannel(((IMChannel)getSelectedValue()).getRemoteID());
				}
				if(e.getButton() == MouseEvent.BUTTON3)
				{
				}
				
//				final IMChannel channel = (IMChannel)getSelectedValue();
//				setToolTipText("<html>Channel name: <b>"+channel.getRemoteUsername()+"</b><br>"+
//						"Channel ID: <b>"+channel.getRemoteID()+"</b><br></html>");
//				
//				if (e.getButton() == MouseEvent.BUTTON3 && !channel.getRemoteID().equals("ALL")) {
//					JPopupMenu popup = new JPopupMenu();
//					
//					popup.add(new AbstractAction("Nudge "+channel.getRemoteUsername()) {
//						public void actionPerformed(java.awt.event.ActionEvent e) {
//							neptusIM.nudge(channel.getRemoteID());
//						};
//					});
//					
//					popup.show(ChannelsList.this, e.getX(), e.getY());
//				}
						
			}
		});
	}
	
	/**
	 * @param folder
	 */
	public void addFolder(LogFolderInfo folder) {
		if (!myModel.contains(folder))
			myModel.addElement(folder);
	}
	
	/**
	 * @param folder
	 */
	public void removeFolder(LogFolderInfo folder) {
		myModel.removeElement(folder);
	}
	
	/**
	 * @param folder
	 * @return
	 */
	public boolean containsFolder (LogFolderInfo folder) {
		return myModel.contains(folder);
	}

	/**
	 * @param name
	 * @return
	 */
	public LogFolderInfo getFolder(String name) {
		if (!containsFolder(new LogFolderInfo(name)))
			return null;
		
		Enumeration<?> iter = this.myModel.elements();
		LogFolderInfo lfd;
		while (iter.hasMoreElements()) {
			try {
				lfd = (LogFolderInfo)iter.nextElement();
				if (lfd.getName().equals(name))
					return lfd;
			} catch (Exception e) {
			}
		}
		return null;
	}

	
	public static void main(String[] args) {
		LogFolderInfoList list = new LogFolderInfoList();
		list.addFolder(new LogFolderInfo("22/A"));
		list.addFolder(new LogFolderInfo("12/A"));
		list.addFolder(new LogFolderInfo("22/a"));
		
		list.setSortable(true); //list.setFilterEnabled(true); // Changed from swingx 1.6.+ !!!!
		//list.setComparator((Comparator<?>) new LogFolder("<><>"));
		list.setAutoCreateRowSorter(true);
        list.setSortOrder(SortOrder.ASCENDING);
		
		GuiUtils.testFrame(list);
	}
}
