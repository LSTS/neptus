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
 * Author: zp
 * Jul 2, 2015
 */
package pt.lsts.neptus.plugins.europtus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.TrexAttribute;
import pt.lsts.imc.TrexOperation;
import pt.lsts.imc.TrexToken;

/**
 * @author zp
 *
 */
public class TokenHistory {

    LinkedHashMap<String, Token> states = new LinkedHashMap<String, Token>();

    private ArrayList<Token> history = new ArrayList<TokenHistory.Token>();
    private BufferedWriter writer = null; 
    SimpleDateFormat sdf = new SimpleDateFormat("[YYYY-MM-dd HH:mm:ss: "); 
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public void store(TrexOperation op) {
        Token t = new Token();
        if (op.getToken() == null) {
            System.out.println(op);
            return;
        }
        t.timeline = op.getToken().getTimeline();
        t.timestamp = op.getTimestamp();
        t.source = op.getSourceName();
        t.token = op.getToken();
        t.goal = op.getOp().equals(TrexOperation.OP.POST_GOAL);        
        history.add(t);
        states.put(t.timeline, t);
        
        try {
            log(t);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void log(Token token) throws IOException {
        BufferedWriter writer = getWriter();
        
        String time = sdf.format(new Date((long)(1000.0 * token.timestamp))); 
        writer.write(time + token.source+ " --> " + token.destination + "]\n");        
        
        if (token.goal)
            writer.write("\tGOAL ("+token.timeline+") {\n");
        else
            writer.write("\tOBS. ("+token.timeline+") {\n");
        
        for (TrexAttribute attr : token.token.getAttributes()) {
            writer.write("\t\t"+attr.getName()+" = ");
            if (attr.getMin().equals(attr.getMax()))
                writer.write(attr.getMin()+"\n");
            else
                writer.write("["+attr.getMin()+" .. "+attr.getMax()+"]\n");
        }        
        writer.write("\t}\n");       
    }
    
    public BufferedWriter getWriter() throws IOException {
        if (writer == null) {
        
            File outputDir = new File("europtus");
            if (!outputDir.exists())
                outputDir.mkdirs();
            
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd_HH-mm-ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            File out = new File("europtus/"+sdf.format(new Date())+".log");
            writer = new BufferedWriter(new FileWriter(out));            
            
            System.out.println("Writing to "+out.getAbsolutePath());
        }
        return writer;
    }
    
    protected void close() throws Exception {
        getWriter().flush();
        getWriter().close();
        System.out.println("Finalizing");
    }
    
    
    public static class Token {

        public double timestamp;
        public String timeline;
        public String source;
        public String destination;
        public boolean goal;
        public TrexToken token;

    }
    
    public static void main(String[] args) throws Exception {
        TrexToken t = new TrexToken();
        IMCUtil.fillWithRandomData(t);
        Token tok = new Token();
        tok.timestamp = t.getTimestamp();
        tok.timeline = "auv1.drifter";
        tok.source = "lauv-xplore-1";
        tok.destination = "europtus";
        tok.goal = true;
        tok.token = t;
        
        TokenHistory th = new TokenHistory();
        th.log(tok);   
        th.close();
    }
}
