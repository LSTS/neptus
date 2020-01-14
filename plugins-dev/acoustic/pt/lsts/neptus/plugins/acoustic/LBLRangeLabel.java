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
 * 2009/09/27
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.gui.LBLUtil;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class LBLRangeLabel extends JXPanel {

    private static final Color COLOR_IDLE = new JXPanel().getBackground();
    private static final Color COLOR_ACCEPT = Color.GREEN.darker(); //new Color(140, 255, 170);
    private static final Color COLOR_REJECTED = new Color(245, 20, 40); //new Color(255, 210, 140);
    private static final Color COLOR_SURFACE = Color.blue;
    private static final Color COLOR_OLD = Color.GRAY.darker(); //new Color(255, 100, 100);

    
	private Timer timer = null;
	private TimerTask colorUpdaterTask = null;
	private TimerTask periodicTask = null;

    private String name = "?";
    private double range = -1;
    private boolean isAccepted = true;
    private String rejectionReason = "";
    
    //UI
    private JXLabel label = null;
    private long timeStampMillis = -1;
    
    /**
	 * 
	 */
	public LBLRangeLabel(String name) {
		this.name = name;
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		setBackgroundPainter(getCompoundBackPainter());
		label = new JXLabel("<html><b>"+name, JLabel.CENTER) {
		    @Override
            public void setText(String text) {
		        if (text.equals(getText()))
		            return;
		        super.setText(text);
		    };
		};
		label.setHorizontalTextPosition(JLabel.CENTER);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(new Font("Arial", Font.BOLD, 12));
		label.setForeground(Color.WHITE);
		label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setLayout(new BorderLayout());
		add(label, BorderLayout.CENTER);
		
		setPreferredSize(new Dimension(215, 180));
		setSize(new Dimension(215, 180));
	}

	
	/**
	 * @return the range
	 */
	public double getRange() {
		return range;
	}
	
	/**
	 * @param range the range to set
	 */
	public void setRange(double range) {
		this.range = range;
		//setAccepted(true, null);
		updateLabel();
	}
	
	/**
	 * @return the isAccepted
	 */
	public boolean isAccepted() {
		return isAccepted;
	}
	
	/**
	 * @param isAccepted the isAccepted to set
	 */
	public void setAccepted(boolean isAccepted, String rejectionReason) {
		this.isAccepted = isAccepted;
		this.rejectionReason = rejectionReason;
		if(isAccepted)
		    updateBackColor(COLOR_ACCEPT);
		else
		    updateBackColor(rejectionReason.contains("SURFACE")? COLOR_SURFACE : COLOR_REJECTED);
		setToolTipText(isAccepted ? I18n.text("Accepted")
				: (I18n.text("Rejected") + (rejectionReason != null ? " "
						+ rejectionReason : "")));
		updateLabel();
		revokeScheduleUpdateTask();
		scheduleUpdateTask();
	}
	
    /**
     * @param timeStampMillis
     */
    public void setTimeStampMillis(long timeStampMillis) {
        this.timeStampMillis = timeStampMillis;
        
        if (periodicTask == null) {
            periodicTask = new TimerTask() {
                @Override
                public void run() {
                    updateLabel(false);
                }
            };
            timer.scheduleAtFixedRate(periodicTask, 50, 1000);
        }
    }

    private void updateLabel() {
        updateLabel(true);
    }

	private void updateLabel(boolean triggerChangeColorIndicator) {
        String ellapsedTime = LBLUtil.writeTimeLabel(timeStampMillis);
    label.setText(I18n.textf("<html><b>Beacon %beaconid (%range m) %datetime <br>%reason", 
               "ch" + name, MathMiscUtils.round(range, 1), ellapsedTime,
                (isAccepted ? I18n.text("Accepted") : (I18n.text("Rejected") + (rejectionReason != null ? ": "
                        + rejectionReason : "")))));

	    if (triggerChangeColorIndicator) {
	        revokeScheduleUpdateTask();
	        scheduleUpdateTask();
	    }
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
	    if(obj instanceof LBLRangeLabel) {
	        return ((LBLRangeLabel) obj).getName().equals(name);
	    }
	    else {
	        return false;
	    }
	}

	private void scheduleUpdateTask() {
		if (colorUpdaterTask == null) {
			if (timer == null)
                timer = new Timer(LBLRangeLabel.class.getSimpleName() + " color updater: " + LBLRangeLabel.this.name, true);
			colorUpdaterTask = createTimerTask();
			timer.schedule(colorUpdaterTask, 1*1000);
		}
	}
	
	private void revokeScheduleUpdateTask() {
		if (colorUpdaterTask != null) {
			colorUpdaterTask.cancel();
			colorUpdaterTask = null;
		}
	}

	/**
	 * @return
	 */
	private TimerTask createTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				updateBackColor(((Color) rectPainter.getFillPaint()).darker());
				colorUpdaterTask = createTimerTask2();
				timer.schedule(colorUpdaterTask, 4*1000);
			}
		};
	}

	private TimerTask createTimerTask2() {
		return new TimerTask() {
			@Override
			public void run() {
				updateBackColor(COLOR_OLD);
			}
		};
	}


	//Background Painter Stuff
	private RectanglePainter rectPainter;
	private CompoundPainter<JXPanel> compoundBackPainter;
	/**
	 * @return the rectPainter
	 */
	private RectanglePainter getRectPainter() {
		if (rectPainter == null) {
	        rectPainter = new RectanglePainter(5,5,5,5, 20,20);
	        rectPainter.setFillPaint(COLOR_IDLE);
	        rectPainter.setBorderPaint(COLOR_IDLE.darker().darker().darker());
	        rectPainter.setStyle(RectanglePainter.Style.BOTH);
	        rectPainter.setBorderWidth(2);
	        rectPainter.setAntialiasing(true);
		}
		return rectPainter;
	}
	/**
	 * @return the compoundBackPainter
	 */
	private CompoundPainter<JXPanel> getCompoundBackPainter() {
		compoundBackPainter = new CompoundPainter<JXPanel>(
					//new MattePainter(Color.BLACK),
					getRectPainter(), new GlossPainter());
		return compoundBackPainter;
	}
	/**
	 * @param color
	 */
	private void updateBackColor(Color color) {
		getRectPainter().setFillPaint(color);
		getRectPainter().setBorderPaint(color.darker());

		//this.setBackgroundPainter(getCompoundBackPainter());
		repaint();
	}

	/**
     * Call this to completely stop the timer that this class uses 
     */
    public void dispose() {
        revokeScheduleUpdateTask();
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }    
    }
    
	@Override
	protected void finalize() throws Throwable {
	    dispose();
	}
	
	public static void main(String[] args) {
		final LBLRangeLabel lb = new LBLRangeLabel("1");
		GuiUtils.testFrame(lb);
		lb.setRange(233.5);
		lb.setAccepted(false, "ABOVE_THRESHOLD");
		Timer timer = new Timer("LblRangeLabel Main");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				lb.setAccepted(!lb.isAccepted(), "ABOVE_THRESHOLD");
			}
		}, 6000, 6000);
	}
}
