/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.fileeditor;

import java.awt.Color;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultEditorKit.InsertContentAction;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

import org.apache.batik.util.gui.xmleditor.XMLContext;
import org.apache.batik.util.gui.xmleditor.XMLEditorKit;
import org.apache.batik.util.gui.xmleditor.XMLTextEditor;

import pt.lsts.neptus.types.coord.CoordinateUtil;

@SuppressWarnings("serial")
public class SyntaxDocument extends DefaultStyledDocument
{
	private DefaultStyledDocument doc;
	private Element rootElement;

	private boolean multiLineComment;
	private MutableAttributeSet normal;
	private MutableAttributeSet keyword1;
	private MutableAttributeSet keyword2;	
	private MutableAttributeSet latlon;	
	private MutableAttributeSet numeric;
	private MutableAttributeSet comment;
	private MutableAttributeSet quote;

	private HashSet<String> keywords1, keywords2;

	private String lineComment = "//", multilineCommentStart = "/*", multilineCommentEnd = "*/", quoteDelimiters = "\"'";
	private String delimiters = ";:{}()[]+-/%<=>!&|^~*";
	public static JEditorPane getJavaEditorPane() {
		EditorKit javaEditor = new StyledEditorKit() {
			@Override
			public Document createDefaultDocument() {
				return new SyntaxDocument();
			}
		};
		
		JEditorPane editor = new JEditorPane("text/java", "");
		editor.setEditorKit(javaEditor);
		return editor;
	}
	
	public static JEditorPane getJavaScriptEditorPane() {
		return getCustomEditor(new String[] {
							 "abstract", "boolean",	"break", "byte", "byvalue", "case", "cast",
							 "catch", "char", "class", "const", "continue", "default", "do", 
							 "double", "else", "extends", "false", "final", "finally", "float",
							 "for", "future", "generic", "goto", "if", "implements", "import",
							 "inner", "instanceof", "int", "interface", "long", "native", "new",
							 "null", "operator", "outer", "package", "private", "protected", "public",
							 "rest", "return", "short", "static", "super", "switch", "synchronized", 
							 "this", "throw", "throws", "transient", "true", "try", "var", "void",
							 "volatile", "while"
							 
					}, new String[] {"#USE", "#ENV", "out.println", "err.println", 
				             "tree", "tree.setValue", "tree.getValue", "tree.getValueAsString", "tree.getValueAsInteger", "tree.getValueAsDouble",
				             "msg", "msg.setMsg", "msg.getMsg",
				             "env", "env.setEnv", "env.getEnv", "env.initEnv", "env.removeEnv", "env.getEnvAsString", "env.getEnvAsInteger", "env.getEnvAsDouble"
				             },
				    "//", "\"'");	
	}

	public static JEditorPane getXmlEditorPane() {
//		return getCustomEditor(new String[] {
//							 "Sequence","Parallel","Component","Gap","GapComponents",
//							 "PreferredGap","LinkSizeGroup",
//							 
//					}, new String[] {"alignment","resizable","min","pref","max","id",
//									"firstComponent","secondComponent","type"
//				             },
//				    "<!--", "\"'");
		
		//XMLContext.ELEMENT_STYLE
		XMLEditorKit editorKit = new XMLEditorKit();
		editorKit.getStylePreferences().setSyntaxForeground(new HashMap<String, Color>()
				{{
					put(XMLContext.DEFAULT_STYLE, Color.BLACK);
					put(XMLContext.XML_DECLARATION_STYLE, new Color(0, 128, 128));
					put(XMLContext.DOCTYPE_STYLE, new Color(0, 0, 124));
					put(XMLContext.COMMENT_STYLE, new Color(128, 128, 128));
					put(XMLContext.ELEMENT_STYLE, new Color(128, 0, 0));
					put(XMLContext.CHARACTER_DATA_STYLE, Color.BLACK);
					put(XMLContext.ATTRIBUTE_NAME_STYLE, new Color(255, 0, 0));
					put(XMLContext.ATTRIBUTE_VALUE_STYLE, new Color(0, 0, 124));
					put(XMLContext.CDATA_STYLE, new Color(0, 0, 255));
				}});
		
		JEditorPane editor = new XMLTextEditor();
		editor.setEditorKitForContentType(XMLEditorKit.XML_MIME_TYPE, editorKit);
		editor.setContentType(XMLEditorKit.XML_MIME_TYPE);

		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, Event.CTRL_MASK),
				DefaultEditorKit.copyAction
		);
		
		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, Event.SHIFT_MASK),
				DefaultEditorKit.pasteAction
		);
		
		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Event.SHIFT_MASK),
				DefaultEditorKit.cutAction
		);
		InsertContentAction ica = new DefaultEditorKit.InsertContentAction() {
			public void actionPerformed(ActionEvent e) {
				JTextComponent target = getTextComponent(e);
				if ((target != null) && (e != null)) {
					if ((!target.isEditable()) || (!target.isEnabled())) {
						UIManager.getLookAndFeel().provideErrorFeedback(target);
						return;
					}
					String content = "    ";
					target.replaceSelection(content);
				}
			}
		};
		editor.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
				ica);

		return editor;
	}

	
	public static JEditorPane getCustomEditor(String[] keywords1, String[] keywords2, String startLineComment, String strDelimiters) {
		final SyntaxDocument doc = new SyntaxDocument(keywords1, keywords2);
		doc.setLineComment(startLineComment);
		doc.setQuoteDelimiters(strDelimiters);
		//doc.addUndoableEditListener()
		//doc.setDelimiters(delimiters);
		
		EditorKit customEditor = new StyledEditorKit() {
			@Override
			public Document createDefaultDocument() {
				
				return doc;
			}
		};
		
		
		
		JEditorPane editor = new JEditorPane("text/plain", "");
		customEditor.install(editor);
		editor.setEditorKit(customEditor);
		
		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, Event.CTRL_MASK),
				DefaultEditorKit.copyAction
		);
		
		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, Event.SHIFT_MASK),
				DefaultEditorKit.pasteAction
		);
		
		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Event.SHIFT_MASK),
				DefaultEditorKit.cutAction
		);
		InsertContentAction ica = new DefaultEditorKit.InsertContentAction() {
			public void actionPerformed(ActionEvent e) {
				JTextComponent target = getTextComponent(e);
				if ((target != null) && (e != null)) {
					if ((!target.isEditable()) || (!target.isEnabled())) {
						UIManager.getLookAndFeel().provideErrorFeedback(target);
						return;
					}
					String content = "   ";
					if (content != null) {
						target.replaceSelection(content);
					}
				}
			}
		};

		editor.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		
		editor.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
				ica
		);
		
		
		
		return editor;
	}
	
	public SyntaxDocument() {
		this(new String[] {
				 "abstract", "boolean",	"break", "byte", "byvalue", "case", "cast",
				 "catch", "char", "class", "const", "continue", "default", "do", 
				 "double", "else", "extends", "false", "final", "finally", "float",
				 "for", "future", "generic", "goto", "if", "implements", "import",
				 "inner", "instanceof", "int", "interface", "long", "native", "new",
				 "null", "operator", "outer", "package", "private", "protected", "public",
				 "rest", "return", "short", "static", "super", "switch", "synchronized", 
				 "this", "throw", "throws", "transient", "true", "try", "var", "void",
				 "volatile", "while"
				 
		});		
	}
	
	public SyntaxDocument(String[] words) 
	{
		this(words, new String[] {});		
	}
	
	public SyntaxDocument(String[] words1, String words2[]) {
		doc = this;
		rootElement = doc.getDefaultRootElement();
		putProperty( DefaultEditorKit.EndOfLineStringProperty, "\n" );

		normal = new SimpleAttributeSet();
		StyleConstants.setForeground(normal, Color.black);

		comment = new SimpleAttributeSet();
		StyleConstants.setForeground(comment, Color.gray);
		StyleConstants.setItalic(comment, true);

		keyword1 = new SimpleAttributeSet();
		StyleConstants.setForeground(keyword1, Color.blue);

		keyword2 = new SimpleAttributeSet();
		StyleConstants.setForeground(keyword2, new Color(150, 0 , 170));
		StyleConstants.setBold(keyword2, true);
		latlon = new SimpleAttributeSet();
		StyleConstants.setForeground(latlon, new Color(0, 150, 50));
		
		numeric = new SimpleAttributeSet();
		StyleConstants.setForeground(numeric, new Color(10, 150, 190));
		
		quote = new SimpleAttributeSet();
		StyleConstants.setForeground(quote, new Color(10, 150, 90));
		
		keywords1 = new HashSet<String>();
		for (String k1 : words1) {
			keywords1.add(k1);
		}
		
		keywords2 = new HashSet<String>();
		for (String k2 : words2) {
			keywords2.add(k2);
		}
	}

	/*
	 *  Override to apply syntax highlighting after the document has been updated
	 */
	public void insertString(int offset, String str, AttributeSet a) throws BadLocationException
	{
		//if (str.equals("{"))
		//	str = addMatchingBrace(offset);

		super.insertString(offset, str, a);
		processChangedLines(offset, str.length());
	}

	/*
	 *  Override to apply syntax highlighting after the document has been updated
	 */
	public void remove(int offset, int length) throws BadLocationException
	{
		super.remove(offset, length);
		processChangedLines(offset, 0);
	}

	/*
	 *  Determine how many lines have been changed,
	 *  then apply highlighting to each line
	 */
	public void processChangedLines(int offset, int length)
		throws BadLocationException
	{
		String content = doc.getText(0, doc.getLength());

		//  The lines affected by the latest document update

		int startLine = rootElement.getElementIndex( offset );
		int endLine = rootElement.getElementIndex( offset + length );

		//  Make sure all comment lines prior to the start line are commented
		//  and determine if the start line is still in a multi line comment

		setMultiLineComment( commentLinesBefore( content, startLine ) );

		//  Do the actual highlighting

		for (int i = startLine; i <= endLine; i++)
		{
			applyHighlighting(content, i);
		}

		//  Resolve highlighting to the next end multi line delimiter

		if (isMultiLineComment())
			commentLinesAfter(content, endLine);
		else
			highlightLinesAfter(content, endLine);
	}

	/*
	 *  Highlight lines when a multi line comment is still 'open'
	 *  (ie. matching end delimiter has not yet been encountered)
	 */
	private boolean commentLinesBefore(String content, int line)
	{
		int offset = rootElement.getElement( line ).getStartOffset();

		//  Start of comment not found, nothing to do

		int startDelimiter = lastIndexOf( content, getStartDelimiter(), offset - 2 );

		if (startDelimiter < 0)
			return false;

		//  Matching start/end of comment found, nothing to do

		int endDelimiter = indexOf( content, getEndDelimiter(), startDelimiter );

		if (endDelimiter < offset & endDelimiter != -1)
			return false;

		//  End of comment not found, highlight the lines

		doc.setCharacterAttributes(startDelimiter, offset - startDelimiter + 1, comment, false);
		return true;
	}

	/*
	 *  Highlight comment lines to matching end delimiter
	 */
	private void commentLinesAfter(String content, int line)
	{
		int offset = rootElement.getElement( line ).getEndOffset();

		//  End of comment not found, nothing to do

		int endDelimiter = indexOf( content, getEndDelimiter(), offset );

		if (endDelimiter < 0)
			return;

		//  Matching start/end of comment found, comment the lines

		int startDelimiter = lastIndexOf( content, getStartDelimiter(), endDelimiter );

		if (startDelimiter < 0 || startDelimiter <= offset)
		{
			doc.setCharacterAttributes(offset, endDelimiter - offset + 1, comment, false);
		}
	}

	/*
	 *  Highlight lines to start or end delimiter
	 */
	private void highlightLinesAfter(String content, int line)
		throws BadLocationException
	{
		int offset = rootElement.getElement( line ).getEndOffset();

		//  Start/End delimiter not found, nothing to do

		int startDelimiter = indexOf( content, getStartDelimiter(), offset );
		int endDelimiter = indexOf( content, getEndDelimiter(), offset );

		if (startDelimiter < 0)
			startDelimiter = content.length();

		if (endDelimiter < 0)
			endDelimiter = content.length();

		int delimiter = Math.min(startDelimiter, endDelimiter);

		if (delimiter < offset)
			return;

		//	Start/End delimiter found, reapply highlighting

		int endLine = rootElement.getElementIndex( delimiter );

		for (int i = line + 1; i < endLine; i++)
		{
			Element branch = rootElement.getElement( i );
			Element leaf = doc.getCharacterElement( branch.getStartOffset() );
			AttributeSet as = leaf.getAttributes();

			if ( as.isEqual(comment) )
				applyHighlighting(content, i);
		}
	}

	/*
	 *  Parse the line to determine the appropriate highlighting
	 */
	private void applyHighlighting(String content, int line)
		throws BadLocationException
	{
		int startOffset = rootElement.getElement( line ).getStartOffset();
		int endOffset = rootElement.getElement( line ).getEndOffset() - 1;

		int lineLength = endOffset - startOffset;
		int contentLength = content.length();

		if (endOffset >= contentLength)
			endOffset = contentLength - 1;

		//  check for multi line comments
		//  (always set the comment attribute for the entire line)

		if (endingMultiLineComment(content, startOffset, endOffset)
		||  isMultiLineComment()
		||  startingMultiLineComment(content, startOffset, endOffset) )
		{
			doc.setCharacterAttributes(startOffset, endOffset - startOffset + 1, comment, false);
			return;
		}

		//  set normal attributes for the line

		doc.setCharacterAttributes(startOffset, lineLength, normal, true);

		//  check for single line comment

		int index = content.indexOf(getSingleLineDelimiter(), startOffset);

		if ( (index > -1) && (index < endOffset) )
		{
			doc.setCharacterAttributes(index, endOffset - index + 1, comment, false);
			endOffset = index - 1;
		}

		//  check for tokens

		checkForTokens(content, startOffset, endOffset);
	}

	/*
	 *  Does this line contain the start delimiter
	 */
	private boolean startingMultiLineComment(String content, int startOffset, int endOffset)
		throws BadLocationException
	{
		int index = indexOf( content, getStartDelimiter(), startOffset );

		if ( (index < 0) || (index > endOffset) )
			return false;
		else
		{
			setMultiLineComment( true );
			return true;
		}
	}

	/*
	 *  Does this line contain the end delimiter
	 */
	private boolean endingMultiLineComment(String content, int startOffset, int endOffset)
		throws BadLocationException
	{
		int index = indexOf( content, getEndDelimiter(), startOffset );

		if ( (index < 0) || (index > endOffset) )
			return false;
		else
		{
			setMultiLineComment( false );
			return true;
		}
	}

	/*
	 *  We have found a start delimiter
	 *  and are still searching for the end delimiter
	 */
	private boolean isMultiLineComment()
	{
		return multiLineComment;
	}

	private void setMultiLineComment(boolean value)
	{
		multiLineComment = value;
	}

	/*
	 *	Parse the line for tokens to highlight
	 */
	private void checkForTokens(String content, int startOffset, int endOffset)
	{
		while (startOffset <= endOffset)
		{
			//  skip the delimiters to find the start of a new token

			while ( isDelimiter( content.substring(startOffset, startOffset + 1) ) )
			{
				if (startOffset < endOffset)
					startOffset++;
				else
					return;
			}

			//  Extract and process the entire token

			if ( isQuoteDelimiter( content.substring(startOffset, startOffset + 1) ) )
				startOffset = getQuoteToken(content, startOffset, endOffset);
			else
				startOffset = getOtherToken(content, startOffset, endOffset);
		}
	}

	/*
	 *
	 */
	private int getQuoteToken(String content, int startOffset, int endOffset)
	{
		String quoteDelimiter = content.substring(startOffset, startOffset + 1);
		String escapeString = getEscapeString(quoteDelimiter);

		int index;
		int endOfQuote = startOffset;

		//  skip over the escape quotes in this quote

		index = content.indexOf(escapeString, endOfQuote + 1);

		while ( (index > -1) && (index < endOffset) )
		{
			endOfQuote = index + 1;
			index = content.indexOf(escapeString, endOfQuote);
		}

		// now find the matching delimiter

		index = content.indexOf(quoteDelimiter, endOfQuote + 1);

		if ( (index < 0) || (index > endOffset) )
			endOfQuote = endOffset;
		else
			endOfQuote = index;

		doc.setCharacterAttributes(startOffset, endOfQuote - startOffset + 1, quote, false);

		return endOfQuote + 1;
	}

	/*
	 *
	 */
	private int getOtherToken(String content, int startOffset, int endOffset)
	{
		int endOfToken = startOffset + 1;

		while ( endOfToken <= endOffset )
		{
			if ( isDelimiter( content.substring(endOfToken, endOfToken + 1) ) )
				break;

			endOfToken++;
		}

		String token = content.substring(startOffset, endOfToken);

		if ( isKeyword1( token ) )
		{
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset, keyword1, false);
			return endOfToken + 1;
		}
		
		if ( isKeyword2( token ) )
		{
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset, keyword2, false);
			return endOfToken + 1;
		}
		
		if ( isLatLon( token ) )
		{
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset, latlon, false);
			return endOfToken + 1;
		}
		
		if ( isNumeric( token ) )
		{
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset, numeric, false);
			return endOfToken + 1;
		}

		return endOfToken + 1;
	}

	/*
	 *  Assume the needle will the found at the start/end of the line
	 */
	private int indexOf(String content, String needle, int offset)
	{
		int index;

		while ( (index = content.indexOf(needle, offset)) != -1 )
		{
			String text = getLine( content, index ).trim();

			if (text.startsWith(needle) || text.endsWith(needle))
				break;
			else
				offset = index + 1;
		}

		return index;
	}

	/*
	 *  Assume the needle will the found at the start/end of the line
	 */
	private int lastIndexOf(String content, String needle, int offset)
	{
		int index;

		while ( (index = content.lastIndexOf(needle, offset)) != -1 )
		{
			String text = getLine( content, index ).trim();

			if (text.startsWith(needle) || text.endsWith(needle))
				break;
			else
				offset = index - 1;
		}

		return index;
	}

	private String getLine(String content, int offset)
	{
		int line = rootElement.getElementIndex( offset );
		Element lineElement = rootElement.getElement( line );
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		return content.substring(start, end - 1);
	}

	/*
	 *  Override for other languages
	 */
	protected boolean isDelimiter(String character)
	{
		

		if (Character.isWhitespace( character.charAt(0) ) ||
			delimiters.indexOf(character) != -1 )
			return true;
		else
			return false;
	}

	/*
	 *  Override for other languages
	 */
	protected boolean isQuoteDelimiter(String character)
	{
		if (quoteDelimiters.indexOf(character) < 0)
			return false;
		else
			return true;
	}

	/*
	 *  Override for other languages
	 */
	protected boolean isKeyword1(String token)
	{
		return keywords1.contains( token );
	}
	
	protected boolean isKeyword2(String token)
	{
		return keywords2.contains( token );
	}
	
	protected boolean isLatLon(String token) {
		return (!Double.isNaN(CoordinateUtil.parseCoordString(token)) ||
				!Double.isNaN(CoordinateUtil.parseCoordString(token)));
	}
	
	protected boolean isNumeric(String token) {
		try {
			Double.parseDouble(token);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	

	/*
	 *  Override for other languages
	 */
	protected String getStartDelimiter()
	{
		return getMultilineCommentStart();
	}

	/*
	 *  Override for other languages
	 */
	protected String getEndDelimiter()
	{
		return getMultilineCommentEnd();
	}

	/*
	 *  Override for other languages
	 */
	protected String getSingleLineDelimiter()
	{
		return getLineComment();
	}

	/*
	 *  Override for other languages
	 */
	protected String getEscapeString(String quoteDelimiter)
	{
		return "\\" + quoteDelimiter;
	}

	/*
	 *
	 */
	protected String addMatchingBrace(int offset) throws BadLocationException
	{
		StringBuffer whiteSpace = new StringBuffer();
		int line = rootElement.getElementIndex( offset );
		int i = rootElement.getElement(line).getStartOffset();

		while (true)
		{
			String temp = doc.getText(i, 1);

			if (temp.equals(" ") || temp.equals("\t"))
			{
				whiteSpace.append(temp);
				i++;
			}
			else
				break;
		}

		return "{\n" + whiteSpace.toString() + "\t\n" + whiteSpace.toString() + "}";
	}

	public String getLineComment() {
		return lineComment;
	}

	public void setLineComment(String lineComment) {
		this.lineComment = lineComment;
	}

	public String getMultilineCommentEnd() {
		return multilineCommentEnd;
	}

	public void setMultilineCommentEnd(String multilineCommentEnd) {
		this.multilineCommentEnd = multilineCommentEnd;
	}

	public String getMultilineCommentStart() {
		return multilineCommentStart;
	}

	public void setMultilineCommentStart(String multilineCommentStart) {
		this.multilineCommentStart = multilineCommentStart;
	}

	public String getQuoteDelimiters() {
		return quoteDelimiters;
	}

	public void setQuoteDelimiters(String quoteDelimiters) {
		this.quoteDelimiters = quoteDelimiters;
	}

	public String getDelimiters() {
		return delimiters;
	}

	public void setDelimiters(String delimiters) {
		this.delimiters = delimiters;
	}
}
