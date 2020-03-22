/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Mar 19, 2020
 */
package pt.lsts.neptus.mra.api;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.TreeSet;

import javax.swing.JFileChooser;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.gz.MultiMemberGZIPInputStream;
import pt.lsts.imc.lsf.UnserializedMessage;
import pt.lsts.neptus.mra.api.LsfTreeSet.LsfLog;

/**
 * @author zp
 *
 */
public class LsfTreeSet extends TreeSet<LsfLog> {

    private static final long serialVersionUID = 1L;

    public LsfTreeSet(File... roots) {
        for (File f : roots)
        addRecursively(f);
    }

    public UnserializedMessage next() {
        LsfLog lower = pollFirst();
        if (lower == null)
            return null;

        UnserializedMessage msg = lower.curMessage;
        try {
            lower.curMessage = UnserializedMessage.readMessage(lower.definitions, lower.input);
            add(lower);
        }
        catch (EOFException e) {
            // expected...
        }
        catch (Exception e) {
            e.printStackTrace();
            return next();
        }
        return msg;
    }

    private void addRecursively(File root) {

        LsfLog log = LsfLog.create(root);
        if (log != null)
            add(log);
        
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                addRecursively(f);
            }
        }
    }

    public static class LsfLog implements Comparable<LsfLog> {
        public IMCDefinition definitions;
        public InputStream input;
        public UnserializedMessage curMessage;
        public String root;
        public File lsfSource;
        
        private LsfLog(String root) {
            this.root = root;
        }

        @Override
        public int hashCode() {
            return root.hashCode();
        }

        public static LsfLog create(File root) {
            LsfLog log = new LsfLog(root.getAbsolutePath());
            try {
                if (new File(root, "IMC.xml").canRead())
                    log.definitions = new IMCDefinition(new File(root, "IMC.xml"));
                else if (new File(root, "IMC.xml.gz").canRead()) {
                    log.definitions = new IMCDefinition(
                            new MultiMemberGZIPInputStream(new FileInputStream(new File(root, "IMC.xml.gz"))));
                }
                else
                    return null;

                if (new File(root, "Data.lsf").canRead()) {
                    log.lsfSource = new File(root, "Data.lsf");
                    log.input = new DataInputStream(new FileInputStream(new File(root, "Data.lsf")));
                }
                else if (new File(root, "Data.lsf.gz").canRead()) {
                    log.lsfSource = new File(root, "Data.lsf.gz");
                    log.input = new DataInputStream(
                            new MultiMemberGZIPInputStream(new FileInputStream(new File(root, "Data.lsf.gz"))));
                }
                else
                    return null;
                log.curMessage = UnserializedMessage.readMessage(log.definitions, log.input);

                return log;
            }
            catch (Exception e) {
                return null;
            }
        }

        @Override
        public int compareTo(LsfLog o) {
            return curMessage.compareTo(o.curMessage);
        }
    }
    
    public static LsfTreeSet selectFolders() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select top-level log directory");
        int op = chooser.showOpenDialog(null);

        if (op != JFileChooser.APPROVE_OPTION)
            return null;

        LsfTreeSet lsfSet = new LsfTreeSet();
        for (File f : chooser.getSelectedFiles())
            lsfSet.addRecursively(f);

        return lsfSet;
    }
}
