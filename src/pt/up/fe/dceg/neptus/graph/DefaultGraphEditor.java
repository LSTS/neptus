/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.graph;

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

import pt.up.fe.dceg.neptus.gui.PropertiesTable;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.util.GuiUtils;
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
