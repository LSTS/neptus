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
 */
package pt.lsts.neptus.console;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.AlarmListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * 
 * @author RJPG A Panel to include subpanels and change them
 **/

public class MainPanel extends JPanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 7355410533809066603L;

    protected AlarmListener alarmlistener = null;

    protected String adding = "";
    protected ConsolePanel hitPanel;
    private int deltaX, deltaY, oldX, oldY;
    private final int TOL = 3; // tolerance
    protected Class<?> subadd = null;
    private boolean editFlag = false;

    public boolean relayoutOnResize = false;
    private final ConsoleLayout console;

    public MainPanel(ConsoleLayout console) {
        this.console = console;
        setLayout(new MigLayout("ins 0"));
        setName("MainPanel");
    }

    /**
     * Add ContainerSubPanel to the console main panel This will add a panel maximized
     * 
     * @param panel
     */
    public void addSubPanel(ConsolePanel panel) {
        add(panel, "width 100%!, height 100%!");
        console.getSubPanels().add(panel);
        console.informSubPanelListener(panel, SubPanelChangeAction.ADDED);
    }

    public void addSubPanel(ConsolePanel panel, int x, int y) {
        add(panel);
        panel.setLocation(x, y);
        this.revalidate();
        this.repaint();
        console.getSubPanels().add(panel);
        console.informSubPanelListener(panel, SubPanelChangeAction.ADDED);
    }

    public void removeSubPanel(ConsolePanel panel) {
        console.getSubPanels().remove(panel);
        console.informSubPanelListener(panel, SubPanelChangeAction.REMOVED);
        panel.clean();
        remove(panel);
        console.repaint();
    }
    
//    private static void cleanPanels(Collection<ConsolePanel> panels) {
//        
//        Vector<Thread> launched = new Vector<>();
//        for (final ConsolePanel panel : panels) {
//            Thread t = new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    panel.clean();
//                }
//            }, panel.getName());
//            t.setDaemon(true);
//            t.start();
//            launched.add(t);
//        }
//
//        while (!launched.isEmpty()) {
//            try {
//                Thread t = launched.firstElement();
//                if (t.isAlive())
//                    NeptusLog.pub().info("Waiting for " + t.getName() + " to cleanup...");
//                launched.firstElement().join();
//                NeptusLog.pub().info("Cleaned " + t.getName());
//                launched.remove(0);
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        NeptusLog.pub().info("All panels have been cleaned up.");
//    }

    public void clean() {
        removeAll();
        for (ConsolePanel panel : console.getSubPanels()) {
            try {
                panel.clean();
                NeptusLog.pub().info("Cleaned " + panel.getName() + " in " + MainPanel.this.getName());
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error cleaning " + panel.getName() + " in " + MainPanel.this.getName() + " :: " + e.getMessage(), e);
            }
        }
        console.getSubPanels().clear();
    }

    public void resetPanelLocations() {

        if (!relayoutOnResize || editFlag || getConsole().getMaximizedPanel() != null)
            return;

        for (ConsolePanel p : getConsole().getSubPanels()) {
            if (p.getParent().equals(this)) {
                if (!p.isFixedSize() || !p.isFixedPosition()) {
                    p.recalculateRelativePosAndSize();
                }
            }
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        Component outerMostComponent = getComponentAt(e.getPoint());
        ConsolePanel innerMostSubPanel = null;
        ContainerSubPanel innerMostContainer = null;

        Component comp = SwingUtilities.getDeepestComponentAt(this, e.getX(), e.getY());

        while (comp != null && !(comp instanceof ConsolePanel))
            comp = comp.getParent();

        innerMostSubPanel = (ConsolePanel) comp;

        while (comp != null && !(comp instanceof ContainerSubPanel))
            comp = comp.getParent();
        innerMostContainer = (ContainerSubPanel) comp;
        if (!adding.equals("")) {
            setEditOff();
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

            ConsolePanel sub = new ConsolePanel(this.getConsole()) {
                private static final long serialVersionUID = 8543725153078587308L;
                @Override
                public void cleanSubPanel() {}                
                @Override
                public void initSubPanel() {}
            };
            sub.setSize(sub.getPreferredSize());
            sub.setLocation(e.getX(), e.getY());
            sub.setName(adding);

            if (innerMostContainer != null)
                innerMostContainer.addSubPanel(sub);
            else
                add(sub);
            revalidate();
            sub.init();

            console.getSubPanels().add(sub);
            console.informSubPanelListener(sub, SubPanelChangeAction.ADDED);
            adding = "";

            setEditOn();
            return;
        }
        if (subadd != null) {
            try {
                ConsolePanel sp = PluginsRepository.getPanelPlugin(PluginUtils.getPluginName(subadd), console);
                if (innerMostContainer != null) { // we found a containerSubPanel
                    // NeptusLog.pub().info("<###>found container " + innerMostContainer.getName());
                    innerMostContainer.addSubPanel(sp);
                    NeptusLog.pub().warn("Added new plugin: " + sp.getName());
                }
                else {// we didn't so will add to the main panel
                      // NeptusLog.pub().info("<###>found only the main panel");
                    this.addSubPanel(sp, e.getX(), e.getY());
                }
                sp.init();
                sp.deactivateComponents();
                sp.setEditMode(true);
//                sp.setBorder(new TitledBorder(new LineBorder(Color.YELLOW), sp.getName()));
                console.informSubPanelListener(sp, SubPanelChangeAction.ADDED);
            }
            catch (Exception ex) {
                GuiUtils.errorMessage(this, ex);
            }
            subadd = null;
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            Component c = getComponentAt(e.getPoint());
            if (c instanceof ConsolePanel) {
                hitPanel = (ConsolePanel) c;
                oldX = hitPanel.getX();
                oldY = hitPanel.getY();
                deltaX = e.getX() - oldX;
                deltaY = e.getY() - oldY;
                if (oldX < e.getX() - TOL)
                    oldX += hitPanel.getWidth();
                if (oldY < e.getY() - TOL)
                    oldY += hitPanel.getHeight();
            }
        }
        else {
            final Component c = outerMostComponent;
            Component t = c;

            if (e.isShiftDown() && innerMostContainer != null)
                t = innerMostContainer;
            else if (innerMostSubPanel != null)
                t = innerMostSubPanel;

            final Component son = t;

            if (c instanceof ConsolePanel) {
                final ConsolePanel panel = (ConsolePanel) c;
                JPopupMenu popup = new JPopupMenu();
                popup.add(new JLabel(PluginUtils.i18nTranslate(PluginUtils.getPluginName(c.getClass())), ImageUtils
                        .getScaledIcon(PluginUtils.getPluginIcon(c.getClass()), 16, 16), JLabel.CENTER));
                popup.addSeparator();

                if (innerMostContainer == null) {
                    // popup.set
                    JMenuItem itemRemove;
                    itemRemove = new JMenuItem(I18n.text("Remove"));
                    itemRemove.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeSubPanel(panel);
                        }
                    });

                    JMenuItem itemProperties;
                    itemProperties = new JMenuItem(I18n.textf("%panel properties",
                            PluginUtils.i18nTranslate(son.getName())));
                    itemProperties.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties((ConsolePanel) son, getConsole(), true);
                        }
                    });
                    if (((ConsolePanel) son).getProperties().length > 0)
                        popup.add(itemProperties);
                    popup.addSeparator();
                    // item.addActionListener(this);
                    popup.add(itemRemove);
                }
                else {
                    JMenuItem itemRemove, itemProps;

                    JMenu remove = new JMenu(I18n.text("Remove"));
                    JMenu properties = new JMenu(I18n.text("Properties"));
                    final ContainerSubPanel container = innerMostContainer;
                    itemRemove = new JMenuItem(PluginUtils.i18nTranslate(PluginUtils.getPluginName(c.getClass())),
                            ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(c.getClass()), 16, 16));
                    itemRemove.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            container.removeSubPanel(panel);
                            console.informSubPanelListener(panel, SubPanelChangeAction.REMOVED);
                        }
                    });
                    remove.add(itemRemove);

                    itemProps = new JMenuItem(PluginUtils.i18nTranslate(PluginUtils.getPluginName(c.getClass())),
                            ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(c.getClass()), 16, 16));
                    itemProps.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            PropertiesEditor.editProperties((ConsolePanel) c, true);
                        }
                    });
                    properties.add(itemProps);

                    boolean add = false;

                    String[] ip = innerMostContainer.subPanelList();
                    Arrays.sort(ip, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            Collator collator = Collator.getInstance(Locale.US);
                            return collator.compare(PluginUtils.i18nTranslate(o1), PluginUtils.i18nTranslate(o2));
                        }
                    });

                    for (String name : ip) {
                        if (!add) {
                            add = true;
                            // remove.addSeparator();
                        }
                        final String spname = name;
                        final ContainerSubPanel cont = innerMostContainer;
                        ConsolePanel sp = cont.getSubPanelByName(spname);
                        itemRemove = new JMenuItem(PluginUtils.i18nTranslate(spname), ImageUtils.getScaledIcon(
                                PluginUtils.getPluginIcon(sp.getClass()), 16, 16));
                        itemRemove.addActionListener(new java.awt.event.ActionListener() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                ConsolePanel sp = cont.getSubPanelByName(spname);
                                if (sp != null)
                                    getConsole().getSubPanels().remove(sp);
                                getConsole().informSubPanelListener(sp, SubPanelChangeAction.REMOVED);

                                sp.clean();
                                cont.removeSubPanel(sp);
                                repaint();
                            }
                        });
                        remove.add(itemRemove);

                        itemProps = new JMenuItem(PluginUtils.i18nTranslate(spname), ImageUtils.getScaledIcon(
                                PluginUtils.getPluginIcon(sp.getClass()), 16, 16));
                        itemProps.addActionListener(new java.awt.event.ActionListener() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                ConsolePanel sp = cont.getSubPanelByName(spname);
                                if (getConsole() != null) {
                                    PropertiesEditor.editProperties(sp, getConsole(), true);
                                }
                            }
                        });
                        properties.add(itemProps);
                        // item.addActionListener(this);
                    }

                    // Rectangle screenB = GuiUtils.getScreenBounds(this.getX(), this.getY());
                    // int s = (int) (screenB.getHeight() / 22) - 1;
                    MenuScroller.setScrollerFor(properties, this, 150, 1, 0);
                    MenuScroller.setScrollerFor(remove, this, 150, 1, 0);

                    popup.add(properties);
                    popup.addSeparator();
                    popup.add(remove);
                }

                popup.show(this, e.getX(), e.getY());
            }
            else { // outros butoes do rato
                   // setEditOff();
            }
        }
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        if (!this.editFlag)
            return;
        if (hitPanel != null) {
            hitPanel.revalidate();
            int x = e.getX();
            int y = e.getY();
            int xDiff = x - oldX;
            int yDiff = y - oldY;
            int xH = hitPanel.getX();
            int yH = hitPanel.getY();
            int w = hitPanel.getWidth();
            int h = hitPanel.getHeight();
            Dimension min = hitPanel.getMinimumSize();
            Dimension max = hitPanel.getMaximumSize();
            int wMin = (int) min.getWidth();
            int wMax = (int) max.getWidth();
            int hMin = (int) min.getHeight();
            int hMax = (int) max.getHeight();
            int cursorType = hitPanel.getCursor().getType();
            if (cursorType == Cursor.W_RESIZE_CURSOR) { // West resizing
                if (!((w <= wMin && xDiff > 0) || (w >= wMax && xDiff < 0)))
                    hitPanel.setBounds(x, yH, w - xDiff, h);
            }
            else if (cursorType == Cursor.N_RESIZE_CURSOR) { // North resizing
                if (!((h <= hMin && yDiff > 0) || (h >= hMax && yDiff < 0)))
                    hitPanel.setBounds(xH, y, w, h - yDiff);
            }
            else if (cursorType == Cursor.S_RESIZE_CURSOR) { // South resizing
                if (!((h <= hMin && yDiff < 0) || (h >= hMax && yDiff > 0)))
                    hitPanel.setSize(w, h + yDiff);
            }
            else if (cursorType == Cursor.E_RESIZE_CURSOR) { // East resizing
                if (!((w <= wMin && xDiff < 0) || (w >= wMax && xDiff > 0)))
                    hitPanel.setSize(w + xDiff, h);
            }
            else if (cursorType == Cursor.NW_RESIZE_CURSOR) { // NorthWest resizing
                if (!((h <= hMin && yDiff > 0) || (h >= hMax && yDiff < 0)))
                    if (!((w <= wMin && xDiff > 0) || (w >= wMax && xDiff < 0)))
                        hitPanel.setBounds(x, y, w - xDiff, h - yDiff);
            }
            else if (cursorType == Cursor.NE_RESIZE_CURSOR) { // NorthEast resizing
                if (!((h <= hMin && yDiff > 0) || (h >= hMax && yDiff < 0)))
                    if (!((w <= wMin && xDiff < 0) || (w >= wMax && xDiff > 0)))
                        hitPanel.setBounds(xH, y, w + xDiff, h - yDiff);
            }
            else if (cursorType == Cursor.SW_RESIZE_CURSOR) { // SouthWest resizing
                if (!((h <= hMin && yDiff < 0) || (h >= hMax && yDiff > 0)))
                    if (!((w <= wMin && xDiff > 0) || (w >= wMax && xDiff < 0)))
                        hitPanel.setBounds(x, yH, w - xDiff, h + yDiff);
            }
            else if (cursorType == Cursor.SE_RESIZE_CURSOR) { // SouthEast resizing
                if (!((h <= hMin && yDiff < 0) || (h >= hMax && yDiff > 0)))
                    if (!((w <= wMin && xDiff < 0) || (w >= wMax && xDiff > 0)))
                        hitPanel.setSize(w + xDiff, h + yDiff);
            }
            else { // moving subpanel
                hitPanel.setLocation(x - deltaX, y - deltaY);
            }
            oldX = e.getX();
            oldY = e.getY();
            hitPanel.doLayout();
            invalidate();
            // revalidate();
            hitPanel.repaint();

            repaint();

        }
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        if (!this.editFlag)
            return;
        Component c = getComponentAt(e.getPoint());
        if (!(c instanceof ConsolePanel)) {
            c.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        if (c instanceof ConsolePanel) {

            int x = e.getX();
            int y = e.getY();
            int xC = c.getX();
            int yC = c.getY();
            int w = c.getWidth();
            int h = c.getHeight();

            if (y >= yC - TOL && y <= yC + TOL && x >= xC - TOL && x <= xC + TOL) {
                c.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
            }
            else if (y >= yC - TOL && y <= yC + TOL && x >= xC - TOL + w && x <= xC + TOL + w) {
                c.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
            }
            else if (y >= yC - TOL + h && y <= yC + TOL + h && x >= xC - TOL && x <= xC + TOL) {
                c.setCursor(new Cursor(Cursor.SW_RESIZE_CURSOR));
            }
            else if (y >= yC - TOL + h && y <= yC + TOL + h && x >= xC - TOL + w && x <= xC + TOL + w) {
                c.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
            }
            else if (x >= xC - TOL && x <= xC + TOL) {
                c.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
            }
            else if (y >= yC - TOL && y <= yC + TOL) {
                c.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
            }
            else if (x >= xC - TOL + w && x <= xC + TOL + w) {
                c.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
            }
            else if (y >= yC - TOL + h && y <= yC + TOL + h) {
                c.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
            }
            else {
                c.setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }
            if (!((ConsolePanel) c).isResizable()) {
                c.setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }
        }

    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        hitPanel = null;
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
    }

    @Override
    public void mouseExited(final MouseEvent e) {
    }

    public void setAdding(final String string) {
        NeptusLog.pub().info("<###>adding");
        adding = string;
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public void setAdding(Class<?> sub) {
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        subadd = sub;
    }

    public void setEditOn() {
        this.editFlag = true;
        addMouseListener(this);
        addMouseMotionListener(this);

        Component[] a = this.getComponents();
        for (Component c : a) {
            if (c instanceof ConsolePanel) {
                ConsolePanel panel = (ConsolePanel) c;
                panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                panel.deactivateComponents();
                panel.setEditMode(true);
//                panel.setBorder(new TitledBorder(new LineBorder(GeneralPreferences.consoleEditBorderColor), panel.getName()));
            }
        }
    }

    public void setEditOff() {
        this.editFlag = false;
        removeMouseListener(this);
        removeMouseMotionListener(this);

        Component[] a = this.getComponents();
        for (Component c : a) {
            if (c instanceof ConsolePanel) {
                ConsolePanel panel = (ConsolePanel) c;
                panel.setEditMode(false);
                panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                panel.activateComponents();
//                panel.setBorder(null);
            }
        }
    }

    public ConsoleLayout getConsole() {
        return console;
    }

    public AlarmListener getAlarmlistener() {

        if (alarmlistener != null)
            return alarmlistener;
        else
            return null;

    }

    /**
     * @return the editFlag
     */
    public boolean isEditFlag() {
        return editFlag;
    }

    public void setAlarmlistener(AlarmListener alarmlistener) {
        this.alarmlistener = alarmlistener;
    }

    public boolean isRelayoutOnResize() {
        return relayoutOnResize;
    }

    public void setRelayoutOnResize(boolean relayoutOnResize) {
        this.relayoutOnResize = relayoutOnResize;
    }

}