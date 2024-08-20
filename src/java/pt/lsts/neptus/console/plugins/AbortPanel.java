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
 * Author: Paulo Dias
 * 14/10/2006
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXButton;

import pt.lsts.imc.Abort;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.swing.PanicButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.IAbortSenderProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.ColorUtils;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(icon = "images/buttons/important.png", name = "Abort Button", version = "1.6", documentation = "abort/abort-button.html")
public class AbortPanel extends ConsolePanel implements MainVehicleChangeListener, LockableSubPanel, ConfigurationListener {

    public enum AbortButtonShapeEnum { ROUND, RECTANGULAR };
    
    @NeptusProperty (name = "Button Shape", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public AbortButtonShapeEnum buttonShape = AbortButtonShapeEnum.ROUND;
    
    private PanicButton abortButton = null;
    private JXButton abortButtonRectangular = null;
    
    private ActionListener abortAction = null;
    
    private final String tooltip = I18n.text("Send abort to the main vehicle (Ctrl+click to send to all known vehicles)"); 

    public AbortPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        removeAll();

        this.setResizable(true);
        this.setSize(100, 100);
        this.setPreferredSize(new Dimension(100, 100));
        this.repaint();
        this.setLayout(new BorderLayout());
        this.add(getAbortButton(), BorderLayout.CENTER);
        
        propertiesChanged();
    }

    @Override
    public void propertiesChanged() {
        removeAll();
        switch (buttonShape) {
            case RECTANGULAR:
                this.add(getAbortButtonRectangular(), BorderLayout.CENTER);
                break;

            default:
                removeAll();
                this.add(getAbortButton(), BorderLayout.CENTER);
                break;
        }
        this.invalidate();
        this.revalidate();
        this.repaint(50);
    }

    /**
     * This method initializes abortButton
     * 
     * @return pt.lsts.neptus.gui.swing.PanicButton
     */
    private PanicButton getAbortButton() {
        if (abortButton == null) {
            abortButton = new PanicButton(I18n.textc("Abort", "Abort button. Make it with less letters as possible."));
            abortButton.setCircular(true);
            abortButton.addActionListener(getAbortAction());
            abortButton.setToolTipText(tooltip);
        }
        return abortButton;
    }

    /**
     * @return the abortButtonRectangular
     */
    public JXButton getAbortButtonRectangular() {
        if (abortButtonRectangular == null) {
            abortButtonRectangular = new JXButton(I18n.textc("Abort", "Abort button. Make it with less letters as possible.")) {
                private Paint[] paints = null; 
                private Dimension dim = null;
                private Color redTransp = ColorUtils.setTransparencyToColor(new Color(232,28,28), 210);
                private Color redTranspDarker = redTransp.darker();
                private BufferedImage buffImg = null;
                private boolean state = false;
                private boolean pressed = false;
                
                @Override
                protected void paintComponent(Graphics go) {
                    if (dim == null) {
                        Dimension dimN = getSize(new Dimension());
                        if (dimN.height != 0 && dimN.width != 0)
                            dim = dimN;
                        buffImg = null;
                    }
                    else if(!dim.equals(getSize())) {
                        paints = null;
                        dim = getSize(new Dimension());
                        buffImg = null;
                    }
                    
                    if (state != getModel().isEnabled()) {
                        state = getModel().isEnabled();
                        buffImg = null;
                    }
                    
                    if (pressed != getModel().isPressed()) {
                        pressed = getModel().isPressed();
                        buffImg = null;
                    }

                    if (paints == null)
                        paints = PanicButton.createStripesEnableDisablePaints(dim, getAbortButton().getBackgroundOutter());

                    // super.paintComponent(g);

                    Graphics2D g = (Graphics2D) go;
                    if (buffImg == null) {
                        // buffImg = new BufferedImage(dim.getWidth(), dim.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                        GraphicsDevice gs = ge.getDefaultScreenDevice();
                        GraphicsConfiguration gc = gs.getDefaultConfiguration();
                        buffImg = gc.createCompatibleImage((int) dim.getWidth(), (int) dim.getHeight(), Transparency.BITMASK); 
                        g = buffImg.createGraphics();
                        
                        Graphics2D g2 = (Graphics2D) g.create();
                        RoundRectangle2D rect = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setPaint(getModel().isEnabled() ? paints[0] : paints[1]);
                        g2.fill(rect);
                        g2.setColor(!getModel().isPressed() ? redTransp : redTranspDarker);
                        rect = new RoundRectangle2D.Double(10, 10, getWidth() - 20, getHeight() - 20, 10, 10);
                        g2.fill(rect);
                        g2.dispose();

                        g2 = (Graphics2D) g.create();
                        g2.setFont(new Font("Arial", Font.BOLD, 5));
                        Rectangle2D sB1 = g2.getFontMetrics().getStringBounds(getText(), g2);
                        double sw0 = (getWidth() - 20) / sB1.getWidth();
                        double sh0 = (getHeight() - 20) / sB1.getHeight();
                        g2.translate(10, 10);
                        double scale = Math.min(sw0, sh0);
                        g2.scale(scale, scale);
                        g2.setColor(getForeground());
                        sB1 = g2.getFontMetrics().getStringBounds(getText(), g2);
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2.drawString(getText(), (float) ((getWidth() - 20) / scale / 2d - sB1.getWidth() / 2d), 
                                (float) ((getHeight() - 20) / scale / 2d + sB1.getHeight() / 4d));
                        g2.dispose();
                    }

                    go.drawImage(buffImg, 0, 0, null);
                }
            };
            abortButtonRectangular.setForeground(getAbortButton().getBackgroundOutter());
            abortButtonRectangular.addActionListener(getAbortAction());
            abortButtonRectangular.setToolTipText(tooltip);
        }
        return abortButtonRectangular;
    }

    /**
     * @return
     */
    private ActionListener getAbortAction() {
        if (abortAction == null) {
            abortAction =  new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    abortButton.setEnabled(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            // send(new IMCMessage("Abort"));
                            IMCSendMessageUtils.sendMessage(new IMCMessage("Abort"), ImcMsgManager.TRANSPORT_TCP,
                                    createDefaultMessageDeliveryListener(), getConsole(), "", true,
                                    "", true, true, true,
                                    getConsole().getMainSystem());

                            boolean sentToAll = false;
                            if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
                                sentToAll = true;
                            }

                            Vector<String> systemsToAbort = new Vector<String>();
                            //systemsToAbort.add(getMainVehicleId());
                            if (sentToAll) {
                                ImcSystem[] allSys = ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.VEHICLE);
                                for (ImcSystem imcSystem : allSys) {
                                    if (!getMainVehicleId().equalsIgnoreCase(imcSystem.getName())) {
                                        send(imcSystem.getName(), new Abort());
                                        systemsToAbort.add(imcSystem.getName());
                                    }
                                }
                            }

                            boolean aSent = true;
                            try {
                                Vector<IAbortSenderProvider> newTrackers = getConsole().getSubPanelsOfInterface(
                                        IAbortSenderProvider.class);
                                for (IAbortSenderProvider t : newTrackers) {
                                    boolean sent = t.sendAbortRequest();
                                    aSent &= sent;
                                    for (String sysTA : systemsToAbort) {
                                        try {
                                            t.sendAbortRequest(sysTA);
                                        }
                                        catch (Exception e2) {
                                            NeptusLog.pub().error(e2);
                                        }
                                    }
                                }
                            }
                            catch (Exception ex) {
                                NeptusLog.pub().error(ex);
                            }
                            catch (Error ex) {
                                NeptusLog.pub().error(ex);
                            }
                            if (!aSent) {
                                // post(Notification.success("Accoustic abort sent !"));
                                // else
                                post(Notification.error(I18n.text("Abort"),
                                        I18n.text("Couldn't find a system to send an accoustic abort!")));
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            abortButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            };
        }
        return abortAction;
    }

    private MessageDeliveryListener createDefaultMessageDeliveryListener() {
        return new MessageDeliveryListener() {
            private String  getDest(IMCMessage message) {
                ImcSystem sys = message != null ? ImcSystemsHolder.lookupSystem(message.getDst()) : null;
                String dest = sys != null ? sys.getName() : I18n.text("unknown destination");
                return dest;
            }

            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery destination unreacheable",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery timeout",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery error. (%error)",
                                message.getAbbrev(), getDest(message), error)));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
            }
        };
    }

    @Override
    public boolean isLocked() {
        return !getAbortButton().isEnabled();
    }

    @Override
    public void lock() {
        if(abortButton != null)
            abortButton.setEnabled(false);
        if(abortButtonRectangular != null)
            abortButtonRectangular.setEnabled(false);
    }

    @Override
    public void unLock() {
        if(abortButton != null)
            abortButton.setEnabled(true);
        if(abortButtonRectangular != null)
            abortButtonRectangular.setEnabled(true);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
    
//    public static void main(String[] args) {
//        AbortPanel ap = new AbortPanel(new ConsoleLayout());
//        
//        GuiUtils.testFrame(ap);
//        
//        try { Thread.sleep(3000); } catch (InterruptedException e) { }
//        
//        ap.buttonShape = AbortButtonShapeEnum.RECTANGULAR;
//        ap.propertiesChanged();
//        
//        try { Thread.sleep(3000); } catch (InterruptedException e) { }
//        
//        ap.buttonShape = AbortButtonShapeEnum.ROUND;
//        ap.propertiesChanged();
//        
//        try { Thread.sleep(3000); } catch (InterruptedException e) { }
//        
//        ap.buttonShape = AbortButtonShapeEnum.RECTANGULAR;
//        ap.propertiesChanged();
//        
//    }
}
