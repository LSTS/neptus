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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Mar 31, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.nio.charset.Charset;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.StringUtils;

import fr.cls.argos.dataxmldistribution.service.DixService;
import fr.cls.argos.dataxmldistribution.service.types.XmlRequestType;
import fr.cls.argos.dataxmldistribution.service.types.XsdRequestType;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class ArgosLocationProvider implements ILocationProvider {

    @NeptusProperty
    private String argosUsername = null;

    @NeptusProperty
    private String argosPassword = null;
    
    @NeptusProperty
    private String platformId = "136978";
    
    private boolean enabled = true;
    
    boolean askCredentials = true;
    
    {
        try {
            PluginUtils.loadProperties("conf/argosCredentials.props", this);
        }
        catch (Exception e) {
        }
    }
    
    private String getArgosUsername() {
        if (argosUsername == null)
            return "";
        return argosUsername;
    }

    private String getArgosPassword() {
        if (argosPassword == null)
            return "";
        return StringUtils.newStringUtf8(DatatypeConverter.parseBase64Binary(argosPassword));
    }

    private void setArgosPassword(String password) {
        if (password == null)
            this.argosPassword = null;

        this.argosPassword = DatatypeConverter.printBase64Binary(password.getBytes(Charset.forName("UTF8")));
    }

    private void setArgosUsername(String username) {
        if (username == null)
            this.argosUsername = null;
        this.argosUsername = username;
    }
    
    private String getXsd() throws Exception {
        DixService srv = new DixService();
        XsdRequestType request = new XsdRequestType();
        return srv.getDixServicePort().getXsd(request).getReturn();
    }
    
    @Periodic(millisBetweenUpdates=120000)
    public void updatePositions() {
        if (!enabled)
            return;
        
        DixService srv = new DixService();
        XmlRequestType request = new XmlRequestType();
        request.setUsername("biodivers");

        if (askCredentials || argosPassword == null) {
            Pair<String, String> credentials = GuiUtils.askCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter Argos Credentials", getArgosUsername(), getArgosPassword());
            if (credentials == null) {
                enabled = false;
                return;
            }
            setArgosUsername(credentials.first());
            setArgosPassword(credentials.second());
            try {
                PluginUtils.saveProperties("conf/argosCredentials.props", this);
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
            askCredentials = false;
        }
        
        request.setUsername(getArgosUsername());
        request.setPassword(getArgosPassword());
        request.setPlatformId(platformId);
        request.setNbDaysFromNow(2);
        try {
            System.out.println(srv.getDixServicePort().getXml(request).getReturn());
            //TODO process result data (when there are positions)
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);            
        }
    }
    
    @Override
    public void onInit(SituationAwareness instance) {
    
    }

    @Override
    public void onCleanup() {

    }

    public static void main(String[] args) throws Exception {
        ArgosLocationProvider provider = new ArgosLocationProvider();
        System.out.println(provider.getXsd());

        provider.updatePositions();
    }
}
