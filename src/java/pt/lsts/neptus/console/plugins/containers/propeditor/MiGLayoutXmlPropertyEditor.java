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
 * Author: Paulo Dias
 * 5 de Out de 2010
 */
package pt.lsts.neptus.console.plugins.containers.propeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.fife.ui.autocomplete.CompletionProvider;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.containers.MigLayoutContainer;
import pt.lsts.neptus.events.NeptusEventLayoutChanged;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.fileeditor.SyntaxFormaterTextArea;
import pt.lsts.neptus.gui.editor.XMLPropertyEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StreamUtil;

/**
 * @author pdias
 * 
 */
public class MiGLayoutXmlPropertyEditor extends XMLPropertyEditor {

    protected JButton testButton;
    
    public MiGLayoutXmlPropertyEditor() {
        super();
        rootElement = "";
        xmlSchemaName = "MiGLayoutContainer";
        title = I18n.text("Layout for: MiGLayout Layout XML");
        smallMsg = ""; // I18n.text("This follows the MiGLayout");
        contentType = "text/html";
        helpText = I18n.text("Container using MigLayout") + "<br/>";
        helpText += I18n.textf("(see %url)", "'http://www.migcalendar.com/miglayout/mavensite/docs/cheatsheet.html'") + "<br/>";
        helpText += I18n.text("For reference duplicated components use <Component Name>_N where N=1,2,3...\n");
        helpText += "<br/>";
        helpText += I18n.text("Example of a basic profile:") + "<br/>";
        helpText += "================<br/>";
        helpText += "<br/>";
        helpText += "&lt;profiles&gt;<br/>";
        helpText += "&nbsp;&nbsp;&lt;profile name=\"Normal\"&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&lt;container layoutparam=\"ins 0\" param=\"w 100%, h 100%\"&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;child name=\"Map Panel\" param=\"w 100%, h 100%\"/&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&lt;/container&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&lt;container layoutparam=\"ins 3\" param=\"w 300px!, h 100%\"&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;container layoutparam=\"ins 0, filly\" param=\"w 100%, h 100%\"&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;child name=\"Plan Control\" param=\"w 80%, h 50px!\"/&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;child name=\"Abort Button\" param=\"w 20%, h 50px!, wrap\"/&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;child name=\"Plan Control State\" param=\"w 100%!, h 100px!, span, wrap\"/&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;child name=\"Mission Tree\" param=\"w 100%, h 100%, span\"/&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/container&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&lt;/container&gt;<br/>";
        helpText += "&nbsp;&nbsp;&lt;/profile&gt;<br/>";
        helpText += "&nbsp;&nbsp;&lt;profile name=\"Map\"&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&lt;container layoutparam=\"ins 0\" param=\"w 100%, h 100%\"&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;child name=\"Map Panel\" param=\"w 100%, h 100%\"/&gt;<br/>";
        helpText += "&nbsp;&nbsp;&nbsp;&nbsp;&lt;/container&gt;<br/>";
        helpText += "&nbsp;&nbsp;&lt;/profile&gt;<br/>";
        helpText += "&lt;/profiles&gt;<br/>";
        
        helpText += "<br/>";
        helpText += "<br/>";
        
        helpText += "&lt;!--profiles DTD--&gt;<br/>";
        helpText += "&lt!ELEMENT profiles (profile)+&gt;<br/>";
        helpText += "&lt!ELEMENT profile ((container | child)*, window*)&gt;<br/>";
        helpText += "&lt!ATTLIST profile<br/>";
        helpText += "&nbsp;&nbsp;name CDATA #REQUIRED<br/>";
        helpText += "&gt;<br/>";
        helpText += "&lt!ELEMENT container ((container | child)* | (tab*)*)&gt;<br/>";
        helpText += "&lt!ATTLIST container<br/>";
        helpText += "&nbsp;&nbsp;layoutparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;colparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;rowparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;param CDATA #IMPLIED<br/>";
        helpText += "&gt;<br/>";
        helpText += "&lt!ELEMENT child EMPTY&gt;<br/>";
        helpText += "&lt!ATTLIST child<br/>";
        helpText += "&nbsp;&nbsp;name CDATA #REQUIRED<br/>";
        helpText += "&nbsp;&nbsp;param CDATA #IMPLIED<br/>";
        helpText += "&gt;<br/>";
        helpText += "&lt!ELEMENT tab (container | child)*&gt;<br/>";
        helpText += "&lt!ATTLIST tab<br/>";
        helpText += "&nbsp;&nbsp;tabname CDATA #REQUIRED<br/>";
        helpText += "&nbsp;&nbsp;layoutparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;colparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;rowparam CDATA #IMPLIED<br/>";
        helpText += "&gt;<br/>";
        helpText += "&lt!ELEMENT window (container | child)+&gt;<br/>";
        helpText += "&lt!ATTLIST window<br/>";
        helpText += "&nbsp;&nbsp;name CDATA #REQUIRED<br/>";
        helpText += "&nbsp;&nbsp;layoutparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;colparam CDATA #IMPLIED<br/>";
        helpText += "&nbsp;&nbsp;rowparam CDATA #IMPLIED<br/>";
        helpText += "&gt;<br/>";
    }
    
    @Override
    protected ActionListener getOkButtonAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // don't use rootElement so no need to call getStrippedDownRootElementText()
                String tmpStr = editorPane.getText();
                NeptusEvents.post(new NeptusEventLayoutChanged(tmpStr)); // Review this, but not access to console
                setValue(tmpStr);

                dialog.setVisible(false);
                dialog.dispose();
            }
        };
    }
    
    @Override
    protected ActionListener getCancelButtonAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                NeptusEvents.post(new NeptusEventLayoutChanged(oldXmlStr));
                setValue(oldXmlStr);
                
                dialog.setVisible(false);
                dialog.dispose();
            }
        };
    }
    
    @Override
    protected ArrayList<JComponent> getAdditionalComponentsForButtonsPanel() {
        ArrayList<JComponent> ret = new ArrayList<>();
        
        testButton = new JButton(I18n.text("Test"));
        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String tmpStr = editorPane.getText();
                NeptusEvents.post(new NeptusEventLayoutChanged(tmpStr));
            }
        });
        ret.add(testButton);
        
        return ret;
    }
    
    @Override
    protected InputStream getSchemaInputStream() {
        return MigLayoutContainer.class.getResourceAsStream(MigLayoutContainer.LAYOUT_SCHEMA);
    }
    
    @Override
    public Schema getSchema() {
        SchemaFactory sm = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            File sFx = StreamUtil.copyStreamToTempFile(getSchemaInputStream());
            Schema schema = sm.newSchema(sFx);
            return schema;
        }
        catch (Exception e) {
            NeptusLog.pub().warn(ReflectionUtil.getCallerStamp() + e.getMessage());
        }        return null;
    }

    @Override
    protected ArrayList<CompletionProvider> getAdditionalCompletionProviders() {
        ArrayList<CompletionProvider> ret = new ArrayList<>();
        
        CompletionProvider provider = SyntaxFormaterTextArea.createMigLayoutContainerCompletionProvider();
        ret.add(provider);
        
        return ret;
    }

    public static void main(String[] args) {
        MiGLayoutXmlPropertyEditor xp = new MiGLayoutXmlPropertyEditor();

        GuiUtils.testFrame(xp.button);
    }
}