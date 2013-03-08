/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by guga
 * 10 de Nov de 2012
 * $Id:: Notification.java 9615 2012-12-30 23:08:28Z pdias                      $:
 */
package pt.up.fe.dceg.neptus.console.notifications;

import java.text.SimpleDateFormat;

/**
 * @author Hugo Dias
 * 
 */
public class Notification implements Comparable<Notification> {

    public enum NotificationType {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }

    private final NotificationType type;
    private final long timestamp;
    private final String timeText;

    private final String title;
    private final String text;
    private final SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss");
    private boolean requireHumanAction = false;
    private String src = "Console";

    /**
     * Static factory method for error type This already has require human action activated
     * 
     * @param text
     * @return
     */
    public static Notification error(String title, String text) {
        return new Notification(title, text, NotificationType.ERROR).requireHumanAction(true);
    }

    public static Notification info(String title, String text) {
        return new Notification(title, text, NotificationType.INFO);
    }

    public static Notification success(String title, String text) {
        return new Notification(title, text, NotificationType.SUCCESS);
    }

    public static Notification warning(String title, String text) {
        return new Notification(title, text, NotificationType.WARNING);
    }

    private Notification(String title, String text, NotificationType type) {
        this.timestamp = System.currentTimeMillis();
        this.text = text;
        this.title = title;
        this.timeText = formater.format(this.timestamp);
        this.type = type;
    }

    public Notification requireHumanAction(boolean flag) {
        this.requireHumanAction = flag;
        return this;
    }

    public Notification src(String src) {
        this.src = src;
        return this;
    }

    public boolean needsHumanAction() {
        return requireHumanAction;
    }

    /**
     * @return the src system
     */
    public String getSrc() {
        return src;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return the timeText
     */
    public String getTimeText() {
        return timeText;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @return the type
     */
    public NotificationType getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Notification other = (Notification) obj;
        if (text == null) {
            if (other.text != null)
                return false;
        }
        else if (!text.equals(other.text))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return timeText + " [" + type + "] " + text;
    }

    @Override
    public int compareTo(Notification o) {
        return (int) (o.getTimestamp() - timestamp);
    }
}
