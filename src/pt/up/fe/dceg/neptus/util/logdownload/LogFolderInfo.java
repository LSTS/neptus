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
 */
package pt.up.fe.dceg.neptus.util.logdownload;

import java.util.LinkedHashSet;

/**
 * @author pdias
 *
 */
public class LogFolderInfo {

	public enum State {UNKNOWN, NEW, DOWNLOADING, ERROR, INCOMPLETE, SYNC, LOCAL};
	
	String name = null;
	State state = State.NEW;
	
	LinkedHashSet<LogFileInfo> logFiles = new LinkedHashSet<LogFileInfo>();
	
	/**
	 * @param name
	 */
	public LogFolderInfo(String name) {
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

        if(!(obj instanceof LogFolderInfo))
            return false;

        LogFolderInfo cmp = (LogFolderInfo) obj;
        return name.equals(cmp.getName());
    }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	
	public void addFile(LogFileInfo file) {
		logFiles.add(file);
	}

	public void removeFile(LogFileInfo file) {
		logFiles.add(file);
	}

	/**
	 * @return the logFiles
	 */
	public LinkedHashSet<LogFileInfo> getLogFiles() {
		return logFiles;
	}
	
	/**
	 * @param name
	 * @return
	 */
	public LogFileInfo getLogFile(String name) {
		for (LogFileInfo lfx : getLogFiles()) {
			try {
				if (lfx.getName().equals(name))
					return lfx;
			} catch (Exception e) {
			}
		}
		return null;
	}
}
