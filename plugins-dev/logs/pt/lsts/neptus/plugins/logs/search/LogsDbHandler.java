/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 23 Mar 2017
 */
package pt.lsts.neptus.plugins.logs.search;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogsDbHandler {
    public enum DbTableName {
        LOGS("log"),
        DATA("data");


        private String v;
        DbTableName(String v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return this.v;
        }
    }

    public enum LogTableColumnName {
        ID("id"),
        PATH("path"),
        YEAR("year"),
        VEHICLE_ID("vehicle"),
        DATA_TYPE("data_type");


        private String v;
        LogTableColumnName(String v) {
            this.v = v;
        }

        public String toString() {
            return v;
        }
    }

    public enum DataTableColumnName {
        TYPE("type");

        private String v;
        DataTableColumnName(String v) {
            this.v = v;
        }
    }

    private final String DB_HOST = "10.0.2.70";
    private final int DB_PORT = 3306;
    private final String DB_USER = "root";

    private String url;
    private Connection conn = null;
    private String query = null;
    private String action = null;

    public LogsDbHandler() {
        this.url = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/imclogs";

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean connect() {
        try {
            conn = DriverManager.getConnection(url, DB_USER,"123456789");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ResultSet doQuery(String q) {
        if(q == null)
            return null;

        query = q;

        Statement stmnt;
        try {
            stmnt = conn.createStatement();
            return stmnt.executeQuery(q);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int doAction(String a) {
        if(a == null)
            return -1;

        action = a;
        try {
            Statement stmnt = conn.createStatement();
            return stmnt.executeUpdate(action);
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void close() {
        if(conn == null)
            return;
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
