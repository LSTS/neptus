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
 * 9/Abr/2006
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants.ColorConstants;

import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
public class ShellPanel extends JPanel {
    public static final Color IN = new Color(0, 200, 125); // Green mais visível
    public static final Color OUT = new Color(0, 0, 192); // Indigo
    public static final Color ERR = new Color(220, 0, 78); // Red mais visível
    public static final Color INFO = Color.GRAY;

    static final Color DEFAULT = Color.BLACK;
    static final Color ERROR = Color.RED;
    static final Color WARN = new Color(255, 180, 0); // ORANGE mais visível
    static final Color SENT = Color.BLUE;
    static final Color PING = new Color(193, 176, 135);

    public static final ImageIcon EDIT_COPY_ICON = new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png"));
    public static final ImageIcon EDIT_PASTE_ICON = new ImageIcon(ImageUtils.getImage("images/menus/editpaste.png"));
    public static final ImageIcon CLEAR_ICON = new ImageIcon(ImageUtils.getImage("images/buttons/clear.png")
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH));

    protected static final int MAX_TEXT_MSG_LENGHT = 500000;

    protected static final boolean HISTORY_UP = true;
    protected static final boolean HISTORY_DOWN = false;

    protected String shellPrompt = "$>";

    protected InputStream in;
    protected OutputStream out;
    protected InputStream err;

    protected OutputStream outIN;

    // protected PipedInputStream piOUT;
    // protected PipedOutputStream poOUT;

    protected String cmd = "";

    protected Vector<String> cmdHistory = new Vector<String>();
    protected short maxHistory = 15;
    protected short indexHistory = -1;

    protected boolean processing = false;
    protected boolean caretMoved = false;

    private JTextPane shellTextPane = null;
    private JScrollPane shellScrollPane = null;
    private JPopupMenu editPopupMenu = null; // @jve:decl-index=0:visual-constraint="257,28"
    private JMenuItem copyMenuItem = null;
    private JMenuItem pasteMenuItem = null;
    private JMenuItem clearMenuItem = null;

    /**
     * This is the default constructor
     */
    public ShellPanel() {
        super();
        initialize();
    }

    /**
     * @deprecated Cuidado com este método!! já q não faz grande sentido neste contexto (pelo menos assim).
     */
    public void prepareInStreams() {
        try {
            PipedInputStream ip = new PipedInputStream();
            PipedOutputStream op = new PipedOutputStream(ip);
            BufferedOutputStream or = new BufferedOutputStream(op);
            in = ip;
            outIN = or;
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * This method initializes shellTextPane
     * 
     * @return javax.swing.JTextPane
     */
    private JTextPane getShellTextPane() {
        if (shellTextPane == null) {
            shellTextPane = new JTextPane() {
                public boolean handleEvent(java.awt.Event evt) {
                    return false;
                };
            };

            shellTextPane.setEditable(false);
            shellTextPane.setFont(new Font("DialogInput", Font.PLAIN, 12));
            writeOut(shellPrompt);
            // shellTextPane.add(getEditPopupMenu());
            shellTextPane.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    // System.out.println("keyPressed() " + e.getKeyCode() + " :" + e.getKeyText(e.getKeyCode()));
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        // History previous
                        e.consume(); // Importante!!
                        if (processing)
                            return;
                        processing = true;
                        // System.out.println("UP | caret:" + caretMoved);
                        // System.err.println(">>" + KeyEvent.getKeyText(e.getKeyCode()));
                        processHistory(HISTORY_UP);
                        processing = false;
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        // History next
                        e.consume(); // Importante!!
                        if (processing)
                            return;
                        processing = true;
                        // System.out.println("DOWN | caret:" + caretMoved);
                        // System.err.println(">>" + KeyEvent.getKeyText(e.getKeyCode()));
                        processHistory(HISTORY_DOWN);
                        processing = false;
                    }
                }

                public void keyReleased(java.awt.event.KeyEvent e) {
                    // System.out.println("keyReleased()");
                }

                public void keyTyped(java.awt.event.KeyEvent e) {
                    if (processing)
                        return;
                    processing = true;

                    // System.out.println("keyTyped()" + (int)e.getKeyChar() + " " + e.getModifiers() + " " +
                    // e.getModifiersEx());
                    // System.out.println("keyTyped()" + Character.isDefined(e.getKeyChar()) +
                    // Character.isLetterOrDigit(e.getKeyChar()));
                    if (e.getKeyChar() == KeyEvent.VK_DELETE || e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                        if (!("".equals(cmd) || caretMoved == true))
                            deleteChar();
                    }
                    else if ((e.getKeyChar() == KeyEvent.VK_ENTER) && (e.getModifiers() == KeyEvent.SHIFT_MASK)) {
                        cmd += " ";
                        writeOut("\n");
                    }
                    else if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                        // writeOut("\n" + SHELL_PROMPT + cmd + "\n$>");
                        // writeOut("\n" + SHELL_PROMPT);
                        execute();
                        addToHistory(cmd);
                        cmd = "";
                        writeOut("\n" + shellPrompt);
                    }
                    else {
                        if (!caretMoved) {
                            writeIn("" + e.getKeyChar());
                            cmd += e.getKeyChar();
                        }
                        else {
                            cmd += e.getKeyChar();
                            writeOut("\n" + shellPrompt);
                            writeIn(cmd);
                            caretMoved = false;
                        }
                    }
                    processing = false;
                }
            });
            shellTextPane.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent e) {
                    if (processing)
                        return;
                    processing = true;
                    caretMoved = true;
                    // System.out.println("caretUpdate()");
                    processing = false;
                }
            });
            shellTextPane.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    // System.out.println("mouseReleased()"); // TODO Auto-generated Event stub mouseReleased()
                    caretMoved = false;
                }

                public void mousePressed(java.awt.event.MouseEvent e) {
                    // System.out.println("mousePressed()"); // TODO Auto-generated Event stub mousePressed()
                    caretMoved = false;
                }

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // System.out.println("mouseClicked() caret:" + caretMoved);
                    caretMoved = false;
                    if (e.getButton() == MouseEvent.BUTTON3 & e.getClickCount() == 1) {
                        if (getShellTextPane().isEnabled()) {
                            // getEditPopupMenu().show(ShellPannel.this,
                            // e.getX(), e.getY());

                            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, ShellPanel.this);
                            getEditPopupMenu().show(ShellPanel.this, me.getX(), me.getY());
                        }

                    }
                }
            });
        }
        return shellTextPane;
    }

    protected void addToHistory(String hCmd) {
        if (!hCmd.equalsIgnoreCase("")) {
            cmdHistory.add(0, hCmd);
            // System.err.println("History size: " + cmdHistory.size());
            if (cmdHistory.size() > maxHistory) {
                try {
                    cmdHistory.remove(maxHistory);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                }
            }
        }
        indexHistory = -1;
    }

    protected void processHistory(boolean historyDirection) {
        int indexMax = Math.min(maxHistory - 1, cmdHistory.size() - 1);
        int indexCur;

        int direction;
        if (historyDirection == HISTORY_UP)
            direction = 1;
        else
            direction = -1;
        if (cmdHistory.size() == 0)
            return;
        else {
            // indexCur = (indexHistory + direction) % (indexMax+1);
            indexCur = indexHistory + direction;
            if (indexCur < 0) {
                indexCur = 0;
                // return;
            }
            else if (indexCur > indexMax) {
                indexCur = indexMax;
                // return;
            }
            String hCmd = cmdHistory.get(indexCur);
            substituteCommand(hCmd);
            indexHistory = (short) indexCur;
        }
    }

    private void substituteCommand(String hCmd) {
        if (caretMoved) {
            writeOut("\n" + shellPrompt);
            writeIn(cmd);
            cmd = hCmd;
        }
        else {
            // é pq está na prompt o cmd original
            Document doc = getShellTextPane().getDocument();
            try {
                int docLenght = doc.getLength();
                int offset = docLenght - cmd.length();
                doc.remove(offset, cmd.length());
                int docLength = doc.getLength();
                if (docLength > MAX_TEXT_MSG_LENGHT)
                    doc.remove(0, docLength - MAX_TEXT_MSG_LENGHT);
                // System.err.println("Doc. lenght " + doc.getLength());
                getShellTextPane().setCaretPosition(doc.getLength());
                writeIn(hCmd);
                cmd = hCmd;
                caretMoved = false;
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * This method initializes shellScrollPane
     * 
     * @return javax.swing.JScrollPane
     */
    protected JScrollPane getShellScrollPane() {
        if (shellScrollPane == null) {
            shellScrollPane = new JScrollPane();
            shellScrollPane.setViewportView(getShellTextPane());
        }
        return shellScrollPane;
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        // this.setSize(300, 200);
        // this.setPreferredSize(new Dimension(300, 200));
        this.add(getShellScrollPane(), BorderLayout.CENTER);
    }

    public String getShellPrompt() {
        return shellPrompt;
    }

    public void setShellPrompt(String shellPrompt) {
        this.shellPrompt = shellPrompt;
    }

    public void clear() {
        Document doc = getShellTextPane().getDocument();
        try {
            doc.remove(0, doc.getLength());
            writeOut(shellPrompt);
        }
        catch (BadLocationException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
    }

    protected synchronized void writeMessageText(String message, Color type) {
        // getMsgTextArea().append(message);
        // getMsgTextArea().setCaretPosition(getMsgTextArea().getText().length());
        // SimpleAttributeSet attrTS = new SimpleAttributeSet();
        // attrTS.addAttribute(ColorConstants.Foreground, Color.DARK_GRAY);
        // attrTS.addAttribute(StyleConstants.Bold, true);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(ColorConstants.Foreground, type);
        Document doc = getShellTextPane().getDocument();
        try {
            doc.insertString(doc.getLength(), message, attr);
            int docLength = doc.getLength();
            if (docLength > MAX_TEXT_MSG_LENGHT)
                doc.remove(0, docLength - MAX_TEXT_MSG_LENGHT);
            // System.err.println("Doc. lenght " + doc.getLength());
            getShellTextPane().setCaretPosition(doc.getLength());
        }
        catch (Exception e) {
        }
    }

    protected void writeIn(String message) {
        writeMessageText(message, IN);
    }

    protected void writeOut(String message) {
        writeMessageText(message, OUT);
    }

    protected void writeErr(String message) {
        writeMessageText(message, ERR);
    }

    protected void writeInfo(String message) {
        writeMessageText(message, INFO);
    }

    protected synchronized void deleteChar() {
        Document doc = getShellTextPane().getDocument();
        // System.err.println(doc.getLength());
        if (cmd.length() == 0)
            return;
        if (doc.getLength() == 0)
            return;
        try {
            if (doc.getLength() >= 2) {
                String sd = doc.getText(doc.getLength() - 2, 2);
                if (sd.equals(shellPrompt)) {
                    System.err.println("#");
                    return;
                }
            }

            String charT = doc.getText(doc.getLength() - 1, 1);
            doc.remove(doc.getLength() - 1, 1);
            if (charT != null) {
                // Por causa do SHIFT+ENTER para dar um newLine
                if (charT.equalsIgnoreCase("\n")) {
                    // Mas como é inserido um ' ' no cmd por cada
                    // SHIFT+ENTER é necessário retirár.
                    // return;
                }
            }
            if (cmd.length() <= 1)
                cmd = "";
            else
                cmd = cmd.substring(0, cmd.length() - 1);
        }
        catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public boolean validateCommand() {
        return true;
    }

    protected synchronized void execute() {
        // writeOut(cmd);
        try {
            out.write(cmd.concat("\n").getBytes());
            // FIXME: (pdias) Devido a um BUG no Jsch1.30 sou obrigado a fazer flush
            out.flush();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
    }

    public void disable() {
        getShellTextPane().setEnabled(false);
    }

    public void enable() {
        getShellTextPane().setEnabled(true);
    }

    public InputStream getErr() {
        return err;
    }

    public void setErr(InputStream err) {
        this.err = err;
    }

    public InputStream getIn() {
        return in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public OutputStream getOut() {
        return out;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public void startInputProcess() {
        if (in == null)
            return;

        Timer runner = new Timer("Shell Panel");
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    byte[] tmp = new byte[1024];
                    while (true) {
                        while (in.available() > 0) {
                            int i = in.read(tmp, 0, 1024);
                            if (i < 0)
                                break;
                            // processing = true;
                            writeOut(new String(tmp, 0, i));
                            // processing = false;
                        }
                        try {
                            Thread.sleep(1000);
                        }
                        catch (Exception ee) {
                        }
                    }
                }
                catch (Exception e) {
                    // TODO: handle exception
                }
            };
        };

        runner.schedule(task, 10);
    }

    /**
     * This method initializes editPopupMenu
     * 
     * @return javax.swing.JPopupMenu
     */
    private JPopupMenu getEditPopupMenu() {
        if (editPopupMenu == null) {
            editPopupMenu = new JPopupMenu();
            editPopupMenu.add(getCopyMenuItem());
            editPopupMenu.add(getPasteMenuItem());
            editPopupMenu.addSeparator();
            editPopupMenu.add(getClearMenuItem());
        }

        return editPopupMenu;
    }

    /**
     * This method initializes copyMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getCopyMenuItem() {
        if (copyMenuItem == null) {
            copyMenuItem = new JMenuItem();
            copyMenuItem.setText("copy");
            copyMenuItem.setIcon(EDIT_COPY_ICON);
            copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    getShellTextPane().copy();
                }
            });
        }
        return copyMenuItem;
    }

    /**
     * This method initializes pastMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getPasteMenuItem() {
        if (pasteMenuItem == null) {
            pasteMenuItem = new JMenuItem();
            pasteMenuItem.setText("paste");
            pasteMenuItem.setIcon(EDIT_PASTE_ICON);
            pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int cIPos = -1, cFPos = -1;
                    processing = true;
                    getShellTextPane().setCaretPosition(getShellTextPane().getDocument().getLength());
                    cIPos = getShellTextPane().getCaretPosition();
                    writeIn("");
                    if (caretMoved) {
                        writeOut("\n" + shellPrompt);
                        writeIn(cmd);
                    }
                    getShellTextPane().setEditable(true);
                    getShellTextPane().paste();
                    getShellTextPane().setEditable(false);
                    cFPos = getShellTextPane().getCaretPosition();
                    getShellTextPane().setCaretPosition(cIPos);
                    getShellTextPane().moveCaretPosition(cFPos);
                    String sPast = getShellTextPane().getSelectedText();
                    if (sPast != null)
                        cmd += sPast;
                    processing = false;
                }
            });
        }
        return pasteMenuItem;
    }

    /**
     * This method initializes clearMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getClearMenuItem() {
        if (clearMenuItem == null) {
            clearMenuItem = new JMenuItem();
            clearMenuItem.setText("clear");
            clearMenuItem.setIcon(CLEAR_ICON);
            clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    clear();
                }
            });
        }
        return clearMenuItem;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // ConfigFetch.initialize();
        ShellPanel shellP = new ShellPanel();
        // shellP.prepareInStreams();
        shellP.setOut(System.out);
        shellP.setIn(System.in);
        shellP.startInputProcess();
        GuiUtils.setLookAndFeel();
        GuiUtils.testFrame(shellP, "ShellTest");

    }

}
