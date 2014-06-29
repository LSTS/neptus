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
 * Author: zp
 * Jun 29, 2014
 */
package pt.lsts.neptus.plugins.europa.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import psengine.PSToken;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;

/**
 * @author zp
 *
 */
public class PlanVisualization extends JPanel {

    private static final long serialVersionUID = 960157151397992518L;
    private Graph<PSToken, String> graph;
    private VisualizationViewer<PSToken, String> vv = null;
    
    public PlanVisualization(Collection<PSToken> plan) {
        graph = new DirectedSparseGraph<>();
        PSToken prev = null;
        for (PSToken tok : plan) {
            graph.addVertex(tok);
            if (prev != null)
                graph.addEdge("before_"+Math.random()*1000, prev, tok);
            prev = tok;
        }
        
        Layout<PSToken, String> layout = new SpringLayout<PSToken, String>(graph);
        layout.setSize(new Dimension(300,300)); // sets the initial size of the space
        vv = new VisualizationViewer<PSToken, String>(layout);
        DefaultModalGraphMouse<PSToken, String> gm = new DefaultModalGraphMouse<PSToken, String>();
        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        vv.setGraphMouse(gm); 
//        vv.getRenderContext().setVertexShapeTransformer( new Transformer<PSToken, Shape>() {
//            @Override
//            public Shape transform(PSToken arg0) {
//                JLabel lbl = new JLabel("<html><pre>"+((PSToken) arg0).toLongString());
//                return lbl.getBounds();
//            }
//        });
        vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {
            
            @Override
            public <T> Component getVertexLabelRendererComponent(JComponent arg0, Object arg1, Font arg2, boolean arg3, T arg4) {
                if (arg4 instanceof PSToken) {
                    JLabel lbl = new JLabel("<html><pre>"+((PSToken) arg4).toLongString());
                    lbl.setOpaque(true);
                    lbl.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                    lbl.setBackground(new Color(160,160,192,128));
                    lbl.setFont(new Font("Arial", Font.PLAIN, 8));
                    return lbl;
                }
                return new JLabel(arg4 + "");
            }
        });
        setLayout(new BorderLayout());
        add(vv, BorderLayout.CENTER);
    }
    
}
