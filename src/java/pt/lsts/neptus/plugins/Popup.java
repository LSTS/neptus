/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.plugins;

import java.awt.Dialog;
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

    /**
     * The modality of the dialog
     */
    Dialog.ModalityType modality() default Dialog.ModalityType.MODELESS;

    /**
     * If set to true, the popup will be always reset to the position defined by the pos() attribute
     */
    boolean alwaysResetPopupPosition() default false;
}
