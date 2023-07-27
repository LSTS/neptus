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
 * Author: Paulo Dias
 * 14/02/2011
 */
package pt.lsts.neptus.comm.transports;

import java.net.InetSocketAddress;
import java.util.Objects;

public class IdPair {
    private String host = "";
    private int port = 0;

    private IdPair(String host, int port) {
        this.host = host == null ? "" : host.replaceFirst("^/", "");
        this.port = port;
    }

    public static IdPair empty() {
        return new IdPair(null, 0);
    }

    public static IdPair from(String host, int port) {
        return new IdPair(host, port);
    }

    public static IdPair from(InetSocketAddress address) {
        String[] addrStr = address.toString().split(":");
        try {
            return new IdPair(addrStr.length > 0 ? addrStr[0] : "",
                    addrStr.length > 1 ? Integer.parseInt(addrStr[1]) : 0);
        }
        catch (NumberFormatException e) {
            return new IdPair(addrStr.length > 0 ? addrStr[0] : "", 0);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IdPair idPair = (IdPair) o;
        return port == idPair.port && Objects.equals(host, idPair.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ':' + port;
    }
}
