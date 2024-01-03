/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/09/17
 */
package pt.lsts.neptus.mra.visualizations;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author ZP
 *
 */
public abstract class SimpleMRAVisualization extends JPanel implements MRAVisualization {

    private static final long serialVersionUID = 1L;
    protected MRAPanel mraPanel;

    @Override
    public abstract boolean canBeApplied(IMraLogGroup source);	
    protected IMraLogGroup source;
    protected double timestep;
    protected MRAPanel panel;

    public SimpleMRAVisualization(MRAPanel panel) {
        this.mraPanel = panel;
    }

    @Override
    public void onHide() {
        //nothing
    }

    @Override
    public void onShow() {
        //nothing
    }

    @Override
    public final Component getComponent(IMraLogGroup source, double timestep) {
        this.source = source;
        this.timestep = timestep;
        return getVisualization(source, timestep);
    }

    public abstract JComponent getVisualization(IMraLogGroup source, double timestep);

    @Override
    public Double getDefaultTimeStep() {
        return MRAProperties.defaultTimestep;
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
