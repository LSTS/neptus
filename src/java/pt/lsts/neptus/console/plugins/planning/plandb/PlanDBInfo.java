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
 * Author: José Pinto
 * Jun 1, 2011
 */
package pt.lsts.neptus.console.plugins.planning.plandb;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.mp.maneuvers.IMCSerialization;
import pt.lsts.neptus.types.NameId;

/**
 * @author zp
 *
 */
public class PlanDBInfo implements IMCSerialization, NameId {

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

    @Override
    public void parseIMCMessage(IMCMessage imc_PlanDBInfo) {
        planId = imc_PlanDBInfo.getAsString("plan_id");
        planSize = imc_PlanDBInfo.getAsNumber("plan_size").intValue();
        lastChangeAddr = new ImcId16(imc_PlanDBInfo.getInteger("change_sid"));
        lastChangeName = imc_PlanDBInfo.getAsString("change_sname");
        md5 = imc_PlanDBInfo.getRawData("md5");
    }
        
    @Override
    public String toString() {
        return planId;
    }

    @Override
    public String getIdentification() {
        return planId;
    }

    @Override
    public String getDisplayName() {
        return planId;
    }
}
