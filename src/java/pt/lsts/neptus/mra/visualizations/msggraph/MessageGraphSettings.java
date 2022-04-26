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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 05/06/2016
 */
package pt.lsts.neptus.mra.visualizations.msggraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.CheckboxList;
import pt.lsts.neptus.gui.PropertiesTable;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class MessageGraphSettings extends JPanel {

    private static final long serialVersionUID = -192089328017611533L;
    
    private CheckboxList msgCheckList, cmpCheckList;
    PropertiesTable pt = new PropertiesTable();
    
    public MessageGraphSettings(MessageGraphVisualization viz) {
        ArrayList<String> allMessages = new ArrayList<>();
        allMessages.addAll(viz.consumers.keySet());
        viz.producers.keySet().forEach(msg -> {
            if (!allMessages.contains(msg))
                allMessages.add(msg);
        });
        Collections.sort(allMessages);
        
        ArrayList<String> allComponents = new ArrayList<>();
        viz.consumers.values().forEach(clist -> clist.forEach(c -> {
            String consumer = viz.componentNames.inverse().get(c);
            if (consumer != null && !allComponents.contains(consumer))
                allComponents.add(consumer);
        }));
        
        viz.producers.values().forEach(clist -> clist.forEach(c -> {
            String producer = viz.componentNames.inverse().get(c);
            if (producer != null && !allComponents.contains(producer))
                allComponents.add(producer);
        }));
        
        Collections.sort(allComponents);

        msgCheckList = CheckboxList.getInstance(allMessages.toArray(new String[0]));
        cmpCheckList = CheckboxList.getInstance(allComponents.toArray(new String[0]));
        pt.editProperties(PluginUtils.wrapIntoAPlugInPropertiesProvider(viz));
        pt.setDescriptionVisible(false);
        setLayout(new MigLayout());
        add(new JScrollPane(pt), "span, push, grow");
        add(new JLabel(I18n.text("Components:")));
        add(new JLabel(I18n.text("Messages:")), "wrap");
        
        add(new JScrollPane(cmpCheckList), "push, grow");
        add(new JScrollPane(msgCheckList), "push, grow, wrap");      
        
    }
    
    public List<String> selectedMessages() {
        return Arrays.asList(msgCheckList.getSelectedStrings());
    }
    
    public List<String> selectedComponents() {
        return Arrays.asList(cmpCheckList.getSelectedStrings());
    }
    

    public static void main(String[] args) {
        ArrayList<String> components = new ArrayList<>();
        ArrayList<String> messages = new ArrayList<>();
        components.add("Component1");
        components.add("Component2");
        components.add("Component3");
        components.add("Component4");
        
        messages.add("Message3");
        messages.add("Message1");
        messages.add("Message2");
        messages.add("Message5");
        messages.add("Message4");
        
        GuiUtils.setLookAndFeel();
        GuiUtils.testFrame(new MessageGraphSettings(new MessageGraphVisualization(null)));
    }
}
