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

import pt.lsts.neptus.NeptusLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LogsDbHandler {
    public enum DbTableName {
        LOGS("log"),
        DATA("data"),
        VEHICLES("vehicles");


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
        DATE("date"),
        VEHICLE_ID("vehicle"),
        DATA_TYPE("data_type"),
        LAT("lat"),
        LON("lon"),
        DURATION_MILLIS("duration_ms"),
        YEAR("year"),
        LOG_NAME("log_name");


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
    private boolean isConnected = false;

    public LogsDbHandler() {
        this.url = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/imclogs";

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            isConnected = false;
        }
    }

    public void connect() {
        try {
            conn = DriverManager.getConnection(url, DB_USER,"123456789");
            isConnected = true;
        } catch (SQLException e) {
            e.printStackTrace();
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Query database for the given column values
     * */
    public ResultSet doQuery(String payload, String year, String vehicleId) {
        return doQuery(buildQuery(payload, year, vehicleId));
    }

    /**
     * Build query string based on user's selected options
     * */
    private String buildQuery(String payload, String year, String vehicleId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM " + LogsDbHandler.DbTableName.LOGS.toString() + " WHERE ");
        sb.append(buildWhereStatement(payload, year, vehicleId));
        NeptusLog.pub().info(sb.toString());

        return sb.toString();
    }

    /**
     * Query database for logs bounded by the given coordinates
     * */
    public ResultSet searchLogsByCoordinates(String selectedDataTypeStr, String selectedVehicleStr, String selectedYearStr,
                                         double[] minCoordinatesRad, double[] maxCoordinatesRad) {
        String query = "SELECT * FROM log WHERE (lat < " + maxCoordinatesRad[0] +
                " and lat > " + minCoordinatesRad[0] + " and " + " lon > " + minCoordinatesRad[1] + " and " +
                " lon < " + maxCoordinatesRad[1] + ")";

        String optionsQuery = buildWhereStatement(selectedDataTypeStr, selectedYearStr, selectedVehicleStr);

        String finalQuery;
        if(optionsQuery.isEmpty())
            finalQuery = query + ";";
        else
            finalQuery = query + " AND " + optionsQuery + ";";

        return doQuery(finalQuery);
    }

    /**
     * Build the "where" part of a query statement
     * */
    private String buildWhereStatement(String payload, String year, String vehicleId) {
        StringBuilder sb = new StringBuilder();

        boolean searchByPayload = false;
        boolean searchByYear = false;

        if(!payload.equals(LogsSearcher.DataOptionEnum.ANY.toString())) {
            sb.append(LogsDbHandler.LogTableColumnName.DATA_TYPE.toString() + "=" + "\"" + payload + "\"");
            searchByPayload = true;
        }

        if(!year.equals(LogsSearcher.DataOptionEnum.ANY.toString())) {
            if(searchByPayload)
                sb.append(" AND ");
            sb.append(LogsDbHandler.LogTableColumnName.YEAR.toString() + "=" + "\"" + year + "\"");
            searchByYear = true;
        }

        if(!vehicleId.equals(LogsSearcher.DataOptionEnum.ANY.toString())) {
            if(searchByPayload || searchByYear)
                sb.append(" AND ");
            sb.append(LogsDbHandler.LogTableColumnName.VEHICLE_ID.toString() + "=" + "\"" + vehicleId + "\"");
        }

        return sb.toString();
    }

    /**
     * Execute given query
     * */
    private ResultSet doQuery(String q) {
        if(q == null)
            return null;

        query = q;

        Statement stmnt;
        try {
            stmnt = conn.createStatement();
            return stmnt.executeQuery(q);
        } catch (SQLException e) {
            e.printStackTrace();
            isConnected = false;
            return null;
        }
    }

    /**
     * Do action upon the database (e.g. insert entry)
     * */
    public int doAction(String a) {
        if(a == null)
            return -1;

        action = a;
        try {
            Statement stmnt = conn.createStatement();
            return stmnt.executeUpdate(action);
        } catch (SQLException e) {
            e.printStackTrace();
            isConnected = false;
            return -1;
        }
    }

    /**
     * Returns an array of the all the vehicles that have logs
     * */
    public List<String> fetchAvailableVehicles() {
        String query = "SELECT * FROM " + DbTableName.VEHICLES.toString();

        ResultSet res = doQuery(query);
        ArrayList<String> vehicles = new ArrayList<>();
        try {
            while(res.next())
                vehicles.add(res.getString("id"));
        } catch (SQLException e) {
            e.printStackTrace();

            isConnected = false;
        }

        return vehicles;
    }

    public void close() {
        if(conn == null)
            return;
        try {
            conn.close();
            isConnected = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> fetchAvailableYears() {
        String query = "SELECT DISTINCT year FROM " + DbTableName.LOGS.toString();

        ResultSet res = doQuery(query);
        ArrayList<String> years = new ArrayList<>();
        try {
            while(res.next())
                years.add(res.getString("year"));
        } catch (SQLException e) {
            e.printStackTrace();

            isConnected = false;
        }

        return years;
    }

    /**
     * Returns an array of all the data types in the logs
     * */
    public List<String> fetchAvailableDataType() {
        String query = "SELECT * FROM " + DbTableName.DATA.toString();

        ResultSet res = doQuery(query);
        ArrayList<String> dataTypes = new ArrayList<>();
        try {
            while(res.next())
                dataTypes.add(res.getString("type"));
        } catch (SQLException e) {
            e.printStackTrace();

            isConnected = false;
        }

        return dataTypes;
    }

    public void addEntry(DbEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO " + DbTableName.LOGS +
                " (" + LogTableColumnName.PATH.toString() + ", " +
                LogTableColumnName.DATE.toString() + ", " +
                LogTableColumnName.VEHICLE_ID.toString() + ", " +
                LogTableColumnName.LAT.toString() + ", " +
                LogTableColumnName.LON.toString() + ", " +
                LogTableColumnName.DURATION_MILLIS.toString() + ", " +
                LogTableColumnName.YEAR.toString() + ", " +
                LogTableColumnName.LOG_NAME.toString() + ", " +
                LogTableColumnName.DATA_TYPE.toString() + ") VALUES ( " +
                "\"" + entry.logPath + "\"" + ", " +
                "\"" + entry.getDateAsStr() + "\"" + ", " +
                "\"" + entry.vehicleId + "\"" + ", " +
                "\"" + entry.getLatAsStr() + "\"" + ", " +
                "\"" + entry.getLonAsStr() + "\"" + ", " +
                "\"" + entry.getDurationAsStr() + "\"" + ", " +
                "\"" + entry.year + "\"" + ", " +
                "\"" + entry.logName + "\"" + ", ");
        try {
            // add entries
            for(String data : entry.dataType) {
                if(checkEntryExists(entry, data)) {
                    System.out.println("Entry at " + entry.logPath + " with " + entry.dataType +  " already exists");
                    continue;
                }

                Statement stmnt = conn.createStatement();
                stmnt.executeUpdate(sb.toString() + "\"" + data + "\"" + ");");
                System.out.println(sb.toString() + "\"" + data + "\"" + ");");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            isConnected = false;
        }
    }

    private boolean checkEntryExists(DbEntry entry, String dataType) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT 1 FROM " + DbTableName.LOGS + " WHERE ");
        sb.append(LogTableColumnName.PATH.name() + "=" + "\"" + entry.logPath + "\"" + " AND ");
        sb.append(LogTableColumnName.DATA_TYPE.toString() + "=" + "\"" + dataType + "\"" + ";");
        System.out.println(sb.toString());

        try {
            Statement stmnt = conn.createStatement();
            ResultSet res = stmnt.executeQuery(sb.toString());

            return res.next();

        } catch (SQLException e) {
            e.printStackTrace();
            isConnected = false;
        }
        return false;
    }

    public static class DbEntry {
        public String logPath;
        public long dateMillis;
        public long durationMillis;
        public double latRad;
        public double lonRad;
        public String vehicleId;
        public HashSet<String> dataType;
        public int year;
        public String logName;


        public DbEntry() {
            logPath = null;
            dateMillis = -1;
            durationMillis = -1;
            latRad = Double.MAX_VALUE;
            lonRad = Double.MAX_VALUE;
        }

        public String getDateAsStr() {
            if(dateMillis < 0)
                return "-1";

            return String.valueOf(dateMillis);
        }

        public String getDurationAsStr() {
            if(durationMillis < 0)
                return "-1";

            return String.valueOf(durationMillis);
        }

        public String getLatAsStr() {
            if(latRad == Double.MAX_VALUE)
                return "-1";

            return String.valueOf(latRad);
        }

        public String getLonAsStr() {
            if(lonRad == Double.MAX_VALUE)
                return "-1";

            return String.valueOf(lonRad);
        }
    }
}
