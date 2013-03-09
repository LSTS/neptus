/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.env;

import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import pt.up.fe.dceg.neptus.gui.ChartPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.JTreeUtils;

public class EnvironmentBrowser extends JPanel implements EnvironmentListener {

	private static final long serialVersionUID = 1L;
	private JSplitPane jSplitPane = null;
	private JTree variableTree = null;
	private JPanel bottomPanel = null;
	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode root; 
	private Environment environment = new Environment();
	private ChartPanel chartPanel = null;
	
	/**
	 * This method initializes 
	 */
	public EnvironmentBrowser() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
	    root = new DefaultMutableTreeNode("Environment");	    
	    treeModel = new DefaultTreeModel(root);
	    
        this.setLayout(new BorderLayout());
        this.setSize(new java.awt.Dimension(422,383));
        this.add(getJSplitPane(), java.awt.BorderLayout.CENTER);			
	}

	/**
	 * This method initializes jSplitPane	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			jSplitPane.setDividerSize(5);
			
			jSplitPane.setBottomComponent(getJPanel());
			jSplitPane.setTopComponent(getJTree());
			
			jSplitPane.setDividerLocation(0.75);
		}
		return jSplitPane;
	}

	/**
	 * This method initializes jTree	
	 * 	
	 * @return javax.swing.JTree	
	 */
	private JTree getJTree() {
		if (variableTree == null) {
			variableTree = new JTree(treeModel);
		}
		return variableTree;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel() {
		if (bottomPanel == null) {
			bottomPanel = new JPanel();
			bottomPanel.setLayout(new BorderLayout());
			bottomPanel.add(getChartPanel(), BorderLayout.CENTER);
		}
		return bottomPanel;
	}
	
	private ChartPanel getChartPanel() {
		if (chartPanel == null) {
			chartPanel = new ChartPanel(100);
			chartPanel.listenToEnvironment(environment, environment.getVariableNames());
		}
		return chartPanel;
	}

	public Environment getEnvironment() {
		return environment;
	}
	
	public void updateTree() {
		this.root.removeAllChildren();
		
		for (String varName : environment.getVariableNames()) {
			DefaultMutableTreeNode curParent = root;
			//System.out.println("adding var "+varName);
			String[] parts = environment.getEnv(varName).getIdParts();
			for (int j = 0; j < parts.length - 1; j++) {
				String curPart = parts[j];
				boolean dirCreated = false;
				for (Enumeration<?> e = curParent.children(); e.hasMoreElements();) {
					DefaultMutableTreeNode curChild = (DefaultMutableTreeNode) e.nextElement();
					if (curChild.getUserObject().equals(curPart)) {
						dirCreated = true;
						curParent = curChild;
						break;
					}
				}
				if (!dirCreated) {
					//System.out.println("Creating node "+curPart);
					DefaultMutableTreeNode tmp = new DefaultMutableTreeNode(curPart);
					curParent.add(tmp);
					curParent = tmp;
					//JTreeUtils.expandAll(getJTree());
				}
			
			}
			curParent.add(new DefaultMutableTreeNode(environment.getEnv(varName)));
			treeModel.reload();
		}
	}


	public void setEnvironment(Environment environment) {
		this.environment = environment;
		environment.addEnvironmentListener(this);
		updateTree();
		JTreeUtils.expandAll(getJTree());
		getChartPanel().listenToEnvironment(environment, environment.getVariableNames());
		repaint();
	}
	
	public void EnvironmentChanged(EnvironmentChangedEvent event) {
		
		if (event.getType() != EnvironmentChangedEvent.VARIABLE_CHANGED) {
			updateTree();
			JTreeUtils.expandAll(getJTree());
		}
		repaint();
	}
	
	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		EnvironmentBrowser eb = new EnvironmentBrowser();		
		GuiUtils.testFrame(eb, "EnvironmentBrowser");
		final Environment env = new Environment();
		env.putEnv(new NeptusVariable("Isurus.Position.X", new Double(31)));
		env.putEnv(new NeptusVariable("Isurus.Position.Y", new Double(-21.34)));
		env.putEnv(new NeptusVariable("Isurus.Position.Z", new Double(0.234)));
		
		env.putEnv(new NeptusVariable("Isurus.CTD.Conductivity", new Double(12)));
		env.putEnv(new NeptusVariable("Isurus.CTD.Temperature", new Double(23.3)));
		env.putEnv(new NeptusVariable("Isurus.CTD.Depth", new Double(0.0)));
		
		eb.setEnvironment(env);
		
		Thread test = new Thread(new Runnable() {
			public void run() {
				Random rnd = new Random(System.currentTimeMillis());
				while(true) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {}
					
						env.putEnv(new NeptusVariable("Isurus.CTD.Depth", new Double(rnd.nextDouble() * 20 + 15)));
					
				}
			};
		});
		
		test.start();
	}
	
	public DefaultMutableTreeNode findNode(NeptusVariable var) {
		String[] path = var.getIdParts();
		DefaultMutableTreeNode curNode = root;
		for (int i = 0; i < path.length - 1; i++) {
			for (Enumeration<?> e = curNode.children(); e.hasMoreElements(); ) {
				DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)e.nextElement();
				if (tmp.getUserObject().equals(path[i])) {
					curNode = tmp;
					break;
				}
			}
		}
		
		for (Enumeration<?> e = curNode.children(); e.hasMoreElements(); ) {
			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)e.nextElement();
			if (tmp.getUserObject().equals(var)) {
				return tmp;
			}
		}		
		return null;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
