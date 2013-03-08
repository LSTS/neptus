/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 18/02/2013
 * $Id:: PropertyRenderer.java 10068 2013-03-05 03:31:47Z robot                 $:
 */
package pt.up.fe.dceg.neptus.plugins.params.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;

import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PropertyRenderer extends DefaultCellRenderer {
    private Color dirtyColor = new Color(255, 108, 108);
    private Color syncColor = new Color(108, 255, 108);
    @SuppressWarnings("unused")
    private Color blueColor = new Color(108, 108, 255);
    @SuppressWarnings("unused")
    private Color orangeColor = new Color(255, 108, 54);

    private boolean sync = false;
    
    private final String unitsStr;
    
    {
        setShowOddAndEvenRows(false);
    }

    public PropertyRenderer() {
        unitsStr = null;
    }

    /**
     * @param unitsStr
     */
    public PropertyRenderer(String unitsStr) {
        this.unitsStr = unitsStr;
    }

    @Override
    protected String convertToString(Object value) {
        return (value == null ? "" : super.convertToString(value)) + (unitsStr == null ? "" : " " + unitsStr);
    }
    
    /* (non-Javadoc)
     * @see com.l2fprod.common.swing.renderer.DefaultCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return super.getTableCellRendererComponent(table, value, false, hasFocus, row, column);
    }
    
    public void setPropertyInSync(boolean sync) {
        setShowOddAndEvenRows(true);
        this.sync = sync;
        if (this.sync) {
            setOddBackgroundColor(syncColor);
            setEvenBackgroundColor(syncColor);
        }
        else {
            setOddBackgroundColor(dirtyColor);
            setEvenBackgroundColor(dirtyColor);
        }
    }
}