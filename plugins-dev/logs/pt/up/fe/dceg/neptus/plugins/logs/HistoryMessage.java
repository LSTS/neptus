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
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryMessage implements Comparable<HistoryMessage> {
    public enum msg_type {
        info,
        warning,
        error,
        critical
    };

    protected DateFormat format = new SimpleDateFormat("HH:mm:ss");
    public long timestamp;
    public String text;
    public String context;
    public boolean assynchronous = false;
    public msg_type type = msg_type.info;
    
    public HistoryMessage(){
        
    }
    
    @Override
    public String toString() {
        return "["+format.format(new Date(timestamp))+"] "+" ["+context+"] "+text;
    }
    
    public HistoryMessage(long timestamp, String text, String context, boolean assynchronous, msg_type type) {
        super();
        this.timestamp = timestamp;
        this.text = text;
        this.context = context;
        this.assynchronous = assynchronous;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        HistoryMessage o;
        try {
            o = (HistoryMessage) obj;
        }
        catch (Exception e) {
            return false;
        }
        if (timestamp == o.timestamp && text.equalsIgnoreCase(o.text) && context.equalsIgnoreCase(o.context)
                && type == o.type)
            return true;
        else
            return false;
    }

    @Override
    public int compareTo(HistoryMessage o) {
        return (int) (timestamp - o.timestamp);
    }

    @Override
    public int hashCode() {
        return (context + text + timestamp).hashCode();
    }

}