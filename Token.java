import java.util.ArrayList;
/**
 * Represents the tokens of the language {@code myLang} and few helpful tokens for LLVM-IR language, includes some methods to evaluate &amp; compare them.
 * <p>The tokens can be:</p>
 * <ul>
 * 	<li>Special keywords: <ul>
 * 		<li><code>if</code></li>
 * 		<li><code>while</code></li>
 * 		<li><code>choose</code></li><li>
 * 		<code>print</code></li>
 * 		</ul></li>
 * 	<li>Operators:<ul>
 * 		<li>Assignment operator <code>'='</code></li>
 * 		<li>Addition <code>'+'</code></li>
 * 		<li>Subtraction <code>'-'</code></li>
 * 		<li>Multiplication <code>'*'</code></li>
 * 		<li>Division <code>'/'</code></li>
 * 		</ul></li>
 * 	<li>Seperators:<ul>
 * 		<li>Left parentheses <code>'('</code></li>
 * 		<li>Right parentheses <code>')'</code></li>
 * 		<li>Left curly braces <code>'{'</code></li>
 * 		<li>Right curly braces <code>'}'</code></li>
 * 		<li>Comma <code>','</code></li>
 * 		</ul></li>
 * 	<li>Identifiers:<ul>
 * 		<li>Variable</li>
 * 		<li>Temporary variable (for LLVM-IR)</li>
 * 		</ul></li>
 * <li>Literals:<ul>
 * 		<li>Integer</li>
 * 		</ul></li>
 * </ul>
 * @author Aral Dortogul
 */
public class Token {
			// SPECIAL KEYWORDS
	/**
	 * Symbolic variable to denote the token: {@code if}
	 */
	public static final int _if = -11;
	/**
	 * Symbolic variable to denote the token: {@code while}
	 */
	public static final int _while = -12;
	/**
	 * Symbolic variable to denote the token: {@code choose}
	 */
	public static final int _choose = -13;
	/**
	 * Symbolic variable to denote the token: {@code print}
	 */
	public static final int _print = -14;
			// OPERATORS
	/**
	 * Symbolic variable to denote the token: assignment operator '='
	 */
	public static final int _assgn = -21;
	/**
	 * Symbolic variable to denote the token: addition operator '+'
	 */
	public static final int _add = -22;
	/**
	 * Symbolic variable to denote the token: subtraction operator '-'
	 */
	public static final int _sub = -23;
	/**
	 * Symbolic variable to denote the token: multiplication operator '*'
	 */
	public static final int _mult = -24;
	/**
	 * Symbolic variable to denote the token: division operator '/'
	 */
	public static final int _div = -25;
			// SEPERATORS
	/**
	 * Symbolic variable to denote the token: left parentheses '('
	 */
	public static final int _lpar = -31;
	/**
	 * Symbolic variable to denote the token: right parentheses ')'
	 */
	public static final int _rpar = -32;
	/**
	 * Symbolic variable to denote the token: left curly braces '{'
	 */
	public static final int _lcurl = -33;
	/**
	 * Symbolic variable to denote the token: right curly braces '}'
	 */
	public static final int _rcurl = -34;
	/**
	 * Symbolic variable to denote the token: comma ','
	 */
	public static final int _comma = -35;
			// IDENTIFIERS
	/**
	 * Symbolic variable to denote the token: variable
	 */
	public static final int _variable = -41;
	/**
	 * Symbolic variable to denote the token: temporary variable
	 */
	public static final int _tempvar = -42;
			// LITERALS
	/**
	 * Symbolic variable to denote the token: integer
	 */
	public static final int _integer = -51;
	
	/**
	 * the type of the token
	 */
	public int type;
	/**
	 * the value of the token ({@code null} if its not a variable, integer, tempvar)
	 */
	public String value;
	
	/**
	 * Constructs a token with the given type and value.
	 * @param type the type of the token
	 * @param value the value of the token
	 */
	public Token(int type, String value) {
		this.type = type;
		this.value = value;
	}
	/**
	 * Constructs a token with the given type, its value is initialized as null.
	 * @param type the type of the token
	 */
	public Token(int type) {
		this.type = type;
		this.value = null;
	}
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Token))
			return false;
		Token other = (Token) obj;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	/**
	 * Checks if this token is an operation ({@code +, -, *, /, =}).
	 * @return true if this token is an operation
	 */
	public boolean isOperator () {
		if (this.type == Token._add || this.type == Token._sub || this.type == Token._mult || this.type == Token._div || this.type == Token._assgn)
			return true;
		return false;
	}
	/**
	 * Checks if this token is an operand (integer, variable, choose token).
	 * @return true if this token is an operand
	 */
	public boolean isOperand () {
		if (this.type == Token._integer || this.type == Token._variable || this.type == Token._choose)
			return true;
		return false;
	}
	/**
	 * A lexer for a string of input.
	 * <p>Gets a string of input and returns a list of tokens.</p>
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li>If the token's type is unknown.</li>
	 * </ol>
	 * @param input the input string
	 * @return An ArrayList of Tokens
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	public static ArrayList<Token> lex(String input) throws SyntaxErrorException {
		ArrayList<Token> result = new ArrayList<Token>();
		int i = 0;
		while(i < input.length()) {
			switch(input.charAt(i)) {
			case '(': result.add(new Token(Token._lpar)); i++;
				break;
			case ')': result.add(new Token(Token._rpar)); i++;
				break;
			case '{': result.add(new Token(Token._lcurl)); i++;
				break;
			case '}': result.add(new Token(Token._rcurl)); i++;
				break;
			case '+': result.add(new Token(Token._add)); i++;
				break;
			case '-': result.add(new Token(Token._sub)); i++;
				break;
			case '*': result.add(new Token(Token._mult)); i++;
				break;
			case '/': result.add(new Token(Token._div)); i++;
				break;
			case '=': result.add(new Token(Token._assgn)); i++;
				break;
			case ',': result.add(new Token(Token._comma)); i++;
				break;
			default:
				if(Character.isWhitespace(input.charAt(i))) i++;
				else {
					if (Character.isLetter(input.charAt(i)) || input.charAt(i) == '_') {
						String variableName = "" + input.charAt(i);
						i++;
						while(i != input.length() && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
							variableName += input.charAt(i);
							i++;
						}
						switch (variableName) {
						case "if": result.add(new Token(Token._if)); break;
						case "while": result.add(new Token(Token._while)); break;
						case "choose": result.add(new Token(Token._choose)); break;
						case "print": result.add(new Token(Token._print)); break;
						default:
							result.add(new Token(Token._variable, "v_" + variableName));
						}

					} else if (Character.isDigit(input.charAt(i))) {
						String NumVal = "" + input.charAt(i);
						i++;
						while (i != input.length() && Character.isDigit(input.charAt(i))) {
							NumVal += input.charAt(i);
							i++;
						}
						result.add(new Token(Token._integer, NumVal));
					} else if (input.charAt(i) == '#')		// Anything after a '#' is considered to be a comment. 
						return result;
					else
						throw new SyntaxErrorException();	// Unknown token
				}
				break;
			}
		}
		return result;
	}
	
}