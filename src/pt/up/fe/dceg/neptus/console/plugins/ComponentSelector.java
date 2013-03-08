/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Oct 3, 2012
 * $Id:: ComponentSelector.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

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
import pt.up.fe.dceg.neptus.console.MainPanel;
import pt.up.fe.dceg.neptus.plugins.PluginClassLoader;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.PluginsRepository;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

/**
 * @author jqcorreia
 * 
 */
public class ComponentSelector extends JComboBox<Class<?>> {
    private static final long serialVersionUID = 5643009716126585165L;

    public ComponentSelector(final MainPanel mainPanel) {
        Class<?>[] subpanels = ReflectionUtil.listSubPanels();
        for (Class<?> sp : subpanels) {
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

    public static void main(String args[]) {
        PluginClassLoader.install();
        GuiUtils.testFrame(new ComponentSelector(null));
    }
}
