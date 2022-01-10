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
 * Author: José Correia
 * Oct 3, 2012
 */
package pt.lsts.neptus.console.plugins;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.MainPanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author jqcorreia
 * 
 */
public class ComponentSelector extends JComboBox<Class<?>> {
    private static final long serialVersionUID = 5643009716126585165L;

    public ComponentSelector(final MainPanel mainPanel) {

        for (Class<?> sp : PluginsRepository.getPanelPlugins().values()) {
            if (sp.getAnnotation(PluginDescription.class) != null)
                PluginsRepository.addPlugin(sp.getCanonicalName());
        }

        String[] subPanelList = PluginsRepository.getPanelPlugins().keySet().toArray(new String[0]);
        Arrays.sort(subPanelList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Collator collator = Collator.getInstance(Locale.US);
                return collator.compare(PluginUtils.i18nTranslate(o1), PluginUtils.i18nTranslate(o2));
            }
        });

        int c = 0;
        for (String plugin : subPanelList) {
            Class<?> clazz = PluginsRepository.getPanelPlugins().get(plugin);
            insertItemAt(clazz, c++);
        }

        setRenderer(new ComponentSelectorRenderer());
        addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    mainPanel.setAdding((Class<?>) e.getItem());
                }
            }
        });
    }

    class ComponentSelectorRenderer extends JPanel implements ListCellRenderer<Class<?>> {
        private static final long serialVersionUID = 1L;

        public ComponentSelectorRenderer() {
            setLayout(new MigLayout());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Class<?>> list, Class<?> value, int index,
                boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                removeAll();
                JLabel icon = new JLabel();
                icon.setIcon(ImageUtils.createScaleImageIcon(PluginUtils.getPluginIcon(value), 16, 16));
                add(icon);
                add(new JLabel(PluginUtils.i18nTranslate(PluginUtils.getPluginName(value))));
                //add(new JLabel(PluginUtils.getPluginDescription(value)), "wrap");
            }
            return this;
        }
    }

}
