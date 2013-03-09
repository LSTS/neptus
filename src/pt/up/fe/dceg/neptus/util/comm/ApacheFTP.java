/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 31/Mai/2005
 */
package pt.up.fe.dceg.neptus.util.comm;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.types.comm.protocol.FTPArgs;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;

/**
 * @author Paulo Dias
 * 
 */
public class ApacheFTP {

    /*
     * public static boolean ftp(String action, String filename, String hostDirectory, String localDirectory) throws
     * IOException { String xmlFile = "files/config.xml"; //File fx = ConfigFetch.getLabFTPConfAsFile(); File fx = new
     * File (xmlFile); //LabFtpConfigParser configParser = new LabFtpConfigParser(xmlPath // + xmlFile);
     * LabFtpConfigParser configParser = new LabFtpConfigParser(fx.getAbsolutePath());
     * 
     * // parametros necessarios para a ligacao String host = LabFtpConfigParser.REMOTE_HOST; int port =
     * LabFtpConfigParser.REMOTE_PORT; String user = LabFtpConfigParser.USERNAME; String password =
     * LabFtpConfigParser.PASSWORD; String mode = LabFtpConfigParser.TRANSFER_MODE; String connMode =
     * LabFtpConfigParser.CONN_MODE;
     * 
     * return ftp(action, filename, hostDirectory, localDirectory, host, port, user, password, mode, connMode); }
     */

    /**
     * Este método é responsável pelo envio ou recepção de um ficheiro que se encontre num computador remoto através do
     * protocolo FTP.
     * 
     * @param action Indica qual a operação que se pretende realizar ("get" ou "put")
     * @param filename Nome do ficheiro que se pretende transmitir
     * @param hostDirectory Nome da directoria pela qual se pretende transmitir o ficheiro. Caso este parametro seja
     *            null a directoria escolhida será a por defeito.
     * @param localDirectory Nome da directoria pela qual se pretende receber o ficheiro. Caso este parametro seja null
     *            a directoria escolhida será a por defeito.
     * @param vehicleId
     * @return
     * @throws IOException
     */
    public static boolean ftp(String action, String filename, String hostDirectory, String localDirectory,
            String vehicleId) throws IOException, FTPException {
        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleId);
        if (vehicle == null) {
            NeptusLog.pub().error("ApacheFTP :: No vehicle found for id: " + vehicleId);
            return false;
        }
        else {
            /*
             * CommMean cm = CommUtil.getActiveCommMean(vehicle); if (cm == null) {
             * NeptusLog.pub().error("LabFTP :: No active CommMean for " + "vehicle with id: " + vehicleId); return
             * false; } if (!CommUtil.testCommMeanForProtocol(cm, "ftp")) { NeptusLog.pub()
             * .error("LabFTP :: No ftp protocol for CommMean " + "[" + cm.getName() + "] for " + "vehicle with id: " +
             * vehicleId); return false; }
             */
            CommMean cm = CommUtil.getActiveCommMeanForProtocol(vehicle, "ftp");
            if (cm == null) {
                NeptusLog.pub().error(
                        "ApacheFTP :: No active CommMean for " + "ftp protocol for vehicle with id: " + vehicleId);
                return false;
            }
            else {
                try {
                    FTPArgs ftpArgs = (FTPArgs) cm.getProtocolsArgs().get("ftp");

                    String host = cm.getHostAddress();
                    int port = 21;
                    String user = cm.getUserName();
                    String password = cm.getPassword();
                    String mode = ftpArgs.getTransferMode();
                    String connMode = ftpArgs.getConnectionMode();

                    return ftp(action, filename, hostDirectory, localDirectory, host, port, user, password, mode,
                            connMode);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(
                            "ApacheFTP :: No ftp args for CommMean " + "[" + cm.getName() + "] for "
                                    + "vehicle with id: " + vehicleId);
                    return false;
                }
            }
        }
    }

    /**
     * @param action
     * @param filename
     * @param hostDirectory
     * @param localDirectory
     * @param host
     * @param port
     * @param user
     * @param password
     * @param mode
     * @param connMode
     * @return
     * @throws FTPException
     * @throws IOException
     */
    public static boolean ftp(String action, String filename, String hostDirectory, String localDirectory, String host,
            int port, String user, String password, String mode, String connMode) throws IOException, FTPException {

        // FTPClientConfig
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(host);
            // System.out.println("Connected to " + host + ".");
            // System.out.print(ftp.getReplyString());
            NeptusLog.pub().debug("ApacheFTP:: " + "Connected to " + host + ".");
            NeptusLog.pub().debug("ApacheFTP:: " + "Reply string " + ftp.getReplyString() + ".");

            // After connection attempt, you should check the reply code to
            // verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                // System.err.println("FTP server refused connection.");
                NeptusLog.pub().debug("ApacheFTP:: " + "FTP server refused connection.");
                // System.exit(1);
                return false;
            }
        }
        catch (IOException e) {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                }
                catch (IOException f) {
                    e.printStackTrace();
                }
            }
            // System.err.println("Could not connect to server.");
            NeptusLog.pub().debug("ApacheFTP:: " + "Could not connect to server.");
            // e.printStackTrace();
            // System.exit(1);
            return false;
        }

        /*
         * try { ftp.login(user, password); } catch (IOException e1) { e1.printStackTrace(); if (ftp.isConnected()) {
         * try { ftp.disconnect(); } catch (IOException f) { // do nothing } } FTPException fe = new
         * FTPException("Login failed"); fe.setExceptionCode(FTPException.LOGIN_FAILED); throw fe; }
         */
        if (!ftp.login(user, password)) {
            NeptusLog.pub().debug("ApacheFTP:: " + "Login failed.");
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                }
                catch (IOException f) {
                    // do nothing
                }
            }
            FTPException fe = new FTPException("Login failed");
            fe.setExceptionCode(FTPException.LOGIN_FAILED);
            throw fe;
        }

        // System.out.println("Remote system is " + ftp.getSystemName());
        NeptusLog.pub().debug("ApacheFTP:: " + "Remote system is " + ftp.getSystemName() + ".");

        if (connMode.equalsIgnoreCase("pasv"))
            ftp.enterLocalPassiveMode();
        else
            ftp.enterLocalActiveMode();

        try {
            if (mode.equalsIgnoreCase("ASCII"))
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            else
                ftp.setFileType(FTP.IMAGE_FILE_TYPE);
        }
        catch (IOException e2) {
            e2.printStackTrace();
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                }
                catch (IOException f) {
                    // do nothing
                }
            }
            return false;
        }

        if (action.equals("get")) { // retira ficheiro do servidor
            try {
                // caso em que foi especificado um directorio de destino para o
                // ficheiro ser colocado
                if (hostDirectory != null)
                    ftp.changeWorkingDirectory(hostDirectory);

                // System.out.println("Efectuando upload do ficheiro " +
                // filename + "...");
                if (localDirectory != null) {
                    FileOutputStream fos = new FileOutputStream(localDirectory + "/" + filename);
                    ftp.retrieveFile(filename, fos);
                }
                else {
                    FileOutputStream fos = new FileOutputStream(filename);
                    ftp.retrieveFile(filename, fos);
                }
            }
            catch (IOException e) {
                // System.out.println(e.getMessage());
                NeptusLog.pub().debug("ApacheFTP::get:: " + e.getMessage());
                // TODO um System.exit(-1) aqui é perigoso
                // System.exit(-1);
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    }
                    catch (IOException f) {
                        // do nothing
                    }
                }
                return false;
            }
            // System.out.println("concluido!!");
            NeptusLog.pub().debug("ApacheFTP::get:: " + "done");
        }
        else if (action.equals("put")) { // coloca ficheiro no servidor
            try {
                // caso em que foi especificado um directorio onde se encontre o
                // ficheiro
                if (hostDirectory != null)
                    ftp.changeWorkingDirectory(hostDirectory);
                if (localDirectory != null) {
                    FileInputStream fis = new FileInputStream(localDirectory + "/" + filename);
                    ftp.storeFile(filename, fis);
                }
                else {
                    FileInputStream fis = new FileInputStream(filename);
                    ftp.storeFile(filename, fis);
                }
            }
            catch (IOException e) {
                // e.printStackTrace();
                NeptusLog.pub().debug("ApacheFTP::put:: " + e.getMessage());
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    }
                    catch (IOException f) {
                        // do nothing
                    }
                }
                return false;
            }
            NeptusLog.pub().debug("ApacheFTP::put:: " + "done");
        }
        else {
            NeptusLog.pub().debug("ApacheFTP:: " + "Unknown action...");
            return false;
        }

        if (!ftp.logout()) {
            NeptusLog.pub().debug("ApacheFTP:: " + "Logout failed...");
        }
        if (ftp.isConnected()) {
            try {
                ftp.disconnect();
            }
            catch (IOException f) {
                // do nothing
            }
        }

        return true;
    }
}
