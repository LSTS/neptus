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
 * $Id:: Popup.java 9737 2013-01-18 15:41:57Z mfaria                      $:
 */
package pt.up.fe.dceg.neptus.plugins;

import java.awt.event.KeyEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark Console Plug-ins that can be seen as pop-ups.
 * Examples:<br/>
 * <code>@Popup(pos=TOP_LEFT, width=320, height=240, accelerator='J')</code>,<br/>
 * <code>@Popup(pos=BOTTOM, width=640, height=100, accelerator=KeyEvent.VK_F7)</code>
 * @author zp, hugo
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Popup {

    public enum POSITION {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, RIGHT, BOTTOM, TOP, CENTER};

    String name() default "";

    String icon() default "";
    /**
     * Where to locate the popup dialog
     */
    POSITION pos() default POSITION.CENTER;
    
    /**
     * The desired popup window width
     */
    int width() default 100;
    
    /**
     * The desired popup window height
     */
    int height() default 100;
    
    /**
     * If set to a positive value, the combination CTRL+<accelerator> can be pressed to open / close the popup dialog.<br/>
     * To set it, you can use the constants in the class {@link KeyEvent} 
     */
    int accelerator() default KeyEvent.VK_UNDEFINED;
}
