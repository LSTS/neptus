/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: lsts
 * 01/06/2017
 */
package pt.lsts.neptus.plugins.nvl;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.GroovyTokenMaker;
/**
 * @author lsts
 *
 */
public class NVLHighlightSupport extends GroovyTokenMaker implements SyntaxConstants {

    TokenMap extraTokens;
    private String[]   functionTokens= {"task",         //language instructions
            "during","message",
            "type","timeout","id","payload","count",               //vehicle requirements
            "post","test","consume"};                              //signals
    
     private String[] highlightTokens ={"execute","halt","idle","pick","until","allOff","choose","pause","action","when","then"};
   
    NVLHighlightSupport(){
        super();
        getKeywords();
    }
    
    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset) {
        if (tokenType == TokenTypes.FUNCTION || tokenType== Token.RESERVED_WORD ) {
            int newType = extraTokens.get(array, start, end);
            if (newType>-1) {
                tokenType = newType;
            }
        }
        super.addToken(array, start, end, tokenType, startOffset, false);
        
    }
    
    
    public TokenMap getKeywords() {
        if (extraTokens == null) {
          try {
            extraTokens = new TokenMap(false);
            
            for (String key : functionTokens) {

              extraTokens.put(key, Token.FUNCTION);
            }
            for (String key : highlightTokens) {

                extraTokens.put(key, Token.RESERVED_WORD);
              }

          } catch (Exception e) {
            //TODO auto-generated method stub
          }
        }
        return extraTokens;
      }

}
