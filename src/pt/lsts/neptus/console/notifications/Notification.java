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
 * Author: Hugo Dias
 * 10 de Nov de 2012
 */
package pt.lsts.neptus.console.notifications;

import java.text.SimpleDateFormat;

import pt.lsts.neptus.i18n.I18n;

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
    private String src = I18n.text("Console");

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

    public static Notification newNotification(String title, String text, NotificationType type) {
        return new Notification(title, text, type);
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
