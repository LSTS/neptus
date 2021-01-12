/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 2010/06/27
 */
package pt.lsts.neptus.mp.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PlanActionsPanel extends JPanel {

	public static ImageIcon ADD_ICON = ImageUtils.getScaledIcon("images/buttons/add.png", 30, 30);
	public static ImageIcon REMOVE_ICON = ImageUtils.getScaledIcon("images/buttons/remove.png", 30, 30);
	
	protected PlanActions actions = new PlanActions();
	
	protected JDialog dialog = null;
	
	//private JPanel holderPanel;
	
	private JPanel holderMessages;
	private JScrollPane scrollPaneMessages;
	private JPanel controlsMessages;
	private ToolbarButton addMessages;
	private ToolbarButton removeMessages;

	private AbstractAction actionAddMessages;
	private AbstractAction actionRemoveMessages;
    private boolean userCanceled = false;

	/**
	 * 
	 */
	private PlanActionsPanel(PlanActions actions) {
		this.actions = actions;
		initializeActions();
		initialize();
	}
	
	private void initialize() {
		removeAll();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		add(new JLabel("<html><b>Messages"));

		holderMessages = new JPanel(true);
		holderMessages.setLayout(new BoxLayout(holderMessages, BoxLayout.Y_AXIS));
		
		scrollPaneMessages = new JScrollPane();
		scrollPaneMessages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPaneMessages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneMessages.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		scrollPaneMessages.setPreferredSize(new Dimension(600,200));
		scrollPaneMessages.setViewportView(holderMessages);
		add(scrollPaneMessages);
		
		controlsMessages = new JPanel();
		controlsMessages.setLayout(new BoxLayout(controlsMessages, BoxLayout.X_AXIS));

		addMessages = new ToolbarButton(actionAddMessages);
		removeMessages = new ToolbarButton(actionRemoveMessages);
		
		controlsMessages.add(addMessages);
		controlsMessages.add(removeMessages);

		add(controlsMessages);
		
		add(new JSeparator(JSeparator.HORIZONTAL));
		
		refreshGUILists();
	}
	
	private void refreshGUILists() {
	    
	    holderMessages.removeAll();
        for (PlanActionElementConfig plCfg : actions.getActionMsgs()) {
            holderMessages.add(plCfg);
        }
	}
	
	private void initializeActions() {
		
		actionAddMessages = new AbstractAction("add message", ADD_ICON) {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(PlanActionsPanel.this));
				dialog.setLayout(new BorderLayout());
	            Vector<String> mValid = new Vector<String>();
				Collection<String> mtypes = IMCDefinition.getInstance().getMessageNames();
				for (String mt : mtypes) {
	                mValid.add(mt);
				}
				final JComboBox<?> messagesComboBox = new JComboBox<Object>(mValid.toArray(new String[]{}));
				dialog.add(messagesComboBox, BorderLayout.CENTER);
				JButton okButton = new JButton("ok");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						String mName = (String) messagesComboBox.getSelectedItem();
						IMCMessage sMsg = (IMCMessage) IMCDefinition.getInstance().create(mName);
						PlanActionElementConfig plActionCfg = new PlanActionElementConfig();
						plActionCfg.message = sMsg;
						try {
							plActionCfg.setXmlImcNode((Element) DocumentHelper.parseText(sMsg.asXml(false)).getRootElement().detach());
							actions.actionMsgs.add(plActionCfg);
							holderMessages.add(plActionCfg);
							holderMessages.repaint();
						} catch (DocumentException e1) {
							e1.printStackTrace();
						}
						dialog.setVisible(false);
						dialog.dispose();
					}
				});
				dialog.add(okButton, BorderLayout.SOUTH);
				dialog.setSize(200, 100);
				dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
				GuiUtils.centerOnScreen(dialog);
				dialog.setVisible(true);
			}
		};

		actionRemoveMessages = new AbstractAction("remove message", REMOVE_ICON) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (holderMessages.getComponentCount() > 0) {
						PlanActionElementConfig rem = actions.actionMsgs.removeLast();
						holderMessages.remove(rem);
						holderMessages.repaint();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		};

	}

	private boolean getDialog(Component parent, String title) {
        final PlanActions orig = actions;
        final PlanActions changed = (PlanActions) actions.clone();
        actions = changed;
        refreshGUILists();

        if (parent instanceof Window)
			dialog = new JDialog((Window)parent);
		else
			dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
		
		JButton okButton = new JButton(new AbstractAction("ok") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
				setUserCanceled(false);
                orig.getActionMsgs().clear();
                orig.getActionMsgs().addAll(changed.getActionMsgs());
                actions = orig;
                refreshGUILists();
			}
		});
		okButton.setPreferredSize(new Dimension(80, 30));

		final JButton cancelButton = new JButton(new AbstractAction("cancel") {
		    @Override
		    public void actionPerformed(ActionEvent e) {
                setUserCanceled(true);
                dialog.setVisible(false);
                dialog.dispose();
                actions = orig;
                refreshGUILists();
		    }
		});
        cancelButton.setPreferredSize(new Dimension(80, 30));
        GuiUtils.reactEscapeKeyPress(cancelButton);

		dialog.setTitle(title);
		dialog.setSize(600, 300);
		dialog.setLayout(new BorderLayout());
		dialog.getContentPane().add(this, BorderLayout.CENTER);

		JPanel bPanel = new JPanel();
		bPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bPanel.add(okButton);
		bPanel.add(cancelButton);
        dialog.getContentPane().add(bPanel, BorderLayout.SOUTH);
		
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
		dialog.setAlwaysOnTop(true);
		GuiUtils.centerOnScreen(dialog);
		dialog.setResizable(true);
		dialog.setVisible(true);
		
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
			    cancelButton.doClick();
			}
		});
		
		return userCanceled;
	}

	/**
     * @param userCanceled the userCanceled to set
     */
    private void setUserCanceled(boolean userCanceled) {
        this.userCanceled = userCanceled;
    }

    /**
     * Shows a PlanActions Editor and changes the planActions if user does not cancel it;
     * @param planActions
     * @param parent
     * @param title
     * @return if user has canceled the edition or not. Either way the changes will be made 
     *          to the original planActions by copying the changes to the planActions hash tables.
     */
    public static boolean showDialog(PlanActions planActions, Component parent, String title) {
        PlanActionsPanel plPanel = new PlanActionsPanel(planActions);
        return plPanel.getDialog(parent, title); 
	}
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		PlanActions pa = new PlanActions();
		PlanActionsPanel pap = new PlanActionsPanel(pa);
		pap.getDialog(new JFrame(), "eeeeeeeeeeee");
		
        Element eActionsElm = pa.asElement("end-actions");
        NeptusLog.pub().info("<###> "+eActionsElm.asXML());
	}
}
