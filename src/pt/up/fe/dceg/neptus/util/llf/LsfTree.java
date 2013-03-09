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
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Component;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author ZP
 */
public class LsfTree extends JTree {

    private static final long serialVersionUID = 1L;
    private DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(I18n.text("LSF Listing")));
	private IMraLogGroup source;
	
	public LsfTree(IMraLogGroup source) {		
	    this.source = source;
	    setModel(treeModel);   
        setCellRenderer(new MyTreeCellRenderer());
	    setRootVisible(false);
	    setShowsRootHandles(true);
		parseLogSource(source);
	}
	
	public void parseLogSource(IMraLogGroup source) {
	    String[] logs = source.listLogs();
	    if (logs == null)
	        logs = new String[0];
	    Arrays.sort(logs);
		for (String l : logs)
			addLog(l);
		
		expandPath(new TreePath(treeModel.getRoot()));
	}
	
	public void addLog(String logName) {
	    
	    try {
			IMraLog parser = source.getLog(logName);
			
			IMCMessage entry = parser.getLastEntry();
			
			LLFTreeLog file = new LLFTreeLog(parser, logName);
			DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file);			
			treeModel.insertNodeInto(fileNode, (DefaultMutableTreeNode)treeModel.getRoot(), ((DefaultMutableTreeNode)treeModel.getRoot()).getChildCount());			
			for (String fieldName : entry.getFieldNames()) {
			    String fieldType = entry.getTypeOf(fieldName);
			    LLFField field = new LLFField(fieldName, fieldType);
			    treeModel.insertNodeInto(new DefaultMutableTreeNode(field),fileNode, treeModel.getChildCount(fileNode));
			    }
		}
		catch (Exception e) {			
		    e.printStackTrace();
		    System.out.println("Missing file: " + logName);
		}
	}
}



class LLFField {
	private String fieldName = null;
	private String fieldType = null;
	
	public LLFField(String fieldName, String fieldType) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldType() {
		return fieldType;
	}
	
	public String toString() {
		return getFieldName();
	}
}


class MyTreeCellRenderer extends DefaultTreeCellRenderer {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final ImageIcon LLF_FILE = ImageUtils.getIcon("images/files-icons/lsf16.png");
	private static final ImageIcon LLF_FIELD = ImageUtils.getIcon("images/llf/llf_field.png");
	
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		
		if (node.getUserObject() instanceof LLFTreeLog) {
			setIcon(LLF_FILE);
			setText(((LLFTreeLog)node.getUserObject()).getSimpleName());			
		}
		
		if (node.getUserObject() instanceof LLFField) {
			setIcon(LLF_FIELD);
			setText(((LLFField)node.getUserObject()).getFieldName());			
		}
		
		return this;
	}
}

class LLFTreeLog {
	IMraLog log;
	String simpleName = null;
	
	public LLFTreeLog(IMraLog log, String name) {
		this.log = log;
		this.simpleName = name;
	}

	public IMraLog getLog() {
		return log;
	}

	public String getSimpleName() {
		return simpleName;
	}
	
	public String toString() {
		return getSimpleName();
	}
}
