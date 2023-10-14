/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 5/10/2010
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.lsts.neptus.fileeditor.SyntaxFormaterTextArea;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author pdias
 * 
 */
public class XMLPropertyEditor extends AbstractPropertyEditor {

    protected String title = "XML";
    protected String rootElement = "";
    protected String xmlSchemaName = "xml";

    protected JButton button;
    protected String xmlStr = "";
    protected String oldXmlStr;

    protected String helpText = I18n.text("Container using GroupLayout\n" + "(see 'http://download.oracle.com/javase/"
            + "tutorial/uiswing/layout/group.html'\n" + "and 'http://download.oracle.com/javase/"
            + "tutorial/uiswing/layout/groupExample.html')\n"
            + "For reference duplicated components use <Component Name>_N where N=1,2,3...\n"
            + "\n\nDefinition:\n===========\n\n");
    protected String contentType = "text/plain";

    protected String smallMsg = ""; // I18n.text("This follows the Java Group Layout");
    protected JDialog dialog;

    // GUI
    protected RSyntaxTextArea editorPane;
    protected RTextScrollPane editorScrollPane;
    
    protected JButton okButton;
    protected JButton cancelButton;
    protected JButton validateButton;
    protected JButton extractSchemaButton;

    public XMLPropertyEditor() {
        initialize();
    }

    private void initialize() {
        button = new JButton(I18n.text("Edit"));
        editor = new JPanel(new BorderLayout(0, 0));
        ((JPanel) editor).add(button, BorderLayout.CENTER);
        
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                buildUI();
                dialog.setVisible(true);
            }
        });
    }

    protected void buildUI() {
        dialog = new JDialog(SwingUtilities.getWindowAncestor(editor));
        dialog.setTitle(title);
        dialog.setSize(800, 600);
        dialog.setLayout(new BorderLayout());
        GuiUtils.centerOnScreen(dialog);
        dialog.setResizable(true);

        // editor panel
        oldXmlStr = xmlStr;
        
        editorPane = SyntaxFormaterTextArea.createXMLFormatTextArea();
        SyntaxFormaterTextArea.installXMLLanguageSupport(editorPane, getSchemaInputStream());
        
        ArrayList<CompletionProvider> completionProviders = getAdditionalCompletionProviders();
        for (CompletionProvider provider : completionProviders) {
            AutoCompletion ac = new AutoCompletion(provider);
            ac.install(editorPane);
        }
        
        editorScrollPane = new RTextScrollPane(editorPane);

        editorScrollPane.setViewportView(editorPane);
        editorScrollPane.setVisible(true);
        try {
            if (!"".equalsIgnoreCase(xmlStr)) {
                xmlStr = XMLUtil.getAsCompactFormatedXMLString(xmlStr);
                xmlStr = XMLUtil.getAsPrettyPrintFormatedXMLString(xmlStr);
            }
        }
        catch (Exception e1) {
            xmlStr = oldXmlStr;
        }
        
        editorPane.setText(xmlStr.trim()); // To avoid null pointer
        String editorTxt = getTextWithRootElementText();
        try {
            if (!xmlStr.isEmpty() && !editorTxt.isEmpty()) {
                editorTxt = XMLUtil.getAsCompactFormatedXMLString(editorTxt);
                editorTxt = XMLUtil.getAsPrettyPrintFormatedXMLString(editorTxt);
            }
            else {
                editorTxt.replace("><", ">\n<");
            }
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        editorPane.setText(editorTxt); // xmlStr.trim()

        // help panel
        JScrollPane helpScrollPane = new JScrollPane();
        JEditorPane help = new JEditorPane();
        help.setEditable(false);
        help.setContentType(contentType);
        help.setBackground(new Color(255, 255, 160));
        help.setText(helpText);
        help.setCaretPosition(0);
        helpScrollPane.setViewportView(help);
        helpScrollPane.setVisible(true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScrollPane, helpScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation((int) (dialog.getWidth() * 0.7));

        dialog.add(splitPane, BorderLayout.CENTER);
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BorderLayout());
        JPanel buttons = new JPanel();
        GroupLayout layout = new GroupLayout(buttons);
        buttons.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        okButton = new JButton(I18n.text("OK"));
        okButton.addActionListener(getOkButtonAction());

        cancelButton = new JButton(I18n.text("Cancel"));
        cancelButton.addActionListener(getCancelButtonAction());

        validateButton = new JButton(I18n.text("Validate"));
        validateButton.addActionListener(getValidateButtonAction());

        extractSchemaButton = new JButton(I18n.text("Extract Schema"));
        extractSchemaButton.addActionListener(getExtractSchemaButtonAction());
        // If schema doesn't exist disable validation and extraction button
        if (getSchema() == null) {
            validateButton.setEnabled(false);
            extractSchemaButton.setEnabled(false);
        }

        JLabel label = new JLabel(smallMsg);
        
        ArrayList<JComponent> additionalComponentsForButtonsPanel = getAdditionalComponentsForButtonsPanel();
        
        SequentialGroup horizSeqGroup = layout.createSequentialGroup();
        ParallelGroup vertParallelGrp = layout.createParallelGroup();
        
        if (validateButton.isEnabled()) {
            horizSeqGroup.addComponent(extractSchemaButton).addComponent(validateButton);
            vertParallelGrp.addComponent(extractSchemaButton).addComponent(validateButton);
        }
        if (!label.getText().isEmpty()) {
            horizSeqGroup.addComponent(label);
            vertParallelGrp.addComponent(label);
        }
        for (JComponent comp : additionalComponentsForButtonsPanel) {
            horizSeqGroup.addComponent(comp);
            vertParallelGrp.addComponent(comp);
        }
        horizSeqGroup.addComponent(okButton).addComponent(cancelButton);
        vertParallelGrp.addComponent(okButton).addComponent(cancelButton);
        
        layout.setHorizontalGroup(horizSeqGroup);
        layout.setVerticalGroup(vertParallelGrp);

        if (validateButton.isEnabled()) {
            layout.linkSize(SwingConstants.HORIZONTAL, validateButton, extractSchemaButton);
        }

        ArrayList<JComponent> buttonsComp = new ArrayList<>();
        buttonsComp.add(okButton);
        buttonsComp.add(cancelButton);
        for (JComponent comp : additionalComponentsForButtonsPanel) {
            if (comp instanceof JButton)
                buttonsComp.add(comp);
        }
        layout.linkSize(SwingConstants.HORIZONTAL, buttonsComp.toArray(new JComponent[buttonsComp.size()]));

        toolbar.add(buttons, BorderLayout.EAST);
        dialog.add(toolbar, BorderLayout.SOUTH);
    }

    protected ArrayList<JComponent> getAdditionalComponentsForButtonsPanel() {
        return new ArrayList<>();
    }

    protected ActionListener getExtractSchemaButtonAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    InputStream sstream = getSchemaInputStream();
                    File fx = new File(xmlSchemaName + ".xsd");
                    fx = new File(fx.getName());
                    fx.createNewFile();
                    StreamUtil.copyStreamToFile(sstream, fx);
                    GuiUtils.infoMessage(dialog, I18n.text("Extract Schema"), I18n.text("Schema extracted to file:")
                            + "\n" + fx.getAbsolutePath());
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(dialog, I18n.text("Extract Schema"),
                            I18n.text("Error while extracting schema to file!!") + "\n" + e.getMessage());
                }
            }
        };
    }

    protected ActionListener getValidateButtonAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String[] vmsgs = validateLayoutXML(getStrippedDownRootElementText()); //  editorPane.getText()
                if (vmsgs.length == 0) {
                    GuiUtils.infoMessage(dialog, I18n.text("Validation"), I18n.text("Valid XML."));
                }
                else {
                    String strMsg = I18n.text("Invalid XML!") + "\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    GuiUtils.infoMessage(dialog, I18n.text("Validation"), strMsg);
                }
            }
        };
    }

    protected ActionListener getCancelButtonAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        };
    }

    protected ActionListener getOkButtonAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String[] vmsgs = validateLayoutXML(getStrippedDownRootElementText()); // editorPane.getText()
                if (vmsgs.length == 0) {
                    
                    String tmpStr = getStrippedDownRootElementText(); //editorPane.getText();
                    try {
                        if (!"".equalsIgnoreCase(tmpStr)) {
                            tmpStr = XMLUtil.getAsCompactFormatedXMLString(tmpStr);
                        }
                    }
                    catch (Exception e1) {
                    }
                    setValue(tmpStr);
                    firePropertyChange(oldXmlStr, tmpStr);
                    dialog.setVisible(false);
                    dialog.dispose();
                }
                else {
                    String strMsg = I18n.text("Invalid XML!") + "\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    GuiUtils.infoMessage(dialog, I18n.text("Validation"), strMsg);
                }
            }
        };
    }

    public Object getValue() {
        return xmlStr;
    }

    public void setValue(Object arg0) {
        if (arg0 instanceof String) {
            xmlStr = (String) arg0;
        }
    }

    protected String getStrippedDownRootElementText() {
        String txt = editorPane.getText();
        if (rootElement != null && !"".equalsIgnoreCase(rootElement) && !txt.isEmpty()) {
            txt = txt.trim();
            txt = txt.replaceAll("^[[\\s]*?]?<" + rootElement + ">", "")
                    .replaceAll("[[\\s]]*?]?</" + rootElement + ">[[\\s]]*?]?$", "");
            txt = txt.trim();
        }
        
        return txt;
    }

    protected String getTextWithRootElementText() {
        String txt = xmlStr;
        if (rootElement != null && !"".equalsIgnoreCase(rootElement))
            txt = "<" + rootElement + ">" + txt + "</" + rootElement + ">";
        return txt;
    }

    private String[] validateLayoutXML(String strXml) {
        ByteArrayInputStream bais = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + ((rootElement != null && !"".equalsIgnoreCase(rootElement)) ? "<" + rootElement + ">" : "")
                + strXml.trim() + ((rootElement != null && !"".equalsIgnoreCase(rootElement)) ? "</" + rootElement
                + ">" : "")).getBytes());

        final Vector<String> validationMsgs = new Vector<String>();

        if (getSchema() == null)
            return new String[0];

        Validator validator = getSchema().newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                validationMsgs.add("WARNING: " + exception.getMessage());
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                validationMsgs.add("ERROR: " + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                validationMsgs.add("FATAL: " + exception.getMessage());
            }
        });
        try {
            validator.validate(new StreamSource(bais));
        }
        catch (Exception e) {
            // validationMsgs.add("SOURCE: " + e.getMessage());
        }
        return validationMsgs.toArray(new String[validationMsgs.size()]);
    }

    public Schema getSchema() {
        return null;
    }

    protected InputStream getSchemaInputStream() {
        return null;
    }

    protected ArrayList<CompletionProvider>  getAdditionalCompletionProviders() {
        return new ArrayList<>();
    }
    
    public static void main(String[] args) {
        XMLPropertyEditor xp = new XMLPropertyEditor();

        GuiUtils.testFrame(xp.button);
    }
}
