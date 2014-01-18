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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.state.ImcSysState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelProvider;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.XmlInOutMethods;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ListenerManager;
import pt.lsts.neptus.util.ReflectionUtil;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author Rui Gonçalves, ZP
 * 
 */
public abstract class SubPanel extends JPanel implements PropertiesProvider, XmlInOutMethods, SubPanelProvider, MessageListener<MessageInfo, IMCMessage>,
MainVehicleChangeListener {

    private static final long serialVersionUID = -2131046685846552482L;
    private static final String DEFAULT_ROOT_ELEMENT = "subpanel";

    private boolean editmode;
    private boolean resizable = true;

    private ListenerManager listenerManager;
    private boolean fixedSize = false;
    private boolean fixedPosition = false;
    private final MainPanel mainpanel;
    private final ConsoleLayout console;
    private double percentXPos, percentYPos, percentWidth, percentHeight;

    private boolean visibility = true;
    private final Vector<String> addedMenus = new Vector<String>();
    private final Vector<Integer> messagesToListen = new Vector<Integer>();
    private String mainVehicleId = null;
    private JMenuItem menuItem = null;
    protected JDialog dialog = null;
    private boolean popupPositionFlag = false;

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

    protected void recalculateRelativePosAndSize() {

        if (mainpanel == null || mainpanel.getWidth() <= 0)
            return;

        percentWidth = (double) getWidth() / (double) mainpanel.getWidth();
        percentHeight = (double) getHeight() / (double) mainpanel.getHeight();
        percentXPos = (double) getX() / (double) mainpanel.getWidth();
        percentYPos = (double) getY() / (double) mainpanel.getHeight();
    }

    final protected void parentResized(Dimension oldSize, Dimension newSize) {

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

    protected void setEditMode(boolean b) {
        editmode = b;
        if (!getEditMode() && !visibility)
            setVisible(false);
        else
            setVisible(true);
    }

    final protected boolean getEditMode() {
        return editmode;
    }

    final protected void deactivateComponents() {
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
     * Use this method to set the panel invisible (only visible at edition). Useful for daemons
     * 
     * @param visibility Whether this panel is to be visible or not. Panels are always visible at edit time
     */
    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public boolean getVisibility() {
        return this.visibility;
    }

    /**
     * Empty implementation. This method is called after console is completely loaded with all panels (override it if
     * needed).
     */
    public void init() {
        mainVehicleId = getConsole().getMainSystem();

        if (this instanceof MissionChangeListener)
            getConsole().addMissionListener((MissionChangeListener) this);

        if (this instanceof MainVehicleChangeListener || this instanceof NeptusMessageListener)
            getConsole().addMainVehicleListener((MainVehicleChangeListener)this);

        if (this instanceof PlanChangeListener)
            getConsole().addPlanListener((PlanChangeListener) this);

        if (this instanceof Renderer2DPainter) {
            Vector<MapPanel> pp = getConsole().getSubPanelsOfClass(MapPanel.class);
            for (MapPanel p : pp)
                p.addPostRenderPainter((Renderer2DPainter) this, PluginUtils.getPluginName(getClass()));
        }

        if (this instanceof StateRendererInteraction) {
            Vector<CustomInteractionSupport> panels = getConsole().getSubPanelsOfInterface(
                    CustomInteractionSupport.class);
            for (CustomInteractionSupport cis : panels)
                cis.addInteraction((StateRendererInteraction) this);
        }

        if (!getVisibility()) {
            setVisible(false);
        }

        // for miglayout parent will be null if the subpanel isnt in the layout
        if (this.getParent() == null) {
            this.buildPopup();
        }

        initSubPanel();

        // After all setup let us register the IPeriodicUpdates and Message callbacks

        if (this instanceof IPeriodicUpdates) {
            PeriodicUpdatesService.register((IPeriodicUpdates) this);
        }

        ImcMsgManager.registerBusListener(this);

        if (this instanceof NeptusMessageListener) {
            // NeptusLog.pub().info("<###>Adding myself as message listener");
            for (String msg : ((NeptusMessageListener) this).getObservedMessages()) {
                int id = -1;
                try {
                    id = IMCDefinition.getInstance().getMessageId(msg);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                if (id != -1 && !messagesToListen.contains(id))
                    messagesToListen.add(id);
                else if (id == -1)
                    System.err.println("Message " + msg
                            + " is not valid in the current IMC specification (requested by "
                            + PluginUtils.getPluginName(this.getClass()) + ")");
            }

            if (getConsole() != null && !messagesToListen.isEmpty())
                ImcMsgManager.getManager().addListener(this, getConsole().getMainSystem());
            else {
                NeptusLog.pub().info("<###>Console is null..." + this.getName());
            }
        }
    }

    private JMenuItem createMenuItem(final POSITION popupPosition, String name2, ImageIcon icon) {
        JMenuItem menuItem = new JMenuItem(new AbstractAction(name2,
                ImageUtils.getScaledIcon(icon, 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(!dialog.isVisible());
                setPopupPosition(popupPosition);
            }

        });
        return menuItem;
    }

    private void buildPopup() {
        final Popup cAction = getClass().getAnnotation(Popup.class);
        if (cAction == null)
            return;
        final POSITION popupPosition = cAction.pos();
        // String name2 = cAction.name().isEmpty() ? PluginUtils.i18nTranslate(getName()) : I18n.text(cAction.name());
        String name2 = cAction.name().isEmpty() ? getName() : cAction.name();
        name2 = I18n.text(name2);
        String iconPath = cAction.icon().isEmpty() ? PluginUtils.getPluginIcon(this.getClass()) : cAction.icon();
        int width = cAction.width();
        int height = cAction.height();
        KeyStroke accelerator = null;
        if (cAction.accelerator() != KeyEvent.VK_UNDEFINED) {
            int key = cAction.accelerator();
            if (key == KeyEvent.VK_C || key == KeyEvent.VK_V || key == KeyEvent.VK_X) {
                NeptusLog.pub().error("Cant assign CTRL-X, CTRL-C or CTRL-V to popups.");
            }
            else {
                accelerator = KeyStroke.getKeyStroke(cAction.accelerator(), KeyEvent.CTRL_DOWN_MASK);
            }
        }
        // Build menu
        JMenu menu = getConsole().getOrCreateJMenu(new String[] { I18n.text("View") });
        ImageIcon icon = ImageUtils.getIcon(iconPath);
        menuItem = createMenuItem(popupPosition, name2, icon);
        if (accelerator != null)
            menuItem.setAccelerator(accelerator);
        menu.add(menuItem);

        // Build Dialog
        dialog = new JDialog(getConsole());
        dialog.setTitle(name2);
        dialog.setIconImage(icon.getImage());
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        dialog.setSize(width, height);
        // dialog.setFocusable(true);

        if (accelerator != null) {
            getConsole().registerGlobalKeyBinding(accelerator, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(!dialog.isVisible());
                    setPopupPosition(popupPosition);
                }
            });
        }
        dialog.add(this);
    }

    protected void setPopupPosition(final POSITION popupPosition) {
        if (dialog.isVisible() && popupPositionFlag == false) {
            Point p = getConsole().getLocationOnScreen();
            switch (popupPosition) {
                case TOP_LEFT:
                    break;
                case TOP_RIGHT:
                    p.x += getConsole().getWidth() - dialog.getWidth();
                    break;
                case BOTTOM_LEFT:
                    p.y += getConsole().getHeight() - dialog.getHeight();
                    break;
                case BOTTOM_RIGHT:
                    p.x += getConsole().getWidth() - dialog.getWidth();
                    p.y += getConsole().getHeight() - dialog.getHeight();
                    break;
                case TOP:
                    p.x += getConsole().getWidth() / 2 - dialog.getWidth() / 2;
                    break;
                case BOTTOM:
                    p.x += getConsole().getWidth() / 2 - dialog.getWidth() / 2;
                    p.y += getConsole().getHeight() - dialog.getHeight();
                    break;
                case LEFT:
                    p.y += getConsole().getHeight() / 2 - dialog.getHeight() / 2;
                    break;
                case RIGHT:
                    p.x += getConsole().getWidth() - dialog.getWidth();
                    p.y += getConsole().getHeight() / 2 - dialog.getHeight() / 2;
                    break;

                default:
                    p.x += getConsole().getWidth() / 2 - dialog.getWidth() / 2;
                    p.y += getConsole().getHeight() / 2 - dialog.getHeight() / 2;
                    break;
            }
            dialog.setLocation(p);
            this.popupPositionFlag = true;
        }
    }

    /**
     * Empty implementation. This is called when the console wants to remove the panel from the console (override it if
     * needed to properly disposal of the component).
     */
    public void clean() {
        NeptusEvents.unregister(this, this.console);
        if (this instanceof MissionChangeListener) {
            getConsole().removeMissionListener((MissionChangeListener) this);
        }

        if (this instanceof MainVehicleChangeListener) {
            getConsole().removeMainVehicleListener(this);
        }

        if (this instanceof PlanChangeListener) {
            getConsole().removePlanListener((PlanChangeListener) this);
        }

        if (this instanceof IPeriodicUpdates)
            PeriodicUpdatesService.unregister((IPeriodicUpdates) this);

        if (this instanceof NeptusMessageListener) {
            if (getConsole() != null) {
                messagesToListen.clear();
                ImcMsgManager.getManager().removeListener(this, getConsole().getMainSystem());
            }
        }

        ImcMsgManager.unregisterBusListener(this);

        for (String menuPath : addedMenus) {
            JMenu parent = getConsole().removeMenuItem(menuPath.split(">"));
            // if parent became empty, remove parent
            if (parent != null && parent.getItemCount() == 0)
                getConsole().removeMenuItem(menuPath.substring(0, menuPath.lastIndexOf(">")).split(">"));
        }

        if (this instanceof Renderer2DPainter) {
            Vector<MapPanel> pp = getConsole().getSubPanelsOfClass(MapPanel.class);
            for (MapPanel p : pp)
                p.removePostRenderPainter((Renderer2DPainter) this);
        }

        if (menuItem != null || dialog != null) {
            JMenu menu = getConsole().getOrCreateJMenu(new String[] { I18n.text("View") });
            menu.remove(menuItem);
            if (dialog.isVisible()) {
                NeptusLog.pub().info("<###> " + this.getName());
                dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                dialog.dispose();
            }
        }
        cleanSubPanel();
    }

    final protected void activateComponents() {
        listenerManager.turnon();
    }

    public final ConsoleLayout getConsole() {
        return console;
    }

    protected final MainPanel getMainpanel() {
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

    protected final SubPanel[] getChildren() {
        return new SubPanel[0];
    }

    @Override
    public String getDescription() {
        return PluginUtils.getPluginDescription(this.getClass());
    }

    @Override
    public ImageIcon getImageIcon() {
        return ImageUtils.getIcon(PluginUtils.getPluginIcon(this.getClass()));
    }

    public String getPropertiesDialogTitle() {
        return PluginUtils.getPluginName(this.getClass()) + " parameters";
    }

    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }
    
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        LinkedHashMap<String, PluginProperty> props = new LinkedHashMap<String, PluginProperty>();

        for (Property p : properties)
            props.put(p.getName(), new PluginProperty(p));
        Vector<String> errors = new Vector<String>();

        Class<? extends Object> providerClass = this.getClass();

        for (Field f : providerClass.getFields()) {
            NeptusProperty a = f.getAnnotation(NeptusProperty.class);
            if (a != null) {
                // Find field name
                String name = a.name();
                String fieldName = f.getName();
                if (name.length() == 0) {
                    name = fieldName;
                }
                if (props.get(name) == null)
                    continue;
                // Find method
                String validateMethodUpper = "validate" + Character.toUpperCase(fieldName.charAt(0))
                        + fieldName.substring(1);
                String validateMethodLower = "validate" + Character.toLowerCase(fieldName.charAt(0))
                        + fieldName.substring(1);
                Method m;
                Object propValue = props.get(name).getValue();
                if (propValue == null) {
                    NeptusLog.pub().debug(
                            "Property " + providerClass.getSimpleName() + "." + name
                                    + " has no method to validate user input!");
                    continue;
                }
                try {
                    m = providerClass.getMethod(validateMethodUpper, propValue.getClass());
                }
                catch (NoSuchMethodException e1) {
                    try {
                        m = providerClass.getMethod(validateMethodLower, propValue.getClass());
                    }
                    catch (NoSuchMethodException e) {
                        NeptusLog.pub().debug(
                                "Property " + providerClass.getSimpleName() + "." + name
                                        + " has no method to validate user input!");
                        continue;
                    }
                    catch (SecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        continue;
                    }
                }
                catch (SecurityException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    continue;
                }

                // If method has been found, invoke it
                Object res;
                try {
                    res = m.invoke(this, propValue);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    continue;
                }
                // In case of error add error message to the error message array
                if (res != null)
                    errors.add(res.toString());
            }
        }

        return errors.toArray(new String[0]);
    }

    public void XML_PropertiesWrite(Element e) {
        String xml = PluginUtils.getConfigXml(this);
        try {
            Element el = DocumentHelper.parseText(xml).getRootElement();

            for (Object child : el.elements()) {
                Element aux = (Element) child;
                aux.detach();
                e.add(aux);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void XML_PropertiesRead(Element e) {
        PluginUtils.setConfigXml(this, e.asXML());        
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
        return PluginUtils.getPluginName(this.getClass());
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public final void onMessage(MessageInfo arg0, IMCMessage arg1) {
        if (messagesToListen.contains(arg1.getMessageType().getId())) {
            ((NeptusMessageListener) this).messageArrived(arg1);
        }
    }

    /**
     * If you need to react to main vehicle change override {@link #mainVehicleChangeNotification(String)} instead.
     * 
     * @see pt.lsts.neptus.console.plugins.MainVehicleChangeListener#mainVehicleChange(java.lang.String) The
     *      {@link #mainVehicleChangeNotification(String)} is called between the removal and addition of the new vehicle
     *      listener The {@link #mainVehicleId} is changed before this call.
     */
    @Override
    public final void mainVehicleChange(String id) {
        if (messagesToListen != null && !messagesToListen.isEmpty()) {
            ImcMsgManager.getManager().removeListener(this, mainVehicleId);
        }

        mainVehicleId = id;

        try {
            mainVehicleChangeNotification(id);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        if (messagesToListen != null && !messagesToListen.isEmpty()) {
            ImcMsgManager.getManager().addListener(this, id);
        }

        if (this instanceof NeptusMessageListener) {
            for (Integer msgStr : messagesToListen) {
                IMCMessage msg = getConsole().getImcState().get(msgStr);
                if (msg != null)
                    ((NeptusMessageListener) this).messageArrived(msg);
            }
        }
    }

    /**
     * Subclasses should override this method in order to react to main vehicle change
     */
    public void mainVehicleChangeNotification(String id) {
        // nothing
    }

    /**
     * Send IMCMessage
     * 
     * @param destination
     * @param message
     * @return
     */
    public boolean send(String destination, IMCMessage message) {
        if (destination == null || destination.isEmpty()) {
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + ": destination null or empty");
            return false;
        }

        try {
            if (!ImcMsgManager.getManager().sendMessageToSystem(message, destination)) {
                NeptusLog.pub().error(
                        ReflectionUtil.getCallerStamp() + ": " + "Error while communicating with " + destination + ".");
                return false;
            }
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
            return false;
        }
        return true;
    }

    /**
     * Send IMCMessage to Main System
     * 
     * @param message
     * @return
     */
    public boolean send(IMCMessage message) {
        String destination = getConsole().getMainSystem();
        return send(destination, message);
    }

    /**
     * @return the mainVehicleId
     */
    public String getMainVehicleId() {
        return mainVehicleId;
    }
    
    /**
     * Creates and retrieves a console menu item
     * 
     * @param itemPath The path to the menu item separated by ">". Examples: <li>
     *            <b>"Tools > Local Network > Test Network"</b> <li><b>"Tools>Test Network"</b>
     * @param icon The icon to be used in the menu item. <br>
     *            Size is automatically adjusted to 16x16 pixels.
     * @param actionListener The {@link ActionListener} that will be warned on menu activation
     * @return The created {@link JMenuItem} or <b>null</b> if an error as occurred.
     */
    public JMenuItem addMenuItem(String itemPath, ImageIcon icon, ActionListener actionListener) {
        String[] ptmp = itemPath.split(">");
        if (ptmp.length < 2) {
            NeptusLog.pub().error("Menu path has to have at least two components");
            return null;
        }

        String[] path = new String[ptmp.length - 1];
        System.arraycopy(ptmp, 0, path, 0, path.length);

        String menuName = ptmp[ptmp.length - 1];

        JMenu menu = getConsole().getOrCreateJMenu(path);

        final ActionListener l = actionListener;
        AbstractAction action = new AbstractAction(menuName) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                l.actionPerformed(e);
            }
        };
        if (icon != null)
            action.putValue(AbstractAction.SMALL_ICON, ImageUtils.getScaledIcon(icon.getImage(), 16, 16));

        JMenuItem item = menu.add(action);
        addedMenus.add(itemPath);
        return item;
    }

    public JMenu addMenu(String itemPath, ImageIcon icon, ActionListener actionListener) {
        String[] ptmp = itemPath.split(">");
        if (ptmp.length < 1) {
            NeptusLog.pub().error("Menu path has to have at least one component");
            return null;
        }

        String[] path = new String[ptmp.length - 1];
        System.arraycopy(ptmp, 0, path, 0, path.length);

        JMenu menu = getConsole().getOrCreateJMenu(path);
        addedMenus.add(itemPath);
        return menu;
    }


    /**
     * Creates and retrieves a console check menu item (toggle)
     * 
     * @param itemPath The path to the menu item separated by ">". Examples: <li>
     *            <b>"Tools > Local Network > Test Network"</b> <li><b>"Tools>Test Network"</b>
     * @param icon The icon to be used in the menu item. <br>
     *            Size is automatically adjusted to 16x16 pixels.
     * @param actionListener The {@link CheckMenuChangeListener} that will be warned on menu selection changes
     * @return The created {@link JCheckMenuItem} or <b>null</b> if an error as occurrred.
     */
    public JCheckBoxMenuItem addCheckMenuItem(String itemPath, ImageIcon icon, CheckMenuChangeListener checkListener) {
        String[] ptmp = itemPath.split(">");
        if (ptmp.length < 2) {
            NeptusLog.pub().error("Menu path has to have at least two components");
            return null;
        }

        String[] path = new String[ptmp.length - 1];
        System.arraycopy(ptmp, 0, path, 0, path.length);

        String menuName = ptmp[ptmp.length - 1];

        JMenu menu = getConsole().getOrCreateJMenu(path);

        final CheckMenuChangeListener l = checkListener;
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(new AbstractAction(menuName, icon == null ? null
                : ImageUtils.getScaledIcon(icon.getImage(), 16, 16)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((JCheckBoxMenuItem) e.getSource()).getState())
                    l.menuChecked(e);
                else
                    l.menuUnchecked(e);
            }
        });
        menu.add(item);
        addedMenus.add(itemPath);
        return item;
    }

    /**
     * 
     */
    public void removeMenuItem(String itemPath) {
        addedMenus.remove(itemPath);
        JMenu parent = getConsole().removeMenuItem(itemPath.split(">"));
        // if parent became empty, remove parent
        if (parent != null && parent.getItemCount() == 0)
            getConsole().removeMenuItem(itemPath.substring(0, itemPath.lastIndexOf(">")).split(">"));
    }

    public ImcSysState getState() {
        return ImcMsgManager.getManager().getState(getConsole().getMainSystem());
    }

    public void removeCheckMenuItem(String itemPath) {
        removeMenuItem(itemPath);
    }


    public abstract void initSubPanel();
    public abstract void cleanSubPanel();
}
