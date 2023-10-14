/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 14/05/2016
 */
package pt.lsts.neptus.mp.maneuvers.editors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import pt.lsts.neptus.gui.editor.StringPatternEditor;
import pt.lsts.neptus.mp.maneuvers.ScheduledGoto;

/**
 * @author pdias
 *
 */
public class ScheduledGotoDateEditor extends StringPatternEditor {
    @SuppressWarnings("serial")
    private SimpleDateFormat sdf = new SimpleDateFormat(ScheduledGoto.TIME_FORMAT_STR) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    public ScheduledGotoDateEditor() {
        super("20[0-9][0-9]\\-[0-1][0-9]\\-[0-3][0-9] [0-2][0-9]\\:[0-5][0-9]\\:[0-5][0-9]");
    }
    
    @Override
    protected String convertToString(Object value) {
        String str = sdf.format(value);
        return super.convertToString(str);
    }
    
    @Override
    protected Object convertFromString(String value) {
        String str = (String) super.convertFromString(value);
        try {
            return sdf.parse(str);
        }
        catch (ParseException e) {
            return new Date();
        }
    }
}
