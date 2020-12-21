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
 * Apr 20, 2019
 */
package pt.lsts.neptus.plugins.cdc3.msg;

import java.util.Date;

/**
 * @author pdias
 *
 */
public class Cdc3Message {
    public final String ANY = "";
    public final String BROADCAST = "broadcast";
    
    private long timestampMillis = System.currentTimeMillis();
    private String source = "";
    private String destination = "";
    
    /**
     * @return the timestampMillis
     */
    public long getTimestampMillis() {
        return timestampMillis;
    }
    
    /**
     * @param timestampMillis the timestampMillis to set
     */
    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    /**
     * @return
     */
    public long getTimestampSeconds() {
        return timestampMillis / 1000;
    }
    
    /**
     * @param timestampSeconds
     */
    public void setTimestampSeconds(long timestampSeconds) {
        this.timestampMillis = timestampSeconds * 1000;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"abbrev\" : ");
        sb.append("\"");
        sb.append(this.getClass().getSimpleName());
        sb.append("\"");
        sb.append(",\n");
        sb.append("  \"from\" : ");
        sb.append("\"");
        sb.append(source);
        sb.append("\"");
        sb.append(",\n");
        sb.append("  \"to\" : ");
        sb.append("\"");
        sb.append(destination);
        sb.append("\"");
        sb.append(",\n");
        sb.append("  \"time\" : ");
        sb.append(timestampMillis);
        sb.append(",\n");
        sb.append("  \"dateTime\" : ");
        sb.append("\"");
        sb.append(new Date(timestampMillis));
        sb.append("\"");
        sb.append(",\n");
        sb.append(toStringFields());
        sb.append("}");
        
        return sb.toString();
    }

    /**
     * @return
     */
    public String toStringFields() {
        return "";
    }
}
