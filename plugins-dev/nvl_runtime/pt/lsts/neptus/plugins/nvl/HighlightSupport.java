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
 * http://www.programcreek.com/java-api-examples/index.php?source_dir=JIFI-master/src/robotinterface/gui/panels/editor/syntaxtextarea/FunctionTokenMaker.java
 * Author: lsts
 * 13/06/2017
 */
package pt.lsts.neptus.plugins.nvl;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.modes.GroovyTokenMaker;

import pt.lsts.neptus.NeptusLog;

/**
 * @author lsts
 *
 */
public class HighlightSupport extends GroovyTokenMaker {

  
     private static TokenMap tokenMap = new TokenMap(); 
 
     
     public HighlightSupport(){
         super();
         tokenMap = getWordsToHighlight();
     }
     @Override
     public void addToken(Segment segment, int start, int end, int tokenType, int startOffset){
         switch (tokenType) { 
             // Since reserved words, functions, and data types are all passed into here 
             // as "identifiers," we have to see what the token really is... 
             case Token.IDENTIFIER: 
                 int value = tokenMap.get(segment, start, end); 
                 if (value != -1) { 
                     tokenType = value; 
                 } 
                 break; 
             case Token.WHITESPACE: 
             case Token.SEPARATOR: 
             case Token.OPERATOR: 
             case Token.LITERAL_NUMBER_DECIMAL_INT: 
             case Token.LITERAL_STRING_DOUBLE_QUOTE: 
             case Token.LITERAL_CHAR: 
             case Token.LITERAL_BACKQUOTE: 
             case Token.COMMENT_EOL: 
             case Token.PREPROCESSOR: 
             case Token.VARIABLE: 
                 break; 
               
             default: 
                 NeptusLog.pub().warn("Unknown token type in HighlightSupport");
                 tokenType = Token.IDENTIFIER; 
                 break; 
  
         } 
         super.addToken(segment, start, end, tokenType, startOffset); 

     }
//     @Override
//     public  Token getTokenList(Segment text, int initialTokenType, int startOffset) {
//
//         resetTokenList();
//         this.offsetShift = -text.offset + startOffset;
//
//         // Start off in the proper state.
//         int state = Token.NULL;
//         switch (initialTokenType) {
//             
//             case Token.RESERVED_WORD:
//                 break;
//                 
//             case Token.RESERVED_WORD_2:
//                 break;
//             
//             default:
//                 return super.getTokenList(text, initialTokenType, startOffset);
//         }
//     return firstToken;//TODO
//     }
//    /* (non-Javadoc)
//     * @see org.fife.ui.rsyntaxtextarea.TokenMaker#getTokenList(javax.swing.text.Segment, int, int)
//     */
//    @Override
//    public Token getTokenList(Segment text, int startTokenType, int startOffset) {
//        
//        
//        char[] array = text.array; 
//        int offset = text.offset; 
//        int count = text.count; 
//        int end = offset + count; 
//        int newStartOffset = startOffset - offset; 
//        currentTokenStart = offset; 
//        currentTokenType = startTokenType; 
//        boolean backslash = false; 
//        
//        
//        
//        for (int i = offset; i < end; i++) {
//            char c = array[i]; 
//            
//            switch (currentTokenType) {
//                
//                case Token.NULL:
//                    switch(c){
//                        
//                    }
//                case Token.WHITESPACE:
//                    switch(c){
//                        
//                    }
//                case Token.IDENTIFIER:
//                    switch(c){
//                        
//                    }
//                case Token.LITERAL_NUMBER_DECIMAL_INT:
//                    switch(c){
//                        
//                    }
//                case Token.LITERAL_BACKQUOTE:
//                    switch(c){
//                        
//                    }
//                case Token.LITERAL_STRING_DOUBLE_QUOTE:
//                    switch(c){
//                        
//                    }
//                case Token.VARIABLE:
//                case Token.COMMENT_EOL:
//                case Token.LITERAL_CHAR:
//            }// End of switch (currentTokenType)
//        }// End of for (int i=offset; i<end; i++)
//        
//        
//            switch (currentTokenType) { 
//                case Token.LITERAL_BACKQUOTE:
//                case Token.LITERAL_STRING_DOUBLE_QUOTE:
//                case Token.LITERAL_CHAR:
//                    addToken(text,currentTokenStart, end-1,currentTokenType, newStartOffset+currentTokenStart);
//                    break;
//                case Token.NULL: 
//                    addNullToken(); 
//                    break;
//                default: 
//                    addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart); 
//                    addNullToken(); 
//
//            }
//            
//        return firstToken;
//    }
     
     
     @Override 
     public String[] getLineCommentStartAndEnd(int languageIndex) { 
         return new String[]{"//", null}; 
     } 

    /* (non-Javadoc)
     * @see org.fife.ui.rsyntaxtextarea.AbstractTokenMaker#getWordsToHighlight()
     */
    public TokenMap getWordsToHighlight() {
          tokenMap.put("task", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("during", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("message", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("type", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("timeout", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("id", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("payload", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("count", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("consume", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("test", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;
          tokenMap.put("post", Token.IDENTIFIER); //or Token.RESERVED_WORD_2;

          
          tokenMap.put("execute", Token.RESERVED_WORD); 
          tokenMap.put("halt", Token.RESERVED_WORD); 
          tokenMap.put("idle", Token.RESERVED_WORD); 
          tokenMap.put("pick", Token.RESERVED_WORD); 
          tokenMap.put("until", Token.RESERVED_WORD); 
          tokenMap.put("allOff", Token.RESERVED_WORD); 
          tokenMap.put("choose", Token.RESERVED_WORD); 
          tokenMap.put("pause", Token.RESERVED_WORD); 
          tokenMap.put("action", Token.RESERVED_WORD); 
          tokenMap.put("when", Token.RESERVED_WORD); 
          tokenMap.put("then", Token.RESERVED_WORD); 

          
        return tokenMap;
    }

}
