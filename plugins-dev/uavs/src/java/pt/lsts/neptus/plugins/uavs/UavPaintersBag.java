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
 * Author: canasta
 * 27 de Fev de 2012
 */
package pt.lsts.neptus.plugins.uavs;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author zp
 *
 */
public class UavPaintersBag {

    protected LinkedHashMap<String, IUavPainter> paintersByName = new LinkedHashMap<String, IUavPainter>();    
    protected LinkedHashMap<String, Boolean> activePainters = new LinkedHashMap<String, Boolean>();
    protected LinkedHashMap<String, BufferedImage> painterCaches = new LinkedHashMap<String, BufferedImage>();
    protected LinkedHashMap<String, Integer> painterPriorities = new LinkedHashMap<String, Integer>();
    protected LinkedHashMap<String, Integer> cacheMillis = new LinkedHashMap<String, Integer>();
    protected Vector<String> sortByPriority = new Vector<String>();
    protected Vector<String> sortByName = new Vector<String>();

    /**
     * Add a new painter
     * @param name The name of the painter to be added
     * @param painter The painter to be added
     * @param priority The priority of the new painter - negative values mean pre render painters
     * @param cacheMillis The minimum time between painting this layer
     */
    public void addPainter(String name, IUavPainter painter, int priority, int cacheMillis) {
        
        for (IUavPainter p : paintersByName.values())
            if (p == painter)
                return;
        
        paintersByName.put(name, painter);
        activePainters.put(name, true);
        painterPriorities.put(name, priority);
        this.cacheMillis.put(name, cacheMillis);

        sortThem();
    }

    /**
     * Remove a painter from this list of painters
     * @param name The name of the painter to be removed
     */
    public void removePainter(String name) {
        if (paintersByName.containsKey(name)) {
            paintersByName.remove(name);
            activePainters.remove(name);
            painterCaches.remove(name);
            painterPriorities.remove(name);
            cacheMillis.remove(name);
            sortThem();
        }
    }

    /**
     * Sort all the painters (according to priority and name)
     */
    protected void sortThem() {
        sortByPriority.clear();
        sortByName.clear();

        sortByName.addAll(paintersByName.keySet());
        Collections.sort(sortByName);

        Vector<String> painterNames = new Vector<String>();
        Vector<Integer> priorities = new Vector<Integer>();
        
        painterNames.addAll(sortByName);        
        priorities.addAll(painterPriorities.values());
        Collections.sort(priorities);
        
        while (!priorities.isEmpty()) {
            int prio = priorities.get(0);
            priorities.remove(0);
            
            for (int i = 0; i < painterNames.size(); i++) {
                if (painterPriorities.get(painterNames.get(i)) == prio) {
                    sortByPriority.add(painterNames.get(i));
                    painterNames.remove(i);
                    break;
                }
            }
        }                
    }

    /**
     * Remove all painters which extend the given class
     * @param c The class / superclass of the painters to be removed
     */
    public void removePaintersOfType(Class<?> c) {
        Vector<String> namesToRemove = new Vector<String>();
        for (String name : paintersByName.keySet()) {
            if (ReflectionUtil.isSubclass(paintersByName.get(name).getClass(), c))
                namesToRemove.add(name);
        }

        for (String name : namesToRemove)
            removePainter(name);        
    }

    /**
     * Return a list of painters sorted by priority (lowest priority first)
     * @return The painters sorted by its priority
     */
    public Vector<IUavPainter> getPaintersSortedByPriority() {
        Vector<IUavPainter> painters = new Vector<IUavPainter>();

        for (String name : sortByPriority) {
            painters.add(paintersByName.get(name));
        }
        return painters;
    }

    /**
     * Retrieve the list of painters sorted by its name (alphabetical order)
     * @return The painters sorted by its name
     */
    public Vector<IUavPainter> getPaintersSortedByName() {
        Vector<IUavPainter> painters = new Vector<IUavPainter>();

        for (String name : sortByName) {
            painters.add(paintersByName.get(name));
        }
        return painters;
    }
    
    /**
     * Set whether a painter should or not be active (painted)
     * @param name The name of the painter
     * @param active <b>true</b> if the painter should be drawn or <b>false</b> otherwise
     */
    public void setPainterActive(String name, boolean active) {
        activePainters.put(name, active);
    }
   
    /**
     * Retrieve all pre-render painters
     * @return All painters whose priority is less than or equal to 0
     */
    public Vector<IUavPainter> getPreRenderPainters() {
        Vector<IUavPainter> ret = new Vector<IUavPainter>();
        
        for (String painter : sortByPriority) {
            if (painterPriorities.get(painter) <= 0 && activePainters.get(painter)) {
                ret.add(paintersByName.get(painter));                
            }
        }
        return ret;
    }
    
    /**
     * Remove a painter from this bag
     * @param painter The painter to be removed (if it is present) from this bag
     */
    public void remove(IUavPainter painter) {
        String nameToRemove = null;
        for (String name : paintersByName.keySet()) {
            if (paintersByName.get(name).equals(painter)) {
                nameToRemove = name;
                break;
            }
        }
        if (nameToRemove != null)
            removePainter(nameToRemove);
    }
    
    /**
     * Retrieve post render painters
     * @return All painters whose priority is bigger than 0
     */
    public Vector<IUavPainter> getPostRenderPainters() {
        
        Vector<IUavPainter> ret = new Vector<IUavPainter>();
        
        for (String painter : sortByPriority) {
            if (painterPriorities.get(painter) > 0 && activePainters.get(painter)) {
                ret.add(paintersByName.get(painter));                
            }
        }
        return ret;
    }
    
    /**
     * Show a window dialog where the user can select which painters are active
     * @param owner The window which will be parenting the created dialog
     */
    public void showSelectionDialog(Window owner) {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.PAGE_AXIS));
        ActionListener listener = new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = e.getActionCommand();
                UavPaintersBag.this.setPainterActive(name, ((JCheckBox)e.getSource()).isSelected());
            }
        }; 
        for (String name : sortByName) {
            JCheckBox check = new JCheckBox(name);
            check.setSelected(activePainters.get(name));    
            check.setActionCommand(name);
            check.addActionListener(listener);
            inner.add(check);
        }
        JDialog dialog = new JDialog(owner);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.setTitle("Active layers selection");
        JScrollPane scrollPane = new JScrollPane(inner);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        dialog.getContentPane().add(scrollPane);
        dialog.setSize(300, 300);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GuiUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
    }
}
