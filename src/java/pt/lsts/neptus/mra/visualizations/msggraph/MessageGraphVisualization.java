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
 * 05/06/2016
 */
package pt.lsts.neptus.mra.visualizations.msggraph;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;

import com.google.common.collect.HashBiMap;

import pt.lsts.imc.EntityInfo;
import pt.lsts.imc.TransportBindings;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Messages Graph", active=false)
public class MessageGraphVisualization extends SimpleMRAVisualization {
    private static final long serialVersionUID = 8203929813051504069L;

    @NeptusProperty
    boolean antialias = false;
    
    private DefaultGraph graph;
    private ViewPanel view;
    private MessageGraphSettings settings = null;
    private JButton btnSettings, btnSave;
    
    LinkedHashMap<String, HashSet<Integer>> producers = new LinkedHashMap<>();
    LinkedHashMap<String, HashSet<Integer>> consumers = new LinkedHashMap<>();
    HashBiMap<String, Integer> componentNames = HashBiMap.create();
    HashBiMap<String, Integer> entityNames = HashBiMap.create();
    
    /**
     * @param panel
     */
    public MessageGraphVisualization(MRAPanel panel) {
        super(panel);
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }
    
    private void parseEntities() {
        componentNames = HashBiMap.create();
        entityNames = HashBiMap.create();
        
        for (EntityInfo i : source.getLsfIndex().getIterator(EntityInfo.class)) {
            componentNames.put(i.getComponent(), (int)i.getSrcEnt());
            entityNames.put(i.getEntityName(), (int)i.getSrcEnt());
        }
    }
    
    private void parseConsumers() {
        consumers.clear();
        
        for (TransportBindings t : source.getLsfIndex().getIterator(TransportBindings.class)) {
            String msg = source.getLsfIndex().getDefinitions().getMessageName(t.getMessageId());
            if (!consumers.containsKey(msg))
                consumers.put(msg, new HashSet<>());
            consumers.get(msg).add(componentNames.get(t.getConsumer()));
        }
    }
    
    private void parseMessages() {
        producers.clear();
        
        LsfIndex index = source.getLsfIndex();
        int localId = index.sourceOf(0);
        for (int i = 0; i < index.getNumberOfMessages(); i++) {
            if (index.sourceOf(i) != localId)
                continue;
            int producer = index.entityOf(i);
            String msg = index.getDefinitions().getMessageName(index.typeOf(i));
            if (!producers.containsKey(msg))
                producers.put(msg, new HashSet<>());
            producers.get(msg).add(producer);
        }
    }
    
    private Thread getParser() {
        return new Thread(getClass().getSimpleName()) {
            @Override
            public void run() {
                parseEntities();
                parseConsumers();
                parseMessages();           
                settings = new MessageGraphSettings(MessageGraphVisualization.this);
                applySettings();
                btnSettings.setEnabled(true);
            }
        };
    }
    
    private void settings(ActionEvent event) {
            
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(mraPanel), I18n.text("Message Graph Settings"),
                ModalityType.APPLICATION_MODAL);
        
        JPanel ui = new JPanel(new BorderLayout());
        ui.add(settings, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton apply = new JButton(I18n.text("Apply"));
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        bottom.add(apply);
        
        JButton cancel = new JButton(I18n.text("Cancel"));
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        bottom.add(cancel);
        ui.add(bottom, BorderLayout.SOUTH);
        ui.revalidate();
        dialog.setSize(600, 600);
        dialog.setContentPane(ui);
        GuiUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
        graph.clear();
       
        applySettings();
    }
    
    private void applySettings() {
        graph.clear();
        graph.setStrict(false);
        if (antialias)
            graph.addAttribute("ui.antialias");
        else
            graph.removeAttribute("ui.antialias");
        graph.setAutoCreate(true);
        graph.addAttribute("ui.quality");
        graph.addAttribute("stylesheet", "node { "
                + "     shape: rounded-box; "
                + "     padding: 5px; "
                + "     fill-color: white; "
                + "     stroke-mode: plain; "
                + "     size-mode: fit; "
                + "} "
                + "edge { "
                + "     shape: blob; "
                + "}");
        List<String> messagesOfInterest = settings.selectedMessages();
        
        for (String s : messagesOfInterest) {
            Node n = graph.addNode(s);
            n.addAttribute("ui.style", "fill-color: #eef;");
            if (consumers.containsKey(s)) {
                for (int id : consumers.get(s)) {
                    String compName = componentNames.inverse().get(id);
                    Edge consume = graph.addEdge(s+" --> "+compName, s, compName, true);
                    consume.addAttribute("ui.style", "fill-color: #333;");
                }
            }
            
            if (producers.containsKey(s)) {
                for (int id : producers.get(s)) {
                    String compName = componentNames.inverse().get(id);
                    Edge produce = graph.addEdge(compName+" --> "+s, compName, s, true);
                    produce.addAttribute("ui.style", "fill-color: #393;");
                }
            }
        }
        
        List<String> componentsOfInterest = settings.selectedMessages();
        
        for (String s : componentsOfInterest) {
            if (!componentNames.containsKey(s))
                continue;
            int entity = componentNames.get(s);
            for (Entry<String, HashSet<Integer>> entry : consumers.entrySet()) {
                if (entry.getValue().contains(entity)) {
                    Node n = graph.addNode(s);
                    n.addAttribute("ui.style", "fill-color: #eef;");                    
                }
            }
        }
        
        for (Node node : graph)
            node.setAttribute("ui.label", node.getId());

    }
    
    private File saveDir = null;
    private void save(ActionEvent event) {
        if (saveDir == null)
            saveDir = new File(source.getDir().getAbsolutePath()+"/mra/graph_snapshot.png");
        
        JFileChooser fileChooser = new JFileChooser(saveDir);
        fileChooser.setDialogTitle("Save Graph snapshot");
        fileChooser.setFileFilter(GuiUtils.getCustomFileFilter("PNG Images", "png"));
        int op = fileChooser.showSaveDialog(this);
        if (op != JFileChooser.APPROVE_OPTION)
            return;
        saveDir = fileChooser.getSelectedFile();
        graph.addAttribute("ui.screenshot", saveDir.getAbsolutePath());
        NeptusLog.pub().info("Saved to "+saveDir.getAbsolutePath());
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        setLayout(new BorderLayout());
        JPanel bottom = new JPanel();
        
        btnSettings = new JButton(I18n.text("Settings"));
        btnSettings.addActionListener(this::settings);
        btnSettings.setEnabled(false);
        bottom.add(btnSettings);
        
        
        btnSave = new JButton(I18n.text("Save Snapshot"));
        btnSave.addActionListener(this::save);
        bottom.add(btnSave);
        
        add(bottom, BorderLayout.SOUTH);
        
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        graph = new DefaultGraph("Messages");
        
        
        Viewer viewer = new Viewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.addDefaultView(false);
        viewer.enableAutoLayout();
        view = viewer.getDefaultView();
        add(view, BorderLayout.CENTER);
        
        getParser().start();
        
        return this;
    }
}
