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
 * 18 de Nov de 2011
 * $Id:: SpeedUnitsEditor.java 9913 2013-02-11 19:11:17Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

/**
 * @author pdias
 *
 */
public class SpeedUnitsEditor extends ComboEditor<String> {

    /**
     * @param options
     */
    public SpeedUnitsEditor() {
        super(new String[] {"RPM", "m/s", "%"});
    }
}
