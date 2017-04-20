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
 * 18 Apr 2017
 */
package pt.lsts.neptus.plugins.logs.search;


import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ConnConfig {
    private final File CONFIG_FILE = new File(System.getProperty("user.dir") + "/conf/logs-searcher.conf");
    public String ftpHost;
    public int ftpPort;
    public String ftpBaseDir;

    public String dbHost;
    public int dbPort;

    public boolean isValid;

    public ConnConfig() {
        isValid = load();
    }

    private boolean load() {
        try {
            ArrayList<String> content = Files.lines(CONFIG_FILE.toPath())
                    .collect(Collectors.toCollection(ArrayList::new));

            for(String line : content) {
                if(line.contains("ftp: ")) {
                    String[] ftpConfig = parseLine(line, "ftp: ", 3);

                    if(ftpConfig == null)
                        return false;

                    ftpHost = ftpConfig[0];
                    ftpPort = Integer.parseInt(ftpConfig[1]);
                    ftpBaseDir = ftpConfig[2];

                    if(!ftpBaseDir.endsWith("/"))
                        ftpBaseDir = ftpBaseDir + "/";
                }
                else if(line.contains("db: ")) {
                    String[] dbConfig = parseLine(line, "db: ", 2);

                    if(dbConfig == null)
                        return false;

                    dbHost = dbConfig[0];
                    dbPort = Integer.parseInt(dbConfig[1]);
                }
            }
        // fail on any exception
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String[] parseLine(String line, String sep, int nParts) {
        String[] config = line.split(sep)[1].split(",[\\s]+");

        // string cleanup
        Arrays.stream(config)
                .map(s -> s.replace(" ", ""));

        if(config.length < nParts)
            return null;

        // empty field(s)
        if(Arrays.stream(config).anyMatch(f -> f.equals("")))
            return null;

        return config;
    }

    public static void main(String[] args) {
        ConnConfig config = new ConnConfig();
        System.out.println(config.isValid);

        System.out.println(config.ftpHost);
        System.out.println(config.ftpPort);
        System.out.println(config.ftpBaseDir);

        System.out.println(config.dbHost);
        System.out.println(config.dbPort);
    }
}
