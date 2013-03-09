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
 * ????/??/??
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.BusyPainter;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
public class InfiniteProgressPanel extends JPanel {
	
	private JLabel text = null;
	private JXBusyLabel busyLabel = null;
	
	public InfiniteProgressPanel() {
		initialize();
	}

	public InfiniteProgressPanel(JXBusyLabel busyLabel, String message) {
		setText(message);
		setBusyLabel(busyLabel);
		initialize();
	}

	public InfiniteProgressPanel(String message) {
		setText(message);
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.setAlignmentY(Component.CENTER_ALIGNMENT);
		//cPanel.add(Box.createRigidArea(new Dimension(10,10)));
		this.add(Box.createVerticalGlue());
		this.add(getBusyLabel());
		this.add(getText());
		this.add(Box.createVerticalGlue());
//		this.setBackground(ColorUtils.setTransparencyToColor(Color.WHITE, 200));
//		getBusyLabel().setBackground(this.getBackground());
		
		if (busyLabel != null)
		    busyLabel.setBusy(false);
	}

	
	/**
	 * @return
	 */
	public JXBusyLabel getBusyLabel() {
		if (busyLabel == null) {
			busyLabel = createBusyAnimationInfiniteBeans(200);
			busyLabel.setBusy(false);
		}
		return busyLabel;
	}

	/**
	 * @param busyLabel the busyLabel to set
	 */
	public void setBusyLabel(JXBusyLabel busyLabel) {
		this.busyLabel = busyLabel;
        if (this.busyLabel != null)
            this.busyLabel.setBusy(false);
		
	}
	
	/**
	 * @return the text
	 */
	private JLabel getText() {
		if (text == null) {
			text = new JLabel("<html><b>" + I18n.text("Wait please") + "...", JLabel.CENTER);
			text.setAlignmentX(Component.CENTER_ALIGNMENT);
			text.setAlignmentY(Component.CENTER_ALIGNMENT);
		}
		return text;
	}
	
    /**
	 * @param message
	 */
	public void setText(String message) {
	    if (message != null && !"".equalsIgnoreCase(message))
		    getText().setText("<html><b>" + message + "...");
	    else
	        getText().setText("");
	}
	
	public void start() {
	    busyLabel.setBusy(true);
	}
	
	public void stop() {
	    busyLabel.setBusy(false);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean aFlag) {
	    super.setVisible(aFlag);
	    if (aFlag)
	        start();
	    else
	        stop();
	}
	
	/**
     * @param size
     * @param shape
     * @param trajectory
     * @param trailLength
     * @param nShapes
     * @return
     */
    private static JXBusyLabel createBusyAnimation(int size, Shape shape,
			Shape trajectory, int trailLength, int nShapes) {
    	//RGB=168,204,241

    	float scale = 1/100f * size;
		
    	JXBusyLabel label = new JXBusyLabel(new Dimension((int)(scale*100), (int)(scale*100)));
		BusyPainter painter = new BusyPainter(shape, trajectory);
		painter.setTrailLength(trailLength);
		painter.setPoints(nShapes);
		painter.setFrame(1);
		painter.setBaseColor(new Color(168, 204, 241));
		//painter1.setHighlightColor(new Color(168,254,251));
		painter.setPaintCentered(true);
        label.setPreferredSize(new Dimension((int) (scale * 100), (int) (scale * 100)));
        label.setIcon(new EmptyIcon((int) (scale * 100), (int) (scale * 100)));
		label.setBusyPainter(painter);
		
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setAlignmentY(Component.CENTER_ALIGNMENT);

		label.setBusy(false);
		return label;
    }

    
    /**
     * @param label
     * @param message
     * @return
     */
    private static InfiniteProgressPanel createPanel (JXBusyLabel label, String message) {
//		JLabel text = new JLabel("<html><b>" + message + "...", JLabel.CENTER);
//		
//		text.setAlignmentX(Component.CENTER_ALIGNMENT);
//		text.setAlignmentY(Component.CENTER_ALIGNMENT);
//
//		JPanel cPanel = new JPanel();
//		cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.Y_AXIS));
//		cPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//		cPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
//		//cPanel.add(Box.createRigidArea(new Dimension(10,10)));
//		cPanel.add(Box.createVerticalGlue());
//		cPanel.add(label);
//		cPanel.add(text);
//		cPanel.add(Box.createVerticalGlue());
//		//cPanel.setBackground(Color.magenta);
//		return cPanel;
    	return new InfiniteProgressPanel(label, message);
    }
    
    
    
    
    //_____________________________________________________________________________
    
    /**
     * @param size
     * @return
     */
    public static JXBusyLabel createBusyAnimationInfiniteBeans (int size) {
    	float scale = 1/100f * size;
		return createBusyAnimation(size, new Ellipse2D.Float(0, 0,
				scale * 10.5f, scale * 24.5f), new Ellipse2D.Float(
				scale * 15.0f, scale * 15.0f, scale * 70.0f, scale * 70.0f), 5,
				8);
    }

	/**
	 * @param message
	 * @return
	 */
	public static InfiniteProgressPanel createInfinitePanelBeans(String message) {
		return createInfinitePanelBeans(message, 200);
    }

    /**
     * @param message
     * @param size
     * @return
     */
    public static InfiniteProgressPanel createInfinitePanelBeans(String message, int size) {
		JXBusyLabel label = createBusyAnimationInfiniteBeans(size);
		return createPanel(label, message);
    }

    //_____________________________________________________________________________
    
    /**
     * @param size
     * @return
     */
    public static JXBusyLabel createBusyAnimationInfiniteCircles(int size) {
		float scale = 1 / 100f * size;
		return createBusyAnimation(size, new Ellipse2D.Float(0, 0,
				scale * 10.5f, scale * 10.5f), new Ellipse2D.Float(
				scale * 15.0f, scale * 15.0f, scale * 70.0f, scale * 70.0f), 5,
				12);
    }

	/**
	 * @param message
	 * @return
	 */
	public static InfiniteProgressPanel createInfinitePanelCircles(String message) {
		return createInfinitePanelCircles(message, 200);
	}

	/**
	 * @param message
	 * @param size
	 * @return
	 */
	public static InfiniteProgressPanel createInfinitePanelCircles(String message, int size) {
		JXBusyLabel label = createBusyAnimationInfiniteCircles(size);
		return createPanel(label, message);
    }

    //_____________________________________________________________________________
    
    /**
     * @param size
     * @return
     */
    public static JXBusyLabel createBusyAnimationInfiniteFeather2(int size) {
		float scale = 1 / 100f * size;
		return createBusyAnimation(size, new RoundRectangle2D.Float(0, 0,
				scale * 30.5f, scale * 10.0f, scale * 10.0f, scale * 10.0f),
				new Ellipse2D.Float(scale * 15.0f, scale * 15.0f,
						scale * 70.0f, scale * 70.0f), 4, 8);
			//		painter.setFrame(0);
    }

	/**
	 * @param message
	 * @return
	 */
	public static InfiniteProgressPanel createInfinitePanelFeather2(String message) {
		return createInfinitePanelFeather2(message, 200);
	}

		
	/**
	 * @param message
	 * @param size
	 * @return
	 */
	public static InfiniteProgressPanel createInfinitePanelFeather2(String message, int size) {
		JXBusyLabel label = createBusyAnimationInfiniteFeather2(size);
		return createPanel(label, message);
    }

    
    //_____________________________________________________________________________
    
    /**
     * @param size
     * @return
     */
    public static JXBusyLabel createBusyAnimationInfiniteFeather(int size) {
		float scale = 1 / 100f * size;
		return createBusyAnimation(size, new RoundRectangle2D.Float(0, 0,
				scale * 22.0f, scale * 6.5f, scale * 8.0f, scale * 8.0f),
				new Ellipse2D.Float(scale * 15.0f, scale * 15.0f,
						scale * 70.0f, scale * 70.0f), 4, 14);
			//		painter.setFrame(0);
    }


	/**
	 * @param message
	 * @return
	 */
	public static InfiniteProgressPanel createInfinitePanelFeather(String message) {
		return createInfinitePanelFeather(message, 200);
	}

    /**
     * @param message
     * @param size
     * @return
     */
	public static InfiniteProgressPanel createInfinitePanelFeather(String message, int size) {
		JXBusyLabel label = createBusyAnimationInfiniteFeather(size);
		return createPanel(label, message);
    }


	//_____________________________________________________________________________

    public static void main(String[] args) {
    	//InfiniteProgressPanel progress = new InfiniteProgressPanel("Please wait...");
		//int ss = 42*2+45*2+400;
    	//GuiUtils.testFrame(progress, "", ss, ss);

//    	GuiUtils.testFrame(createInfinitePanelFeather2("Please wait"), "", 400, 400);
    	GuiUtils.testFrame(createInfinitePanelBeans("Please wait"), "", 400, 400);
//    	GuiUtils.testFrame(createInfinitePanelCircles("Please wait"), "", 400, 400);
//    	GuiUtils.testFrame(createInfinitePanelFeather("Please wait"), "", 400, 400);

//    	GuiUtils.testFrame(createInfinitePanelBeans("Please wait", 50), "", 400, 400);
    }
}