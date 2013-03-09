/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/10/20
 */
package pt.up.fe.dceg.neptus.mra.importers;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;

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
	
	public void cleanup();
	
	public String getEntityName(int src, int src_ent);
	public String getSystemName(int src);
	
	public Collection<Integer> getMessageGenerators(String msgType);
	public Collection<Integer> getVehicleSources(); 
	
	public LsfIndex getLsfIndex();
}
