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
 * Author: Manuel Ribeiro
 * 14/11/2018
 */
package pt.lsts.neptus.plugins.djiimporter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;

import javax.swing.table.AbstractTableModel;

/**
 * @author Manuel Ribeiro
 *
 */
public class MetadataModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    public ArrayList<String> columnNames = new ArrayList<>();
    private ArrayList<ImageMetadata> data = new ArrayList<>();
    {
        columnNames.addAll(Arrays.asList("Name", "Date", "Location", "GPS Alt.", "Altitude (AGL)", "Resolution", "Make", "Model"));
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    public MetadataModel(ArrayList<ImageMetadata> data) {
        this.data = data;
    }

    public Object getValueAt(int row, int col) {
        ImageMetadata imgMD = data.get(row);
        if (imgMD == null)
            return null;

        switch (col) {
            case 0:
                return imgMD.getName();
            case 1:
                DateFormat formatterUTC = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC")); // UTC timezone
                return formatterUTC.format(imgMD.getDate());
            case 2:
                return imgMD.getLocation().toString();
            case 3:
                return Double.toString(imgMD.getMSL());
            case 4:
                return Double.toString(imgMD.getAGL());
            case 5:
                return imgMD.getImgWidth() + "x" + imgMD.getImgHeight();
            case 6:
                return imgMD.getMake();
            case 7:
                return imgMD.getModel();
            default:
                return "";
        }
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    public void update(ArrayList<ImageMetadata> dataArray) {
        this.data = dataArray;
        fireTableDataChanged();
    }


}
