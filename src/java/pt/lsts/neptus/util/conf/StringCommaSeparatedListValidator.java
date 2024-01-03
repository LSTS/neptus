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
 * 2009/04/02
 */
package pt.lsts.neptus.util.conf;

import java.util.HashSet;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias
 *
 */
public class StringCommaSeparatedListValidator extends StringListValidator {

	/**
	 * @param vals
	 */
	public StringCommaSeparatedListValidator(String... vals) {
		super(vals);
	}

	@Override
	public String validate(Object newValue) {
		try {
			String comp = (String) newValue;
			String[] lt = comp.split("[ ,]+");
			if (lt.length == 0)
				return "No valid value found.";
			for (String val : lt)
			{
				if (super.validate(val) != null)
					return "No valid value found.";
			}
			HashSet<String> ve = new HashSet<String>();
			for (String val : lt) {
				if (ve.contains(val))
					return "No valid value found.";
				else
					ve.add(val);
			}
			return null;
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public static void main(String[] args) {
		NeptusLog.pub().info("<###> "+new StringCommaSeparatedListValidator("UDP", "RTPS").validate(""));
		NeptusLog.pub().info("<###> "+new StringCommaSeparatedListValidator("UDP", "RTPS").validate("UDP"));
		NeptusLog.pub().info("<###> "+new StringCommaSeparatedListValidator("UDP", "RTPS").validate("UDP,RTPS"));
		NeptusLog.pub().info("<###> "+new StringCommaSeparatedListValidator("UDP", "RTPS").validate("RTPS , UDP"));
		NeptusLog.pub().info("<###> "+new StringCommaSeparatedListValidator("UDP", "RTPS").validValuesDesc());
	}
}
