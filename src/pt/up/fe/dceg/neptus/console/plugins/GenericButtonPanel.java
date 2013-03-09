/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import org.dom4j.Attribute;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.editor.Script;
import pt.up.fe.dceg.neptus.gui.swing.JRoundButton;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.util.ImageUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author ZP
 */
@PluginDescription(icon = "images/buttons/star.png", name = "Generic (Scriptable) Button")
public class GenericButtonPanel extends SubPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JRoundButton roundButton;
    private String iconFile = "images/buttons/star.png";
    private String tooltip = "Do nothing";
    private Script script = new Script(
            "/*Example:\n$i = tree.getValue('i');\n$i++;\ntree.setValue('i', $i);\nshell.print('i = '+$i);*/\n");

    public GenericButtonPanel(ConsoleLayout console) {
        super(console);
        roundButton = new JRoundButton(ImageUtils.getIcon(iconFile));
        roundButton.setPreferredSize(new Dimension(30, 30));
        roundButton.addActionListener(this);
        roundButton.setToolTipText(tooltip);
        setLayout(new BorderLayout(2, 2));
        add(roundButton);

        Dimension dim = new Dimension(32, 32);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        setSize(dim);
    }

    public void actionPerformed(ActionEvent e) {
        getConsole().evaluateScript(script.getSource());
    }

    @Override
    public DefaultProperty[] getProperties() {
        return new DefaultProperty[] { PropertiesEditor.getPropertyInstance("Script (JS)", Script.class, script, true),
                PropertiesEditor.getPropertyInstance("Icon (relative path)", String.class, iconFile, true),
                PropertiesEditor.getPropertyInstance("Tooltip text", String.class, tooltip, true) };
    }

    @Override
    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            if (p.getName().equals("Script (JS)")) {
                script = (Script) p.getValue();
            }
            if (p.getName().equals("Tooltip text")) {
                tooltip = p.getValue().toString();
                roundButton.setToolTipText(tooltip);
            }
            if (p.getName().equals("Icon (relative path)")) {
                iconFile = p.getValue().toString();
                ImageIcon icon;
                try {
                    icon = ImageUtils.getIcon(iconFile);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    iconFile = "images/buttons/star.png";
                    icon = ImageUtils.getIcon(iconFile);
                }
                roundButton.setIcon(icon);
            }
        }
    }

    public void XML_PropertiesWrite(Element e) {

        e.addAttribute("iconfile", iconFile);
        Element sc = e.addElement("script");
        sc.addCDATA(script.getSource());
        e.addAttribute("tooltip", tooltip);
    }

    public void XML_PropertiesRead(Element e) {
        List<?> list = e.selectNodes("@*");
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Attribute attribute = (Attribute) iter.next();

            if ("iconfile".equals(attribute.getName())) {
                iconFile = attribute.getValue();
                try {
                    roundButton.setIcon(ImageUtils.getIcon(iconFile));
                }
                catch (Exception ex) {
                    iconFile = "images/buttons/star.png";
                    roundButton.setIcon(ImageUtils.getIcon(iconFile));
                }
            }

            if ("tooltip".equals(attribute.getName())) {
                tooltip = attribute.getValue();
                roundButton.setToolTipText(tooltip);
            }
        }

        Element scr = e.element("script");

        if (scr != null)
            script.setSource(scr.getText());
    }

    public static void main(String[] args) {
    }
}
