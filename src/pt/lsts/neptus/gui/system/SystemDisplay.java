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
 * Author: Paulo Dias
 * 2010/05/27
 */
package pt.lsts.neptus.gui.system;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.dom4j.Document;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.lsts.imc.EntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystem.IMCAuthorityState;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.gui.system.ConnectionSymbol.ConnectionStrengthEnum;
import pt.lsts.neptus.gui.system.EmergencyTaskSymbol.EmergencyStatus;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemConfigurationEditorPanel;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SystemDisplay extends JXPanel implements Comparable<SystemDisplay>, IPeriodicUpdates{

	public static enum BlinkingStateEnum {
		NOT_BLINKING, BLINKING_NORMAL, BLINKING_BRILLIANT
	};

    private static final Icon ICON_UP = ImageUtils.getScaledIcon("images/systems/uparrow.png", 12, 12);
    private static final Icon ICON_DOWN = ImageUtils.getScaledIcon("images/systems/downarrow.png", 12, 12);

    private static final Color COLOR_IDLE = Color.DARK_GRAY.brighter(); //new JXPanel().getBackground();
    private static final Color COLOR_GREEN = Color.GREEN.darker(); //new Color(50, 164, 193); //new Color(140, 255, 170);
    private static final Color COLOR_RED = new Color(245, 20, 40); //new Color(255, 210, 140);
    private static final Color COLOR_OLD = Color.GRAY.darker(); //new Color(255, 100, 100);
    //private static final Color COLOR_RED2 = new Color(255, 100, 100);
    private static final Color COLOR_BLUE = new Color (43, 182, 227); // new Color(11, 88, 107); //new Color(190, 220, 240).darker(); // blue
    private static final Color COLOR_ORANGE = new Color(255, 128, 0);

    protected static Color BLUE_1 = new Color(181, 198, 216);
    
	protected BlinkingStateEnum blinkingState = BlinkingStateEnum.NOT_BLINKING;

	private String id = "";
	private Image systemImage = null;
	private ImageIcon icon = null;
	private Color displayColor = null;
	private boolean active = false;
	private boolean selected = false;
	private boolean enableSelection = true;
	private ImcSystem.IMCAuthorityState withAuthority = IMCAuthorityState.NONE;
	private boolean taskAlocated = false;
	private boolean emergencyTaskAlocated = false;
	private boolean attentionAlert = false;
	private boolean idAlert = false;
	private boolean mainVehicle = false;
	private String systemType = "";
	private boolean showSystemSymbolOrText = true;
	
	private int iconSize = 20;
	private int indicatorsSize = 20;
	private int incrementFontSize = 0;
	
	
	// UI
	private JXLabel label = null;
	private ConnectionSymbol symConnected = null;
    private LocationSymbol symLoc = null;
	private MainVehicleSymbol symMain = null;
	private AuthoritySymbol symAuth = null;
	private TaskSymbol symTask = null;
	private AttentionSymbol symAttention = null;
	private SystemTypeSymbol symType = null;
	private IdProblemSymbol symIdAttention = null;
    private EmergencyTaskSymbol sysEmergencyTask = null;
    private DisplayColorSymbol symDisplayColor = null;
    private SystemParamsSymbol symSystemParamsSymbol = null;
    private FuelLevelSymbol symFuelLevel = null;
    // U+02EF Modifier Letter Low Down Arrowhead
    private JButton expandButton = new JButton(ICON_DOWN); // "\u25BC"
    private JLabel infoLabel = new JLabel(I18n.text("No extra info"));
	private GroupLayout layout;
	
	private Font labelOriginFont = null;
	
    private boolean showExtraInfoVisible = false;
    private long selectionTimeMillis = -1;

    private SystemConfigurationEditorPanel systemConfEditor = null;
    
	public SystemDisplay(String id) {
		this.id = id;
		initialize();
	}

	public void updateId(String id) {
        this.id = id;
        label.setText(id);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#getMinimumSize()
	 */
	@Override
	public Dimension getMinimumSize() {
		//NeptusLog.pub().info("<###>......");
		return new Dimension(iconSize + label.getWidth() + 10, iconSize
				+ indicatorsSize + label.getHeight() + infoLabel.getHeight()); //20
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
        return !isShowExtraInfoVisible() ? super.getPreferredSize() : new Dimension((int)super
                .getPreferredSize().getWidth(), (int)super.getPreferredSize().getHeight() + label.getHeight() + infoLabel.getHeight()); // 40
	}
	
	private void initialize() {
	    initializeSymbols();
	    
		setPreferredSize(new Dimension(100,60));
		setSelected(isSelected());
		setActive(isActive());
		setWithAuthority(getWithAuthority());
		setDisplayColor(null);
		
        expandButton.setMargin(new Insets(1, 1, 1, 1));
		expandButton.setBackground(BLUE_1);
		expandButton.setForeground(Color.BLACK);
		expandButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleShowMoreInfoVisible();
            }
        });
		
		symConnected.setPreferredSize(new Dimension(indicatorsSize,indicatorsSize));
		symConnected.setSize(new Dimension(indicatorsSize,indicatorsSize));
		//sys2.setPreferredSize(new Dimension(40,40));
		//sys2.setSize(new Dimension(40,40));
		
		this.addMouseListener(new MouseListener() {
		    @Override
			public void mouseClicked(MouseEvent e) {
				if (MouseEvent.BUTTON1 == e.getButton()) {
					if (e.isControlDown())
						e.setSource(SystemDisplay.this);
					else
						toggleSelected();
					propagate(e, MouseEvent.MOUSE_CLICKED);
				}
				else {
				    try {
				        if (e.getSource() instanceof SymbolLabel) {
				            SymbolLabel symb = (SymbolLabel) e.getSource();
				            if (symb.isRightClickable()) {
				                symb.mouseClicked(e);
				            }
				        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
				}
			}
            @Override
            public void mouseReleased(MouseEvent e) {
                propagate(e, MouseEvent.MOUSE_RELEASED);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                propagate(e, MouseEvent.MOUSE_PRESSED);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                propagate(e, MouseEvent.MOUSE_EXITED);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                propagate(e, MouseEvent.MOUSE_ENTERED);
            }
            private void propagate(MouseEvent e, int mete) {
                e.setSource(SystemDisplay.this);
                if (SystemDisplay.this.getParent() != null) {
                    for (MouseListener ml : SystemDisplay.this.getParent().getMouseListeners()) {
                        switch (mete) {
                            case MouseEvent.MOUSE_CLICKED:
                                ml.mouseClicked(e);
                                break;
                            case MouseEvent.MOUSE_ENTERED:
                                ml.mouseEntered(e);
                                break;
                            case MouseEvent.MOUSE_EXITED:
                                ml.mouseExited(e);
                                break;
                            case MouseEvent.MOUSE_PRESSED:
                                ml.mousePressed(e);
                                break;
                            case MouseEvent.MOUSE_RELEASED:
                                ml.mouseReleased(e);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
		});

		label = new JXLabel(id);
		labelOriginFont = label.getFont();
		label.setForeground(Color.WHITE);
		if (systemImage != null) {
			icon = new ImageIcon(systemImage.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
			label.setIcon(icon);
		}

		infoLabel.setVisible(false);
        infoLabel.setForeground(Color.WHITE);
		
        Component glue1 = Box.createHorizontalGlue();
        Component glue2 = Box.createHorizontalGlue();
        this.setBorder(new EmptyBorder(10, 10, 10, 10));
		layout = new GroupLayout(this);
		this.setLayout(layout);
		
		layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);
        
        layout.setHorizontalGroup(
    		layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addComponent(label)//, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
                        .addComponent(glue1)
                        .addComponent(symSystemParamsSymbol, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
                        .addGap(5)
                        .addComponent(symDisplayColor, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
                )
    			.addGroup(
    				layout.createSequentialGroup()
                        .addComponent(symType, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
	    				.addComponent(symConnected, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
	    				.addComponent(symLoc, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
                        .addComponent(symMain, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
                        .addComponent(symAuth, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
	    				.addComponent(symTask, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
						.addComponent(symAttention, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
						.addComponent(symFuelLevel, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
						.addComponent(symIdAttention, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
						.addComponent(sysEmergencyTask, symConnected.getHeight(), symConnected.getHeight(), symConnected.getHeight())
                        .addComponent(glue2)
                        .addComponent(expandButton)
    			)
    			.addComponent(infoLabel)
		);

        layout.setVerticalGroup(
       		layout.createSequentialGroup()
                .addGroup(
                    layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(label)
                        .addComponent(glue1)
                        .addComponent(symSystemParamsSymbol)
                        .addComponent(symDisplayColor)
                )
    			.addGroup(
    				layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(symType)
						.addComponent(symConnected)
						.addComponent(symLoc)
                        .addComponent(symMain)
                        .addComponent(symAuth)
						.addComponent(symTask)
						.addComponent(symAttention)
						.addComponent(symFuelLevel)
						.addComponent(symIdAttention)
						.addComponent(sysEmergencyTask)
                        .addComponent(glue2)
                        .addComponent(expandButton)
    			)
                .addComponent(infoLabel)
		);
        
		layout.linkSize(SwingConstants.VERTICAL, symConnected, symMain, symLoc,
				symAuth, symTask, symAttention, symType, symIdAttention, sysEmergencyTask,
				symDisplayColor, symSystemParamsSymbol, symFuelLevel);
		layout.linkSize(SwingConstants.HORIZONTAL, symConnected, symMain, symLoc,
				symAuth, symTask, symAttention, symType, symIdAttention,sysEmergencyTask,
                symDisplayColor, symSystemParamsSymbol, symFuelLevel);
	}

   /**
     * 
     */
    private void initializeSymbols() {
        symConnected = new ConnectionSymbol();
        
        symLoc = new LocationSymbol() {
            @Override
            public boolean isRightClickable() {
                return true;
            }
            @Override
            void mouseClicked(MouseEvent e) {
                JPopupMenu pop = new JPopupMenu();
                if(this.isActive()) {
                    pop.add(new AbstractAction(I18n.text("Copy location")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ImcSystem sysA = ImcSystemsHolder.lookupSystemByName(id);
                            if (sysA != null) {
                                String loc = sysA.getLocation().getClipboardText();
                                ClipboardOwner owner = new ClipboardOwner() {
                                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                                    };
                                };
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(loc), owner);
                            }
                        }
                    });
                }
                pop.add(new AbstractAction(I18n.text("Paste location")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ImcSystem sysA = ImcSystemsHolder.lookupSystemByName(id);
                        if (sysA != null) {
                            @SuppressWarnings({ "unused" })
                            ClipboardOwner owner = new ClipboardOwner() {
                                public void lostOwnership(Clipboard clipboard, Transferable contents) {
                                };
                            };
                            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                            boolean hasTransferableText = (contents != null)
                                    && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                            if (hasTransferableText) {
                                try {
                                    String text = (String)contents.getTransferData(DataFlavor.stringFlavor);
                                    LocationType lt = new LocationType();
                                    lt.fromClipboardText(text);
                                    sysA.setLocation(lt);
                                }
                                catch (Exception e1) {
                                    NeptusLog.pub().error(e1);
                                }
                            }               
                        }
                    }
                });
                pop.show((Component) e.getSource(), e.getX(), e.getY());
            };
        };
        
        symMain = new MainVehicleSymbol();
        
        symAuth = new AuthoritySymbol() {
            {
                fullOrNoneOnly = true;
            }
            @Override
            public boolean isRightClickable() {
                return true;
            }
            @Override
            void mouseClicked(MouseEvent e) {
                final ImcSystem sysA = ImcSystemsHolder.lookupSystemByName(id);
                if (sysA != null) {
                    JPopupMenu pop = new JPopupMenu();
                    ButtonGroup chooseButtonGroup = new ButtonGroup();
                    for (final IMCAuthorityState as : ImcSystem.IMCAuthorityState.values()) {
                        JCheckBoxMenuItem rButton = new JCheckBoxMenuItem(new AbstractAction(as.toString()) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                sysA.setAuthorityState(as);
                            }
                        });
                        rButton.setSelected(as == sysA.getAuthorityState());
                        boolean isEnable = true;
                        if (as.toString().toLowerCase().contains("payload")
                                || as.toString().toLowerCase().contains("monitor")) {
                            isEnable = false;
                        }
                        rButton.setEnabled(isEnable);
                        if (isEnable) {
                            chooseButtonGroup.add(rButton);
                            pop.add(rButton);
                        }
                    }
                    pop.show((Component) e.getSource(), e.getX(), e.getY());
                }
            };
        };
        
        symTask = new TaskSymbol();

        symAttention = new AttentionSymbol();
        
        symType = new SystemTypeSymbol();
        
        symIdAttention = new IdProblemSymbol();
        
        sysEmergencyTask = new EmergencyTaskSymbol();
        
        symDisplayColor = new DisplayColorSymbol() {
            @Override
            public boolean isRightClickable() {
                return this.isActive();
            };
            @Override
            void mouseClicked(MouseEvent e) {
                Color newColor = JColorChooser.showDialog(this, I18n.text("System color"), getDisplayColor());
                if (newColor == null || getDisplayColor() == newColor)
                    return;
                VehicleType veh = VehiclesHolder.getVehicleById(id);
                if (veh != null) {
                    veh.setIconColor(newColor);
                    String filePath = veh.getOriginalFilePath();
                    Document doc = veh.asDocument();
                    if (VehicleType.validate(doc)) {
                        String dataToSave = FileUtil.getAsPrettyPrintFormatedXMLString(doc);
                        FileUtil.saveToFile(filePath, dataToSave);                                                              
                    }
                }
            };
        };
        
        symSystemParamsSymbol = new SystemParamsSymbol() {
            @Override
            public boolean isRightClickable() {
                return this.isActive();
            };
            @Override
            void mouseClicked(MouseEvent e) {
                systemConfEditor = new SystemConfigurationEditorPanel(id, Scope.GLOBAL,
                        Visibility.USER, true, false, true, ImcMsgManager.getManager());
                JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(SystemDisplay.this));
                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                dialog.add(systemConfEditor);
                dialog.setSize(600, 600);
                GuiUtils.centerParent(dialog, dialog.getOwner());
                dialog.setVisible(true);
                systemConfEditor = null;
                dialog.dispose();
            };
        };
        if (ConfigurationManager.getInstance().hasProperties(id, Visibility.DEVELOPER, Scope.GLOBAL))
            symSystemParamsSymbol.setActive(true);
        else
            symSystemParamsSymbol.setActive(false);
        
        symFuelLevel = new FuelLevelSymbol();
    }

/**
     * Call this to dispose of the component. 
     */
    public void dispose() {
        PeriodicUpdatesService.unregister(this);
        symConnected.dispose();
        symLoc.dispose();
        symMain.dispose();
        symAuth.dispose();
        symTask.dispose();
        symAttention.dispose();
        symType.dispose();
        symIdAttention.dispose();
        sysEmergencyTask.dispose();
        symDisplayColor.dispose();
        symSystemParamsSymbol.dispose();
    }

	/**
     * @return the showExtraInfoVisible
     */
    public boolean isShowExtraInfoVisible() {
        return showExtraInfoVisible;
    }
    
    /**
     * @param showExtraInfoVisible the showExtraInfoVisible to set
     */
    public void setShowExtraInfoVisible(boolean showExtraInfoVisible) {
        this.showExtraInfoVisible = showExtraInfoVisible;
        infoLabel.setVisible(showExtraInfoVisible);
        if (showExtraInfoVisible)
            expandButton.setIcon(ICON_UP); //.setText("\u25B2");
        else
            expandButton.setIcon(ICON_DOWN); //.setText("\u25BC");
    }
    
    /**
     * @param info
     */
    public void setInfoLabel(String info) {
        this.infoLabel.setText(info);
    }
    
	/**
     * 
     */
    public void toggleShowMoreInfoVisible() {
        setShowExtraInfoVisible(!isShowExtraInfoVisible());
    }

    /* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SystemDisplay o) {
		String thisVal = this.id;
		String anotherVal = o.id;
		return thisVal.compareTo(anotherVal);
	}

	@Override
	public boolean equals(Object obj) {
		try {
            return (compareTo((SystemDisplay) obj) == 0) ? true : false;
		} catch (Exception e) { 
			return false;
		}
	}

	/**
	 * @return the indicatorsSize
	 */
	public int getIndicatorsSize() {
		return indicatorsSize;
	}
	
	/**
	 * @param indicatorsSize the indicatorsSize to set
	 */
	public void setIndicatorsSize(int indicatorsSize) {
		this.indicatorsSize = indicatorsSize;
		symConnected.setPreferredSize(new Dimension(indicatorsSize,indicatorsSize));
		symConnected.setSize(new Dimension(indicatorsSize,indicatorsSize));
		symAuth.setPreferredSize(new Dimension(indicatorsSize,indicatorsSize));
		symAuth.setSize(new Dimension(indicatorsSize,indicatorsSize));
		validate();
		repaint();
	}
	
	/**
	 * @return the iconSize
	 */
	public int getIconSize() {
		return iconSize;
	}
	
	/**
	 * @param iconSize the iconSize to set
	 */
	public void setIconSize(int iconSize) {
		this.iconSize = iconSize;
		if (systemImage != null) {
			icon.setImage(systemImage.getScaledInstance(-1, iconSize, Image.SCALE_SMOOTH));
			validate();
			repaint();
		}
	}
	
	/**
     * @return the incrementFontSize
     */
    public int getIncrementFontSize() {
        return incrementFontSize;
    }
    
    /**
     * @param incrementFontSize the incrementFontSize to set
     */
    public void setIncrementFontSize(int incrementFontSize) {
        this.incrementFontSize = incrementFontSize < 0 ? 0 : incrementFontSize;
        if (this.incrementFontSize == 0)
            label.setFont(labelOriginFont);
        else
            label.setFont(labelOriginFont.deriveFont(labelOriginFont.getSize() + incrementFontSize));
    }

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

//	/**
//	 * @param id the id to set
//	 */
//	private void setId(String id) {
//		this.id = id;
//	}
	
    /**
	 * @return the systemImage
	 */
	public Image getSystemImage() {
		return systemImage;
	}
	
	/**
	 * @param systemImage the systemImage to set
	 */
	public void setSystemImage(Image systemImage) {
		this.systemImage = systemImage;
		icon = new ImageIcon(systemImage.getScaledInstance(-1, iconSize,
				Image.SCALE_SMOOTH));
		label.setIcon(icon);
		repaint();
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}
	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
		symConnected.setActive(active);
		reCalculateBackgroundColor();
		repaint();
	}
	
	/**
	 * @param text
	 */
	public void setActiveToolTip(String text) {
		symConnected.setToolTipText(text);
	}
	
	
	/**
	 * @return the strength
	 */
	public ConnectionStrengthEnum getConnectionStrength() {
		return symConnected.getStrength();
	}
	
	/**
	 * @param strength the strength to set
	 */
	public void setConnectionStrength(ConnectionStrengthEnum strength) {
		symConnected.setStrength(strength);
	}
	
	/**
	 * 
	 */
	public void setFullConnectionStrength() {
		symConnected.setStrength(ConnectionStrengthEnum.FULL);
	}

	/**
	 * @return
	 */
	public ConnectionStrengthEnum reduceConnectionStrength() {
		return symConnected.reduceStrength();
	}

	/**
     * @return the announceReceived
     */
    public boolean isAnnounceReceived() {
        return symConnected.isActiveAnnounce();
    }
    
    /**
     * @param announceReceived the announceReceived to set
     */
    public void setAnnounceReceived(boolean announceReceived) {
        symConnected.setActiveAnnounce(announceReceived);
    }
	
	/**
	 * @return the selected
	 */
	public boolean isSelected() {
		return selected;
	}
	
	/**
	 * @param selected the selected to set
	 */
	public void setSelected(boolean selected) {
        if (isEnableSelection()) {
            if (!selected)
                selectionTimeMillis = -1;
            else {
                if (!this.selected)
                    selectionTimeMillis = System.currentTimeMillis();
            }
        }
		this.selected = selected;
		if (!isEnableSelection())
		    this.selected = false;
		reCalculateBorderColor();
		repaint();
	}
	
	/**
	 * Toggles the boolean value
	 */
	public boolean toggleSelected() {
		setSelected(!selected);
		return selected;
	}

	/**
     * @return the enableSelection
     */
    public boolean isEnableSelection() {
        return enableSelection;
    }
    
    /**
     * @param enableSelection the enableSelection to set
     */
    public void setEnableSelection(boolean enableSelection) {
        this.enableSelection = enableSelection;
    }
	
    /**
     * The selection time for this SystemDisplay. Equals normally to -1 
     * if not selected, but one should check {@link #isSelected()} to see
     * if this value has meaning.
     * @return the selectionTimeMillis
     */
    public long getSelectionTimeMillis() {
        return selectionTimeMillis;
    }
    
    /**
     * @return the displayColor
     */
    public Color getDisplayColor() {
        return displayColor;
    }
    
    /**
     * @param displayColor the displayColor to set
     */
    public void setDisplayColor(Color displayColor) {
        this.displayColor = displayColor;
        if (displayColor == null) {
            symDisplayColor.setActive(false);
        }
        else {
            symDisplayColor.setDisplayColor(displayColor);
            symDisplayColor.setActive(true);
        }
    }

    /**
     * @return
     */
    public boolean isLocationKnown() {
        return symLoc.isActive();
    }
    
    /**
     * @param locationKnown
     */
    public void setLocationKnown(boolean locationKnown) {
        symLoc.setActive(locationKnown);
    }    

    /**
     * @return
     */
    public String getLocationKnownToolTip() {
        return symLoc.getToolTipText();
    }
    
    /**
     * @param text
     */
    public void setLocationKnownToolTip(String text) {
        symLoc.setToolTipText(text);
    }

	/**
	 * @return the mainVehicle
	 */
	public boolean isMainVehicle() {
		return mainVehicle;
	}
	
	/**
	 * @param mainVehicle the mainVehicle to set
	 */
	public void setMainVehicle(boolean mainVehicle) {
		this.mainVehicle = mainVehicle;
		symMain.setActive(mainVehicle);
		reCalculateBackgroundColor();
		reCalculateBorderColor();
		if (mainVehicle)
			setMainVehicleToolTip(I18n.text("Main vehicle"));
		else
			setMainVehicleToolTip(null);
		repaint();
	}
	
	/**
	 * @param text
	 */
	public void setMainVehicleToolTip(String text) {
		symMain.setToolTipText(text);
	}

	/**
	 * Toggles the boolean value
	 */
	public boolean toggleMainVehicle() {
		setMainVehicle(!selected);
		return selected;
	}

	/**
	 * @return the showSystemSymbolOrText
	 */
	public boolean isShowSystemSymbolOrText() {
		return showSystemSymbolOrText;
	}
	
	/**
	 * @param showSystemSymbolOrText the showSystemSymbolOrText to set
	 */
	public void setShowSystemSymbolOrText(boolean showSystemSymbolOrText) {
		this.showSystemSymbolOrText = showSystemSymbolOrText;
		symType.setShowSymbolOrText(showSystemSymbolOrText);
		repaint();
	}

	/**
	 * Toggles the boolean value
	 */
	public boolean toggleShowSystemSymbolOrText() {
		setShowSystemSymbolOrText(!showSystemSymbolOrText);
		return showSystemSymbolOrText;
	}

	/**
	 * @return the systemType
	 */
	public String getSystemType() {
		return systemType;
	}
	
	/**
	 * @param systemType the systemType to set
	 */
	public void setSystemType(String systemType) {
		this.systemType = systemType;
		symType.setSystemType(systemType);
	}
	
	/**
     * 
     */
    public boolean isWithAuthority() {
        if (withAuthority != ImcSystem.IMCAuthorityState.NONE && withAuthority != ImcSystem.IMCAuthorityState.OFF)
            return true;
        else
            return false;
    }
	
	/**
	 * @return the withAuthority
	 */
	public ImcSystem.IMCAuthorityState getWithAuthority() {
		return withAuthority;
	}
	
	/**
	 * @param withAuthority the withAuthority to set
	 */
	public void setWithAuthority(ImcSystem.IMCAuthorityState withAuthority) {
		this.withAuthority = withAuthority;
		symAuth.setAuthorityType(withAuthority);
		reCalculateBackgroundColor();
		repaint();
	}
	
	/**
	 * @param text
	 */
	public void setWithAuthorityToolTip(String text) {
		symAuth.setToolTipText(text);
	}

	/**
	 * @return the taskAlocated
	 */
	public boolean isTaskAlocated() {
		return taskAlocated;
	}
	
	/**
	 * @param taskAlocated the taskAlocated to set
	 */
	public void setTaskAlocated(boolean taskAlocated) {
		this.taskAlocated = taskAlocated;
		symTask.setActive(taskAlocated);
		repaint();
	}

	/**
	 * @param text
	 */
	public void setTaskAlocatedToolTip(String text) {
		symTask.setToolTipText(text);
	}

	/**
	 * @return the emergencyTaskAlocated
	 */
	public boolean isEmergencyTaskAlocated() {
		return emergencyTaskAlocated;
	}
	
	/**
	 * @param emergencyTaskAlocated the emergencyTaskAlocated to set
	 */
	public void setEmergencyTaskAlocated(boolean emergencyTaskAlocated) {
		this.emergencyTaskAlocated = emergencyTaskAlocated;
		sysEmergencyTask.setActive(emergencyTaskAlocated);
		repaint();
	}

	public void setEmergencyTaskAlocatedToolTip(String text) {
		sysEmergencyTask.setToolTipText(text);
	}

	public EmergencyStatus getEmergencyStatus() {
		return sysEmergencyTask.getStatus();
	}
	
	public void setEmergencyStatus(EmergencyStatus status) {
		sysEmergencyTask.setStatus(status);
	}
	
	/**
	 * @return the attentionAlert
	 */
	public boolean isAttentionAlert() {
		return attentionAlert;
	}
	
	/**
	 * @param attentionAlert the attentionAlert to set
	 */
	public void setAttentionAlert(boolean attentionAlert) {
		this.attentionAlert = attentionAlert;
		symAttention.setActive(attentionAlert);
		reCalculateBackgroundColor();
		repaint();
	}

    public boolean isFuelLevel() {
        return symFuelLevel.isActive();
    }
    
    public void setFuelLevel(boolean active) {
        symFuelLevel.setActive(active);
    }
	
	/**
     * -1 for none
     */
    public double getFuelLevelPercentage() {
        return symFuelLevel.getPercentage();
    }
	/**
     * -1 for none
     */
    public void setFuelLevelPercentage(double value) {
        symFuelLevel.setPercentage(value);
    }

    /**
     * @return
     */
    public String getFuelLevelToolTip() {
        return symFuelLevel.getToolTipText();
    }
    
    /**
     * @param text
     */
    public void setFuelLevelToolTip(String text) {
        symFuelLevel.setToolTipText(text);
    }

	/**
	 * @return
	 */
	public String getAttentionToolTip() {
        return symAttention.getToolTipText();
    }
	
	/**
	 * @param text
	 */
	public void setAttentionToolTip(String text) {
		symAttention.setToolTipText(text);
	}

	/**
	 * @return the idAlert
	 */
	public boolean isIdAlert() {
		return idAlert;
	}
	
	/**
	 * @param idAlert the idAlert to set
	 */
	public void setIdAlert(boolean idAlert) {
		this.idAlert = idAlert;
		symIdAttention.setActive(idAlert);
		repaint();
	}
	
	public void setIdAttentionToolTip(String text) {
		symIdAttention.setToolTipText(text);
	}

	private void reCalculateBackgroundColor() {
	    boolean withAuthorityBoolean = false;
        if (withAuthority != ImcSystem.IMCAuthorityState.NONE && withAuthority != ImcSystem.IMCAuthorityState.OFF)
	        withAuthorityBoolean = true;
	    
		if (active && withAuthorityBoolean) {
			if (attentionAlert || idAlert)
				updateBackColor(COLOR_ORANGE, null);
			else
				updateBackColor(COLOR_BLUE, null);
		}
		else if (!active && withAuthorityBoolean) {
			if (attentionAlert || idAlert)
				updateBackColor(COLOR_RED, null);
			else
				updateBackColor(COLOR_BLUE.darker().darker(), null); // #15596F
		}
		else if (active && !withAuthorityBoolean) {
			updateBackColor(COLOR_OLD, null);
		}
		else {
			updateBackColor(COLOR_IDLE, null);
		}
	}

	private void reCalculateBorderColor() {
		if (selected)
			updateBackColor(null, COLOR_ORANGE.brighter());
		else
			if (isMainVehicle())
				updateBackColor(null, COLOR_GREEN);
			else
				updateBackColor(null, COLOR_IDLE);

	}

	
	//Background Painter Stuff
	private RectanglePainter rectPainter;
	private CompoundPainter<JXPanel> compoundBackPainter;
	private GlossPainter glossy = null;
	
	/**
	 * @return the rectPainter
	 */
	private RectanglePainter getRectPainter() {
		if (rectPainter == null) {
	        rectPainter = new RectanglePainter(5,5,5,5, 10,10);
	        rectPainter.setFillPaint(COLOR_IDLE);
	        rectPainter.setBorderPaint(COLOR_IDLE.darker().darker().darker());
	        rectPainter.setStyle(RectanglePainter.Style.BOTH);
	        rectPainter.setBorderWidth(6);
	        rectPainter.setAntialiasing(true);//RectanglePainter.Antialiasing.On);
		}
		return rectPainter;
	}
	
	
	/**
	 * @return the compoundBackPainter
	 */
	private CompoundPainter<JXPanel> getCompoundBackPainter() {
	    if (compoundBackPainter == null) {
	        glossy = new GlossPainter();
            compoundBackPainter = new CompoundPainter<JXPanel>(getRectPainter(), glossy);	        
	    }
	    else
	        compoundBackPainter.setPainters(getRectPainter(), glossy);
	    
		return compoundBackPainter;
	}
	/**
	 * @param color
	 */
	private void updateBackColor(Color color, Color borderColor) {
		if (color != null)
			getRectPainter().setFillPaint(color);
		if (borderColor != null)
			getRectPainter().setBorderPaint(borderColor/*.darker().darker().darker()*/);

		if (color != null || borderColor != null)
		this.setBackgroundPainter(getCompoundBackPainter());
	}

	
	public void blink(boolean blink) {
		if (blink && (blinkingState == BlinkingStateEnum.NOT_BLINKING)) {
			blinkingState = BlinkingStateEnum.BLINKING_NORMAL;
			PeriodicUpdatesService.register(this);
		}
		else if (!blink) {
			blinkingState = BlinkingStateEnum.NOT_BLINKING;
		}
	}

	/*
	 * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
	 */
	@Override
	public long millisBetweenUpdates() {
		switch (blinkingState) {
		case NOT_BLINKING:
			return 0;
		case BLINKING_NORMAL:
			return 200;
		case BLINKING_BRILLIANT:
			return 900;
		default:
			return 0;
		}
	}


	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
	 */
	@Override
	public boolean update() {
		if (blinkingState == BlinkingStateEnum.NOT_BLINKING)
			return false;
		
		if (blinkingState == BlinkingStateEnum.BLINKING_NORMAL)
			blinkingState = BlinkingStateEnum.BLINKING_BRILLIANT;
		else
			blinkingState = BlinkingStateEnum.BLINKING_NORMAL;
		repaint();
		return true;
	}

	private long lastUpdatedTimeMillis = -1;
	/**
     * @return the systemConfEditor
     */
    public void updateSystemParameters(EntityParameters message) {
        if (systemConfEditor != null) {
            try {
                if (lastUpdatedTimeMillis < message.getTimestampMillis()) {
                    SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(systemConfEditor, message);
                    lastUpdatedTimeMillis = message.getTimestampMillis();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		SystemDisplay sys1 = new SystemDisplay("lauv-seacon-1");
		Image image = new ImageIcon("vehicles-files\\lauv\\conf\\images\\lauv-seacon0-presentation.png").getImage();
		sys1.setSystemImage(image);
		sys1.setSystemType("UUV");

		SystemDisplay sys2 = new SystemDisplay("lauv-seacon-lsts");
		SystemDisplay sys3 = new SystemDisplay("lauv-seacon-lsts");

		final JXPanel panel = new JXPanel(true);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(sys2);
		panel.add(sys1);
//		panel.add(sys2);
//		panel.add(sys3);

//		sys2.setActive(true);
//		Image image = new ImageIcon("vehicles-files\\lauv\\conf\\images\\lauv-seacon0-presentation.png").getImage();
//		sys1.setSystemImage(image);
//
//		image = new ImageIcon("vehicles-files\\lauv\\conf\\images\\lauv-seacon0-presentation.png").getImage();
//		sys2.setSystemImage(image);
//
		GuiUtils.testFrame(panel);
//
//		GuiUtils.testFrame(sys1);
		
		sys1.setActive(true);
		sys1.setTaskAlocated(true);
		sys1.setWithAuthority(ImcSystem.IMCAuthorityState.NONE);
		//sys1.setMainVehicle(true);
		sys1.setAttentionAlert(true);
		
//		try {Thread.sleep(5000);} catch (Exception e) {}
////		sys1.setIconSize(100);
////		sys1.setIndicatorsSize(70);
//
//		try {Thread.sleep(3000);} catch (Exception e) {}
//        sys1.setActive(true);
//        sys1.setTaskAlocated(false);
//        sys1.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_FULL);
//        sys1.setMainVehicle(true);
//        sys1.setAttentionAlert(false);
//
//        try {Thread.sleep(3000);} catch (Exception e) {}
//        sys1.setActive(true);
//        sys1.setTaskAlocated(true);
//        sys1.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_MONITOR);
//        sys1.setMainVehicle(false);
//        sys1.setAttentionAlert(true);
//
//        try {Thread.sleep(7000);} catch (Exception e) {}
//        sys1.setActive(true);
//        sys1.setTaskAlocated(false);
//        sys1.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_FULL);
//        sys1.setMainVehicle(true);
//        sys1.setAttentionAlert(false);
//
//        try {Thread.sleep(7000);} catch (Exception e) {}
//        sys1.setActive(true);
//        sys1.setTaskAlocated(true);
//        sys1.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_MONITOR);
//        sys1.setMainVehicle(false);
//        sys1.setAttentionAlert(true);
//
//        try {Thread.sleep(2000);} catch (Exception e) {}
//        sys1.setActive(true);
//        sys1.setTaskAlocated(false);
//        sys1.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_FULL);
//        sys1.setMainVehicle(true);
//        sys1.setAttentionAlert(false);
//
//        try {Thread.sleep(1000);} catch (Exception e) {}
//        sys1.setActive(true);
//        sys1.setTaskAlocated(true);
//        sys1.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_MONITOR);
//        sys1.setMainVehicle(false);
//        sys1.setAttentionAlert(true);
	}
}
