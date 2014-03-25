/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 26/Out/2005
 */
package pt.lsts.neptus.comm.ssh;

import pt.lsts.neptus.util.conf.ConfigFetch;


/**
 * @author Paulo Dias
 *
 */
public class SSHAdjustDate extends SSHExec
{	
    public SSHAdjustDate(String vehicleId)
    {
        super(vehicleId);
    }

    /**
     * Don't use this use the one without arguments, this only will work
     * with {@link SSHExec.ADJUST_DATE}, other wise return false;
     * @see pt.lsts.neptus.comm.ssh.SSHExec#exec(java.lang.String)
     */
    @Override
    public boolean exec(String command) {
        if (SSHExec.ADJUST_DATE.equalsIgnoreCase(command))
            return false;
        return exec();
    }

    public boolean exec() {
        boolean ret = super.exec(SSHExec.ADJUST_DATE);
        //resInterface.tMsg.writeMessageTextln(getExecResponse(), (ret) ? MessagePanel.INFO : MessagePanel.ERROR);
        return ret;
    }

    public static boolean adjust (String vehicleId)
    {
        return SSHExec.exec(vehicleId, SSHExec.ADJUST_DATE);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        ConfigFetch.initialize();
        //boolean rt = SSHExec.exec("lauv", SSHExec.ADJUST_DATE);
        //NeptusLog.pub().info("<###> "+rt);

        //		boolean rt = adjust("lauv");
        //        NeptusLog.pub().info("<###> "+rt);

        //        PanelResult pRes = new PanelResult((Window)null);
        //        pRes.setVisible(true);

        //SSHAdjustDate sshAdj = new SSHAdjustDate("lauv-blue");
        //sshAdj.showInterface();
        //sshAdj.exec();

        adjust("lauv-blue");
    }
}
