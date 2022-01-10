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
package pt.lsts.neptus.util.llf;

import java.awt.Component;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.ImageUtils;

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
			LsfIndex index = source.getLsfIndex();
			int mgid = index.getDefinitions().getMessageId(logName);
			int firstPos = index.getFirstMessageOfType(mgid);
			IMCMessage entry = index.getMessage(firstPos);
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
