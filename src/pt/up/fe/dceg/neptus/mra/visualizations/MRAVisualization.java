/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/04
 */
package pt.up.fe.dceg.neptus.mra.visualizations;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 *
 */
public interface MRAVisualization {
    enum Type {
        VISUALIZATION, CHART, TABLE
    }
    
    public String getName();
	public JComponent getComponent(IMraLogGroup source, double timestep);
	public boolean canBeApplied(IMraLogGroup source);
	public ImageIcon getIcon();
	public Double getDefaultTimeStep();
	public boolean supportsVariableTimeSteps();
	public Type getType();
	public void onHide();
	public void onShow();
	public void onCleanup();
}
