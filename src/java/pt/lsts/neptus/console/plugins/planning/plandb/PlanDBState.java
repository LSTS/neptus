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
 * Author: José Pinto
 * Jun 1, 2011
 */
package pt.lsts.neptus.console.plugins.planning.plandb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.mp.maneuvers.IMCSerialization;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ByteUtil;

/**
 * @author zp
 *
 */
public class PlanDBState implements IMCSerialization {

    protected long lastStateUpdated = -1;
    
    protected Map<String, PlanDBInfo> storedPlans = Collections.synchronizedMap(new LinkedHashMap<String, PlanDBInfo>());
    // protected LinkedHashMap<String, PlanType> storedPlansTypes = new LinkedHashMap<String, PlanType>();
    protected byte[] md5 = null;
    protected Double lastChange = null;
    protected ImcId16 lastChangeAddr = null;
    protected String lastChangeName = null;
    
    @Override
    public IMCMessage serializeToIMC() {
        return null;
    }
    
    @Override
    public synchronized void parseIMCMessage(IMCMessage imc_PlanDBState) {
        lastChangeAddr = new ImcId16(imc_PlanDBState.getInteger("change_sid"));
        lastChangeName = imc_PlanDBState.getAsString("change_sname");
        md5 = imc_PlanDBState.getRawData("md5");
        Vector<IMCMessage> planInfos = imc_PlanDBState.getMessageList("plans_info");
        ArrayList<String> plansIdInVehicle = new ArrayList<>();
        for (IMCMessage m : planInfos) {
            String planId = m.getString("plan_id");
            PlanDBInfo pinfo;
            if (!storedPlans.containsKey(planId))
                pinfo = new PlanDBInfo();
            else 
                pinfo = storedPlans.get(planId);
            
            pinfo.parseIMCMessage(m);
            storedPlans.put(planId, pinfo);
            plansIdInVehicle.add(planId);
        }
        
        // Let us clean the deleted ones
        for (String key : storedPlans.keySet().toArray(new String[storedPlans.size()])) {
            if (!plansIdInVehicle.contains(key))
                storedPlans.remove(key);
        }
        
        lastStateUpdated = System.currentTimeMillis();
    }
    
    /**
     * @return the storedPlans
     */
    public Map<String, PlanDBInfo> getStoredPlans() {
        return storedPlans;
    }

    /**
     * @param storedPlans the storedPlans to set
     */
    public void setStoredPlans(LinkedHashMap<String, PlanDBInfo> storedPlans) {
        this.storedPlans = storedPlans;
    }

    /**
     * @return the md5
     */
    public byte[] getMd5() {
        return md5;
    }

    /**
     * @param md5 the md5 to set
     */
    public void setMd5(byte[] md5) {
        this.md5 = md5;
    }

    /**
     * @return the lastStateUpdated
     */
    public long getLastStateUpdated() {
        return lastStateUpdated;
    }
    
    /**
     * @return the lastChange
     */
    public Double getLastChange() {
        return lastChange;
    }

    /**
     * @param lastChange the lastChange to set
     */
    public void setLastChange(Double lastChange) {
        this.lastChange = lastChange;
    }

    /**
     * @return the lastChangeAddr
     */
    public ImcId16 getLastChangeAddr() {
        return lastChangeAddr;
    }

    /**
     * @param lastChangeAddr the lastChangeAddr to set
     */
    public void setLastChangeAddr(ImcId16 lastChangeAddr) {
        this.lastChangeAddr = lastChangeAddr;
    }

    /**
     * @return the lastChangeName
     */
    public String getLastChangeName() {
        return lastChangeName;
    }

    /**
     * @param lastChangeName the lastChangeName to set
     */
    public void setLastChangeName(String lastChangeName) {
        this.lastChangeName = lastChangeName;
    }

    
    /**
     * Verifies if the given plan matches the one that is stored in this planDB
     * @param localPlan The plan type to verify if is stored
     * @return True if MD5 checksums of the plans match or false otherwise
     */
    public boolean matchesRemotePlan(PlanType localPlan) {
        if (!storedPlans.containsKey(localPlan.getId()))
            return false;
        
        byte[] localMD5 = localPlan.asIMCPlan().payloadMD5();
        byte[] remoteMD5 = storedPlans.get(localPlan.getId()).md5;
        boolean same = ByteUtil.equal(localMD5, remoteMD5);
        return same;
        
    }
}
