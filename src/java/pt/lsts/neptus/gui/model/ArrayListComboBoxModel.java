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
 * Author: pdias
 * 11/11/2016
 */
package pt.lsts.neptus.gui.model;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * This class is a model to back an {@link ArrayList}.
 * 
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class ArrayListComboBoxModel<E extends Object> extends AbstractListModel<E> implements ComboBoxModel<E> {

    private ArrayList<E> arrayList;
    private E selectedItem = null;
    private boolean sort = false;

    public ArrayListComboBoxModel(ArrayList<E> arrayList) {
        this.arrayList = arrayList;
    }

    public ArrayListComboBoxModel(ArrayList<E> arrayList, boolean sort) {
        this.arrayList = arrayList;
        this.sort = sort;
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#getSize()
     */
    @Override
    public int getSize() {
        return arrayList.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#getElementAt(int)
     */
    @Override
    public E getElementAt(int index) {
        return arrayList.get(index);
    }

    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setSelectedItem(Object anItem) {
        try {
            selectedItem = (E) anItem;
            fireContentsChanged(this, 0, getSize());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#getSelectedItem()
     */
    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }

    public boolean addValue(E val) {
        if (val == null)
            return false;

        if (arrayList.contains(val))
            return false;

        arrayList.add(val);
        if (sort)
            arrayList.sort(null);
        fireIntervalAdded(this, sort ? 0 : getSize(), getSize());

        return true;
    }

    public boolean removeValue(E val) {
        if (arrayList.contains(val))
            return false;

        int idx = arrayList.indexOf(val);
        arrayList.remove(val);
        fireIntervalRemoved(this, idx, idx);
        return true;
    }

    public E getValue(int index) {
        return arrayList.get(index);
    }

    /**
     * Same as {@link #clear()}
     */
    public void removeAll() {
        int size = arrayList.size();
        arrayList.clear();
        fireIntervalRemoved(this, 0, size);
    }

    /**
     * Same as {@link #removeAll()}
     */
    public void clear() {
        removeAll();
    }
}
