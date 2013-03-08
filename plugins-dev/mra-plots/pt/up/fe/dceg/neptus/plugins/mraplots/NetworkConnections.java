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
 * Jun 5, 2012
 * $Id:: NetworkConnections.java 9615 2012-12-30 23:08:28Z pdias                $:
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.dot.IMCGraph;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;

/**
 * @author zp and ribcar
 * 
 */
@PluginDescription(name = "Network", icon="pt/up/fe/dceg/neptus/plugins/mraplots/msg_inspector.png", experimental=true)
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
