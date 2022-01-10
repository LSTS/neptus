/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 26/01/2018
 */
package pt.lsts.neptus.soi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "SOI Plan Edition", author = "Paulo Dias", version = "0.1.0", category = CATEGORY.PLANNING)
@LayerPriority(priority = 100)
public class SoiPlanEditor extends InteractionAdapter
        implements Renderer2DPainter, MissionChangeListener, ConfigurationListener {

    protected final UndoManager undoManager;
    protected AbstractAction undoAction, redoAction;
    
    protected StateRenderer2D renderer;
    
    private HashMap<Component, Object> componentList = new HashMap<>();
    private StateRendererInteraction delegate = null;
    
    /**
     * @param console
     */
    public SoiPlanEditor(ConsoleLayout console) {
        super(console);
        
        undoManager = new UndoManager() {
            @Override
            public synchronized boolean addEdit(UndoableEdit anEdit) {
                boolean ret = super.addEdit(anEdit);
                updateUndoRedo();
                return ret;
            };

            @Override
            public synchronized void undo() throws CannotUndoException {
                super.undo();
                updateUndoRedo();
            };

            @Override
            public synchronized void redo() throws CannotRedoException {
                super.redo();
                updateUndoRedo();
            };
        };
        
        initialize();
    }

    private void initialize() {
    }
    
    protected void updateUndoRedo() {
        getUndoAction().putValue(AbstractAction.SHORT_DESCRIPTION, undoManager.getUndoPresentationName());
        getRedoAction().putValue(AbstractAction.SHORT_DESCRIPTION, undoManager.getRedoPresentationName());
        getUndoAction().setEnabled(undoManager.canUndo());
        getRedoAction().setEnabled(undoManager.canRedo());
        
        // TODO
    }

    protected AbstractAction getUndoAction() {
        if (undoAction == null) {
            undoAction = new AbstractAction(I18n.text("Undo"), ImageUtils.getScaledIcon("images/planning/undo.png", 16,
                    16)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    undoManager.undo();
                }
            };
        }
        return undoAction;
    }

    protected AbstractAction getRedoAction() {
        if (redoAction == null) {
            redoAction = new AbstractAction(I18n.text("Redo"), ImageUtils.getScaledIcon("images/planning/redo.png", 16,
                    16)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    undoManager.redo();
                }
            };
        }
        return redoAction;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.MissionChangeListener#missionReplaced(pt.lsts.neptus.types.mission.MissionType)
     */
    @Override
    public void missionReplaced(MissionType mission) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.MissionChangeListener#missionUpdated(pt.lsts.neptus.types.mission.MissionType)
     */
    @Override
    public void missionUpdated(MissionType mission) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 125));
        g2.fill(new Ellipse2D.Double(100 - 4, 100 - 4, 8, 8));
        g2.dispose();
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        //getPropertiesPanel().setManeuver(null);
        this.renderer = source;

        JSplitPane horizontalSplit = null;
        if (mode) {
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                componentList.clear();

                BorderLayout bl = (BorderLayout) c.getLayout();
                for (Component component : c.getComponents()) {
                    Object constraint = bl.getConstraints(component);
                    componentList.put(component, constraint);
                }

                Component comp = bl.getLayoutComponent(BorderLayout.CENTER);

                horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, comp, new JLabel("SidePanel"));

                horizontalSplit.setResizeWeight(1.0);

                c.add(horizontalSplit);
                
                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new LineBorder(Color.orange.darker(), 3));
            }
        }
        else {
            if (delegate != null) {
                delegate.setActive(false, source);
                //getPropertiesPanel().getEditBtn().setSelected(false);
                delegate = null;
            }
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                // c.remove(getSidePanel());
                c.removeAll();
                for (Entry<Component, Object> e : componentList.entrySet()) {
                    c.add(e.getKey(), e.getValue());
                }
                componentList.clear();

                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new EmptyBorder(0, 0, 0, 0));
            }
            renderer.setToolTipText("");
        }
    }

}
