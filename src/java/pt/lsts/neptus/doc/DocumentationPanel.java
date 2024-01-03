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
 * Author: José Pinto
 * Aug 29, 2011
 */
package pt.lsts.neptus.doc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import pt.lsts.neptus.renderer2d.WorldRenderPainter;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * This class provides a visualization of a given {@link DocumentationProvider}
 * 
 * @author zp
 */
public class DocumentationPanel extends JPanel implements HyperlinkListener {

    private static final long serialVersionUID = -6368710839048154767L;
    protected static DocumentationPanel instance = null;
    protected static JFrame frame = null;
    
    /**
     * All documentation files will use this prefix (and should be placed inside
     * doc/manual folder)
     */
    public static String docSource;

    static {
        try {
            docSource = new File("doc/manual").toURI().toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected JScrollPane scrollMain, scrollIndex;
    protected JSplitPane split;
    protected JEditorPane indexPane;
    protected JEditorPane htmlPane;
    protected JLabel title;

    /**
     * Class constructor, starts with empty document
     */
    public DocumentationPanel() {
        setLayout(new BorderLayout());
        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        htmlPane.setBackground(Color.white);
        htmlPane.addHyperlinkListener(this);
        
        indexPane = new JEditorPane();
        indexPane.setEditable(false);
        indexPane.setBackground(Color.white);
        indexPane.addHyperlinkListener(this);
        
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        scrollMain = new JScrollPane(htmlPane);
        scrollIndex = new JScrollPane(indexPane);
        split.add(scrollIndex);
        split.add(scrollMain);
        split.setDividerLocation(200);
        try {
            indexPane.setPage(new File("doc/manual/index.html").toURI().toURL());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        title = new JLabel();
        title.setFont(new Font("Helvetica", Font.ITALIC, 18));
        //add(title, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    /**
     * Sets the documentation source from a generic object
     * @param o If the object provides {@link NeptusDoc} annotation, it will be used, otherwise the object should implement the DocumentProvider interface
     */
    public void setProvider(Object o) {
        if (o instanceof Class || !(o instanceof DocumentationProvider))
            setProvider(new DocumentationWrapper(o));
        else
            setProvider((DocumentationProvider) o);
    }

    /**
     * Set the documentation source provider
     * 
     * @param provider
     *            A DocumentationProvider that will be requested to provide
     *            documentation files
     */
    public void setProvider(DocumentationProvider provider) {

        title.setText(provider.getArticleTitle());

        try {
            htmlPane.setPage(docSource + provider.getDocumentationFile());
        }
        catch (Exception e) {
            htmlPane.setText("Unable to read file '" + docSource + "/"
                    + provider.getDocumentationFile() + "'");
        }
    }
    
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() != EventType.ACTIVATED)
            return;
        
        try {            
            htmlPane.setPage(e.getURL());
            htmlPane.repaint();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    protected static JFrame getFrame() {
        if (instance == null)
            instance = new DocumentationPanel();
        
        if (frame == null) {
            frame = new JFrame("Neptus Help");
            frame.setSize(800, 500);
            frame.getContentPane().add(instance);
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            GuiUtils.centerOnScreen(frame);
            frame.setIconImages(ConfigFetch.getIconImagesForFrames());
        }   
        frame.setVisible(true);
        frame.toFront();
        return frame;
    }
    
    /**
     * Show the documentation for the given {@link DocumentationProvider}
     * @param provider The provides for documentation
     */
    public static void showDocumentation(DocumentationProvider provider) {
        getFrame();
        instance.setProvider(provider);
    }
        
    /**
     * Tries to surround the given class with a {@link DocumentationWrapper} and show its documentation
     * @param c a Class
     */
    public static void showDocumentation(Class<?> c) {
        getFrame();        
        instance.setProvider(new DocumentationWrapper(c));       
    }
    
    /**
     * Given an html manual page, it will show it in the DocumentationPanel
     * @param pathToHtml The path to the manual page (relative to doc/manual)
     */
    public static void showDocumentation(String pathToHtml) {
       
        getFrame();

        try {
            instance.htmlPane.setPage(docSource + pathToHtml);
        }
        catch (Exception e) {
            instance.htmlPane.setText("Unable to read file '" + docSource + "/"
                    + pathToHtml + "'");
        }
    }
    
    /**
     * Unitary test
     */
    public static void main(String[] args) {
        ConfigFetch.initialize();
        DocumentationPanel.showDocumentation(WorldRenderPainter.class);
    }
}
