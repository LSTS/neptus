/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 05/02/2016
 */
package pt.lsts.neptus.fileeditor;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;

import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsta.ac.xml.SchemaValidationConfig;
import org.fife.rsta.ac.xml.XmlLanguageSupport;
import org.fife.rsta.ac.xml.XmlParser;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import pt.lsts.neptus.console.plugins.containers.MigLayoutContainer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class SyntaxFormaterTextArea {

    private SyntaxFormaterTextArea() {
    }

    public static RSyntaxTextArea createXMLFormatTextArea() {
        RSyntaxTextArea xmlTextArea;

        xmlTextArea = new RSyntaxTextArea();
        xmlTextArea.setCaretPosition(0);
        xmlTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        xmlTextArea.setCodeFoldingEnabled(true);
        xmlTextArea.setAutoIndentEnabled(true);
        xmlTextArea.setClearWhitespaceLinesEnabled(true);
        xmlTextArea.setCloseMarkupTags(true);
        xmlTextArea.setFadeCurrentLineHighlight(true);
        
        // xmlTextArea.addHyperlinkListener(this);
        xmlTextArea.requestFocusInWindow();
        xmlTextArea.setMarkOccurrences(true);
        xmlTextArea.setTabsEmulated(true);
        xmlTextArea.setTabSize(2);

        // LanguageSupportFactory.get().register(xmlTextArea);
        
        return xmlTextArea;
    }
    
    public static CompletionProvider createMigLayoutContainerCompletionProvider() {

        // A DefaultCompletionProvider is the simplest concrete implementation
        // of CompletionProvider. This provider has no understanding of
        // language semantics. It simply checks the text entered up to the
        // caret position for a match against known completions. This is all
        // that is needed in the majority of cases.
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

//        // Add completions for all Java keywords. A BasicCompletion is just
//        // a straightforward word completion.
//        provider.addCompletion(new BasicCompletion(provider, "profiles"));
//        provider.addCompletion(new BasicCompletion(provider, "profile"));

        // Add a couple of "shorthand" completions. These completions don't
        // require the input text to be the same thing as the replacement text.
        provider.addCompletion(new ShorthandCompletion(provider, "<profiles>",
                "<profiles>\n  <profile name=\"Normal\">\n  </profile>\n</profiles>", I18n.text("profiles template")));

        provider.addCompletion(new ShorthandCompletion(provider, "<profiles>",
                "<profiles>\n  <profile name=\"Normal\">\n    <child name=\"?\" param=\"\" />"
                + "\n    <window name=\"Window 2\">\n        <child name=\"?\" param=\"\" />"
                + "\n    </window>\n    <window name=\"Window 3\">\n        <child name=\"?\" param=\"\" />"
                + "\n    </window>\n  </profile>\n</profiles>", I18n.text("profiles with windows template")));

        provider.addCompletion(new ShorthandCompletion(provider, "<profile>",
                "<profile name=\"?\"></profile>", I18n.text("profile tag")));

        provider.addCompletion(new ShorthandCompletion(provider, "<container>",
                "<container layoutparam=\"\" param=\"\"></container>", I18n.text("container tag")));

        provider.addCompletion(new ShorthandCompletion(provider, "<child>",
                "<child name=\"?\" param=\"\" />", I18n.text("child tag")));

        provider.addCompletion(new ShorthandCompletion(provider, "<child>",
                "<window name=\"?\" />", I18n.text("window tag")));

        provider.addCompletion(new ShorthandCompletion(provider, "<tab>",
                "<tab tabname=\"\" layoutparam=\"\"></tab>", I18n.text("tab tag")));

        return provider;
     }

    public static void installXMLLanguageSupport(RSyntaxTextArea xmlTextArea) {
        installXMLLanguageSupport(xmlTextArea, null);
    }

    public static void installXMLLanguageSupport(RSyntaxTextArea xmlTextArea, InputStream xmlSchemaInputStream) {
        XmlLanguageSupport xmlLanSup = (XmlLanguageSupport) LanguageSupportFactory.get().getSupportFor(SyntaxConstants.SYNTAX_STYLE_XML);
        xmlLanSup.install(xmlTextArea);
        
        if (xmlSchemaInputStream == null)
            return;
        
        XmlParser xmlParser = xmlLanSup.getParser(xmlTextArea);
        
        try {
            SchemaValidationConfig schemaValConfig = new SchemaValidationConfig(XMLConstants.W3C_XML_SCHEMA_NS_URI,
                    xmlSchemaInputStream);
            xmlParser.setValidationConfig(schemaValConfig);
        }
        catch (IOException e) {
            e.printStackTrace();        
        }
    }

    public static void main(String[] args) {
        RSyntaxTextArea xmlTextArea = createXMLFormatTextArea();
        RTextScrollPane xmlScrollPane;
        xmlScrollPane = new RTextScrollPane(xmlTextArea);
    
        CompletionProvider provider = createMigLayoutContainerCompletionProvider();

        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        AutoCompletion ac = new AutoCompletion(provider);
//        ac.install(xmlTextArea);
        
        InputStream is = MigLayoutContainer.class.getResourceAsStream("miglayout-container.xsd");
        installXMLLanguageSupport(xmlTextArea, is);

//        xmlTextArea.putClientProperty("org.fife.rsta.ac.LanguageSupport", xmlLanSup);
        
//        LanguageSupportFactory.get().register(xmlTextArea);

        ac.install(xmlTextArea);

        GuiUtils.testFrame(xmlScrollPane);
    }
}
