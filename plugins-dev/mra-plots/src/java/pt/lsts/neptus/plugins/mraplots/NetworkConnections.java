/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 5, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import pt.lsts.dot.IMCGraph;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp and ribcar
 * 
 */
@PluginDescription(name = "Network", icon="pt/lsts/neptus/plugins/mraplots/msg_inspector.png", experimental=true)
public class NetworkConnections extends SimpleMRAVisualization {

    /**
     * @param panel
     */
    public NetworkConnections(MRAPanel panel) {
        super(panel);
    }

    private static final long serialVersionUID = -8135678083471983681L;
    protected LsfIndex index;
    protected JLabel label = new JLabel();
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("Data.lsf") != null && source.getFile("IMC.xml") != null;
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        try {
            if (index == null)
                index = new LsfIndex(source.getFile("Data.lsf"), new IMCDefinition(new FileInputStream(
                        source.getFile("IMC.xml"))));
            
            try {
                
                IMCGraph graph = new IMCGraph(index);
                BufferedImage img = graph.generateSystemsGraph().generateImage();
                
                label.setIcon(new ImageIcon(img));
                
            }
            catch (Exception e) {
                e.printStackTrace();
                label.setText(I18n.text("Your system doesn't have dot support"));
            }
                        
            return new JScrollPane(label);
        }
        catch (Exception e) {
            return new JLabel(I18n.text("ERROR")+": " + e.getMessage());
        }
    }
    
    public Type getType() {
        return Type.VISUALIZATION;
    }
}
