/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/09/13
 */
package pt.lsts.neptus.util.logdownload;

import java.util.List;

import org.apache.commons.net.ftp.FTPFile;

import pt.lsts.neptus.util.logdownload.LogFolderInfo.State;

/**
 * @author pdias
 *
 */
public class LogFileInfo {

	private String name = null;
	private String uriPartial = null;

	private State state = State.NEW;

	private FTPFile file = null;
	
	private String host = null;
	
	private long size = -1;
	
	// In case is a directory
	private List<LogFileInfo> directoryContents = null;
	
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
	
	public FTPFile getFile() {
	    return file;
	}

	/**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    public void setFile(FTPFile file) {
	    this.file = file;
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
	
    public boolean isDirectory() {
        if (file != null && file.isDirectory())
            return true;
        return false;
    }
    
    /**
     * @return the directoryContents
     */
    public List<LogFileInfo> getDirectoryContents() {
        return directoryContents;
    }
    
    /**
     * @param directoryContents the directoryContents to set
     */
    public void setDirectoryContents(List<LogFileInfo> directoryContents) {
        this.directoryContents = directoryContents;
    }
}
