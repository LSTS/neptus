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
 * Author: 
 * 2007/04/28
 */
package pt.lsts.neptus.gui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleConstants.ColorConstants;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class MessagePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss,SSS");  //  @jve:decl-index=0:

    public static final Color DEFAULT = Color.BLACK;  //  @jve:decl-index=0:
    public static final Color INFO    = Color.BLUE.darker();
    public static final Color ERROR   = Color.RED;
    public static final Color WARN    = new Color(255, 180, 0); //ORANGE mais visível
    public static final Color SENT    = Color.BLUE;
    public static final Color RECEIVE = new Color(0, 200, 125); //Green mais visível
    public static final Color PING = new Color(193, 176, 135);

    private Color defaultColor = DEFAULT;  //  @jve:decl-index=0:

    private static final int MAX_TEXT_MSG_LENGHT = 500000;

    private boolean isTimeStampEnable = false;
	
	private JScrollPane msgScrollPane = null;
	private JTextPane msgTextPane = null;
	private JButton clearMsgsButton = null;
	private JButton copyToClipboardButton = null;
	private JPanel buttonsPanel = null;

	
	/**
	 * This is the default constructor
	 */
	public MessagePanel() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(300, 200);
		this.setLayout(new BorderLayout());
		this.add(getMsgScrollPane(), BorderLayout.CENTER);
		this.add(getButtonsPanel(), BorderLayout.SOUTH);
	}

	/**
	 * @param show
	 */
	public void showButtons(boolean show) {
		if (show) {
			getButtonsPanel().setVisible(true);
		}
		else {
			getButtonsPanel().setVisible(false);
		}
	}

	
	/**
	 * This method initializes msgScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getMsgScrollPane() {
		if (msgScrollPane == null) {
			msgScrollPane = new JScrollPane();
			msgScrollPane.setViewportView(getMsgTextPane());
		}
		return msgScrollPane;
	}

	/**
	 * This method initializes msgTextPane	
	 * 	
	 * @return javax.swing.JTextPane	
	 */
	private JTextPane getMsgTextPane() {
		if (msgTextPane == null) {
			msgTextPane = new JTextPane();
			msgTextPane.setEditable(false);
		}
		return msgTextPane;
	}

	/**
	 * This method initializes clearMsgsButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getClearMsgsButton() {
		if (clearMsgsButton == null) {
			clearMsgsButton = new JButton();
			clearMsgsButton.setText("clear");
            clearMsgsButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    getMsgTextPane().setText("");
                }
            });
		}
		return clearMsgsButton;
	}
	
	private JButton getCopyToClipboardButton() {
		if (copyToClipboardButton == null) {
			copyToClipboardButton = new JButton("copy to clipboard");
			copyToClipboardButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ClipboardOwner owner = new ClipboardOwner() {
						public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable contents) {};						
					};
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(msgTextPane.getText()), owner);
					writeMessageTextln("Text was copied to the system clipboard", DEFAULT);
					return;
				}
			});
		}
		return copyToClipboardButton;
	}

	/**
	 * @return the buttonsPanel
	 */
	private JPanel getButtonsPanel() {
		if(buttonsPanel == null) {
			buttonsPanel = new JPanel(new GridLayout(1,2));
			buttonsPanel.add(getClearMsgsButton());
			buttonsPanel.add(getCopyToClipboardButton());
		}
		return buttonsPanel;
	}

	
    /**
	 * @return the isTimeStampEnable
	 */
	public boolean isTimeStampEnable() {
		return isTimeStampEnable;
	}

	/**
	 * @param isTimeStampEnable the isTimeStampEnable to set
	 */
	public void setTimeStampEnable(boolean isTimeStampEnable) {
		this.isTimeStampEnable = isTimeStampEnable;
	}

	/**
	 * @return the defaultColor
	 */
	public Color getDefaultColor() {
		return defaultColor;
	}

	/**
	 * @param defaultColor the defaultColor to set
	 */
	public void setDefaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
	}

	private synchronized void writeMessageTextWorker(String message, Color type, int offset)
    {
        //getMsgTextArea().append(message);
        //getMsgTextArea().setCaretPosition(getMsgTextArea().getText().length());
        SimpleAttributeSet attrTS = new SimpleAttributeSet();
        attrTS.addAttribute(ColorConstants.Foreground, Color.DARK_GRAY);
        attrTS.addAttribute(StyleConstants.Bold, true);
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(ColorConstants.Foreground, type);
        Document doc = getMsgTextPane().getStyledDocument();
		if (offset == -1)
			offset = doc.getLength(); //(doc.getLength() == 0)?doc.getLength():doc.getEndPosition().getOffset();
        try {
        	if (isTimeStampEnable()) {
        		String msgTs = "[" + getTimeStamp() + "]: ";
        		doc.insertString(offset, msgTs, attrTS);
        		offset += msgTs.length();
        	}
            doc.insertString(offset, message, attr);
            int docLength = doc.getLength();
            if (docLength > MAX_TEXT_MSG_LENGHT)
                doc.remove(0, docLength - MAX_TEXT_MSG_LENGHT);
            //System.err.println("Doc. length " + doc.getLength());
            getMsgTextPane().setCaretPosition(doc.getLength());
        }
        catch (Exception e) {}
    }

    public void writeMessageTextln(String message, Color type)
    {
        if (!message.endsWith("\n"))
        	writeMessageTextWorker(message + "\n", type, -1);
        else
        	writeMessageTextWorker(message, type, -1);
    }

    public void writeMessageText(String message)
    {
        writeMessageTextWorker(message, getDefaultColor(), -1);
    }

    public void writeMessageText(String message, Color type)
    {
        writeMessageTextWorker(message, type, -1);
    }

    public void writeMessageTextln(String message)
    {
        writeMessageTextln(message, getDefaultColor());
    }

    public void writeMessageText(String message, Color type, int offset) {
    	writeMessageTextWorker(message, type, offset);
    }

    public void writeMessageText(String message, int offset) {
    	writeMessageTextWorker(message, getDefaultColor(), offset);
    }

    public synchronized void removeTextFromDoc(int offset, int length) {
    	Document doc = getMsgTextPane().getStyledDocument();
    	try {
			doc.remove(offset, length);
		} catch (BadLocationException e) {
			NeptusLog.pub().info("<###>offset="+offset+"  length="+length+"   docsize="+doc.getLength());
			e.printStackTrace();
		}
    }

    public synchronized String getTextFromDoc(int offset, int length) {
    	Document doc = getMsgTextPane().getStyledDocument();
    	try {
			return doc.getText(offset, length);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return "";
		}
    }
        
    public synchronized Position getCurrentPosition() {
    	Document doc = getMsgTextPane().getStyledDocument();
    	try {
			//return doc.createPosition(doc.getEndPosition().getOffset());
			return doc.createPosition(doc.getLength());
		} catch (BadLocationException e) {
			return doc.getEndPosition();
		}
    }
    
    private String getTimeStamp()
    {
        return dateFormat.format(new Date(System.currentTimeMillis()));
    }
    
    public static void main(String[] args) {
		MessagePanel msgPanel = new MessagePanel();
		GuiUtils.testFrame(msgPanel);
		msgPanel.writeMessageText("123456 ");
		Position cpos = msgPanel.getCurrentPosition();
		msgPanel.writeMessageText("12%");
		msgPanel.removeTextFromDoc(cpos.getOffset()-3, 3);
		msgPanel.writeMessageText("\n" + cpos.getOffset());
		msgPanel.removeTextFromDoc(0, 14);
		msgPanel.writeMessageText("oooooooo%", WARN);
		
	}
}
