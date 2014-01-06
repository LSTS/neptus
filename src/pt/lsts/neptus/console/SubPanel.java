/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.console;

import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.SubPanelProvider;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.types.XmlInOutMethods;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ListenerManager;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author Rui Gonçalves
 * 
 */
public class SubPanel extends JPanel implements PropertiesProvider, XmlInOutMethods, SubPanelProvider {

    private static final long serialVersionUID = -2131046685846552482L;
    public static final ImageIcon DEFAULT_ICON = ImageUtils.createImageIcon("images/buttons/star.png");
    private static final String DEFAULT_ROOT_ELEMENT = "subpanel";

    protected String description = "";
    protected ImageIcon imageIcon = DEFAULT_ICON;

    protected boolean editmode;
    protected boolean resizable = true;

    protected ListenerManager listenerManager;
    protected boolean fixedSize = false;
    protected boolean fixedPosition = false;
    protected final MainPanel mainpanel;
    protected final ConsoleLayout console;

    private double percentXPos, percentYPos, percentWidth, percentHeight;

    public SubPanel(ConsoleLayout console) {
        this.console = console;
        this.mainpanel = console == null ? null : console.getMainPanel();
        if (console != null)
            NeptusEvents.register(this, console);
    }

    /**
     * Alias method to send console events
     * 
     * @param event
     */
    public void post(Object event) {
        NeptusEvents.post(event, console);
    }

    public void recalculateRelativePosAndSize() {

        if (mainpanel == null || mainpanel.getWidth() <= 0)
            return;

        percentWidth = (double) getWidth() / (double) mainpanel.getWidth();
        percentHeight = (double) getHeight() / (double) mainpanel.getHeight();
        percentXPos = (double) getX() / (double) mainpanel.getWidth();
        percentYPos = (double) getY() / (double) mainpanel.getHeight();
    }

    public void parentResized(Dimension oldSize, Dimension newSize) {

        if (getWidth() <= 0 || oldSize.getWidth() <= 0 || newSize.getWidth() <= 0)
            return;

        if (percentWidth == 0 && getWidth() != 0) {
            percentWidth = (double) getWidth() / oldSize.getWidth();
            percentHeight = (double) getHeight() / oldSize.getHeight();
            percentXPos = (double) getX() / oldSize.getWidth();
            percentYPos = (double) getY() / oldSize.getHeight();
        }

        if (!isFixedPosition()) {
            setLocation((int) (percentXPos * newSize.getWidth()), (int) (percentYPos * newSize.getHeight()));
        }

        if (!isFixedSize()) {
            setSize((int) (percentWidth * newSize.getWidth()), (int) (percentHeight * newSize.getHeight()));
        }

    }

    public void setEditMode(boolean b) {
        editmode = b;
    }

    public boolean getEditMode() {
        return editmode;
    }

    public void deactivateComponents() {
        listenerManager = new ListenerManager(this);
        listenerManager.turnoff();
    }

    @Override
    final public ToolbarButton getPaletteToolbarButton(Dimension dim) {
        return getPaletteToolbarButton((int) dim.getWidth(), (int) dim.getHeight());
    }

    @Override
    final public ToolbarButton getPaletteToolbarButton(int width, int height) {
        return new ToolbarButton(ImageUtils.getScaledIcon(getImageIcon(), width, height), getName(), null);
    }

    /**
     * Empty implementation. This method is called after console is completely loaded with all panels (override it if
     * needed).
     */
    public void init() {
    }

    /**
     * Empty implementation. This is called when the console wants to remove the panel from the console (override it if
     * needed to properly disposal of the component).
     */
    public void clean() {
        NeptusEvents.unregister(this, this.console);
    }

    public void activateComponents() {
        listenerManager.turnon();
    }

    public ConsoleLayout getConsole() {
        return console;
    }

    public MainPanel getMainpanel() {
        return mainpanel;
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isFixedSize() {
        return fixedSize;
    }

    public void setFixedSize(boolean fixedSize) {
        this.fixedSize = fixedSize;
    }

    public boolean isFixedPosition() {
        return fixedPosition;
    }

    public void setFixedPosition(boolean fixedPosition) {
        this.fixedPosition = fixedPosition;
    }

    public SubPanel[] getChildren() {
        return new SubPanel[0];
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    /*
     * OVERRIDES
     */

    public DefaultProperty[] getProperties() {
        return new DefaultProperty[] {};
    }

    public String getPropertiesDialogTitle() {
        return "SubPanel properties";
    }

    public void setProperties(Property[] properties) {
        // Default implementation does absolutely nothing
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    public void XML_PropertiesWrite(Element e) {
        e.getParent().remove(e);
    }

    public void XML_PropertiesRead(Element e) {

    }

    public void XML_ChildsWrite(Element e) {

    }

    public void XML_ChildsRead(Element e) {

    }

    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    public Document asDocument(String rootElementName) {

        Document doc = null;

        doc = DocumentHelper.createDocument();

        Element root = doc.addElement(rootElementName);

        root.addAttribute("class", this.getClass().getName());
        root.addAttribute("x", "" + this.getX());
        root.addAttribute("y", "" + this.getY());
        root.addAttribute("width", "" + this.getWidth());
        root.addAttribute("height", "" + this.getHeight());

        Element properties = root.addElement("properties");
        XML_PropertiesWrite(properties);
        XML_ChildsWrite(root);

        return doc;
    }

    public void inElement(Element e) {
        XML_PropertiesRead(e.element("properties")); // propriedades
        XML_ChildsRead(e); // resto que possa haver tipo mainpanels dentro...
    }

    public void inDocument(Document d) {
        inElement((Element) d);
    }

    public void inXML(String str) {
        Document document = null;
        try {
            document = DocumentHelper.parseText(str);
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("Subpanel parse XML string [" + e.getStackTrace() + "]");
            return;
        }
        if (document != null)
            inDocument(document);
    }

    @Override
    public SubPanel getSubPanel() {
        return this;
    }

    @Override
    public String getName() {
        if (super.getName() == null)
            return getClass().getSimpleName();
        return super.getName();
    }
}
