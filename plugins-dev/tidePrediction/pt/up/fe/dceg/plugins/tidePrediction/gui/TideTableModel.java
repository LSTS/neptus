/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: meg
 * May 15, 2013
 */
package pt.up.fe.dceg.plugins.tidePrediction.gui;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.bathymetry.TidePrediction;
import pt.up.fe.dceg.neptus.util.bathymetry.TidePrediction.TIDE_TYPE;


/**
 * @author meg
 *
 */
public class TideTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 3748803424658938532L;
    private final String label = "High tide (m)";
    private final Date startDate;
    private final Vector<TidePrediction> predictions;

    /**
     * Start date start as null.
     */
    public TideTableModel() {
        super();
        this.startDate = new Date();
        this.predictions = new Vector<>();
        predictions.add(new TidePrediction(0, startDate, TIDE_TYPE.HIGH_TIDE));
        Calendar cal = Calendar.getInstance(); // creates calendar
        cal.setTime(startDate); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, 6); // adds one hour
        Date secondTide = cal.getTime(); // returns new date object, one hour in the future
        predictions.add(new TidePrediction(0, secondTide, TIDE_TYPE.HIGH_TIDE));
    }

    @Override
    public int getRowCount() {
        return predictions.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    /**
     * Both time and height of tide are in the TidePrediction object. Time goes in the first column and tide height in
     * the second.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        // Header
        if(rowIndex == 0){
            switch (columnIndex) {
                case 0:
                    return startDate;
                case 1:
                    return label;
            }
        }
        // Values of tide predictions
        else {
            switch (columnIndex) {
                case 0:
                    return predictions.get(rowIndex).getTimeAndDate();
                case 1:
                    return predictions.get(rowIndex).getHeight();
            }
        }
        NeptusLog.pub().error("Case not covered in TideTableModel row:" + rowIndex + ", collumn:" + columnIndex);
        return null;
    }

    /**
     * Only thing not editable is the title of the heights column.
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == 1 && row == 0) {
            return false;
        }
        else {
            return true;
        }
    }

}



