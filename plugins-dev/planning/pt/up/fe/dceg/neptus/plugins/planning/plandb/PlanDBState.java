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
 * Jun 1, 2011
 * $Id:: PlanDBState.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.plugins.planning.plandb;

import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.maneuvers.IMCSerialization;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

/**
 * @author zp
 *
 */
public class PlanDBState implements IMCSerialization {

    protected long lastStateUpdated = -1;
    
    protected LinkedHashMap<String, PlanDBInfo> storedPlans = new LinkedHashMap<String, PlanDBInfo>();
    protected LinkedHashMap<String, PlanType> storedPlansTypes = new LinkedHashMap<String, PlanType>();
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
        for (IMCMessage m : planInfos) {
            PlanDBInfo pinfo = new PlanDBInfo();
            pinfo.parseIMCMessage(m);
            storedPlans.put(pinfo.getPlanId(), pinfo);
        }
        lastStateUpdated = System.currentTimeMillis();
    }
    
    /**
     * @return the storedPlans
     */
    public LinkedHashMap<String, PlanDBInfo> getStoredPlans() {
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
        
//        if (localMD5.length != remoteMD5.length)
//            return false;
//        
//        for (int i = 0; i < localMD5.length; i++)
//            if (localMD5[i] != remoteMD5[i])
//                return false;
//        
//        return true;
        return ByteUtil.equal(localMD5, remoteMD5);
    }
}
