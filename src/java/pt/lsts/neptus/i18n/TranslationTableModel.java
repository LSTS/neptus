/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 5, 2012
 */
package pt.lsts.neptus.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.JXTable;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
public class TranslationTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_KEY = 0, COL_TRANSLATION = 1, COL_FILE = 2;

    protected File propsDir;
    protected LinkedHashMap<String, Properties> props = new LinkedHashMap<String, Properties>();
    protected Vector<String> keys = new Vector<String>();
    protected Vector<String> translations = new Vector<String>();
    protected Vector<String> files = new Vector<String>();

    public TranslationTableModel(File propsDir) {
        this.propsDir = propsDir;
        load();
    }

    protected void load() {
        for (File f : propsDir.listFiles()) {

            if (f.isDirectory())
                continue;

            try {
                Properties p = new Properties();
                p.load(new FileInputStream(f));
                props.put(f.getName(), p);
                for (Object key : p.keySet()) {
                    keys.add(key.toString());
                    translations.add(p.get(key).toString());
                    files.add(f.getName());
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (rowIndex == 0)
            return false;
        switch (columnIndex) {
            case COL_KEY:
            case COL_FILE:
                return false;
            default:
                return true;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getRowCount() {
        return keys.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COL_KEY:
                return keys.get(rowIndex);
            case COL_TRANSLATION:
                return translations.get(rowIndex);
            case COL_FILE:
                return files.get(rowIndex);
            default:
                return "?";
        }
    }

    public final Vector<Integer> search(String search) {
        search = search.toUpperCase();
        Vector<Integer> selection = new Vector<Integer>();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).toUpperCase().contains(search)) {
                selection.add(i);
            }
            else if (files.get(i).toUpperCase().contains(search)) {
                selection.add(i);
            }
            else if (translations.get(i).toUpperCase().contains(search)) {
                selection.add(i);
            }
        }
        return selection;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        File propsFile = new File(propsDir, files.get(rowIndex));

        props.get(propsFile.getName()).put(keys.get(rowIndex), aValue.toString());

        try {
            props.get(propsFile.getName()).store(new FileOutputStream(propsFile), "");
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        translations.set(rowIndex, aValue.toString());
    }

    public static void main(String[] args) {
        TranslationTableModel model = new TranslationTableModel(
                new File("conf/localization/pt"));
        JXTable table = new JXTable(model);
        table.setRowSelectionAllowed(true);
        JScrollPane scroll = new JScrollPane(table);
        GuiUtils.testFrame(scroll);

        Vector<Integer> searchResults = model.search("PLUGINNAME");

        for (int i : searchResults) {
            table.getSelectionModel().addSelectionInterval(i, i);
        }
    }
}
