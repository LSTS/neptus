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
 * 2009/09/17
 */
package pt.up.fe.dceg.neptus.mra.visualizations;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author ZP
 *
 */
public abstract class SimpleMRAVisualization extends JPanel implements MRAVisualization {

    private static final long serialVersionUID = 1L;
    protected MRAPanel mraPanel;
    
    public abstract boolean canBeApplied(IMraLogGroup source);	
	protected IMraLogGroup source;
	protected double timestep;
	protected MRAPanel panel;
	
	public SimpleMRAVisualization(MRAPanel panel) {
	    this.mraPanel = panel;
	}
	
	public void onHide() {
	    //nothing
	}

	public void onShow() {
	    //nothing
	}
	
	@Override
	public final JComponent getComponent(IMraLogGroup source, double timestep) {
		this.source = source;
		this.timestep = timestep;
		return getVisualization(source, timestep);
	}
	
	public abstract JComponent getVisualization(IMraLogGroup source, double timestep);
	
	@Override
    public Double getDefaultTimeStep() {
        return NeptusMRA.defaultTimestep;
    }
	
	@Override
	public ImageIcon getIcon() {
		return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(this.getClass()), 16, 16);
	}

	@Override
	public boolean supportsVariableTimeSteps() {
		return false;
	}
	
	@Override
	public String getName() {
		return I18n.text(PluginUtils.getPluginName(this.getClass()));
	}

	@Override
	public void onCleanup() {
	    mraPanel = null;
	}
}
