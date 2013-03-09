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
 */
package pt.up.fe.dceg.neptus.plugins.planning.plandb;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.maneuvers.IMCSerialization;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

/**
 * @author zp
 *
 */
public class PlanDBInfo implements IMCSerialization {

    protected String planId;
    protected int planSize;
    protected double changeTime;
    protected ImcId16 lastChangeAddr;
    protected String lastChangeName;
    protected byte[] md5;
    
    /**
     * @return the planId
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * @param planId the planId to set
     */
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    /**
     * @return the planSize
     */
    public int getPlanSize() {
        return planSize;
    }

    /**
     * @return the changeTime
     */
    public double getChangeTime() {
        return changeTime;
    }

    /**
     * @return the lastChangeAddr
     */
    public ImcId16 getLastChangeAddr() {
        return lastChangeAddr;
    }

    /**
     * @return the lastChangeName
     */
    public String getLastChangeName() {
        return lastChangeName;
    }

    /**
     * @return the md5
     */
    public byte[] getMd5() {
        return md5;
    }

    @Override
    public IMCMessage serializeToIMC() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.IMCSerialiazation#parseIMCMessage(pt.up.fe.dceg.neptus.util.comm.vehicle.IMCMessage)
     */
    @Override
    public void parseIMCMessage(IMCMessage imc_PlanDBInfo) {
        planId = imc_PlanDBInfo.getAsString("plan_id");
        planSize = imc_PlanDBInfo.getAsNumber("plan_size").intValue();
        lastChangeAddr = new ImcId16(imc_PlanDBInfo.getInteger("change_sid"));
        lastChangeName = imc_PlanDBInfo.getAsString("change_sname");
        md5 = imc_PlanDBInfo.getRawData("md5");
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return planId;
    }
}
