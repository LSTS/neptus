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
 * 28/Jun/2005
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.mme.MissionMapEditor;
import pt.up.fe.dceg.neptus.mme.MoveObjectEdit;
import pt.up.fe.dceg.neptus.mme.RotateObjectEdit;
import pt.up.fe.dceg.neptus.mme.ScaleObjectEdit;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.EllipsoidElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.RotatableElement;
import pt.up.fe.dceg.neptus.types.map.ScalableElement;
import pt.up.fe.dceg.neptus.util.GuiUtils;
/**
 * @author ZP
 */
public class MapObjectInteraction extends JPanel {


	private static final long serialVersionUID = 8053634486397085258L;
	
	private JPanel jPanel = null;
	private JLabel jLabel = null;
	private JLabel objIdLabel = null;
	private JPanel jPanel1 = null;
	private JPanel translationPanel = null;
	private JButton moveNorth = null;
	private JButton moveSouth = null;
	private JButton moveWest = null;
	private JButton moveEast = null;
	private JPanel rotationPanel = null;
	private JButton rotateLeft = null;
	private JButton rotateRight = null;
	private JPanel rotateScalePanel = null;
	private JPanel scalePanel = null;
	private JButton scaleBigger = null;
	private JButton scaleSmaller = null;
	private AbstractElement interactingObject = null;
	private MissionMapEditor mme = null;
	private final int ROTATE_LEFT = 0, ROTATE_RIGHT = 1, MOVE_NORTH = 2, MOVE_SOUTH = 3, MOVE_EAST = 4,
		MOVE_WEST = 5, SHRINK = 6, GROW = 7, NOTHING = -1;
	private Thread transformingThread = null;
	
	private LocationType originalLocation = new LocationType();
	private double originalRotation = 0;
	private double[] originalDimension = new double[3];
	
	/**
	 * This method initializes 
	 * 
	 */
	public MapObjectInteraction(MissionMapEditor mme) {
		super();
		this.mme = mme;
		initialize();
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(321, 164);
        this.setPreferredSize(new java.awt.Dimension(200,150));
        this.add(getJPanel(), java.awt.BorderLayout.NORTH);
        this.add(getInteractionsPanel(), java.awt.BorderLayout.CENTER);
			
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			objIdLabel = new JLabel();
			jLabel = new JLabel();
			jPanel = new JPanel();
			jLabel.setText("Interacting with ");
			objIdLabel.setText(" ");
			objIdLabel.setForeground(java.awt.Color.blue);
			jPanel.add(objIdLabel, null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getInteractionsPanel() {
		if (jPanel1 == null) {
			FlowLayout flowLayout4 = new FlowLayout();
			jPanel1 = new JPanel();
			jPanel1.setLayout(flowLayout4);
			flowLayout4.setHgap(5);
			flowLayout4.setVgap(0);
			jPanel1.add(getTranslationPanel(), null);
			jPanel1.add(getRotateScalePanel(), null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes translationPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getTranslationPanel() {
		if (translationPanel == null) {
			translationPanel = new JPanel();
			translationPanel.setLayout(null);
			translationPanel.setPreferredSize(new java.awt.Dimension(100,100));
			translationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Translation", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			translationPanel.add(getMoveNorth(), null);
			translationPanel.add(getMoveSouth(), null);
			translationPanel.add(getMoveWest(), null);
			translationPanel.add(getMoveEast(), null);
		}
		return translationPanel;
	}
	/**
	 * This method initializes moveNorth	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getMoveNorth() {
		if (moveNorth == null) {
			moveNorth = new JButton();
			moveNorth.setBounds(40, 20, 20, 20);
			moveNorth.setIcon(new ImageIcon(getClass().getResource("/images/buttons/move_north.png")));
			moveNorth.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null)
						return;
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(MOVE_NORTH, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(MOVE_NORTH));					
					transformingThread.start();
					originalLocation = new LocationType(getInteractingObject().getCenterLocation());
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					MoveObjectEdit edit = new MoveObjectEdit(getInteractingObject(), originalLocation, new LocationType(getInteractingObject().getCenterLocation()));
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return moveNorth;
	}
	/**
	 * This method initializes moveSouth	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getMoveSouth() {
		if (moveSouth == null) {
			moveSouth = new JButton();
			moveSouth.setBounds(40, 66, 20, 20);
			moveSouth.setIcon(new ImageIcon(getClass().getResource("/images/buttons/move_south.png")));
			moveSouth.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null)
						return;		
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(MOVE_SOUTH, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(MOVE_SOUTH));
					transformingThread.start();
					originalLocation = new LocationType(getInteractingObject().getCenterLocation());
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					MoveObjectEdit edit = new MoveObjectEdit(getInteractingObject(), originalLocation, new LocationType(getInteractingObject().getCenterLocation()));
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return moveSouth;
	}
	/**
	 * This method initializes moveWest	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getMoveWest() {
		if (moveWest == null) {
			moveWest = new JButton();
			moveWest.setBounds(17, 43, 20, 20);
			moveWest.setIcon(new ImageIcon(getClass().getResource("/images/buttons/move_west.png")));
			moveWest.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null)
						return;		
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(MOVE_WEST, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(MOVE_WEST));
					transformingThread.start();
					originalLocation = new LocationType(getInteractingObject().getCenterLocation());
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					MoveObjectEdit edit = new MoveObjectEdit(getInteractingObject(), originalLocation, new LocationType(getInteractingObject().getCenterLocation()));
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return moveWest;
	}
	/**
	 * This method initializes moveEast	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getMoveEast() {
		if (moveEast == null) {
			moveEast = new JButton();
			moveEast.setBounds(63, 43, 20, 20);
			moveEast.setIcon(new ImageIcon(getClass().getResource("/images/buttons/move_east.png")));
			moveEast.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null)
						return;		
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(MOVE_EAST, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(MOVE_EAST));
				
					transformingThread.start();
					originalLocation = new LocationType(getInteractingObject().getCenterLocation());
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					MoveObjectEdit edit = new MoveObjectEdit(getInteractingObject(), originalLocation, new LocationType(getInteractingObject().getCenterLocation()));
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return moveEast;
	}
	/**
	 * This method initializes rotationPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getRotationPanel() {
		if (rotationPanel == null) {
			FlowLayout flowLayout1 = new FlowLayout();
			rotationPanel = new JPanel();
			rotationPanel.setLayout(flowLayout1);
			rotationPanel.setPreferredSize(new java.awt.Dimension(100,50));
			rotationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Rotation", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			flowLayout1.setHgap(10);
			flowLayout1.setVgap(0);
			rotationPanel.add(getRotateLeft(), null);
			rotationPanel.add(getRotateRight(), null);
		}
		return rotationPanel;
	}
	/**
	 * This method initializes rotateLeft	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getRotateLeft() {
		if (rotateLeft == null) {
			rotateLeft = new JButton();
			rotateLeft.setPreferredSize(new java.awt.Dimension(20,20));
			rotateLeft.setIcon(new ImageIcon(getClass().getResource("/images/buttons/rotate_left.png")));
			rotateLeft.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null || 
							!(getInteractingObject() instanceof RotatableElement))
						return;
					
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(ROTATE_LEFT, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(ROTATE_LEFT));

					transformingThread.start();
					originalRotation = ((RotatableElement)getInteractingObject()).getYaw();
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					RotateObjectEdit edit = new RotateObjectEdit(getInteractingObject(), originalRotation, ((RotatableElement)getInteractingObject()).getYaw());
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return rotateLeft;
	}
	/**
	 * This method initializes rotateRight	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getRotateRight() {
		if (rotateRight == null) {
			rotateRight = new JButton();
			rotateRight.setPreferredSize(new java.awt.Dimension(20,20));
			rotateRight.setIcon(new ImageIcon(getClass().getResource("/images/buttons/rotate_right.png")));
			rotateRight.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null || 
							!(getInteractingObject() instanceof RotatableElement))
						return;
					
					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(ROTATE_RIGHT, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(ROTATE_RIGHT));					
					transformingThread.start();
					originalRotation = ((RotatableElement)getInteractingObject()).getYaw();
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					RotateObjectEdit edit = new RotateObjectEdit(getInteractingObject(), originalRotation, ((RotatableElement)getInteractingObject()).getYaw());
					mme.getUndoSupport().postEdit(edit);
				}
			});	
		}
		return rotateRight;
	}
	/**
	 * This method initializes rotateScalePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getRotateScalePanel() {
		if (rotateScalePanel == null) {
			rotateScalePanel = new JPanel();
			GridLayout gridLayout2 = new GridLayout();
			rotateScalePanel.setLayout(gridLayout2);
			gridLayout2.setRows(2);
			gridLayout2.setColumns(1);
			rotateScalePanel.add(getRotationPanel(), null);
			rotateScalePanel.add(getScalePanel(), null);
		}
		return rotateScalePanel;
	}
	/**
	 * This method initializes scalePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getScalePanel() {
		if (scalePanel == null) {
			FlowLayout flowLayout3 = new FlowLayout();
			scalePanel = new JPanel();
			scalePanel.setLayout(flowLayout3);
			scalePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Scale", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			flowLayout3.setHgap(10);
			flowLayout3.setVgap(0);
			scalePanel.add(getScaleBigger(), null);
			scalePanel.add(getScaleSmaller(), null);
		}
		return scalePanel;
	}
	/**
	 * This method initializes scaleBigger	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getScaleBigger() {
		if (scaleBigger == null) {
			scaleBigger = new JButton();
			scaleBigger.setPreferredSize(new java.awt.Dimension(20,20));
			scaleBigger.setIcon(new ImageIcon(getClass().getResource("/images/buttons/scale_bigger.png")));
			scaleBigger.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null || 
							!(getInteractingObject() instanceof ScalableElement))
						return;

					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(GROW, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(GROW));
					
					transformingThread.start();
					originalDimension = ((ScalableElement)getInteractingObject()).getDimension();
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					ScaleObjectEdit edit = new ScaleObjectEdit(getInteractingObject(), originalDimension, ((ScalableElement)getInteractingObject()).getDimension());
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return scaleBigger;
	}
	/**
	 * This method initializes scaleSmaller	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getScaleSmaller() {
		if (scaleSmaller == null) {
			scaleSmaller = new JButton();
			scaleSmaller.setPreferredSize(new java.awt.Dimension(20,20));
			scaleSmaller.setIcon(new ImageIcon(getClass().getResource("/images/buttons/scale_smaller.png")));
			scaleSmaller.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (getInteractingObject() == null || 
							!(getInteractingObject() instanceof ScalableElement))
						return;

					if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
						transformingThread = new Thread(new Transformer(SHRINK, 0.1));
					}
					else
						transformingThread = new Thread(new Transformer(SHRINK));

					transformingThread.start();
					originalDimension = ((ScalableElement)getInteractingObject()).getDimension();
				}
				public void mouseReleased(MouseEvent e) {
					transformingThread = null;
					ScaleObjectEdit edit = new ScaleObjectEdit(getInteractingObject(), originalDimension, ((ScalableElement)getInteractingObject()).getDimension());
					mme.getUndoSupport().postEdit(edit);
				}
			});
		}
		return scaleSmaller;
	}
	
	public AbstractElement getInteractingObject() {
		return interactingObject;
	}
	
	public void setInteractingObject(AbstractElement interactingObject) {
		this.interactingObject = interactingObject;
				
			
			getRotateScalePanel().add(getRotationPanel());
			getRotateScalePanel().add(getScalePanel());
		
		if (interactingObject == null || interactingObject instanceof HomeReferenceElement) {
			objIdLabel.setText("");
		}

		else {
			
			if (!(interactingObject instanceof RotatableElement)) {
				getRotateScalePanel().remove(getRotationPanel());
			}
			if (!(interactingObject instanceof ScalableElement)) {
				getRotateScalePanel().remove(getScalePanel());
			}
			
			getRotateScalePanel().doLayout();
			
			objIdLabel.setText(interactingObject.getId());
		}
		
		
	}
	
	public static void main(String[] args) {
		EllipsoidElement ellis = new EllipsoidElement(null, null);
		ellis.setCenterLocation(new LocationType());
		MapObjectInteraction moi = new MapObjectInteraction(null);
		moi.setInteractingObject(ellis);
		GuiUtils.testFrame(moi, "Testing...");
	}
	
	class Transformer implements Runnable {
		
		private int action = NOTHING;
		private double baseIncrement = 1.0;
		public Transformer(int action) {
			setAction(action);
		}
		
		public Transformer(int action, double baseIncrement) {
			setAction(action);
			this.baseIncrement = baseIncrement;
		}
		
		public void setAction(int action) {
			this.action = action;
		}
		
		public void run() {

			double incrementMultiplier = 1.0;
			int count = 0;

			MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
			mce.setChangedObject(interactingObject);
			
			while (transformingThread != null) {
				
				if ((count % 10) == 0)
					incrementMultiplier *= 1.25;
				double increment = baseIncrement * incrementMultiplier;
				
				switch(action) {
					case(MOVE_EAST):						
						interactingObject.getCenterLocation().translatePosition(0,increment,0);
						mce.setChangeType(MapChangeEvent.OBJECT_MOVED);
						break;
					case(MOVE_NORTH):
						interactingObject.getCenterLocation().translatePosition(increment,0,0);
						mce.setChangeType(MapChangeEvent.OBJECT_MOVED);
						break;
					case(MOVE_WEST):
						interactingObject.getCenterLocation().translatePosition(0,-increment,0);
						mce.setChangeType(MapChangeEvent.OBJECT_MOVED);
						break;
					case(MOVE_SOUTH):
						interactingObject.getCenterLocation().translatePosition(-increment,0,0);
						mce.setChangeType(MapChangeEvent.OBJECT_MOVED);
						break;
					case(GROW):
						((ScalableElement)interactingObject).grow(increment);
						mce.setChangeType(MapChangeEvent.OBJECT_SCALED);
						break;
					case(SHRINK):
						mce.setChangeType(MapChangeEvent.OBJECT_SCALED);
						((ScalableElement)interactingObject).shrink(increment);
						break;
					case(ROTATE_LEFT):
						mce.setChangeType(MapChangeEvent.OBJECT_ROTATED);
						((RotatableElement)interactingObject).rotateLeft(increment);
						break;
					case(ROTATE_RIGHT):
						mce.setChangeType(MapChangeEvent.OBJECT_ROTATED);
						((RotatableElement)interactingObject).rotateRight(increment);
						break;
					default:
						break;
				}
				
				interactingObject.getMapGroup().warnListeners(mce);
				
				mme.setMapChanged(true);
				
				try {
					Thread.sleep(100);
				}
				catch (Exception e) {}
				count++;
			}
		}
	}
}  //  @jve:decl-index=0:visual-constraint="11,-4"