/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/06/27
 */
package pt.up.fe.dceg.neptus.mp.actions;

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
import java.util.LinkedHashMap;
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

import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystems;
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystemsHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
	
	private JPanel holderPayload;
	private JScrollPane scrollPanePayload;
	private JPanel controlsPayload;
	private ToolbarButton addPayload;
	private ToolbarButton removePayload;

	private JPanel holderMessages;
	private JScrollPane scrollPaneMessages;
	private JPanel controlsMessages;
	private ToolbarButton addMessages;
	private ToolbarButton removeMessages;

	private AbstractAction actionAddPayload;
	private AbstractAction actionRemovePayload;
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
		
		add(new JLabel("<html><b>Payload Configurations"));
		
		holderPayload = new JPanel(true);
		holderPayload.setLayout(new BoxLayout(holderPayload, BoxLayout.Y_AXIS));
		
		scrollPanePayload = new JScrollPane();
		scrollPanePayload.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPanePayload.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPanePayload.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		scrollPanePayload.setPreferredSize(new Dimension(600,200));
		scrollPanePayload.setViewportView(holderPayload);
		add(scrollPanePayload);
		
		controlsPayload = new JPanel();
		controlsPayload.setLayout(new BoxLayout(controlsPayload, BoxLayout.X_AXIS));

		addPayload = new ToolbarButton(actionAddPayload);
		removePayload = new ToolbarButton(actionRemovePayload);
		
		controlsPayload.add(addPayload);
		controlsPayload.add(removePayload);

		add(controlsPayload);
		add(new JSeparator(JSeparator.HORIZONTAL));

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
	    holderPayload.removeAll();
	    for (PayloadConfig plCfg : actions.getPayloadConfigs()) {
            holderPayload.add(plCfg);
        }
	    
	    holderMessages.removeAll();
        for (PlanActionElementConfig plCfg : actions.getActionMsgs()) {
            holderMessages.add(plCfg);
        }
	}
	
	private void initializeActions() {
		actionAddPayload = new AbstractAction("add payload", ADD_ICON) {
			/* (non-Javadoc)
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
//				MiscSystems ms = MiscSystemsHolder.getMiscSystemsList().get("imagenex-881ss");
//				if (ms != null) {
//					PayloadConfig plCfg = new PayloadConfig();
//					plCfg.setBaseSystem(ms);
//					plCfg.load();
//					actions.payloadConfigs.add(plCfg);
//					holderPayload.add(plCfg);
//					holderPayload.repaint();
//				}
			    
			    final LinkedHashMap<String, MiscSystems> pldl = MiscSystemsHolder.getPayloadList();
			    if (pldl.size() == 0) {
			        GuiUtils.infoMessage(PlanActionsPanel.this, "Payload", 
			                "No payload configurations available!");
			    }
			    else {
			        MiscSystems ms = null;
			        if (pldl.size() == 1) {
			            ms = pldl.values().toArray(new MiscSystems[1])[0];
			        }
			        else {
                        JComboBox<?> payloadComboBox = new JComboBox<Object>(pldl.values().toArray(new MiscSystems[pldl.size()]));
                        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(PlanActionsPanel.this));
                        dialog.setLayout(new BorderLayout());
                        dialog.add(payloadComboBox, BorderLayout.CENTER);
                        JButton okButton = new JButton("ok");
                        okButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                dialog.setVisible(false);
                                dialog.dispose();
                            }
                        });
                        dialog.add(okButton, BorderLayout.SOUTH);
                        dialog.setSize(300, 100);
                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                        GuiUtils.centerOnScreen(dialog);
                        dialog.setVisible(true);
                        ms = (MiscSystems) payloadComboBox.getSelectedItem();
			        }
			        if (ms != null) {
			            PayloadConfig plCfg = new PayloadConfig();
			            plCfg.setBaseSystem(ms);
			            plCfg.load();
			            actions.payloadConfigs.add(plCfg);
			            holderPayload.add(plCfg);
			            holderPayload.repaint();
			        }
			    }
			}
		};

		actionRemovePayload = new AbstractAction("remove payload", REMOVE_ICON) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (holderPayload.getComponentCount() > 0) {
						PayloadConfig rem = actions.payloadConfigs.removeLast();
						holderPayload.remove(rem);
						holderPayload.repaint();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		};

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
						IMCMessage sMsg = (IMCMessage) IMCDefinition.getInstance().create(mName);;
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
				orig.getPayloadConfigs().clear();
				orig.getPayloadConfigs().addAll(changed.getPayloadConfigs());
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
		MiscSystemsHolder.loadMiscSystems();
		PlanActions pa = new PlanActions();
		PlanActionsPanel pap = new PlanActionsPanel(pa);
		pap.getDialog(new JFrame(), "eeeeeeeeeeee");
		
        Element eActionsElm = pa.asElement("end-actions");
        System.out.println(eActionsElm.asXML());
	}
}
