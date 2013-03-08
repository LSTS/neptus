/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2009/09/13
 * $Id:: LogFileInfo.java 9616 2012-12-30 23:23:22Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.util.logdownload;

import pt.up.fe.dceg.neptus.util.logdownload.LogFolderInfo.State;

/**
 * @author pdias
 *
 */
public class LogFileInfo {

	String name = null;
	
	String uriPartial = null;
	
	State state = State.NEW;

	private long size = -1;
	
	/**
	 * @param name
	 */
	public LogFileInfo(String name) {
		super();
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * @return the uriPartial
	 */
	public String getUriPartial() {
		return uriPartial;
	}
	
	/**
	 * @param uriPartial the uriPartial to set
	 */
	public void setUriPartial(String uriPartial) {
		this.uriPartial = uriPartial;
	}
	
	/**
	 * @return the state
	 */
	public State getState() {
		return state;
	}
	
	/**
	 * @param state the state to set
	 */
	public void setState(State state) {
		this.state = state;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}
	
	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
	

	@Override
	public boolean equals(Object obj) {
	    if(this == obj)
	        return true;

	    if(!(obj instanceof LogFileInfo))
	        return false;

	    LogFileInfo cmp = (LogFileInfo) obj;
	    return name.equals(cmp.getName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
