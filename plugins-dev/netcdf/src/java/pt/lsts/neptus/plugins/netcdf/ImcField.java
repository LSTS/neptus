/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 30, 2012
 */
package pt.lsts.neptus.plugins.netcdf;

/**
 * @author zp
 *
 */
public class ImcField {

    protected String message;
    protected String field;
    protected String entity;
    protected String varName = null;
    
    public String getVarName() {
        if (varName == null)
            varName = message+"."+field;
        return varName;
    }
    
    public ImcField(String message, String field, String entity) {
        this.message = message;
        this.entity = entity;
        this.field = field;
    }

    /**
     * @return the message
     */
    public final String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public final void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the field
     */
    public final String getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public final void setField(String field) {
        this.field = field;
    }

    /**
     * @return the entity
     */
    public final String getEntity() {
        return entity;
    }

    /**
     * @param entity the entity to set
     */
    public final void setEntity(String entity) {
        this.entity = entity;
    }
    
    
}
