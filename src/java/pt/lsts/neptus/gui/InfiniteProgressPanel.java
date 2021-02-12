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
 * ????/??/??
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

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
		this.add(getTextJLabel());
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
	private JLabel getTextJLabel() {
		if (text == null) {
			text = new JLabel("<html><b>" + I18n.text("Wait please") + "...", JLabel.CENTER);
			text.setAlignmentX(Component.CENTER_ALIGNMENT);
			text.setAlignmentY(Component.CENTER_ALIGNMENT);
		}
		return text;
	}
	
    public String getText() {
        return text.getText();
    }
	
    /**
	 * @param message
	 */
	public void setText(String message) {
	    if (message != null && !"".equalsIgnoreCase(message))
		    getTextJLabel().setText("<html><b>" + message + "...");
	    else
	        getTextJLabel().setText("");
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
	
	public void setBusy(boolean busy) {
	    getBusyLabel().setBusy(busy);
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