/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.graph;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import pt.lsts.neptus.gui.PropertiesTable;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author zp
 */
@SuppressWarnings("unchecked")
public class DefaultGraphEditor extends JPanel implements UndoableEditListener, GraphSelectionListener, PropertyChangeListener {

    private static final long serialVersionUID = 1L;
    protected NeptusGraph<?,?> graph;
	private AbstractAction undoAction, redoAction, layoutAction;
	protected PropertiesTable propsTable = new PropertiesTable();
	
	public DefaultGraphEditor(NeptusGraph<?,?> graph) {
		this.graph = graph;
		graph.setBackground(Color.white);
		graph.getUndoSupport().addUndoableEditListener(this);
		graph.addGraphSelectionListener(this);
		setLayout(new TableLayout(new double[][] {{TableLayout.FILL, 150},{32, TableLayout.FILL}}));		
		this.add(new JScrollPane(graph), "0,1");
		this.add(createToolbar(), "0,0,1,0");
		this.add(propsTable, "1,1");
		propsTable.addPropertyChangeListener(this);
	}
	
	private JToolBar createToolbar() {
		JToolBar toolbar = new JToolBar();
		undoAction = new AbstractAction("Undo") {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
				graph.getUndoManager().undo();
				updateUndoRedo();
			}
		};
		redoAction = new AbstractAction("Redo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {				
				graph.getUndoManager().redo();
				updateUndoRedo();
			}
		};
		layoutAction = new AbstractAction("Layout") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {				
				graph.autoLayout();
				updateUndoRedo();
			}
		};
		
		updateUndoRedo();
		
		toolbar.add(undoAction);
		toolbar.add(redoAction);
		toolbar.add(layoutAction);
		toolbar.setFloatable(false);
		return toolbar;
		
	}
	
	private void updateUndoRedo() {
		undoAction.setEnabled(graph.getUndoManager().canUndo());
		undoAction.putValue(AbstractAction.SHORT_DESCRIPTION, graph.getUndoManager().getUndoPresentationName());
		
		redoAction.setEnabled(graph.getUndoManager().canRedo());
		redoAction.putValue(AbstractAction.SHORT_DESCRIPTION, graph.getUndoManager().getRedoPresentationName());
	}
	
	public void undoableEditHappened(UndoableEditEvent e) {
		updateUndoRedo();
	}
	
	public void selectionChanged(NeptusGraphElement<?>[] selection) {
		if (selection.length == 1) {
			propsTable.editProperties(selection[0]);			
		}
		else {
			propsTable.editProperties(null);
		}
	}
	
	public void propertyChange(PropertyChangeEvent evt) {		
		graph.repaint();
	}
	
	public static void main(String[] args) {
		GuiUtils.testFrame(new DefaultGraphEditor(new NeptusGraph<DefaultNode<?>, DefaultEdge<?>>(new DefaultGraphFactory())), "Graph Editor");
	}
}
