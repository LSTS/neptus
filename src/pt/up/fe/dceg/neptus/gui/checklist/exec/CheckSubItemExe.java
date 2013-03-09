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
package pt.up.fe.dceg.neptus.gui.checklist.exec;

import java.util.Vector;

import javax.swing.JPanel;

public abstract class CheckSubItemExe extends JPanel{
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Vector<CheckSubItemProvider> checkSubItemsProvider=new Vector<CheckSubItemProvider>();
	
	public void addCheckSubItemProvider(CheckSubItemProvider cp)
	{
		checkSubItemsProvider.add(cp);
	}
	
	public void removeCheckSubItemProvider(CheckSubItemProvider cp)
	{
		checkSubItemsProvider.remove(cp);
	}

	public void warnCheckSubItemProviders()
	{
		for(CheckSubItemProvider c:checkSubItemsProvider)
		{
			c.checkSubItemChange(this);
		}
	}
	
	public void close()
	{
		//clean mem.
	}
	
	public boolean isCheck()
	{
		return false;
	}

}
