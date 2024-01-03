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
 * Author: Paulo Dias
 * 2008/12/17
 */
package pt.lsts.neptus.util.conf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias
 * 
 */
public class StringPatternValidator implements Validator {

    protected String redex = "(\\w(((\\s)*)?,((\\s)*)?)?)*";

    public StringPatternValidator() {
    }

    public StringPatternValidator(String redex) {
        this.redex = redex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.util.conf.Validator#validate(java.lang.Object)
     */
    public String validate(Object newValue) {
        try {
            String comp = (String) newValue;

            Pattern p = Pattern.compile(redex);
            Matcher m = p.matcher(comp);
            boolean b = m.matches();
            return (b) ? null : ("The value '" + comp + "' should be in the form '" + redex + "'");
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String validValuesDesc() {
        String ret = "The value should match redex " + redex;
        return ret;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        StringPatternValidator t = new StringPatternValidator();
        NeptusLog.pub().info("<###>ok: " + t.validate("Main"));
        NeptusLog.pub().info("<###>ok: " + t.validate(""));
        NeptusLog.pub().info("<###>ok: " + t.validate("Main,W1"));
        NeptusLog.pub().info("<###>ok: " + t.validate("Main, W1"));
        NeptusLog.pub().info("<###>ok: " + t.validate("Main, W1 ,Main , W1"));
        NeptusLog.pub().info("<###>ok: " + t.validate("Main, W1 ,Main , W1,"));
        NeptusLog.pub().info("<###>nok: " + t.validate("Main:, W1 ,Main , W1"));
        NeptusLog.pub().info("<###>nok: " + t.validate(",Main, W1 ,Main , W1"));
    }

}
