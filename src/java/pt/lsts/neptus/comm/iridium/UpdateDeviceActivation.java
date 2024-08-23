/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * Jun 19, 2024
 */
package pt.lsts.neptus.comm.iridium;

import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;

import java.util.Collection;
import java.util.Vector;

/**
 * @author zp
 *
 */
public class UpdateDeviceActivation extends IridiumMessage {

    public static enum OperationType {
        OP_DEACTIVATE,
        OP_ACTIVATE;

    }

    public UpdateDeviceActivation() {
        super(2012);
    }


    protected double timestampSeconds = System.currentTimeMillis() / 1000.;
    protected OperationType operation = OperationType.OP_ACTIVATE;

    public double getTimestampSeconds() {
        return timestampSeconds;
    }

    public void setTimestampSeconds(double timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        out.writeDouble(timestampSeconds);
        out.writeByte(operation.ordinal());
        return 9;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        try {
            timestampSeconds = in.readDouble();
            operation = OperationType.values()[in.readUnsignedByte()];
        }
        catch (Exception e) {
            // empty
        }
        return 9;
    }

    @Override
    public Collection<IMCMessage> asImc() {
        return new Vector<>();
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s +=  "\t[timestampSeconds: "+timestampSeconds+"]\n";
        s +=  "\t[operation: "+operation.name()+"]\n";
        return s;
    }
}
