/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/09/13
 */
package pt.lsts.neptus.util.logdownload;

import java.util.LinkedHashSet;

/**
 * @author pdias
 *
 */
public class LogFolderInfo {

	public enum State {UNKNOWN, NEW, DOWNLOADING, ERROR, INCOMPLETE, SYNC, LOCAL};
	
	private String name = null;
	private State state = State.NEW;
	
	private LinkedHashSet<LogFileInfo> logFiles = new LinkedHashSet<LogFileInfo>();
	
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
			}
			catch (Exception e) {
			    e.printStackTrace();
			}
		}
		return null;
	}
}
