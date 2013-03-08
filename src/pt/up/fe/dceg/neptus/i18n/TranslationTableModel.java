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
 * $Id:: TranslationTableModel.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.JXTable;

import pt.up.fe.dceg.neptus.util.GuiUtils;

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
