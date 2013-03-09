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
package pt.up.fe.dceg.neptus.plugins.update;

import pt.up.fe.dceg.neptus.NeptusLog;

public class UpdateRequest implements Comparable<UpdateRequest> {

	private IPeriodicUpdates source;
	

	private long nextUpdateTime;
	
	public long getNextUpdateTime() {
		return nextUpdateTime;
	}

	public UpdateRequest(IPeriodicUpdates source) {
		this.source = source;
		if (source == null)
			new Exception().printStackTrace();		
		nextUpdateTime = System.currentTimeMillis();
	}
	
	@Override
	public int compareTo(UpdateRequest o) {
		return (int) (nextUpdateTime - o.nextUpdateTime);
	}
	
	public boolean update() {
		if (source == null)
			return false;
		nextUpdateTime = System.currentTimeMillis() + source.millisBetweenUpdates();
        try {
            return source.update();
        }
        catch (Exception e) {
            NeptusLog.pub().error(PeriodicUpdatesService.class.getSimpleName() + 
                    " exception on updated call from " + "'" +
                    		source +"' " + e.getMessage(), e);
        }
        catch (Error e) {
            NeptusLog.pub().error(PeriodicUpdatesService.class.getSimpleName() + 
                    " error on updated call from " + "'" +
                            source +"' " + e.getMessage(), e);
        }
        return false;
	}
	
	public IPeriodicUpdates getSource() {
		return source;
	}
	
}