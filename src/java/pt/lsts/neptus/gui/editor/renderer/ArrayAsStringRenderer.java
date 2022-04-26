/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 05/10/2017
 */
package pt.lsts.neptus.gui.editor.renderer;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import pt.lsts.neptus.i18n.I18n;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class ArrayAsStringRenderer extends DefaultCellRenderer {
    {
        setOpaque(false);
    }

    public ArrayAsStringRenderer() {
    }

    @Override
    protected String convertToString(Object value) {
        if (value.getClass().isArray()) {
            Class<?> compType = value.getClass().getComponentType(); 
            if (compType.isPrimitive()) {
                if (compType == Long.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((long[]) value));
                else if (compType == Integer.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((int[]) value));
                else if (compType == Short.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((short[]) value));
                else if (compType == Double.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((double[]) value));
                else if (compType == Float.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((float[]) value));
                else if (compType == Boolean.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((boolean[]) value));
                else if (compType == Byte.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((byte[]) value));
                else if (compType == Character.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((char[]) value));
            }
            else {
                return toStringI18n((Object[]) value);
            }
        }

        return String.valueOf(value);
    }
    
    private static String toStringI18n(Object[] a) {
        if (a == null)
            return "";

        if (!a.getClass().getComponentType().equals(String.class) && !a.getClass().getComponentType().isEnum())
            return Arrays.toString(a);

        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(I18n.text(String.valueOf(a[i])));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

}
