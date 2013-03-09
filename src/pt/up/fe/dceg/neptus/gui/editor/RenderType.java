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
package pt.up.fe.dceg.neptus.gui.editor;

public class RenderType {
	public int Type;
	public static String[] list={"2D Only","3D(Single View) Only","2D and 3D(Single View)"
							,"3D(Multi Views) Only","2D and 3D(Multi Views)"};	
	public RenderType(int t)
	{
		Type=t;
	}
	
	public String toString()
	{
	
		return list[Type];
	}
	
	static RenderType getRenderType(String type)
	{
		for (int i=0;i<list.length;i++)
			if(list[i].equals(type))
				return new RenderType(i); 
		return null;
	}
}
