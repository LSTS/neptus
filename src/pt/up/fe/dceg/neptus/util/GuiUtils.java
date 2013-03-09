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
 * 24/Fev/2005
 */
package pt.up.fe.dceg.neptus.util;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.ErrorMessageBox;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.l2fprod.common.swing.BaseDialog;

/**
 * @author Ze Carlos
 * @author Paulo Dias
 */
public class GuiUtils {

    public static String CLIP_ERROR = "/sounds/error.wav";
    public static String CLIP_WARNING = "/sounds/warning.wav";
    public static String CLIP_COMPLETE = "/sounds/complete.wav";
    public static String CLIP_SNAPSHOT = "/sounds/snapshot.wav";

    public static NumberFormat getNeptusDecimalFormat() {
        NumberFormat df = DecimalFormat.getInstance(Locale.US);
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(15);
        return df;
    }

    public static NumberFormat getNeptusIntegerFormat() {
        NumberFormat df = DecimalFormat.getInstance(Locale.US);
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        df.setParseIntegerOnly(true);
        return df;
    }

    public static Font getFont(InputStream is, int size) {

        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        }
        catch (Exception e) {
            e.printStackTrace();
            font = new Font("serif", Font.PLAIN, size);
        }

        return font.deriveFont((float) size);
    }

    static Hashtable<Integer, NumberFormat> nformats = new Hashtable<Integer, NumberFormat>();

    public static NumberFormat getNeptusDecimalFormat(int fractionDigits) {

        if (nformats.containsKey(fractionDigits))
            return nformats.get(fractionDigits);

        NumberFormat df = getNeptusDecimalFormat();
        df.setMaximumFractionDigits(fractionDigits);
        df.setMinimumFractionDigits(fractionDigits);
        nformats.put(fractionDigits, df);
        return df;
    }

    /**
     * Given an absolute coordinate on screen, returns the bounds of the screen that contains such point
     * 
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @return The position and dimension of the display that holds the given position on screen
     */
    public static Rectangle getScreenBounds(int x, int y) {
        Rectangle[] bounds = getDisplayBounds();
        for (Rectangle b : bounds) {
            if (x >= b.getMinX() && x < b.getMaxX() && y >= b.getMinY() && y < b.getMaxY())
                return b;
        }
        NeptusLog.pub().error("Error determinig the screen for the coordidate " + x + "," + y);
        return bounds[0];
    }

    /**
     * This method returns the bounds of the monitors available in the system, sorted
     * 
     * @return The various bounds sorted first by vertical position and then horizontal position (same as comic books)
     */
    public static Rectangle[] getDisplayBounds() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Rectangle[] bounds = new Rectangle[gs.length];
        for (int j = 0; j < gs.length; j++)
            bounds[j] = gs[j].getDefaultConfiguration().getBounds();

        Arrays.sort(bounds, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle o1, Rectangle o2) {
                int diff = (o1.y - o2.y);
                if (o1.y != o2.y)
                    return diff;
                else
                    return (o1.x - o2.x);
            }
        });
        return bounds;
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be centered in the user screen
     */
    public static void centerOnScreenOld(Window window) {
        Dimension size = window.getSize();
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();

        // java.awt.GraphicsConfiguration.getBounds
        // java.awt.GraphicsDevice.getDisplayMode
        // java.awt.GraphicsEnvironment.isHeadless

        int x = (scr.width - size.width) / 2;
        int y = (scr.height - size.height) / 2;
        window.setBounds(x, y, size.width, size.height);
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be centered in the user screen
     */
    public static void centerOnScreen(Window window) {
        window.setLocationRelativeTo(null);
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be at right top corner on user screen
     */
    public static void rightTopScreen(Window window) {
        Dimension size = window.getSize();
        Point pt = window.getLocation();
        Rectangle bounds = getScreenBounds(pt.x, pt.y);

        int x = ((int) bounds.getMaxX() - size.width);
        window.setBounds(x, bounds.y, size.width, size.height);
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be at right center corner on user screen
     */
    public static void rightCenterScreen(Window window) {
        Dimension size = window.getSize();
        Point pt = window.getLocation();
        Rectangle bounds = getScreenBounds(pt.x, pt.y);

        int x = ((int) bounds.getMaxX() - size.width);
        int y = bounds.y + (bounds.height - size.height) / 2;
        window.setBounds(x, y, size.width, size.height);
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be at left center corner on user screen
     */
    public static void leftCenterScreen(Window window) {
        Dimension size = window.getSize();
        Point pt = window.getLocation();
        Rectangle bounds = getScreenBounds(pt.x, pt.y);
        // int x = (scr.width - size.width);
        int y = bounds.y + (bounds.height - size.height) / 2;
        window.setBounds(bounds.x, y, size.width, size.height);
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be at left bottom corner on user screen
     */
    public static void leftBottomScreen(Window window) {
        Dimension size = window.getSize();
        Point pt = window.getLocation();
        Rectangle bounds = getScreenBounds(pt.x, pt.y);
        int x = bounds.x;
        int y = bounds.y + (bounds.height - size.height);
        window.setBounds(x, y, size.width, size.height);
    }

    /**
     * Centers the given <b>Frame</b> in the user screen
     * 
     * @param window A Frame to be at left top corner on user screen
     */
    public static void leftTopScreen(Window window) {
        Dimension size = window.getSize();
        Point pt = window.getLocation();
        Rectangle bounds = getScreenBounds(pt.x, pt.y);
        int x = bounds.x;
        int y = bounds.y;
        window.setBounds(x, y, size.width, size.height);
    }

    /**
     * Place the given <b>Frame</b> at the bottom of parent window
     * 
     * @param window A Frame to be at left bottom corner of parent window
     */
    public static void bottomParent(Window window, Window parent) {
        Rectangle pos = parent.getBounds();
        Dimension size = window.getSize();
        Point pt = parent.getLocationOnScreen();
        Rectangle screenBounds = getScreenBounds(pt.x, pt.y);

        // Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (pos.x);
        int y = (pos.y + pos.height);
        if (y + size.height > screenBounds.getMaxY())
            window.setBounds(x, (int) screenBounds.getMaxY() - size.height, pos.width, size.height);
        else
            window.setBounds(x, y, pos.width, size.height);
    }

    /**
     * Place the given <b>Frame</b> at the bottom of parent window
     * 
     * @param window A Frame to be at the center of parent window
     */
    public static void centerParent(Window window, Window parent) {
        Rectangle pos = parent.getBounds();
        Rectangle size = window.getBounds();

        // Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (((pos.width) / 2) + pos.x - (size.width / 2));
        int y = (((pos.height) / 2) + pos.y - (size.height / 2));
        window.setBounds(x, y, size.width, size.height);
    }

    /**
     * 
     * @param win
     * @param parent
     */
    public static void southEastParent(Window window, Window parent, int maxWidth, int maxHeight) {
        Rectangle pos = parent.getBounds();

        Point pt = parent.getLocationOnScreen();
        Rectangle screenBounds = getScreenBounds(pt.x, pt.y);

        int x = (pos.x + pos.width);
        int y = (pos.y + pos.height);
        int sizex = (int) screenBounds.getMaxX() - x;
        int sizey = (int) screenBounds.getMaxY() - y;
        if (sizex > maxWidth)
            sizex = maxWidth;
        if (sizey > maxHeight)
            sizey = maxHeight;

        // FIXME we shouldn't have hard-coded maximum sizes...
        if (sizex < 200) // 200 min size X
        {
            sizex += 200;
            x -= 200;
        }
        if (sizey < 100) // 100 min size y
        {
            sizey += 100;
            y -= 100;
        }
        if (x > (int) screenBounds.getMaxX())
            x -= 200;

        if (y > (int) screenBounds.getMaxY())
            x -= 100;

        window.setBounds(x, y, sizex, sizey);
    }

    /**
     * Asks the user for an identifier, returning it.
     * 
     * @param unavailableIDs An array with corrently taken identifiers
     * @return The entered identifier or <b>null</b> if the user pressed "cancel"
     */
    public static String idSelector(Object[] unavailableIDs, String defaultValue) {

        boolean ok = false;
        String id = null;
        JFrame tmp = new JFrame();

        while (!ok || id == null) {

            id = JOptionPane.showInputDialog(tmp, "Enter an identifier", defaultValue);

            if (id != null)
                ok = true;
            else
                return null;

            if (!NameNormalizer.isNeptusValidIdentifier(id)) {
                errorMessage(null, "Not a valid ID", "The entered ID is not valid");
                // JOptionPane.showMessageDialog(tmp,
                // "The entered ID is invalid");
                ok = false;
                continue;
            }

            for (int i = 0; i < unavailableIDs.length; i++) {
                if (id.equals(unavailableIDs[i])) {
                    errorMessage(null, "Not a valid ID", "The entered ID is already in use");
                    ok = false;
                }
            }
        }
        return id;
    }

    private static Color getMixedColor(Color c1, float pct1, Color c2, float pct2) {
        float[] clr1 = c1.getComponents(null);
        float[] clr2 = c2.getComponents(null);
        for (int i = 0; i < clr1.length; i++) {
            clr1[i] = (clr1[i] * pct1) + (clr2[i] * pct2);
        }
        return new Color(clr1[0], clr1[1], clr1[2], clr1[3]);
    }

    public static void paintBorderShadow(Graphics2D g2, int shadowWidth, Color background, Shape shape) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int sw = shadowWidth * 2;
        for (int i = sw; i >= 2; i -= 2) {
            float pct = (float) (sw - i) / (sw - 1);
            g2.setColor(getMixedColor(Color.LIGHT_GRAY, pct, background, 1.0f - pct));
            g2.setStroke(new BasicStroke(i));
            g2.draw(shape);
        }
    }

    public static void paintBorderShadow(Graphics2D g2, int shadowWidth, Shape shape) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color background = new Color(0, 0, 0, 0); // transparent color
        paintBorderShadow(g2, shadowWidth, background, shape);
    }

    public static JFrame testFrame(JComponent component) {
        return testFrame(component, "testing " + component.getClass().getSimpleName());
    }

    public static JFrame testFrame(JComponent component, String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(component, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(width, height);
        centerOnScreen(frame);
        frame.setVisible(true);
        return frame;
    }

    // public static ConsoleLayout testSubPanel(Class<?> subPanelClass) throws
    // Exception {
    // ConfigFetch.initialize();
    // GuiUtils.setLookAndFeel();
    //
    // ConsoleLayout cl = new ConsoleLayout();
    // cl.setVisible(true);
    // cl.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //
    // SubPanel panel =
    // (SubPanel)subPanelClass.getConstructor(MainPanel.class).newInstance(cl.getMainPanel());
    // panel.setBounds(5,5,cl.getMainPanel().getWidth()-10,
    // cl.getMainPanel().getHeight()-10);
    // //cl.getMainPanel().setAdding(panel);
    // cl.getMainPanel().add(panel, BorderLayout.CENTER);
    //
    // cl.getMainPanel().invalidate();
    // cl.getMainPanel().revalidate();
    // return cl;
    // }

    public static JFrame testFrame(JComponent component, String title) {
        return testFrame(component, title, 320, 240);
    }

    /**
     * @return The bounds of the default graphics device (screen)
     */
    public static Rectangle getDefaultScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
                .getBounds();
    }

    /**
     * Given a file wich is an AudioClip (valid formats are .wav and .au), this method will play the clip
     * 
     * @param clipFilename The audioclip to be played
     */
    public static void playAudioClip(String clipFilename) {

        Clip m_clip;

        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new FileInputStream(clipFilename));

            if (audioInputStream != null) {
                AudioFormat format = audioInputStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                m_clip = (Clip) AudioSystem.getLine(info);
                m_clip.open(audioInputStream);
                m_clip.start();
            }
            else {
                NeptusLog.pub().error("ClipPlayer.<init>(): can't get data from file " + clipFilename);
            }
        }
        catch (LineUnavailableException lineException) {
            Toolkit.getDefaultToolkit().beep();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    /**
     * Use this instead of JOptionPane.showMessageDialog(..., JOptionPane.INFORMATION_MESSAGE)
     * @param owner
     * @param title
     * @param message
     */
    public static void infoMessage(Component owner, String title, String message) {
        // JOptionPane.showMessageDialog(owner, message, title,
        // JOptionPane.INFORMATION_MESSAGE);
        JOptionPane jop = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = jop.createDialog(owner, title);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);
    }

    /**
     * Use this instead of JOptionPane.showMessageDialog(..., JOptionPane.QUESTION_MESSAGE)
     * @param owner
     * @param title
     * @param message
     * @return {@link JOptionPane#YES_OPTION}, {@link JOptionPane#NO_OPTION}, or {@link JOptionPane#CLOSED_OPTION}
     */
    public static int confirmDialog(Component owner, String title, String message) {
        // int response = JOptionPane.showConfirmDialog(owner, message, title, JOptionPane.YES_NO_OPTION);
        // return response; // == JOptionPane.YES_OPTION;
        
        JOptionPane jop = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = jop.createDialog(owner, title);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);
        Object selectedValue = jop.getValue();
        if(selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        if(selectedValue instanceof Integer)
            return ((Integer)selectedValue).intValue();
        return JOptionPane.CLOSED_OPTION;
    }

    /**
     * Use this instead of JOptionPane.showMessageDialog(..., JOptionPane.ERROR_MESSAGE)
     * @param owner
     * @param title
     * @param message
     */
    public static void errorMessage(Component owner, String title, String message) {
        // JOptionPane.showMessageDialog(owner, message, title,
        // JOptionPane.ERROR_MESSAGE);
        JOptionPane jop = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = jop.createDialog(owner, title);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);
        NeptusLog.pub().error("[ErrorMessage] " + message);
    }

    public static void errorMessage(Component owner, Exception e) {
        // String message =
        // "<html><b>"+e.getClass().getSimpleName()+"</b><br><br>"+
        // " &nbsp; &nbsp; <b>Cause:</b> "+e.getCause()+"<br>"+
        // " &nbsp; &nbsp; <b>Message:</b> "+e.getMessage()+"</html>";

        // JOptionPane.showMessageDialog(owner, message, "Exception thrown",
        // JOptionPane.ERROR_MESSAGE);

        ErrorMessageBox.showDialog(owner, e);
    }

    /**
     * @see #errorMessage(Component, String, String)
     */
    public static void errorMessage(String title, String message) {
        // JOptionPane.showMessageDialog(ConfigFetch.getSuperParentFrame(),
        // message, title, JOptionPane.ERROR_MESSAGE);
        errorMessage(ConfigFetch.getSuperParentFrame(), title, message);
    }

    public static JDialog htmlMessage(Component owner, String title, String subtitle, String htmlMessage) {
        BaseDialog myDialog;
        if (owner instanceof Frame)
            myDialog = new BaseDialog((Frame) owner);
        else
            myDialog = new BaseDialog((Frame) SwingUtilities.getWindowAncestor(owner));

        JEditorPane editorPane = new JEditorPane("text/html", htmlMessage);
        editorPane.setEditable(false);

        myDialog.getContentPane().setLayout(new BorderLayout());
        myDialog.getContentPane().add(new JScrollPane(editorPane));
        editorPane.setCaretPosition(0);

        ((JButton) myDialog.getButtonPane().getComponent(0)).setMinimumSize(new Dimension(90, 25));
        ((JButton) myDialog.getButtonPane().getComponent(0)).setPreferredSize(new Dimension(90, 25));
        myDialog.getButtonPane().remove(1);
        myDialog.setSize(500, 400);
        myDialog.setTitle(title);
        myDialog.getBanner().setTitle(title);
        myDialog.getBanner().setSubtitle(subtitle);
        myDialog.getBanner().setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        centerOnScreen(myDialog);
        // myDialog.setModal(true);
        myDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        myDialog.setVisible(true);
        return myDialog;
    }

    public static boolean generateCompositeScreenShot(Component parentComp, Component childComp) {
        Rectangle parentBounds = parentComp.getBounds();
        Rectangle childBounds = childComp.getBounds();
        Component rootParent = SwingUtilities.getRoot(parentComp);
        Component rootChild = SwingUtilities.getRoot(childComp);

        while (childComp != rootChild) {
            childComp = childComp.getParent();
            childBounds.x += childComp.getBounds().x;
            childBounds.y += childComp.getBounds().y;
        }

        while (parentComp != rootParent) {
            parentComp = parentComp.getParent();
            parentBounds.x += parentComp.getBounds().x;
            parentBounds.y += parentComp.getBounds().y;
        }

        // System.out.println(childComp.getBounds());

        // childBounds.x += parentBounds.x;
        // childBounds.y += parentBounds.y;

        File fx1 = new File(ConfigFetch.resolvePath("log"));
        File fx = new File(fx1.getAbsoluteFile() + "/images");
        fx.mkdirs();
        String outFileName = fx.getAbsolutePath() + "/" + new Date().getTime() + ".png";

        try {
            Robot robot = new Robot();
            BufferedImage parentImage = robot.createScreenCapture(parentBounds);
            robot = new Robot();
            BufferedImage childImage = robot.createScreenCapture(childBounds);
            Graphics2D g2d = (Graphics2D) parentImage.getGraphics();

            int parentWidth = parentImage.getWidth();
            int childWidth = childImage.getWidth();

            double scale = (parentWidth * 0.3) / childWidth;

            int xpos = (int) (parentImage.getWidth() - 10 - childImage.getWidth() * scale);
            int ypos = (int) (parentImage.getHeight() - 10 - childImage.getHeight() * scale);

            AffineTransform trans = AffineTransform.getTranslateInstance(xpos, ypos);
            trans.scale(scale, scale);

            g2d.drawImage(childImage, trans, null);

            g2d.setColor(Color.black);
            g2d.drawRect(xpos, ypos, (int) (childImage.getWidth() * 0.3), (int) (childImage.getHeight() * 0.3));

            ImageIO.write(parentImage, "png", new File(outFileName));
            // give feedback
            NeptusLog.pub().info(
                    "Saved screen shot (" + parentImage.getWidth() + " x " + parentImage.getHeight()
                            + " pixels) to file \"" + outFileName + "\".");
        }
        catch (Exception e) {
            NeptusLog.pub().error("generateCompositeScreenShot()", e);
            return false;
        }

        NeptusLog.pub().info("Composite screen shot");
        return true;

    }

    public static void takeSnapshot(Component componentToGrab, String prefix) {
        String filename = getLogFileName(prefix, "png");
        Rectangle rect = componentToGrab.getBounds();
        Component rootComp = SwingUtilities.getRoot(componentToGrab);

        while (componentToGrab != rootComp) {
            componentToGrab = componentToGrab.getParent();
            rect.x += componentToGrab.getBounds().x;
            rect.y += componentToGrab.getBounds().y;
        }

        Robot robot = null;
        try {
            robot = new Robot();
        }
        catch (AWTException e) {
            NeptusLog.pub().error("error taking snapshot", e);
        }

        BufferedImage image = robot.createScreenCapture(rect);
        // image.getGraphics().draw... //desenhar por cima do screenshot
        // save captured image to PNG file
        try {
            ImageIO.write(image, "png", new File(filename));
            NeptusLog.pub().debug(filename);
        }
        catch (IOException e) {
            NeptusLog.pub().error("error taking snapshot", e);
        }
    }
    
    /**
     * Get default full path for a log file, this goes to the images folder
     * @param extension
     * @return
     */
    public static String getLogFileName(String extension) {
        return getLogFileName("", extension);
    }
    
    /**
     * Get full path for a log
     * @param prefix some defaults map directly to a folder ( sent_rmf, mission_state, output ) everything else to images folder
     * @param extension
     * @return
     */
    public static String getLogFileName(String prefix, String extension) {
        if (prefix == null)
            prefix = "";
        
        prefix = prefix.toLowerCase();

        File fx1 = new File("log");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS");

        File fx = null;
        if (prefix.equalsIgnoreCase("sent_plans")) {
            fx = new File(fx1.getAbsoluteFile() + ConfigFetch.DS + "sent_rmf");
            fx.mkdirs();
        }
        else if (prefix.equalsIgnoreCase("mission_state") || prefix.equalsIgnoreCase("mission") || prefix.equalsIgnoreCase("mission_")) {
            fx = new File(fx1.getAbsoluteFile() + ConfigFetch.DS + "mission_state");
            fx.mkdirs();
            prefix = "mission_state";
        }
        else if (prefix.equalsIgnoreCase("output")) {
            fx = new File(fx1.getAbsoluteFile() + ConfigFetch.DS + "output");
            fx.mkdirs();
        }
        else {
            fx = new File(fx1.getAbsoluteFile() + ConfigFetch.DS + "images");
            fx.mkdirs();
        }

        return fx.getAbsolutePath() + ConfigFetch.DS + prefix + "-" + sdf.format(new Date()) + "." + extension;
    }

    public static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {

        }
    }

    /**
	 * 
	 */
    public static void setLookAndFeel() {
        // Tries to change the look and feel to an improved one
        PlasticLookAndFeel.setPlasticTheme(new com.jgoodies.looks.plastic.theme.SkyBlue());
        try {

            UIManager.put("ClassLoader", LookUtils.class.getClass().getClassLoader());

            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            NeptusLog.pub().error("SetLookandFeel " + e.getMessage());
        }
    }

    public static void setLookAndFeelNimbus() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                System.out.println(info.getName());
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
    }

    public static FileFilter getCustomFileFilter(String desc, String[] validExtensions) {
        final String d = desc;
        final String[] ext = validExtensions;
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;
                // String extension = FileUtil.getFileExtension(f);
                for (String e : ext) {
                    if (/* e.equalsIgnoreCase(extension) */f.getName().toLowerCase().endsWith("." + e.toLowerCase())
                            || e.equals("*"))
                        return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return d;
            }
        };
    }

    public static void reactEnterKeyPress(JButton btn) {
        btn.registerKeyboardAction(btn.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_IN_FOCUSED_WINDOW);

        btn.registerKeyboardAction(btn.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void reactEscapeKeyPress(JButton btn) {
        btn.registerKeyboardAction(btn.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), JComponent.WHEN_IN_FOCUSED_WINDOW);

        btn.registerKeyboardAction(btn.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void printArray(Object[] array) {
        if (array == null)
            System.out.println(array);

        System.out.println(array.getClass().getSimpleName() + "[" + array.length + "] {");
        for (int i = 0; i < array.length; i++) {
            System.out.println("\t(" + i + ") " + array[i].toString());
        }
        System.out.println('}');
    }

    public static void printList(List<?> list) {
        if (list == null)
            System.out.println(list);

        System.out.println(list.getClass().getSimpleName() + "[" + list.size() + "] {");
        for (int i = 0; i < list.size(); i++) {
            System.out.println("\t(" + i + ") " + list.get(i).toString());
        }
        System.out.println('}');
    }

    /**
     * Verifies if the given image as any translucent pixels
     * 
     * @param image An Image
     * @return <b>true</b> if, and only if, a translucent pixel as been found in the image
     */
    public static boolean hasAlpha(Image image) {
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        }
        catch (InterruptedException e) {

        }

        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    /**
     * Given an image, produces a BufferedImage
     * 
     * @param image An Image
     * @return A BufferedImage with the same contents as <b>image</b>
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        image = new ImageIcon(image).getImage();
        boolean hasAlpha = hasAlpha(image);

        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {

            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        }
        catch (HeadlessException e) {

        }

        if (bimage == null) {
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        Graphics g = bimage.createGraphics();

        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    /**
     * This method explicitly calls the Garbage Collector and returns the freed bytes
     * 
     * @return The amount of freed memory (in bytes)
     */
    public static long callGC() {
        Runtime rt = Runtime.getRuntime();
        long before = rt.freeMemory();
        rt.gc();
        return rt.freeMemory() - before;
    }

    /**
     * Given an Image, returns another image with applied transparency
     * 
     * @param original The image to be processed
     * @param transparency The amount of opaqueness in the image (0.0 - completely translucent, 1.0 - opaque)
     * @return The processed Image
     */
    public static Image applyTransparency(Image original, float transparency) {
        BufferedImage img = new BufferedImage(original.getWidth(null), original.getHeight(null),
                BufferedImage.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return img;
    }

    protected static TrayIcon icon = null;

    public static boolean showInfoPopup(String title, String message) {
        if (SystemTray.isSupported()) {
            if (icon == null) {
                try {
                    icon = new TrayIcon(ImageUtils.getImage("images/neptus-icon.png"));
                    SystemTray.getSystemTray().add(icon);
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            SystemTray.getSystemTray().remove(icon);
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            icon.displayMessage(title, message, MessageType.INFO);
            return true;
        }
        return false;
    }

    public static boolean showErrorPopup(String title, String message) {
        if (SystemTray.isSupported()) {
            if (icon == null) {
                icon = new TrayIcon(ImageUtils.getImage("images/neptus-icon.png"));
                try {
                    SystemTray.getSystemTray().add(icon);
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            SystemTray.getSystemTray().remove(icon);
                        }
                    });
                }
                catch (Exception e) {
                    return false;
                }
            }
            icon.displayMessage(title, message, MessageType.ERROR);
            return true;
        }
        return false;
    }

    /**
     * This method generates an icon which is a circle (background) with a letter showing on top
     * 
     * @param letter The letter of this icon
     * @param fgColor The Color to be used in the letter
     * @param bgColor The Color to be used in the background circle
     * @param size The width/height of the icon to be generated
     * @return The generated icon
     */
    public static ImageIcon getLetterIcon(Character letter, Color fgColor, Color bgColor, int size) {

        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(bgColor);
        g2d.fill(new Ellipse2D.Double(1, 1, size - 2, size - 2));

        g2d.setStroke(new BasicStroke(0.5f));
        g2d.setColor(Color.black);
        g2d.draw(new Ellipse2D.Double(1, 1, size - 2, size - 2));
        g2d.setFont(new Font("Helvetica", Font.BOLD, size));
        int width = g2d.getFontMetrics().charWidth(letter);

        g2d.setColor(new Color(0, 0, 0, 64));
        g2d.drawString(new String(new char[] { letter }), (size - width) / 2 + 3, size - 1);
        g2d.drawString(new String(new char[] { letter }), (size - width) / 2 + 2, size - 2);
        g2d.setColor(fgColor);
        g2d.drawString(new String(new char[] { letter }), (size - width) / 2 + 1, size - 3);
        return new ImageIcon(bi);
    }
    
    /**
     * Search a for a JMenu item in a JMenuBar comparing by name
     * @param bar 
     * @param menu
     * @return true if exists, false otherwise
     */
    public static boolean menuBarContainsMenu(JMenuBar bar, JMenu menu) {
        for(Component c : bar.getComponents()) {
            if(c instanceof JMenu) {
                if(((JMenu) c ).getText() == menu.getText()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static JMenu getJMenuByName(JMenuBar bar, String name) {
        for(Component c : bar.getComponents()) {
            if(c instanceof JMenu) {
                if(((JMenu) c ).getText() == name) {
                    return (JMenu)c;
                }
            }
        }
        return null;
    }
    /**
     * Unitary test.
     * 
     * @param args
     */
    public static void main(String args[]) {
        /*
         * String id = idSelector(new String[] { "ola", "ole" }, NameNormalizer .getRandomID()); System.out.println(id);
         * testFrame(new JLabel("dsdssdsdsdsdssssssssssssss")); GuiUtils.errorMessage(testFrame(new JLabel("dddddddd")),
         * "No more data to read", "Reached the end of stream. Cable unplugged?");
         */
//        testFrame(new JLabel(getLetterIcon('R', Color.white, Color.blue.darker(), 22)));

        System.out.println(getLogFileName("mission_state", "zip"));
    }
}
