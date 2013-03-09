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
 * Nov 30, 2012
 */
package pt.up.fe.dceg.neptus.plugins.netcdf;

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
