/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.util.editors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.swing.NeptusFileView;

/**
 * @version 0.1
 * @author Greg Solon (original author)
 * @author Paulo Dias
 */
public class TextEditor extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private Container pane;
    private JPanel south;
    private JFileChooser fileDialog;
    private File chosenFile;
    private String generalInfo;
    private JLabel infoLabel;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JMenuBar menuBar;
    private JMenu fileMenu, fontMenu, fontTypeMenu, fontSizeMenu, fontStyleMenu, fontColorMenu, helpMenu;
    private JMenuItem saveItem, saveAsItem, openItem, exitItem, aboutItem;
    private ButtonGroup fontTypeGroup, fontSizeGroup, fontColorGroup;
    private JRadioButtonMenuItem courierItem, dialogItem, timesItem, font10Item, font12Item, font14Item, font16Item,
            font18Item, blackItem, grayItem, redItem, blueItem;
    private JCheckBoxMenuItem italicItem, boldItem;
    // private boolean hasSaved;

    // undo helpers
    protected UndoAction undoAction;
    protected RedoAction redoAction;
    protected UndoManager undo = new UndoManager();

    public TextEditor() {
        super("Neptus Text Editor");

        String fi = new File("images/neptus-icon.png").getAbsolutePath();
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(fi));

        MouseOver mouseOver = new MouseOver();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        pane = getContentPane();
        pane.setLayout(new BorderLayout());

        south = new JPanel();
        south.setLayout(new FlowLayout());
        pane.add(south, "South");

        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // hasSaved = false;

        fileDialog = new JFileChooser();
        fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileDialog.setFileView(new NeptusFileView());

        generalInfo = "Type text into the window DUPA";
        infoLabel = new JLabel(generalInfo);
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(java.awt.event.KeyEvent.VK_F);
        fontMenu = new JMenu("Font");
        fontTypeMenu = new JMenu("Type");
        fontSizeMenu = new JMenu("Size");
        fontStyleMenu = new JMenu("Style");
        fontColorMenu = new JMenu("Color");
        helpMenu = new JMenu("Help");
        saveItem = new JMenuItem("Save");
        saveAsItem = new JMenuItem("Save As...");
        openItem = new JMenuItem("Open...");
        openItem.setMnemonic(java.awt.event.KeyEvent.VK_O);
        exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(java.awt.event.KeyEvent.VK_E);
        fontTypeGroup = new ButtonGroup();
        courierItem = new JRadioButtonMenuItem("Courier", true);
        dialogItem = new JRadioButtonMenuItem("Dialog");
        timesItem = new JRadioButtonMenuItem("TimesRoman");
        fontSizeGroup = new ButtonGroup();
        font10Item = new JRadioButtonMenuItem("10");
        font12Item = new JRadioButtonMenuItem("12", true);
        font14Item = new JRadioButtonMenuItem("14");
        font16Item = new JRadioButtonMenuItem("16");
        font18Item = new JRadioButtonMenuItem("18");
        italicItem = new JCheckBoxMenuItem("Italic");
        boldItem = new JCheckBoxMenuItem("Bold");
        fontColorGroup = new ButtonGroup();
        blackItem = new JRadioButtonMenuItem("Black");
        grayItem = new JRadioButtonMenuItem("Gray", true);
        blueItem = new JRadioButtonMenuItem("Blue");
        redItem = new JRadioButtonMenuItem("Red");
        aboutItem = new JMenuItem("About");

        textArea = new JTextArea();
        scrollPane = new JScrollPane(textArea);

        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(openItem);
        fileMenu.add(exitItem);
        fontMenu.add(fontTypeMenu);
        fontMenu.add(fontSizeMenu);
        fontMenu.add(fontStyleMenu);
        fontMenu.add(fontColorMenu);
        fontTypeGroup.add(courierItem);
        fontTypeGroup.add(dialogItem);
        fontTypeGroup.add(timesItem);
        fontTypeMenu.add(courierItem);
        fontTypeMenu.add(dialogItem);
        fontTypeMenu.add(timesItem);
        fontSizeGroup.add(font10Item);
        fontSizeGroup.add(font12Item);
        fontSizeGroup.add(font14Item);
        fontSizeGroup.add(font16Item);
        fontSizeGroup.add(font18Item);
        fontSizeMenu.add(font10Item);
        fontSizeMenu.add(font12Item);
        fontSizeMenu.add(font14Item);
        fontSizeMenu.add(font16Item);
        fontSizeMenu.add(font18Item);
        fontStyleMenu.add(italicItem);
        fontStyleMenu.add(boldItem);
        fontColorGroup.add(blackItem);
        fontColorGroup.add(grayItem);
        fontColorGroup.add(redItem);
        fontColorGroup.add(blueItem);
        fontColorMenu.add(blackItem);
        fontColorMenu.add(blueItem);
        fontColorMenu.add(grayItem);
        fontColorMenu.add(redItem);
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(fontMenu);
        menuBar.add(helpMenu);

        south.add(infoLabel);
        pane.add(scrollPane, "Center");

        saveItem.addActionListener(this);
        saveAsItem.addActionListener(this);
        openItem.addActionListener(this);
        exitItem.addActionListener(this);
        courierItem.addActionListener(this);
        dialogItem.addActionListener(this);
        timesItem.addActionListener(this);
        font10Item.addActionListener(this);
        font12Item.addActionListener(this);
        font14Item.addActionListener(this);
        font16Item.addActionListener(this);
        font18Item.addActionListener(this);
        italicItem.addActionListener(this);
        boldItem.addActionListener(this);
        blackItem.addActionListener(this);
        grayItem.addActionListener(this);
        redItem.addActionListener(this);
        blueItem.addActionListener(this);
        aboutItem.addActionListener(this);

        fileMenu.addMouseListener(mouseOver);
        fontMenu.addMouseListener(mouseOver);
        helpMenu.addMouseListener(mouseOver);
        fontTypeMenu.addMouseListener(mouseOver);
        fontSizeMenu.addMouseListener(mouseOver);
        fontStyleMenu.addMouseListener(mouseOver);
        fontColorMenu.addMouseListener(mouseOver);
        saveItem.addMouseListener(mouseOver);
        saveAsItem.addMouseListener(mouseOver);
        openItem.addMouseListener(mouseOver);
        exitItem.addMouseListener(mouseOver);
        courierItem.addMouseListener(mouseOver);
        dialogItem.addMouseListener(mouseOver);
        timesItem.addMouseListener(mouseOver);
        font10Item.addMouseListener(mouseOver);
        font12Item.addMouseListener(mouseOver);
        font14Item.addMouseListener(mouseOver);
        font16Item.addMouseListener(mouseOver);
        font18Item.addMouseListener(mouseOver);
        italicItem.addMouseListener(mouseOver);
        boldItem.addMouseListener(mouseOver);
        blackItem.addMouseListener(mouseOver);
        grayItem.addMouseListener(mouseOver);
        redItem.addMouseListener(mouseOver);
        blueItem.addMouseListener(mouseOver);
        aboutItem.addMouseListener(mouseOver);

        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        saveItem.setEnabled(false);

        setSize(800, 600);

        addWindowListener(new WindowCloser());
    }

    class MouseOver extends MouseAdapter {
        public void mouseEntered(MouseEvent e) {
            if (e.getSource() == fileMenu)
                infoLabel.setText("Save/Open documents or exit the Text Editor");
            else if (e.getSource() == fontMenu)
                infoLabel.setText("Control the appearance of the text");
            else if (e.getSource() == helpMenu)
                infoLabel.setText("Help information relating to the Text Editor");
            else if (e.getSource() == fontTypeMenu)
                infoLabel.setText("Set the font type");
            else if (e.getSource() == fontSizeMenu)
                infoLabel.setText("Set the font size");
            else if (e.getSource() == fontStyleMenu)
                infoLabel.setText("Set the font style");
            else if (e.getSource() == fontColorMenu)
                infoLabel.setText("Set the font color");
            else if (e.getSource() == saveItem)
                infoLabel.setText("Save the text in the file you are working on");
            else if (e.getSource() == saveAsItem)
                infoLabel.setText("Save the text to a selected file");
            else if (e.getSource() == openItem)
                infoLabel.setText("Open a new text file");
            else if (e.getSource() == exitItem)
                infoLabel.setText("Exit the Text Editor");
            else if (e.getSource() == courierItem)
                infoLabel.setText("Set the font type to Courier");
            else if (e.getSource() == dialogItem)
                infoLabel.setText("Set the font type to Dialog");
            else if (e.getSource() == timesItem)
                infoLabel.setText("Set the font type to TimesRoman");
            else if (e.getSource() == font10Item)
                infoLabel.setText("Set the font size to 10");
            else if (e.getSource() == font12Item)
                infoLabel.setText("Set the font size to 12");
            else if (e.getSource() == font14Item)
                infoLabel.setText("Set the font size to 14");
            else if (e.getSource() == font16Item)
                infoLabel.setText("Set the font size to 16");
            else if (e.getSource() == font18Item)
                infoLabel.setText("Set the font size to 18");
            else if (e.getSource() == italicItem)
                infoLabel.setText("Set the font style to italic");
            else if (e.getSource() == boldItem)
                infoLabel.setText("Set the font style to bold");
            else if (e.getSource() == blackItem)
                infoLabel.setText("Set the font color to black");
            else if (e.getSource() == grayItem)
                infoLabel.setText("Set the font color to gray");
            else if (e.getSource() == redItem)
                infoLabel.setText("Set the font color to red");
            else if (e.getSource() == blueItem)
                infoLabel.setText("Set the font color to blue");
            else if (e.getSource() == aboutItem)
                infoLabel.setText("View information about this Text Editor");
        }

        public void mouseExited(MouseEvent e) {
            infoLabel.setText(generalInfo);
        }
    }

    class WindowCloser extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            exitItemAction();
        }
    }

    private void save() {
        try {
            PrintWriter output = new PrintWriter(new FileWriter(chosenFile));

            output.print(textArea.getText());
            output.close();

            saveItem.setEnabled(true);
        }
        catch (IOException e) {
            errorDialog("Error in saving file! dupa!");
        }
    }

    private void load() {
        boolean eof = false;
        String line = "";
        String fileText = "";

        try {
            BufferedReader input = new BufferedReader(new FileReader(chosenFile));

            while (!eof) {
                line = input.readLine();

                if (line == null)
                    eof = true;
                else
                    fileText += line + "\r\n";
            }

            input.close();
            textArea.setText(fileText);

            saveItem.setEnabled(true);
        }
        catch (IOException e) {
            errorDialog("Error in loading file!");
        }
    }

    private void saveItemAction() {
        int result = fileDialog.showSaveDialog(this);

        if (result == JFileChooser.CANCEL_OPTION)
            return;

        chosenFile = fileDialog.getSelectedFile();
        save();
    }

    private void loadItemAction() {
        int result = fileDialog.showOpenDialog(this);

        if (result == JFileChooser.CANCEL_OPTION)
            return;

        chosenFile = fileDialog.getSelectedFile();
        load();
    }

    private void errorDialog(String errorText) {
        JOptionPane.showMessageDialog(this, errorText, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void exitItemAction() {
        String exitText = "Are you sure you want to exit?";

        int choice = JOptionPane.showConfirmDialog(this, exitText, "Confirm", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION)
            System.exit(0);
    }

    private void aboutItemAction() {
        String aboutText = "Neptus Text Editor" + "\nThis Text Editor was written originaly by Greg Solon.\n"
                + "Version 2.0";

        JOptionPane.showMessageDialog(this, aboutText, "About", JOptionPane.PLAIN_MESSAGE);
    }

    public void actionPerformed(ActionEvent e) {
        String fontName = textArea.getFont().getName();
        int fontStyle = textArea.getFont().getStyle();
        int fontSize = textArea.getFont().getSize();

        if (e.getSource() == saveItem)
            save();
        else if (e.getSource() == saveAsItem)
            saveItemAction();
        else if (e.getSource() == openItem)
            loadItemAction();
        else if (e.getSource() == exitItem)
            exitItemAction();
        else if (e.getSource() == courierItem)
            textArea.setFont(new Font("Monospaced", fontStyle, fontSize));
        else if (e.getSource() == dialogItem)
            textArea.setFont(new Font("Dialog", fontStyle, fontSize));
        else if (e.getSource() == timesItem)
            textArea.setFont(new Font("Serif", fontStyle, fontSize));
        else if (e.getSource() == font10Item)
            textArea.setFont(new Font(fontName, fontStyle, 10));
        else if (e.getSource() == font12Item)
            textArea.setFont(new Font(fontName, fontStyle, 12));
        else if (e.getSource() == font14Item)
            textArea.setFont(new Font(fontName, fontStyle, 14));
        else if (e.getSource() == font16Item)
            textArea.setFont(new Font(fontName, fontStyle, 16));
        else if (e.getSource() == font18Item)
            textArea.setFont(new Font(fontName, fontStyle, 18));
        else if ((e.getSource() == italicItem) || (e.getSource() == boldItem)) {
            if (italicItem.isSelected() && !boldItem.isSelected())
                textArea.setFont(new Font(fontName, Font.ITALIC, fontSize));
            else if (italicItem.isSelected() && boldItem.isSelected())
                textArea.setFont(new Font(fontName, Font.ITALIC | Font.BOLD, fontSize));
            else if (!italicItem.isSelected() && boldItem.isSelected())
                textArea.setFont(new Font(fontName, Font.BOLD, fontSize));
            else
                textArea.setFont(new Font(fontName, Font.PLAIN, fontSize));
        }
        else if (e.getSource() == blackItem)
            textArea.setForeground(Color.black);
        else if (e.getSource() == grayItem)
            textArea.setForeground(Color.gray);
        else if (e.getSource() == redItem)
            // textArea.setForeground(Color.red);
            textArea.setForeground(new Color(140, 45, 25));
        else if (e.getSource() == blueItem)
            // textArea.setForeground(Color.blue);
            textArea.setForeground(new Color(0, 0, 128));
        else if (e.getSource() == aboutItem)
            aboutItemAction();
    }

    class UndoAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            }
            catch (CannotUndoException ex) {
                NeptusLog.pub().info("<###>Unable to undo: " + ex);
                ex.printStackTrace();
            }
            updateUndoState();
            redoAction.updateRedoState();
        }

        protected void updateUndoState() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    class RedoAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            }
            catch (CannotRedoException ex) {
                NeptusLog.pub().info("<###>Unable to redo: " + ex);
                ex.printStackTrace();
            }
            updateRedoState();
            undoAction.updateUndoState();
        }

        protected void updateRedoState() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    public static void main(String[] args) {
        TextEditor editor = new TextEditor();

        editor.setVisible(true);
    }
}