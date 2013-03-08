/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Dec 11, 2012
 * $Id::                                                                        $:
 */
package pt.up.fe.dceg.neptus.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;


/**
 * @author jqcorreia
 *
 */
public class FtpTest {
    public static void main(String[] args) throws Exception {
        FTPClient client = new FTPClient();
        FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
        
        client.connect("10.0.2.60", 21);
        client.configure(conf);
        
        client.enterLocalPassiveMode();
        client.login("anonymous", "");
        
        System.out.println(client.printWorkingDirectory());

        for(FTPFile f: client.listFiles()) {
            System.out.println(f.getName());
        }
        
        System.out.println(client.retrieveFileStream("/lauv-seacon-3/20121211/182554_idle/IMC.xml"));
        client.disconnect();
    }
}
