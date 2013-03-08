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
 * 10 de Nov de 2012
 * $Id:: JoystickAxisComboEditor.java 9913 2013-02-11 19:11:17Z pdias           $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

/**
 * @author pdias
 *
 */
public class JoystickAxisComboEditor extends ComboEditor<String> {

    /**
     * @param options
     */
    public JoystickAxisComboEditor() {
        super(new String[] { "axis0x", "axis0y", "axis1x", "axis1y", "analogic_cmd" });
    }
}
