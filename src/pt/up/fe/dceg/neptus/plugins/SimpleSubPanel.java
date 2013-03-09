/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.plugins;

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
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.state.ImcSysState;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.planning.MapPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.plugins.update.PeriodicUpdatesService;
import pt.up.fe.dceg.neptus.renderer2d.CustomInteractionSupport;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

abstract public class SimpleSubPanel extends SubPanel implements MessageListener<MessageInfo, IMCMessage>,
        MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;
    private boolean visibility = true;
    private final Vector<String> addedMenus = new Vector<String>();
    private final Vector<Integer> messagesToListen = new Vector<Integer>();
    private String mainVehicleId = null;
    private JMenuItem menuItem = null;
    protected JDialog dialog = null;

    public SimpleSubPanel(ConsoleLayout console) {
        super(console);
        // this.setSize(32, 32);
        // JLabel lbl = new JLabel(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 20, 20),
        // JLabel.CENTER);
        // lbl.setText(this.getName());
        // add(lbl, BorderLayout.CENTER);
    }

    /**
     * @return the mainVehicleId
     */
    public String getMainVehicleId() {
        return mainVehicleId;
    }

    /**
     * If you need to react to main vehicle change override {@link #mainVehicleChangeNotification(String)} instead.
     * 
     * @see pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener#mainVehicleChange(java.lang.String) The
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

    @Override
    public void setEditMode(boolean b) {
        super.setEditMode(b);
        if (!editmode && !visibility)
            setVisible(false);
        else
            setVisible(true);
    }

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x / 3 * 3, y / 3 * 3);
    }

    @Override
    public void XML_PropertiesRead(Element e) {
        PluginUtils.setConfigXml(this, e.asXML());
    }

    @Override
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

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return PluginUtils.getPluginName(this.getClass()) + " parameters";
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

    @Override
    public String getName() {
        return PluginUtils.getPluginName(this.getClass());
    }

    @Override
    public String getDescription() {
        return PluginUtils.getPluginDescription(this.getClass());
    }

    @Override
    public ImageIcon getImageIcon() {
        return ImageUtils.getIcon(PluginUtils.getPluginIcon(this.getClass()));
    }

    /**
     * If you need to do any initializations, please override {@link #initSubPanel()}
     * 
     * @see {@link SubPanel#init()}
     */
    @Override
    public final void init() {

        mainVehicleId = console.getMainSystem();

        if (this instanceof MissionChangeListener)
            getConsole().addMissionListener((MissionChangeListener) this);

        if (this instanceof MainVehicleChangeListener || this instanceof NeptusMessageListener)
            getConsole().addMainVehicleListener(this);

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
            // System.out.println("Adding myself as message listener");
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
                // System.out.println(getName()+ "listening to "+messagesToListen.size()+" message types");
            }

            if (getConsole() != null && !messagesToListen.isEmpty())
                ImcMsgManager.getManager().addListener(this, getConsole().getMainSystem());
            else {
                System.out.println("Console is null..." + this.getName());
            }
        }
    }

    private void buildPopup() {
        final Popup cAction = getClass().getAnnotation(Popup.class);
        if (cAction == null)
            return;
        final POSITION popupPosition = cAction.pos();
        // String name2 = cAction.name().isEmpty() ? PluginUtils.i18nTranslate(getName()) : I18n.text(cAction.name());
        String name2 = cAction.name().isEmpty() ? getName() : cAction.name();
        String iconPath = cAction.icon().isEmpty() ? PluginUtils.getPluginIcon(this.getClass()) : cAction.icon();
        int width = cAction.width();
        int height = cAction.height();
        KeyStroke accelerator = null;
        if (cAction.accelerator() != KeyEvent.VK_UNDEFINED) {
            accelerator = KeyStroke.getKeyStroke(cAction.accelerator(), KeyEvent.CTRL_DOWN_MASK);
        }
        // BUILD
        JMenu menu = console.getOrCreateJMenu(new String[] { I18n.text("View") });
        ImageIcon icon = ImageUtils.getIcon(iconPath);
        menuItem = createMenuItem(popupPosition, name2, icon);
        if (accelerator != null)
            menuItem.setAccelerator(accelerator);
        menu.add(menuItem);
        // Dialog
        dialog = new JDialog(console);
        dialog.setTitle(name2);
        dialog.setIconImage(icon.getImage());
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        dialog.setSize(width, height);
        dialog.setFocusable(true);
        if (accelerator != null) {
            JRootPane rootPane = dialog.getRootPane();
            InputMap globalInputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            globalInputMap.put(accelerator, "pressed");
            rootPane.getActionMap().put("pressed", new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
        }
        dialog.add(this);
    }

    protected JMenuItem createMenuItem(final POSITION popupPosition, String name2, ImageIcon icon) {
        JMenuItem menuItem = new JMenuItem(new AbstractAction(PluginUtils.i18nTranslate(name2),
                ImageUtils.getScaledIcon(
                icon, 16, 16)) {
            private static final long serialVersionUID = 1L;

            
            @Override
            public void actionPerformed(ActionEvent e) {
                setPopupPosition(popupPosition);
            }


        });
        return menuItem;
    }

    protected void setPopupPosition(final POSITION popupPosition) {
        dialog.setVisible(!dialog.isVisible());
        if (dialog.isVisible()) {
            dialog.requestFocus();
            Point p = console.getLocationOnScreen();
            switch (popupPosition) {
                case TOP_LEFT:
                    break;
                case TOP_RIGHT:
                    p.x += console.getWidth() - dialog.getWidth();
                    break;
                case BOTTOM_LEFT:
                    p.y += console.getHeight() - dialog.getHeight();
                    break;
                case BOTTOM_RIGHT:
                    p.x += console.getWidth() - dialog.getWidth();
                    p.y += console.getHeight() - dialog.getHeight();
                    break;
                case TOP:
                    p.x += console.getWidth() / 2 - dialog.getWidth() / 2;
                    break;
                case BOTTOM:
                    p.x += console.getWidth() / 2 - dialog.getWidth() / 2;
                    p.y += console.getHeight() - dialog.getHeight();
                    break;
                case LEFT:
                    p.y += console.getHeight() / 2 - dialog.getHeight() / 2;
                    break;
                case RIGHT:
                    p.x += console.getWidth() - dialog.getWidth();
                    p.y += console.getHeight() / 2 - dialog.getHeight() / 2;
                    break;

                default:
                    p.x += console.getWidth() / 2 - dialog.getWidth() / 2;
                    p.y += console.getHeight() / 2 - dialog.getHeight() / 2;
                    break;
            }
            dialog.setLocation(p);
        }
    }

    /**
     * Subclasses should override this method in order to do any initializations
     */
    abstract public void initSubPanel();

    /**
     * Subclasses should override this method in order to do any clean ups
     */
    abstract public void cleanSubPanel();

    /**
     * If you want to perform cleanups, use {@link #cleanSubPanel()}
     * 
     * @see pt.up.fe.dceg.neptus.console.SubPanel#clean()
     */
    @Override
    public final void clean() {
        super.clean();

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
        cleanSubPanel();
        if (menuItem != null || dialog != null) {
            JMenu menu = console.getOrCreateJMenu(new String[] { I18n.text("View") });
            menu.remove(menuItem);
            if (dialog.isVisible()){
                // 20130224 pdias - was JFrame.EXIT_ON_CLOSE but this triggers IllegalArgumentException and this value does not make sense
                dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                dialog.dispose();
            }
        }
    }

    /**
     * Don't override this method. (Implement NeptusMessageListener instead.)
     * 
     * @see pt.up.fe.dceg.neptus.messages.listener.MessageListener#onMessage(pt.up.fe.dceg.neptus.messages.listener.MessageInfo,
     *      pt.up.fe.dceg.neptus.messages.IMessage)
     */
    @Override
    public void onMessage(MessageInfo arg0, IMCMessage arg1) {
        if (messagesToListen.contains(arg1.getMessageType().getId())) {
            ((NeptusMessageListener) this).messageArrived(arg1);
        }
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

}
