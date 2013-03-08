/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2010/07/01
 * $Id:: TeleoperationPanel.java 9615 2012-12-30 23:08:28Z pdias                $:
 */
package pt.up.fe.dceg.neptus.plugins.teleoperation;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class TeleoperationPanel extends SimpleSubPanel implements IPeriodicUpdates {
	
	private static final long serialVersionUID = 1L;
    ColorMap normal = ColorMapFactory.createRedYellowGreenColorMap();
	ColorMap inverted = ColorMapFactory.createInvertedColorMap((InterpolationColorMap)normal);
	protected SimpleTurnGauge turnGauge = new SimpleTurnGauge();
	protected SimpleVerticalGauge motorLeft = new SimpleVerticalGauge();
	protected SimpleVerticalGauge motorRight = new SimpleVerticalGauge();	
	protected SimpleOrientationGauge orientation = new SimpleOrientationGauge();
	protected LocationType destination = null;
	
	public TeleoperationPanel(ConsoleLayout console) {
	    super(console);
		setLayout(new BorderLayout());
		add(motorLeft, BorderLayout.WEST);
		add(motorRight, BorderLayout.EAST);
		add(turnGauge, BorderLayout.SOUTH);
		add(orientation, BorderLayout.CENTER);
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JPopupMenu popup = new JPopupMenu();
				popup.add("Head to copied location").addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

						if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
							try {
								String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
								LocationType lt = new LocationType();
								lt.fromClipboardText(text);
								destination = new LocationType(lt);
							} catch (Exception ex) {
								NeptusLog.pub().error(ex);
							}
						}
					}
				});	
				popup.show(TeleoperationPanel.this, e.getX(), e.getY());
			}
		});
	}
	
	
	@Override
	public long millisBetweenUpdates() {
		return 100;
	}
	
	@Override
	public boolean update() {
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String[] args) {
		GuiUtils.testFrame(new TeleoperationPanel(null));
	}


    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }


    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
