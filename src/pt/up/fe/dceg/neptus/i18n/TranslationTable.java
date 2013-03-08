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
 * Sep 5, 2012
 * $Id:: TranslationTable.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.i18n;

import java.io.File;

import org.jdesktop.swingx.JXTable;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class TranslationTable extends JXTable {

    private static final long serialVersionUID = 1L;

    public TranslationTable(File propsDir) {
        super(new TranslationTableModel(propsDir));
    }
    
    public static void main(String[] args) {
        GuiUtils.testFrame(new TranslationTable(new File("conf/localization/pt")));
    }
    
}
