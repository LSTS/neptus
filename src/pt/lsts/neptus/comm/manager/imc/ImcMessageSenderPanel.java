/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 \* Alternatively, this file may be used under the terms of the Modified EUPL,
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
 * 2008/04/13
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.LocationCopyPastePanel;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
public class ImcMessageSenderPanel extends JPanel {

	private static final long serialVersionUID = 3776289592692060016L;

    private static ImageIcon ICON = new ImageIcon(ImageUtils.getImage(
			"images/imc.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
    private static ImageIcon ICON1 = new ImageIcon(ImageUtils.getImage(
            "images/imc.png").getScaledInstance(48, 48, Image.SCALE_SMOOTH));

	private JComboBox<?> messagesComboBox = null;
	private JButton editMessageButton = null;
	private JButton publishButton = null;
	private JButton burstPublishButton = null;
	private JButton createButton = null;
	
	private LocationCopyPastePanel locCopyPastePanel = null;
	
	private JTextField address = new JTextField("127.0.0.1");
	private NumberFormat nf = new DecimalFormat("#####");
	private JTextField port = null;
	private JTextField bindPort = new JTextField("");
	private JTextField srcId = new JTextField("");
	private JTextField dstId = new JTextField("");
	
    
	private HashMap<String, IMCMessage> messagesPool = new HashMap<String, IMCMessage>();
	
	/**
	 * 
	 */
	public ImcMessageSenderPanel() {
		initialize();
	}
	
	private void initialize() {
//		try {
//			port = new JTextField(nf.format(GeneralPreferences.getPropertyLong(GeneralPreferences.CONSOLE_LOCAL_PORT)));
//		} catch (GeneralPreferencesException e) {
//			port = new JTextField(nf.format(6001));
//		}
		port = new JTextField(nf.format(GeneralPreferences.commsLocalPortUDP));

		JPanel holder = new JPanel();
        GroupLayout layout = new GroupLayout(holder);
        holder.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JLabel addressLabel = new JLabel("Address and Port to UDP send");
        JLabel localBindLabel = new JLabel("Local Port to bind (blanc for don't care)");
        JLabel srcDstIdLabel = new JLabel("Source and Destination IMC IDs (blanc for don't care)");
        
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(localBindLabel)
        				.addComponent(bindPort))
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(addressLabel)
        				.addGroup(layout.createSequentialGroup()
        						.addComponent(address)
        						.addComponent(port)))
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(srcDstIdLabel)
        				.addGroup(layout.createSequentialGroup()
        						.addComponent(srcId)
        						.addComponent(dstId)))
        		.addComponent(getMessagesComboBox())
        		.addGroup(layout.createSequentialGroup()
        				.addComponent(getLocCopyPastPanel())
        				.addComponent(getEditMessageButton())
        				.addComponent(getCreateButton())
        				.addComponent(getPublishButton())
        				.addComponent(getBurstPublishButton())));

        layout.setVerticalGroup(layout.createSequentialGroup()
        		.addGroup(layout.createSequentialGroup()
        				.addComponent(localBindLabel)
        				.addComponent(bindPort, 25,25,25))
        		.addGroup(layout.createSequentialGroup()
        				.addComponent(addressLabel)
        				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        						.addComponent(address, 25,25,25)
        						.addComponent(port, 25,25,25)))
        		.addGroup(layout.createSequentialGroup()
        				.addComponent(srcDstIdLabel)
        				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        						.addComponent(srcId, 25,25,25)
        						.addComponent(dstId, 25,25,25)))
        		.addComponent(getMessagesComboBox(), 25,25,25)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(getLocCopyPastPanel())
        				.addComponent(getEditMessageButton())
        				.addComponent(getCreateButton())
        				.addComponent(getPublishButton())
        				.addComponent(getBurstPublishButton())));
        
        layout.linkSize(SwingConstants.HORIZONTAL, getCreateButton(),
				getEditMessageButton(), getPublishButton(),
				getBurstPublishButton());
		layout.linkSize(SwingConstants.VERTICAL, getCreateButton(),
				getEditMessageButton(), getPublishButton(),
				getBurstPublishButton());
        
        this.setLayout(new BorderLayout());
        add(holder, BorderLayout.CENTER);
	}



	private JComboBox<?> getMessagesComboBox() {
		if (messagesComboBox == null) {
         	List<String> mList = new ArrayList<String>(IMCDefinition.getInstance().getMessageCount());
			for (String mt : IMCDefinition.getInstance().getMessageNames()) {
			    mList.add(mt);
			}
			Collections.sort(mList);
			messagesComboBox = new JComboBox<Object>(mList.toArray(new String[mList.size()]));
		}
		return messagesComboBox;
	}

	
	/**
	 * This method initializes editMessageButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getEditMessageButton() {
		if (editMessageButton == null) {
			editMessageButton = new JButton();
			editMessageButton.setText("Edit");
			editMessageButton.setPreferredSize(new Dimension(76, 26));
			editMessageButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String mName = (String) getMessagesComboBox().getSelectedItem();
					IMCMessage sMsg = getOrCreateMessage(mName);
					MessageEditorImc.showProperties(sMsg, 
							SwingUtilities.getWindowAncestor(ImcMessageSenderPanel.this), true);
				}
			});
		}
		return editMessageButton;
	}

	/**
	 * @return the createButton
	 */
	private JButton getCreateButton() {
		if (createButton == null) {
			createButton = new JButton();
			createButton.setText("Create");
			createButton.setPreferredSize(new Dimension(76, 26));
			createButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String mName = (String) getMessagesComboBox().getSelectedItem();
					getOrCreateMessage(mName);
				}
			});
		}
		return createButton;
	}



	/**
	 * This method initializes publishButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getPublishButton() {
		if (publishButton == null) {
			publishButton = new JButton();
			publishButton.setText("Publish");
			publishButton.setPreferredSize(new Dimension(76, 26));
			publishButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msgName = (String)messagesComboBox.getSelectedItem();
					String msg = null;
					IMCMessage sMsg = messagesPool.get(msgName);
					if (sMsg != null) {
						try {
							fillSrcDstId(sMsg);
                            sMsg.setTimestampMillis(System.currentTimeMillis());
							sMsg.dump(System.out);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							IMCOutputStream ios = new IMCOutputStream(baos);
							sMsg.serialize(ios);
							
							ByteUtil.dumpAsHex(msgName + " [size=" + baos.size() + "]", baos.toByteArray(), System.out);							
							msg = sendUdpMsg(baos.toByteArray(), baos.size());
						}
						catch (Exception e1) {
							e1.printStackTrace();
						}
						if (msg != null)
							JOptionPane.showMessageDialog(publishButton,
									"Error sending " + msgName
											+ " [" + msg +
													"]!");
					}
					else {
						JOptionPane.showMessageDialog(publishButton,
										"Edit first message " + msgName
												+ " to create it!");
					}
				}
			});
		}
		return publishButton;
	}

	/**
	 * @param sMsg
	 */
	protected void fillSrcDstId(IMCMessage sMsg) {
		if (!"".equalsIgnoreCase(srcId.getText())) {
			int id = -1;
			try {
				id = (int) ImcId16.parseImcId16(srcId.getText());
			} catch (NumberFormatException e) {
				try {
					id = Integer.parseInt(srcId.getText());
				} catch (NumberFormatException e1) {
					try {
						id = Integer.parseInt(srcId.getText(), 16);
					} catch (NumberFormatException e2) {
					    e2.printStackTrace();
					}
				}
			}
			if (id < 0) {
				srcId.setText("");
			}
			else {
					sMsg.getHeader().setValue("src", id);
			}
		}

		if (!"".equalsIgnoreCase(dstId.getText())) {
			int id = -1;
			try {
				id = (int) ImcId16.parseImcId16(dstId.getText());
			} catch (NumberFormatException e) {
				try {
					id = Integer.parseInt(dstId.getText());
				} catch (NumberFormatException e1) {
					try {
						id = Integer.parseInt(dstId.getText(), 16);
					} catch (NumberFormatException e2) {
					}
				}
			}
			if (id < 0) {
				dstId.setText("");
			}
			else {
					sMsg.getHeader().setValue("dst", id);
			}
		}
	}



	/**
	 * @return the burstPublishButton
	 */
	private JButton getBurstPublishButton() {
		if (burstPublishButton == null) {
			burstPublishButton = new JButton();
			burstPublishButton.setText("Burst");
			burstPublishButton.setPreferredSize(new Dimension(76, 26));
			burstPublishButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					burstPublishButton.setEnabled(false);
					SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
						@Override
						protected Boolean doInBackground() throws Exception {
							Collection<String> mtypes = IMCDefinition.getInstance().getMessageNames();
							for (String mt : mtypes) {
								//System.out.printf("Message type: %s\n", mt.getShortName());
								String msgName = mt;
								String msg = null;
								IMCMessage sMsg = getOrCreateMessage(msgName);
	                            fillSrcDstId(sMsg);
								sMsg.setTimestampMillis(System.currentTimeMillis());
								sMsg.dump(System.out);
	                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                            IMCOutputStream ios = new IMCOutputStream(baos);
								try {
									sMsg.serialize(ios);
									ByteUtil.dumpAsHex(msgName + " [size=" + baos.size() + "]",baos.toByteArray(), System.out);							
									msg = sendUdpMsg(baos.toByteArray(), baos.size());
								} catch (Exception e1) {
									System.err.println("Msg: " + msg);
									e1.printStackTrace();
								}
							}
							return false;
						}
						
						@Override
						protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
							if (burstPublishButton != null)
								burstPublishButton.setEnabled(true);
						}
					};
					worker.execute();
				}
			});
		}
		return burstPublishButton;
	}



	/**
	 * @return the locCopyPastPanel
	 */
	private LocationCopyPastePanel getLocCopyPastPanel() {
		if (locCopyPastePanel == null) {
			locCopyPastePanel = new LocationCopyPastePanel() {
				private static final long serialVersionUID = 1809942752421373734L;

				@Override
				public void setLocationType(LocationType locationType) {
					super.setLocationType(locationType);
					applyLocation();
//					String mName = "EstimatedState";
//					IMCMessage sMsgES = getOrCreateMessage(mName);
				}
			};
			locCopyPastePanel.setPreferredSize(new Dimension(76, 26));
			locCopyPastePanel.setMaximumSize(new Dimension(76, 26));
			//locCopyPastPanel.setBorder(null);
			locCopyPastePanel.setToolTipText("Pastes to EstimatedState Message (but doesn't copy from there nor touches ref)");
		}
		return locCopyPastePanel;
	}


	public String sendUdpMsg(byte[] msg, int  size) {
		
        try {
            DatagramSocket sock = null;
        	if ("".equalsIgnoreCase(bindPort.getText())) {
                sock = new DatagramSocket();
        	}
        	else {
        		int bport = Integer.parseInt(bindPort.getText());
        		sock = new DatagramSocket(bport);
        	}
            sock.connect(new InetSocketAddress(address.getText(), Integer.parseInt(port.getText())));
            sock.send(new DatagramPacket(msg, size));
            sock.close();
            return null;
        }
        catch (Exception e) {
        	NeptusLog.pub().error(e);
            return "Error sending the UDP message: " + e.getMessage();
        }
    }

    private IMCMessage getOrCreateMessage(String mName) {
        IMCMessage msg = messagesPool.get(mName);
        if (msg == null) {
            msg = IMCDefinition.getInstance().create(mName);
            messagesPool.put(mName, msg);
        }
        applyLocation(msg);
        return msg;
    }
	
	/**
     * @param locationType
     * @param mName
     */
    private void applyLocation() {
        for (IMCMessage sMsg : messagesPool.values()) {
            applyLocation(sMsg);
        }
    }

    private void applyLocation(IMCMessage sMsg) {
        LocationType locationType = getLocCopyPastPanel().getLocationType();
        
        List<String> fieldNames = Arrays.asList(sMsg.getFieldNames());
        boolean hasLatLon = false, hasXY = false, hasDepthOrHeight = false;
        if (fieldNames.contains("lat") || fieldNames.contains("lon"))
            hasLatLon = true;
        if (fieldNames.contains("x") || fieldNames.contains("y"))
            hasXY = true;
        if (fieldNames.contains("depth") || fieldNames.contains("height"))
            hasDepthOrHeight = true;
        
        if (hasLatLon && !hasXY)
            locationType = locationType.getNewAbsoluteLatLonDepth();
        
        sMsg.setValue("lat", locationType.getLatitudeRads());
        sMsg.setValue("lon", locationType.getLongitudeRads());
        sMsg.setValue("depth", locationType.getDepth());
        sMsg.setValue("height", locationType.getHeight());

        double[] val = CoordinateUtil.sphericalToCartesianCoordinates(locationType.getOffsetDistance(), locationType.getAzimuth(), locationType.getZenith());
        sMsg.setValue("x", locationType.getOffsetNorth()+val[0]);
        sMsg.setValue("y", locationType.getOffsetEast()+val[1]);
        
        if (!hasDepthOrHeight) {
            sMsg.setValue("z", locationType.getAllZ());
        }
        else {
            sMsg.setValue("z", locationType.getOffsetDown()+val[2]);
        }
    }

    public static JFrame getFrame() {
		JFrame frame = GuiUtils.testFrame(new ImcMessageSenderPanel());
		frame.setSize(500, 290);
		frame.setTitle("IMC Message Sender (by UDP)");
        ArrayList<Image> imageList = new ArrayList<Image>();
        imageList.add(ICON.getImage());
        imageList.add(ICON1.getImage());
        frame.setIconImages(imageList);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		return frame;
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
    public static void main(String[] args) {
		ConfigFetch.initialize();
		new ImcMessageSenderPanel().getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//		Collection<MessageType> mtypes = IMCDefinition.getInstance().getParser().getMessageTypes();
//		for (MessageType mt : mtypes)
//		{
//			System.out.printf("Message type: %s\n", mt.getShortName());
//		}
//		getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

}
