/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.vtk.mravisualizer.Window;
import pt.lsts.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.lsts.neptus.plugins.vtk.utils.Utils;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import pt.lsts.neptus.util.ImageUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author hfq
 *
 */
@PluginDescription(author = "hfq", name = "CTD 3D", icon = "images/menus/3d.png" )
public class CTD3D extends JPanel implements MRAVisualization, PropertiesProvider {

    private Canvas canvas;
    private Window winCanvas;

    private PointCloud<?> pointcloud;

    private IMraLogGroup source;

    /**
     * 
     */
    public CTD3D(MRAPanel mraPanel) {
        Utils.loadVTKLibraries();
    }

    @Override
    public DefaultProperty[] getProperties() {
        return null;
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return null;
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    @Override
    public String getName() {
        return I18n.text("CTD 3D");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        this.source = source;

        setLayout(new BorderLayout());

        canvas = new Canvas();
        canvas.LightFollowCameraOn();
        canvas.setEnabled(true);

        add(canvas);

        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return (NeptusMRA.vtkEnabled && 
                source.getLsfIndex().containsMessagesOfType("Conductivity"));
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/3d.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.visualizations.MRAVisualization#onHide()
     */
    @Override
    public void onHide() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onShow() {

    }

    @Override
    public void onCleanup() {

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

    }

}
