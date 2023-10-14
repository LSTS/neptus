/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/10/20
 */
package pt.lsts.neptus.mra.importers;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.lsts.imc.lsf.LsfIndex;

/**
 * This interface is used to represent a logical group of log files. For instance, in a mission several log files are produced. 
 * @author zp
 */
public interface IMraLogGroup {
	/**
	 * Retrieves the name of this log group (e.g. folder name)
	 * @return The name of this log group
	 */
	public String name();
	
	/**
	 * Retrieve meta-info data like mission name, description, etc 
	 * @return Meta-info data about this log group
	 */
	public LinkedHashMap<String, Object> metaInfo();
	
	/**
	 * List all log names that exist in this group
	 */
	public String[] listLogs();
	
	/**
	 * Retrieves the {@linkplain IMraLog} named logName 
	 */
	public IMraLog getLog(String logName);
	
	/**
	 * Tries to create a Log Group from the given URI
	 * @param uri The URI for the log group (it may be a file, http, ...)
	 * @return <b>true</b> if it could correctly parse the given URI or <b>false</b> if its not supported
	 */
	public boolean parse(URI uri);
	
	public File getFile(String name);
	public File getDir();
	
	public void cleanup();
	
	public String getEntityName(int src, int src_ent);
	public String getSystemName(int src);
	
	public Collection<Integer> getMessageGenerators(String msgType);
	public Collection<Integer> getVehicleSources(); 
	
	public LsfIndex getLsfIndex();
}
