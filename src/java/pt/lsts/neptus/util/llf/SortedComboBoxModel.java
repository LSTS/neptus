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
 * Author: Manuel R
 * Dec 2, 2015
 */
package pt.lsts.neptus.util.llf;

import java.util.Comparator;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;

@SuppressWarnings("rawtypes")
public class SortedComboBoxModel<E> extends DefaultComboBoxModel<E> {
    private static final long serialVersionUID = 1L;
    private Comparator comparator;

    /**
     * Create an empty model that will use the natural sort order of the item
     */
    public SortedComboBoxModel() {
        super();
    }

    /**
     * Create an empty model that will use the specified Comparator
     * @param comparator
     */
    public SortedComboBoxModel(Comparator comparator) {
        super();
        this.comparator = comparator;
    }

    /**
     * Create a model with data and use the nature sort order of the items
     * @param items
     */
    public SortedComboBoxModel(E items[]) {
        this(items, null);
    }

    /**
     * Create a model with data and use the specified Comparator
     * @param items
     * @param comparator
     */
    public SortedComboBoxModel(E items[], Comparator comparator) {
        this.comparator = comparator;

        for (E item : items) {
            addElement(item);
        }
    }

    /**
     * Create a model with data and use the nature sort order of the items
     * @param items
     */
    public SortedComboBoxModel(Vector<E> items) {
        this(items, null);
    }

    /**
     * Create a model with data and use the specified Comparator
     * @param items
     * @param comparator
     */
    public SortedComboBoxModel(Vector<E> items, Comparator comparator) {
        this.comparator = comparator;

        for (E item : items) {
            addElement(item);
        }
    }

    @Override
    public void addElement(E element) {
        insertElementAt(element, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void insertElementAt(E element, int index) {
        int size = getSize();

        // Determine where to insert element to keep model in sorted order

        for (index = 0; index < size; index++) {
            if (comparator != null) {
                E o = getElementAt(index);

                if (comparator.compare(o, element) > 0)
                    break;
            }
            else {
                Comparable c = (Comparable) getElementAt(index);

                if (c.compareTo(element) > 0)
                    break;
            }
        }

        super.insertElementAt(element, index);

        // Select an element when it is added to the beginning of the model

        if (index == 0 && element != null) {
            setSelectedItem(element);
        }
    }
}