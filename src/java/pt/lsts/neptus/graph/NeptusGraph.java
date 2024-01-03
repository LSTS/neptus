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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import pt.lsts.neptus.graph.edit.AddEdgeEdit;
import pt.lsts.neptus.graph.edit.AddNodeEdit;
import pt.lsts.neptus.graph.edit.MoveSelectionEdit;
import pt.lsts.neptus.graph.edit.RemoveSelectionEdit;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 * @param <N> The allowed Node superclass
 * @param <E> The allowed Edge superclass
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NeptusGraph<N extends NeptusNodeElement, E extends NeptusEdgeElement> extends JComponent 
	implements UndoableEditListener, MouseListener, MouseMotionListener {	

	private static final long serialVersionUID = -5912721048020842022L;
	protected LinkedHashMap<String, N> nodes = new LinkedHashMap<String, N>();
	protected LinkedHashMap<String, E> edges = new LinkedHashMap<String, E>();
	
	protected Vector<NeptusGraphElement<?>> selection = new Vector<NeptusGraphElement<?>>();	
    protected GraphElementFactory<N, E> factory = null;
	protected int graphWidth = getWidth();
	protected int graphHeight = getHeight();
	
	protected Point2D lastMousePoint = null;	
	public Point2D dragDifference = null;
	
	private boolean onlyOneInitialStateAllowed = false;
	private boolean loopingAllowed = true;
	private boolean cyclingAllowed = true;	
	private boolean nonDeterminismAllowed = true;
	private boolean initialStateRequired = true;
	private boolean finalStateRequired = true;
	
	private AffineTransform currentTransform = new AffineTransform();
	
	private DanglingEdge danglingEdge = null;
	
	private UndoManager undoManager = new UndoManager();
	private UndoableEditSupport undoSupport = new UndoableEditSupport();
		
	private Vector<GraphSelectionListener> selectionListeners = new Vector<GraphSelectionListener>();
	
	private boolean editable = true;
	private BufferedImage offscreenBuffer = null;
	
	private LinkedList<GraphPainter> preRenderPainters = new LinkedList<GraphPainter>();
	private LinkedList<GraphPainter> postRenderPainters = new LinkedList<GraphPainter>();

	public NeptusGraph() {
		setPreferredSize(new Dimension(200,150));
		addMouseListener(this);
		addMouseMotionListener(this);	
		undoSupport.addUndoableEditListener(this);
	}
	
	public void clear() {
		nodes.clear();
		edges.clear();
		selection.clear();
	}
	
	/**
	 * Creates a new NeptusGraph
	 * @param factoryInstance The GraphElementFactory that is used to create new nodes and edges 
	 * when requested by the user
	 */
	public NeptusGraph(GraphElementFactory<N,E> factoryInstance) {		
		this.factory = factoryInstance;
		setPreferredSize(new Dimension(200,150));
		addMouseListener(this);
		addMouseMotionListener(this);	
		undoSupport.addUndoableEditListener(this);
	}
	
	/**
	 * Returns an existing node
	 * @param id The id of the node to return
	 * @return The requested node or <b>null</b> id the node doesn't exist
	 */
	public N getNode(String id) {
		return nodes.get(id);
	}
	
	/**
	 * Returns an existing edge
	 * @param id The id of the edge to return
	 * @return The requested edge or <b>null</b> id the edge doesn't exist
	 */
	public E getEdge(String id) {
		return edges.get(id);
	}
	
	public NeptusEdgeElement<?>[] allEdges() {
		return edges.values().toArray(new NeptusEdgeElement<?>[] { } );
	}
	
	public NeptusNodeElement<?>[] allNodes() {
		return nodes.values().toArray(new NeptusNodeElement<?>[] {});
	}
		
	/**
	 * Adds a node to this graph
	 * @param node The node to be added (its position should already be set)
	 */
	public void addNode(N node) {
		if (!nodes.containsKey(node.getID())) 
			nodes.put(node.getID(), node);
		
		if (node.getMaxX() > graphWidth)
			graphWidth = node.getMaxX();
		
		if (node.getMaxY() > graphHeight)
			graphHeight = node.getMaxY();
		
		setPreferredSize(new Dimension(graphWidth, graphHeight));
		setMinimumSize(new Dimension(graphWidth, graphHeight));
		
		repaint();
	}
	
	/**
	 * Adds a node at a random position using the GraphElementFactory
	 * @return The newly created node
	 */
	public N addNode() {
		Random rnd = new Random(System.currentTimeMillis());
		N node = factory.createNode();
		addNode(node);
		node.setPosition(new Point2D.Double(rnd.nextDouble()*getWidth(), rnd.nextDouble()*getHeight()));
		repaint();
		return node;
	}
	
	/**
	 * Creates an Edge between the given nodes
	 * @param src The source node's id
	 * @param tgt The target node's id
	 * @return The newly created edge
	 */
	public E addEdge(String src, String tgt) {
		E edge = factory.createEdge();		
		edge.setSourceNodeID(src);
		edge.setTargetNodeID(tgt);
		if (nodes.containsKey(src) && nodes.containsKey(tgt))
			return addEdge(edge);
		else {
			edge.cleanup();			
		}
		repaint();
		return null;
	}
	
	/**
	 * Adds an edge to the graph
	 * @param edge The edge to be added
	 */
	public E addEdge(E edge) {		
		
		if (edges.containsKey(edge.getID())) {
			System.err.println("Tried to add an invalid edge: Edge '"+edge.getID()+"' already exists!");
			return null;			
		}
				
		if (!nodes.containsKey(edge.getSourceNodeID())) {
			System.err.println("Tried to add an invalid edge ('"+edge.getSourceNodeID()+"' -> '"+edge.getTargetNodeID()+"').");
			return null;
		}
		
		if (!nodes.containsKey(edge.getTargetNodeID())) {
			System.err.println("Tried to add an invalid edge ('"+edge.getSourceNodeID()+"' -> '"+edge.getTargetNodeID()+"').");
			return null;
		}
		
		for (Object key : nodes.get(edge.getSourceNodeID()).getOutgoingEdges().keySet()) {
			if (edges.get(key).getTargetNodeID().equals(edge.getTargetNodeID())) {
				System.err.println("Edge already exists!");
				return null;
			}
		}
		
		edges.put(edge.getID(), edge);
		
		nodes.get(edge.getSourceNodeID()).addOutgoingEdge(edge);
		nodes.get(edge.getTargetNodeID()).addIncomingEdge(edge);
		
		repaint();
		
		return edge;		
	}
	
	@Override
	public void paint(Graphics arg0) {
		
		if (offscreenBuffer == null || offscreenBuffer.getWidth() < getWidth() || offscreenBuffer.getHeight() < getHeight()) {
			offscreenBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);	
		}
		
		Graphics2D g = (Graphics2D) offscreenBuffer.getGraphics();
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(getForeground());
		
		currentTransform = (AffineTransform) g.getTransform().clone();
		
		for (GraphPainter painter : preRenderPainters)
			painter.paint(g, this);
		
		for (E edge : edges.values()) {
			if (selection.contains(edge))
				continue;
			edge.paint(g, this);
		}
		
		for (N node : nodes.values()) {			
			if (selection.contains(node))
				continue;			
			node.paint(g, this);
		}
		
		if (danglingEdge != null) {
			g.setColor(Color.blue);
			danglingEdge.paint(g, this);
			g.setColor(Color.black);
		}
		
		for (NeptusGraphElement<?> elem : selection)
			elem.paint(g, this);
		
		for (GraphPainter painter : postRenderPainters)
			painter.paint(g, this);
		
		arg0.drawImage(offscreenBuffer, 0, 0, this);
	}
	
	protected void editSelectionProperties() {
		if (selection.size() == 1) {
			PropertiesEditor.editProperties(selection.get(0), isEditable());
			repaint();
		}
	}
	
	
	public AbstractAction[] getClickActions(MouseEvent evt){
		
		Vector<AbstractAction> actions = new Vector<AbstractAction>();
		
		if (evt.getButton() == MouseEvent.BUTTON3 && isEditable()) {
			final Point mousePosition = evt.getPoint();

			if (selection.size() == 1) {
				AbstractAction propsAction = new AbstractAction("Properties...") {					
					/**
                     * 
                     */
                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent arg0) {
						editSelectionProperties();						
					}
				};
				actions.add(propsAction);
			}			
	
			if (selection.size() == 1 && selection.get(0) instanceof NeptusNodeElement) {
				
				
				AbstractAction addEdge = new AbstractAction("Add Edge") {					
					/**
                     * 
                     */
                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent arg0) {
						NeptusNodeElement<?> node = (NeptusNodeElement<?>) selection.get(0);
						danglingEdge = new DanglingEdge(node, mousePosition);							
					}
				};
				actions.add(addEdge);
			}
			else {
				AbstractAction addNodeAction = new AbstractAction("Add node") {
					/**
                     * 
                     */
                    private static final long serialVersionUID = 1L;
                    Point mousePos = mousePosition;
					public void actionPerformed(ActionEvent arg0) {
						N node = factory.createNode();
						if (node == null)
							return;
						node.setPosition(mousePos);
						addNode(node);
						undoSupport.postEdit(new AddNodeEdit(NeptusGraph.this, node));
					}
				};
				actions.add(addNodeAction);
			}
			
			if (!selection.isEmpty()) {
				AbstractAction removeSelecion = new AbstractAction("Remove") {					
					/**
                     * 
                     */
                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent arg0) {
						NeptusGraph.this.removeSelection();						
					}
				};
				actions.add(removeSelecion);			
			}
		}
		return actions.toArray(new AbstractAction[] {});
	}
	
	public void mouseClicked(MouseEvent arg0) {
		AbstractAction[] actions = getClickActions(arg0);
		
		if (actions.length > 0) {
			JPopupMenu popup = new JPopupMenu();
			for (AbstractAction action : getClickActions(arg0)) {
				popup.add(action);
			}
		
			popup.show(this, arg0.getX(), arg0.getY());
		}
	}
	
	public NeptusEdgeElement<?>[] getSelectedEdges(boolean includeReferredElements) {
		LinkedHashMap<String, NeptusEdgeElement<?>> selectedElements = new LinkedHashMap<String, NeptusEdgeElement<?>>();
		
		for (NeptusGraphElement<?> elem : selection) {
			
			if (elem instanceof NeptusNodeElement && includeReferredElements) {
				NeptusNodeElement<?> node = (NeptusNodeElement<?>) elem;
				
				selectedElements.putAll(node.getIncomingEdges());
				selectedElements.putAll(node.getOutgoingEdges());
			}
			
			if (elem instanceof NeptusEdgeElement) {
				selectedElements.put(elem.getID(), (NeptusEdgeElement<?>)elem);
			}
		}
		
		return selectedElements.values().toArray(new NeptusEdgeElement<?>[] {});
	}
	
	public NeptusNodeElement[] getSelectedNodes() {
		
		Vector<NeptusNodeElement> selectedNodes = new Vector<NeptusNodeElement>();
		
		for (NeptusGraphElement<?> elem : selection) {			
			if (elem instanceof NeptusNodeElement) {
				selectedNodes.add((NeptusNodeElement<?>)elem);
			}			
		}
		
		return selectedNodes.toArray(new NeptusNodeElement[] {});
	}
	
	private void removeSelection() {
		
		undoSupport.postEdit(new RemoveSelectionEdit(this));
		
		for (NeptusGraphElement<?> elem : selection) {
			
			if (elem instanceof NeptusNodeElement && nodes.containsKey(elem.getID())) {
				removeNode(elem.getID());
			}
			if (elem instanceof NeptusEdgeElement && edges.containsKey(elem.getID())) {
				removeEdge(elem.getID());
			}
		}
				
		selection.clear();
		repaint();
	}
	
	/**
	 * Removes a node from this graph
	 * @param id The id of the node to be removed
	 */
	public Vector<NeptusEdgeElement> removeNode(String id) {
		Vector<NeptusEdgeElement> edgesToBeRemoved = new Vector<NeptusEdgeElement>();
		for (NeptusEdgeElement<?> edge : edges.values()) {
			if (edge.getTargetNodeID().equals(id)) {
				if (!edgesToBeRemoved.contains(edge))
					edgesToBeRemoved.add(edge);
				
			}
			if (edge.getSourceNodeID().equals(id)) {
				if (!edgesToBeRemoved.contains(edge))
					edgesToBeRemoved.add(edge);					
			}
		}
		for (NeptusEdgeElement<?> edge : edgesToBeRemoved) {
			edge.setSelected(false);
			removeEdge(edge.getID());			
		}
		
		NeptusNodeElement<?> node = nodes.get(id);
		
		if (node != null)
			nodes.remove(id);
		
		node.setSelected(false);
				
		repaint();
		
		return edgesToBeRemoved;
	}
	
	/**
	 * Removes an existing edge
	 * @param id The id of the edge to be removed
	 */
	public void removeEdge(String id) {
		NeptusEdgeElement<?> edge = edges.get(id);
		
		if (edge == null)
			return;
		
		if (selection.contains(edge)) {
			edge.setSelected(false);			
		}
		
		if (nodes.get(edge.getSourceNodeID()) != null) {
			nodes.get(edge.getSourceNodeID()).removeOutgoingEdge(edge);
		}
		
		if (nodes.get(edge.getTargetNodeID()) != null) {
			nodes.get(edge.getTargetNodeID()).removeIncomingEdge(edge);
		}
		
		edges.remove(id);		
	}
	
	
	public void mouseDragged(MouseEvent arg0) {
		
		if (!isEditable())
			return;
		
		if (selection.isEmpty())
			return;
		
		if (arg0.getPoint().getX() < 0 ||arg0.getPoint().getY() < 0)
			return;
		
		int maxX = 0;
		int maxY = 0;
		for (N node : nodes.values()) {
			if (node.getMaxX() > maxX)
				maxX = node.getMaxX();
			if (node.getMaxY() > maxY)
				maxY = node.getMaxY();			
		}
		
		graphWidth = maxX;
		graphHeight = maxY;
		
		setSize((int)graphWidth, graphHeight);
		setPreferredSize(getSize());
		setMinimumSize(getSize());
		
		if (lastMousePoint == null) {
			lastMousePoint = arg0.getPoint();
			dragDifference = new Point2D.Double(0,0);
			return;
		}
		else {
			double xdiff = arg0.getPoint().getX() - lastMousePoint.getX();
			double ydiff = arg0.getPoint().getY() - lastMousePoint.getY();
			
			dragDifference.setLocation(dragDifference.getX()+xdiff, dragDifference.getY()+ydiff);
			
			for (NeptusGraphElement<?> el : selection) {
				if (el instanceof NeptusNodeElement) {
					NeptusNodeElement<?> node = (NeptusNodeElement<?>) el;
					node.setPosition(new Point2D.Double(
							node.getPosition().getX()+xdiff, 
							node.getPosition().getY()+ydiff)
					);
				}
			}
			lastMousePoint = arg0.getPoint();
			repaint();
		}
	}
	
	
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void mouseMoved(MouseEvent arg0) {
		if (danglingEdge != null) {
			danglingEdge.setTargetPoint(arg0.getPoint());
			repaint();
		}
	}
	
	int count = 0;
	
	public void mousePressed(MouseEvent arg0) {
			
		boolean selectionChanged = false;
		
		if (arg0.getButton() == MouseEvent.BUTTON3 && !selection.isEmpty())
			return;
		
		Vector<N> interceptedNodes = new Vector<N>();
		Vector<E> interceptedEdges = new Vector<E>();
	
		for (NeptusGraphElement<?> el : selection)
			el.setSelected(false);
		
		for (N node : nodes.values()) {			
			if (node.containsPoint(arg0.getPoint()))
				interceptedNodes.add(node);
		}
		
		for (E edge : edges.values()) {			
			if (edge.containsPoint(arg0.getPoint()))
				interceptedEdges.add(edge);		
		}
		
		// In the case the user clicks outside any element...
		if (interceptedNodes.isEmpty() && interceptedEdges.isEmpty()) {
			selectionChanged = !selection.isEmpty();
			selection.clear();
		}
		
		if (!selectionChanged && arg0.isShiftDown()) {
			selectionChanged = true;
			for (N n : interceptedNodes) {
				if (!selection.contains(n)) {
					selection.add(n);
				}
			}
			
			for (E e : interceptedEdges) {
				if (!selection.contains(e)) {
					selection.add(e);
				}
			}						
		}
		
		// Selecting a single element for the first time
		if (!selectionChanged && !arg0.isShiftDown() && (selection.isEmpty() || selection.size() > 1)) {		
			selectionChanged = true;
			if (interceptedNodes.size() >= 1) {
				N firstSelectedNode = interceptedNodes.get(0);
				selection.clear();
				selection.add(firstSelectedNode);
			}
			else if (interceptedEdges.size() >= 1) {
				E firstSelectedEdge = interceptedEdges.get(0);
				firstSelectedEdge.setSelected(true);
				selection.clear();
				selection.add(firstSelectedEdge);
			}
		}
		
		// Selecting a single element for the nth time (iterate intercepted elements)
		if (!selectionChanged && !arg0.isShiftDown() && selection.size() == 1) {	
			selectionChanged = true;
			
			Vector<NeptusGraphElement> elems = new Vector<NeptusGraphElement>();
			elems.addAll(interceptedNodes);
			elems.addAll(interceptedEdges);
			
			if (elems.indexOf(selection.get(0)) == -1 || elems.size() == 1) {
				selection.clear();
				selection.add(elems.get(0));				
			}
			else {
				int index = elems.indexOf(selection.get(0))+1;
				index = index%(elems.size());
				selection.clear();
				selection.add(elems.get(index));
				elems.get(index).setSelected(true);				
			}			
		}
		
		for (NeptusGraphElement<?> el : selection)
			el.setSelected(true);
		
		if (selectionChanged) {
			for (GraphSelectionListener l : selectionListeners) {
				l.selectionChanged(selection.toArray(new NeptusGraphElement[] {}));
			}
		}
		
		repaint();
		
	}
	
	public void mouseReleased(MouseEvent arg0) {
		
		if (lastMousePoint != null && !selection.isEmpty()) {			
			undoSupport.postEdit(new MoveSelectionEdit(this, getSelectedNodes(), dragDifference));
		}
		
		lastMousePoint = null;
		
		if (danglingEdge != null) {
			N node = getFirstNodeUnder(danglingEdge.getTargetPoint());
			if (node != null) {				
				
				E edge = addEdge(danglingEdge.getSourceNode().getID(), node.getID());
				
				if (edge != null) {
					//E edge = addEdge(danglingEdge.getSourceNode().getID(), node.getID());
					undoSupport.postEdit(new AddEdgeEdit(this, edge));
				}
			}
			danglingEdge = null;
			repaint();
		}
		
	}
	
	protected N getFirstNodeUnder(Point2D point) {
		for (N node : nodes.values()) {
			if (node.containsPoint(point)) {
				return node;
			}				
		}
		return null;
	}

	/**
	 * @return the default affine transform for the current graph state
	 */
	public AffineTransform getCurrentTransform() {
		return currentTransform;
	}
	
	public void undoableEditHappened(UndoableEditEvent e) {
		undoManager.addEdit(e.getEdit());
		repaint();
	}
	
	public boolean isLoopingAllowed() {
		return loopingAllowed;
	}

	public void setLoopingAllowed(boolean loopingAllowed) {
		this.loopingAllowed = loopingAllowed;
	}

	public boolean isNonDeterminismAllowed() {
		return nonDeterminismAllowed;
	}

	public void setNonDeterminismAllowed(boolean nonDeterminismAllowed) {
		this.nonDeterminismAllowed = nonDeterminismAllowed;
	}

	public boolean isOnlyOneInitialStateAllowed() {
		return onlyOneInitialStateAllowed;
	}

	public void setOnlyOneInitialStateAllowed(boolean onlyOneInitialStateAllowed) {
		this.onlyOneInitialStateAllowed = onlyOneInitialStateAllowed;
	}

	public boolean isCyclingAllowed() {
		return cyclingAllowed;
	}

	public void setCyclingAllowed(boolean cyclingAllowed) {
		this.cyclingAllowed = cyclingAllowed;
	}

	public boolean isFinalStateRequired() {
		return finalStateRequired;
	}

	public void setFinalStateRequired(boolean finalStateRequired) {
		this.finalStateRequired = finalStateRequired;
	}

	public boolean isInitialStateRequired() {
		return initialStateRequired;
	}

	public void setInitialStateRequired(boolean initialStateRequired) {
		this.initialStateRequired = initialStateRequired;
	}
	
	public void addGraphSelectionListener(GraphSelectionListener listener) {
		if (!selectionListeners.contains(listener)) {
			selectionListeners.add(listener);
		}
	}
	
	public void removeGraphSelectionListener(GraphSelectionListener listener) {
		selectionListeners.remove(listener);
	}
	
	public NeptusNodeElement[] getAllNodes() {
		return nodes.values().toArray(new NeptusNodeElement[]{});
	}
	
	public NeptusEdgeElement[] getAllEdges() {
		return edges.values().toArray(new NeptusEdgeElement[]{});
	}
	
	
	public static void main(String[] args) {
		DefaultGraphFactory factory = new DefaultGraphFactory();		
        NeptusGraph<DefaultNode, DefaultEdge> graph = new NeptusGraph<DefaultNode, DefaultEdge>(factory);
		graph.setBackground(Color.white);
		GuiUtils.testFrame(new JScrollPane(graph), "NeptusGraph");
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}

	public UndoableEditSupport getUndoSupport() {
		return undoSupport;
	}

	public GraphElementFactory<N, E> getFactory() {
		return factory;
	}

	public void setFactory(GraphElementFactory<N, E> factory) {
		this.factory = factory;
	}
	
	public void autoLayout() {
		
		int margin = 40, incX = 80, incY = 80;
		
		int width = Math.max(getWidth(), 400);
		
		Vector<N> visitedNodes = new Vector<N>();		
		Vector<N> expandList = new Vector<N>();
		
		for (N node : nodes.values()) {
			if (node.getIncomingEdges().size() == 0)
				expandList.add(node);
		}
		
		int curX = margin-incX, curY = margin;
		
		while (!expandList.isEmpty()) {
			N node = expandList.firstElement();
			visitedNodes.add(node);
			
			curX += incX;
			
			if (incX > 0 && curX > width) {
				incX = -incX;
				curY += incY;
				curX += incX;
			}
			
			if (incX < 0 && curX < 0) {
				incX = -incX;
				curY += incY;
				curX += incX;
			}
			
			node.setPosition(new Point2D.Double(curX, curY));			
			
			for (Object key : node.getOutgoingEdges().keySet()) {
				NeptusEdgeElement<?> edge = (NeptusEdgeElement<?>) node.getOutgoingEdges().get(key);
				N tgtNode = nodes.get(edge.getTargetNodeID());
				if (!visitedNodes.contains(tgtNode))
					expandList.add(0, tgtNode);
			}
			
			expandList.remove(node);
			
			repaint();
		}
		graphWidth = 0;
		graphHeight = 0;
		for (N node : nodes.values()) {
			if (node.getMaxX() > graphWidth)
				graphWidth = node.getMaxX();
			
			if (node.getMaxY() > graphHeight)
				graphHeight = node.getMaxY();
			
			setPreferredSize(new Dimension(graphWidth, graphHeight));
			setMinimumSize(new Dimension(graphWidth, graphHeight));
		}
		
		repaint();
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public boolean isEditable() {
		return this.editable;
	}
	
	public void addPreRenderPainter(GraphPainter painter) {
		if (!preRenderPainters.contains(painter))
			preRenderPainters.add(painter);
	}
	
	public void addPostRenderPainter(GraphPainter painter) {
		if (!postRenderPainters.contains(painter))
			postRenderPainters.add(painter);
	}
	
	public void removePreRenderPainter(GraphPainter painter) {
		preRenderPainters.remove(painter);
	}
	
	public void removePostRenderPainter(GraphPainter painter) {
		postRenderPainters.remove(painter);
	}
	
}
