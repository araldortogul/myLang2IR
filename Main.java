import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
/**
 * Translator of .my files ({@code myLang} files) into .ll files (LLVM-IR code).
 * @author Aral Dortogul
 */
public class Main {
	/**
	 * the LLVM-IR body statements
	 */
	private static LinkedList<String> IRstatements = new LinkedList<String>();
	/**
	 * the LLVM-IR variable declaration statements
	 */
	private static LinkedList<String> IRvariabledeclaration = new LinkedList<String>();
	/**
	 * the LLVM-IR variable initialization statements (var = 0)
	 */
	private static LinkedList<String> IRvariableinit = new LinkedList<String>();
	/**
	 * the Scanner for the input file
	 */
	private static Scanner input_file;
	/**
	 * .my file's line counter
	 */
	private static int lineCount = 0;
	/**
	 * the set of declared variables
	 */
	private static Set<String> declaredVariables = new HashSet<String>();
	/**
	 * boolean which is {@code true} when curly braces are open due to if/while statements, {@code false} otherwise. 
	 */
	private static boolean curlyBracesOpen = false;
	/**
	 * temporary variable counter
	 */
	private static int tempVarCount = 0;
	/**
	 * while statement counter
	 */
	private static int whileCount = 0;
	/**
	 * if statement counter
	 */
	private static int ifCount = 0;
	/**
	 * choose function counter
	 */
	private static int chooseCount = 0;
	/**
	 * Translates .my file into .ll intermediate code.
	 * @param args command line arguments
	 * @throws FileNotFoundException when an attempt to open the input file denoted by a specified pathname has failed
	 */
	public static void main(String[] args) throws FileNotFoundException {
		input_file = new Scanner(new File(args[0]));
		String output_file_name = args[0].substring(0, args[0].lastIndexOf('.')).concat(".ll");
		try {
			while (input_file.hasNextLine()) {
				String current_line = input_file.nextLine(); // Get the next input line
				ArrayList<Token> tokens = Token.lex(current_line); // Tokenize the line, may throw SyntaxErrorException
				ListIterator<Token> itr = tokens.listIterator();
				
				while(itr.hasNext()) { // Tokenize choose function if there is any.
					Token current = itr.next();
					if(current.type == Token._choose) {
						itr.remove();
						Choose choose = tokenizeChoose(itr);
						itr.add(choose);
					}
				}				
				ParseLine(tokens); // Parse general expression: A line can start with a variable, print, while, if.
				lineCount++;
			}
			printIR(output_file_name); // Write the LLVM-IR code in the specified output file.
		} catch (SyntaxErrorException e) {
			SyntaxError(output_file_name);
		}
	}
	/**
	 * Creates a single "Choose" token from a list of tokens.
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li><code>choose</code> is not followed by any token.</li>
	 * 	<li><code>choose</code> is not followed by <code>'('</code></li>
	 * 	<li>There are more than three commas inside <code>choose</code> function.</li>
	 * 	<li>Any of <code>choose</code>'s arguments is empty.</li>
	 * </ol>
	 * @param itr the Iterator of the token list
	 * @return the "Choose" token, null if there is a syntax error
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static Choose tokenizeChoose(ListIterator<Token> itr) throws SyntaxErrorException {
		Choose result = new Choose(Token._choose);
		if (itr.hasNext()) {
			Token first = itr.next();
			if (first.type != Token._lpar) throw new SyntaxErrorException(); // "choose" is not followed by '('
			itr.remove();
		} else throw new SyntaxErrorException(); // "choose" is followed by nothing.
		int commaCount = 0, openLeftParentheses = 1;
		
		while(itr.hasNext()) {
			Token next = itr.next();
			itr.remove();
			if (next.type == Token._comma) {
				commaCount++;
				if (commaCount > 3) throw new SyntaxErrorException(); // There are more than 3 commas inside choose.
				continue;
			}
			if (next.type == Token._rpar) {
				if (--openLeftParentheses == 0) break;
			} else if (next.type == Token._lpar)
				openLeftParentheses++;
			if (next.type == Token._choose) {
				Choose choose = tokenizeChoose(itr);
				result.tokens_of_arg.get(commaCount).add(choose);
			} else
				result.tokens_of_arg.get(commaCount).add(next);
		}
		if (openLeftParentheses != 0 || result.tokens_of_arg.get(0).isEmpty() || result.tokens_of_arg.get(1).isEmpty() ||  result.tokens_of_arg.get(2).isEmpty() ||  result.tokens_of_arg.get(3).isEmpty())
			throw new SyntaxErrorException();
		return result;
	}
	/**
	 * Writes the LLVM-IR statements into a file with the given name (Creates the file if it does not exist.).
	 * @param output_file_name the output file's name (with .ll extension)
	 * @throws FileNotFoundException when the file is not found
	 */
	private static void printIR(String output_file_name) throws FileNotFoundException {
		PrintStream output = new PrintStream(new File(output_file_name));
		output.println("; ModuleID = 'mylang2ir'");
		output.println("declare i32 @printf(i8*, ...)");
		output.println("@print.str = constant [4 x i8] c\"%d\\0A\\00\"");
		output.println();
		output.println("define i32 @main() {");

		if (!IRvariabledeclaration.isEmpty()) {
			for (String line : IRvariabledeclaration)
				output.println(line);
			output.println();
		}

		if (!IRvariableinit.isEmpty()) {
			for (String line : IRvariableinit)
				output.println(line);
			output.println();
		}

		for (String line : IRstatements)
			output.println(line);

		output.println("\tret i32 0");
		output.println("}");
		output.close();
	}
	/**
	 * Generates syntax error output.
	 * 
	 * <p>Syntax error output is the LLVM-IR code which displays "Line X: syntax error" when executed. X is the number of the line in which the syntax error is detected. Line numbers start with 0.</p>
	 * @param output_file_name the name of the output file (with .ll extension)
	 * @throws FileNotFoundException when an attempt to open the file denoted by a specified pathname has failed
	 */
	private static void SyntaxError(String output_file_name) throws FileNotFoundException {
		PrintStream output = new PrintStream(new File(output_file_name));

		output.println("; ModuleID = 'mylang2ir'");
		output.println("declare i32 @printf(i8*, ...)");
		output.println("@print.str = constant [23 x i8] c\"Line %d: syntax error\\0A\\00\"");
		output.println();
		output.println("define i32 @main() {");
		output.println("\tcall i32 (i8*, ...)* @printf(i8* getelementptr ([23 x i8]* @print.str, i32 0, i32 0), i32 " + lineCount + " )");
		output.println("\tret i32 0");
		output.println("}");
		output.close();
	}
	/**
	 * Parses <code>myLang</code> lines token by token.
	 * <p>A valid <code>myLang</code> line can be an assignment statement, print statement, while statement, if statement, or curly braces closing statement (which is only "}").</p>
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li>The function starts as if it is an assignment/if/while/print statement but detects a syntax error later.</li>
	 * 	<li>If a statement other than the four possible statements is encountered.</li>
	 * 	<li>If the statement starts with '}', but there are no open curly braces, or '}' is not the only token in the statement.</li>
	 * </ol>
	 * @param tokens the list of tokens of the line
	 * @return
	 * <ul>
	 * 	<li>'a': assignment statement, </li>
	 * 	<li>'p': print statement, </li>
	 * 	<li>'w': while statement, </li><li>'i': if statement, </li>
	 * 	<li>'e': empty statement, </li>
	 * 	<li>'}': if/while closing line </li>
	 * </ul>
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static char ParseLine(ArrayList<Token> tokens) throws SyntaxErrorException {
		if(!tokens.isEmpty()) {
			Token initial = tokens.get(0);
			if (initial.type == Token._variable)		// Assignment line
				return parseAssignment(tokens);

			else if (initial.type == Token._if)			// If line
				return parseIf(tokens);
			
			else if (initial.type == Token._while)		// While line
				return parseWhile(tokens);

			else if (initial.type == Token._print)		// Print line
				return parsePrint(tokens);
				
			else if (initial.type == Token._rcurl) {	// While/If closing line
				if (!curlyBracesOpen || tokens.size() > 1) throw new SyntaxErrorException(); // If there is no open curly braces or the closing curly braces line continues with other tokens.
				return '}';

			} else throw new SyntaxErrorException(); // Statements of other forms
		} else		// Empty line
			return 'e';
	}
	/**
	 * Parses a <code>myLang</code> assignment statement of the form: <code>&lt;variable&gt; = &lt;expression&gt;</code>
	 * <p>This method is called when the first token of a <code>myLang</code> statement is an identifier (variable name).</p>
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li>The second token is not an assignment operator.</li>
	 * 	<li>The token count of the statement is less than three.</li>
	 * 	<li>There is a syntax error in the expression itself. (See <code>parseIR_Expression(Iterator&lt;Token&gt;)</code> for more.)</li>
	 * </ol>
	 * @param tokens the tokens of the statement
	 * @return 'a' for "assignment"
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static char parseAssignment(ArrayList<Token> tokens) throws SyntaxErrorException {
		if (tokens.size() < 3 || tokens.get(1).type != Token._assgn) throw new SyntaxErrorException(); // If a variable name is not followed by '='
		ArrayList<Token> expression = infixToPostFix(tokens, 0, 0, 1); // May throw syntax error
		createIR_Expression(expression.iterator());
		return 'a';
	}
	/**
	 * Parses an if statement of the form: <code>if (&lt;expr&gt;) {</code>
	 * <p>This method is called when the first token of a <code>myLang</code> statement is the {@code "if"} keyword.</p>
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li>The token count of the statement is less than 5, which is the minimum amount of tokens required for an {@code if} statement.</li>
	 * 	<li>The second token is not {@code '('}.</li>
	 * 	<li>The last token is not <code>'{'</code>.</li>
	 * 	<li>The token before the last token is not {@code ')'}.</li>
	 * 	<li>{@code if} condition expression has a syntax error.</li>
	 * 	<li>A body statement in the {@code if} block has a syntax error.</li>
	 * 	<li>The input file ends without closing the {@code if} block's braces.</li>
	 * 	<li>This {@code if} statement is already in  an {@code if/while} statement.<br>
	 * 		(No nested {@code if/while} statements are allowed.)</li>
	 * </ol>
	 * @param tokens the tokens of the statement
	 * @return {@code 'i'} for {@code "if"}
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static char parseIf(ArrayList<Token> tokens) throws SyntaxErrorException {
		if ((tokens.size() < 5) || (tokens.get(1).type != Token._lpar) || (tokens.get(tokens.size() - 2).type != Token._rpar) || (tokens.get(tokens.size() - 1).type != Token._lcurl) || curlyBracesOpen)
			throw new SyntaxErrorException(); // If an if statement is not in the form of: "if ( <expr> ) {" OR if it is going to be a nested if
		IRstatements.add("\tbr label %ifcond" + (++ifCount) + "\n");
		IRstatements.add("ifcond" + ifCount + ":");
		ArrayList<Token> if_condition = infixToPostFix(tokens, 2, 2, 0); // May throw syntax error
		
		createIR_condition_expression(if_condition.iterator(), 'i'); // Create and store the LLVM-IR statements for the if-condition.
		
		IRstatements.add("ifbody" + ifCount + ":"); // LLVM-IR: label of the if block's body
		curlyBracesOpen = true;
		while(input_file.hasNextLine()) { // Read lines until "}" line is encountered.
			String currentBodyLine  = input_file.nextLine();
			lineCount++;
			ArrayList<Token> bodylinetokens = Token.lex(currentBodyLine);	// Tokenize the current line, may throw syntax error
			
			ListIterator<Token> body_itr = bodylinetokens.listIterator();
			while(body_itr.hasNext()) {										// Create single "choose" tokens with tokenizeChoose().
				Token current = body_itr.next();
				if(current.type == Token._choose) {
					body_itr.remove();
					Choose choose = tokenizeChoose(body_itr); // May throw exception
					body_itr.add(choose);
				}
			}
			if (ParseLine(bodylinetokens) == '}') { // Parse the next line in the if block, break if the line is "}", may throw syntax error
				curlyBracesOpen = false;
				IRstatements.add("\tbr label %ifend" + (ifCount) + "\n");
				IRstatements.add("ifend" + (ifCount) + ":");
				break;
			}
		}
		if (curlyBracesOpen) throw new SyntaxErrorException(); // The .my file ended with an open "if"
		return 'i';
	}
	/**
	 * Parses a while statement of form: <code>while (&lt;expr&gt;) {</code>
	 * <p>This method is called when the first token of a <code>myLang</code> statement is the {@code "while"} keyword.</p>
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li>The token count of the statement is less than 5, which is the minimum amount of tokens required for a {@code while} statement.</li>
	 * 	<li>The second token is not {@code '('}.</li><li>The last token is not <code>'{'</code>.</li>
	 * 	<li>The token before the last token is not {@code ')'}.</li>
	 * 	<li>{@code while} condition expression has a syntax error.</li>
	 * 	<li>A body statement in the {@code while} block has a syntax error.</li>
	 * 	<li>The input file ends without closing the {@code while} block's braces.</li>
	 * 	<li>This {@code while} statement is already in  an {@code if/while} statement.<br>
	 * 		(No nested {@code if/while} statements are allowed.)</li>
	 * </ol>
	 * @param tokens the tokens of the statement
	 * @return {@code 'w'} for {@code "while"}
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static char parseWhile(ArrayList<Token> tokens) throws SyntaxErrorException {
		if ((tokens.size() < 5) || (tokens.get(1).type != Token._lpar) || (tokens.get(tokens.size() - 2).type != Token._rpar) || (tokens.get(tokens.size() - 1).type != Token._lcurl) || curlyBracesOpen)
			throw new SyntaxErrorException(); // If the while statement is not in the following form: "while ( <expr> ) {" OR if it is going to be a nested while

		IRstatements.add("\tbr label %whcond" + (++whileCount) + "\n"); // LLVM-IR: Label of the while-loop's condition
		IRstatements.add("whcond" + whileCount + ":");
		ArrayList<Token> while_condition = infixToPostFix(tokens, 2, 2, 0); // Get the postfix notation of the expression, may throw SyntaxErrorException

		createIR_condition_expression(while_condition.iterator(), 'w');

		IRstatements.add("whbody" + whileCount + ":"); // LLVM-IR: Label of the while-loop's body
		curlyBracesOpen = true;

		while(input_file.hasNextLine()) { // Read until '}' line is encountered.
			String currentBodyLine  = input_file.nextLine();
			lineCount++;
			ArrayList<Token> bodylinetokens = Token.lex(currentBodyLine);	// Tokenize the current line, may throw SyntaxErrorException
			
			ListIterator<Token> body_itr = bodylinetokens.listIterator();
			while(body_itr.hasNext()) {										// Create single "choose" tokens with tokenizeChoose().
				Token current = body_itr.next();
				if(current.type == Token._choose) {
					body_itr.remove();
					Choose choose = tokenizeChoose(body_itr); // May throw exception
					body_itr.add(choose);
				}
			}
			char lineType = ParseLine(bodylinetokens); // Parse the current line, may throw exception.
			
			if (lineType == '}') { // Break from the loop if "}" is encountered.
				curlyBracesOpen = false;
				IRstatements.add("\tbr label %whcond" + (whileCount) + "\n");
				IRstatements.add("whend" + (whileCount) + ":");
				break;
			}
		}
		if (curlyBracesOpen) throw new SyntaxErrorException(); // The .my file ended with an open "if"
		return 'w';
	}
	/**
	 * Parses a print statement of form: <code>print (&lt;expr&gt;) {</code>
	 * <p>This method is called when the first token of a <code>myLang</code> statement is the {@code "print"} keyword.</p>
	 * <p>The following cases are regarded as syntax error:</p>
	 * <ol>
	 * 	<li>The token count of the statement is less than 4, which is the minimum amount of tokens required for a {@code print} statement.</li>
	 * 	<li>The second token is not {@code '('}.</li>
	 * 	<li>The last token is not {@code ')'}.</li>
	 * 	<li>{@code print} expression has a syntax error.</li>
	 * </ol>
	 * @param tokens the tokens of the statement
	 * @return {@code 'p'} for {@code "print"}
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static char parsePrint(ArrayList<Token> tokens) throws SyntaxErrorException {
		if ((tokens.size() < 4) || tokens.get(1).type != Token._lpar || tokens.get(tokens.size() - 1).type != Token._rpar)
			throw new SyntaxErrorException(); // If the print statement is not in the following form: print ( <expr> )
		ArrayList<Token> content = infixToPostFix(tokens, 2, 1, 0); // Get the postfix notation of the expression, may throw SyntaxErrorException
		createIR_print_statement(content.iterator()); // Create and store LLVM-IR statements for the print function.
		return 'p';
	}
	/**
	 * Creates and stores LLVM-IR statements for printing lines.
	 * @param itr an iterator for the list of tokens of the print statement
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static void createIR_print_statement(Iterator<Token> itr) throws SyntaxErrorException {
		Token result = createIR_Expression(itr);
		if (result.type == Token._variable) {
			IRstatements.add("\t%t" +(++tempVarCount) + " = load i32* %" + result.value);
			result = new Token(Token._tempvar, "t" + (tempVarCount));
		}
		if (result.type == Token._integer)
			IRstatements.add("\tcall i32 (i8*, ...)* @printf(i8* getelementptr ([4 x i8]* @print.str, i32 0, i32 0), i32 " + result.value + " )");
		else
			IRstatements.add("\tcall i32 (i8*, ...)* @printf(i8* getelementptr ([4 x i8]* @print.str, i32 0, i32 0), i32 %" + result.value + " )");
	}
	/**
	 * Creates and stores LLVM-IR statements for if &amp; while blocks' conditions.
	 * @param itr an iterator for the list of tokens of an expression
	 * @param type {@code 'w'} for {@code while} condition and {@code 'i'} for {@code if} condition
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static void createIR_condition_expression(Iterator<Token> itr, char type) throws SyntaxErrorException {
		Token result = createIR_Expression(itr); // Create the LLVM-IR statements for evaluating the expression.
		if (result.type == Token._variable) {
			IRstatements.add("\t%t" +(++tempVarCount) + " = load i32* %" + result.value);
			result = new Token(Token._tempvar, "t" + (tempVarCount));
		}
		Token cond = new Token(Token._tempvar, "t" + (++tempVarCount));
		if (result.type == Token._integer)
			IRstatements.add("\t%" + cond.value +" = icmp ne i32 " + result.value + ", 0");
		else
			IRstatements.add("\t%" + cond.value +" = icmp ne i32 %" + result.value + ", 0");
		if (type == 'w')
			IRstatements.add("\tbr i1 %" + cond.value + ", label %whbody" + whileCount + ", label %whend" + whileCount + "\n");
		else if (type == 'i')
			IRstatements.add("\tbr i1 %" + cond.value + ", label %ifbody" + ifCount + ", label %ifend" + ifCount + "\n");
	}
	/**
	 * Creates and stores LLVM-IR statements for assignment operation.
	 * @param LHS left hand side of the assignment statement (variable)
	 * @param RHS calculated right hand side of the assignment statement (temp-var)
	 */
	private static void createIR_Assgn_Expression(Token LHS, Token RHS) {
		if (RHS.type == Token._integer)
			IRstatements.add("\tstore i32 " + RHS.value + ", i32* %" + LHS.value);
		else if (RHS.type == Token._tempvar)
			IRstatements.add("\tstore i32 %" + RHS.value + ", i32* %" + LHS.value);
		else if (RHS.type == Token._variable) {
			IRstatements.add("\t%t" +(++tempVarCount) + " = load i32* %" + RHS.value);
			RHS = new Token(Token._tempvar, "t" + (tempVarCount));
			IRstatements.add("\tstore i32 %" + RHS.value + ", i32* %" + LHS.value);
		}
	}
	/**
	 * Creates and stores LLVM-IR statements for an expression.
	 * @param itr the iterator for an expression
	 * @return the result of the expression, {@code null} if the expression is an assignment statement
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static Token createIR_Expression(Iterator<Token> itr) throws SyntaxErrorException {
		Stack<Token> operands = new Stack<Token>();
		while(itr.hasNext()) {
			Token current = itr.next();
			if (current.isOperand()) {	// If the current token is a variable/temporary variable
				if (current.type == Token._variable && !declaredVariables.contains(current.value)) {
					declaredVariables.add(current.value);
					IRvariabledeclaration.add("\t%" + current.value + " = alloca i32");
					IRvariableinit.add("\tstore i32 0, i32* %" + current.value);
				}
				else if (current.type == Token._choose) // If the current token is "choose"
					current = createIR_choose((Choose) current, ++chooseCount); // Create LLVM-IR statements for evaluating choose function.
				operands.push(current);
			}
			else if (current.isOperator()) { // If the current token is an operator.
				Token RHS = operands.pop();
				Token LHS = operands.pop();

				if (current.type != Token._assgn && LHS.type == Token._variable) {
					IRstatements.add("\t%t" +(++tempVarCount) + " = load i32* %" + LHS.value);
					LHS = new Token(Token._tempvar, "t" + (tempVarCount));
				}
				if (RHS.type == Token._variable) {
					IRstatements.add("\t%t" +(++tempVarCount) + " = load i32* %" + RHS.value);
					RHS = new Token(Token._tempvar, "t" + (tempVarCount));
				}
				String operation = "";
				switch (current.type) {
					case Token._add: operation = "add"; break;
					case Token._sub: operation = "sub"; break;
					case Token._mult: operation = "mul"; break;
					case Token._div: operation = "sdiv"; break;
				}
				if (current.type == Token._assgn) {	// If the current token is an assignment operator, create LLVM-IR statements for it.
					createIR_Assgn_Expression(LHS, RHS); return null;}
				else {
					Token result = new Token(Token._tempvar, "t" + (++tempVarCount)); // Create LLVM-IR statement that computes a binary expression.
					if (LHS.type == Token._integer && RHS.type == Token._integer)
						IRstatements.add("\t%" + result.value + " = " + operation + " i32 " + LHS.value +", " + RHS.value);
					else if (LHS.type == Token._integer && RHS.type != Token._integer)
						IRstatements.add("\t%" + result.value + " = " + operation + " i32 " + LHS.value +", %" + RHS.value);
					else if (LHS.type != Token._integer && RHS.type == Token._integer)
						IRstatements.add("\t%" + result.value + " = " + operation + " i32 %" + LHS.value +", " + RHS.value);
					else
						IRstatements.add("\t%" + result.value + " = " + operation + " i32 %" + LHS.value +", %" + RHS.value);
					operands.push(result);
				}
			}
		}
		return operands.pop();
	}
	/**
	 * Creates and stores LLVM-IR statements for the given choose function.
	 * @param choose the choose token which includes all the arguments
	 * @param choose_counter counter for specifying the label names of the choose's switch-like mechanic
	 * @return the result of the choose function (a variable)
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	private static Token createIR_choose(Choose choose, int choose_counter) throws SyntaxErrorException {
		Token chooseResult = new Token(Token._variable, "choosevar" + choose_counter);	// For referring the return value 
		declaredVariables.add(chooseResult.value);
		IRvariabledeclaration.add("\t%" + chooseResult.value + " = alloca i32");
		IRvariableinit.add("\tstore i32 0, i32* %" + chooseResult.value);
		
		Token chooseCondVar = new Token(Token._variable, "chcond" + choose_counter);	// For referring the condition of the choose (first expression)
		declaredVariables.add(chooseCondVar.value);
		IRvariabledeclaration.add("\t%" + chooseCondVar.value + " = alloca i32");
		IRvariableinit.add("\tstore i32 0, i32* %" + chooseCondVar.value);
		
		for (int i = 0; i < 4; i++) {
			choose.tokens_of_arg.get(i).add(0, new Token(Token._assgn));
			choose.tokens_of_arg.get(i).add(0, (i > 0 ? chooseResult : chooseCondVar));
			choose.tokens_of_arg.set(i,infixToPostFix(choose.tokens_of_arg.get(i), 0, 0, 1));
		}
		createIR_Expression(choose.tokens_of_arg.get(0).iterator());		// Calculates the condition of the choose function, may throw SyntaxErrorException.
		Token holder = new Token(Token._tempvar, "t" + (++tempVarCount));
		IRstatements.add("\t%" + holder.value + " = load i32* %" + chooseCondVar.value); // Load condition variable in a temp
		
		Token cond = new Token(Token._tempvar, "t" + (++tempVarCount));		// The condition temporary variable
		IRstatements.add("\t%" + cond.value +" = icmp eq i32 %" + holder.value + ", 0");
		IRstatements.add("\tbr i1 %" + cond.value + ", label %cheq" + choose_counter + ", label %chne" + choose_counter + "\n");
		IRstatements.add("cheq" + choose_counter + ":");					// EQUAL CASE
		
		createIR_Expression(choose.tokens_of_arg.get(1).iterator());		// Calculates the second expression of the choose function, may throw SyntaxErrorException.
		IRstatements.add("\tbr label %chend" + choose_counter + "\n");
		IRstatements.add("chne" + choose_counter + ":");					// NOT EQUAL CASE
		
		holder = new Token(Token._tempvar, "t" + (++tempVarCount));
		IRstatements.add("\t%" + holder.value + " = load i32* %" + chooseCondVar.value);
		cond = new Token(Token._tempvar, "t" + (++tempVarCount));			// The condition temporary variable
		
		IRstatements.add("\t%" + cond.value +" = icmp sgt i32 %" + holder.value + ", 0");
		IRstatements.add("\tbr i1 %" + cond.value + ", label %chsgt" + choose_counter + ", label %chslt" + choose_counter + "\n");
		IRstatements.add("chsgt" + choose_counter + ":");					// POSITIVE CASE
		
		createIR_Expression(choose.tokens_of_arg.get(2).iterator());		// Calculates the third expression of the choose function, may throw SyntaxErrorException.
		IRstatements.add("\tbr label %chend" + choose_counter + "\n");
		IRstatements.add("chslt" + choose_counter + ":");					// NEGATIVE CASE
		
		createIR_Expression(choose.tokens_of_arg.get(3).iterator());		// Calculates the fourth expression of the choose function, may throw SyntaxErrorException.
		IRstatements.add("\tbr label %chend" + choose_counter + "\n");
		IRstatements.add("chend" + choose_counter + ":");					// END LABEL
		return chooseResult;
	}
	/**
	 * Returns the precedence of the token (called only when the token is an operation ({@code =, +, - , /, *}))
	 * @param token the operation
	 * @return
	 * <ul>
	 * 	<li> 3 - if the token is {@code '*'} (multiplication) or {@code '/'} (division)</li>
	 * 	<li> 2 - if the token is {@code '+'} (addition) or {@code '-'} (subtraction)</li>
	 * 	<li> 1 - if the token is {@code '='} (assignment) </li>
	 * </ul>
	 */
	static int Prec(Token token) {
		switch (token.type) {
		case Token._assgn: return 1;
		
		case Token._add:
		case Token._sub: return 2;

		case Token._mult:
		case Token._div: return 3;}
		return -1;
	}
	/**
	 * Transforms an infix notation to a postfix notation.
	 * @param tokens the list of tokens (in infix order)
	 * @param lower the lower boundary for the ArrayList "tokens" that this function can operate
	 * @param upper the upper boundary for the ArrayList "tokens" that this function can operate
	 * @param assgnOpCount assignment operator count that is allowed in the tokens (1 or 0)
	 * @return the ArrayList of tokens in a postfix order.
	 * @throws SyntaxErrorException when a syntax error is detected
	 */
	static ArrayList<Token> infixToPostFix(ArrayList<Token> tokens, int lower, int upper, int assgnOpCount) throws SyntaxErrorException {
		ArrayList<Token> result = new ArrayList<Token>();
		Stack<Token> stack = new Stack<Token>();
		int assgnCounter = 0;
		for (int i = lower, j = i + 1; i < tokens.size() - upper; i++, j++) {
			Token c = tokens.get(i);
			if (i == tokens.size() - upper - 1) {
				if (c.isOperator()) throw new SyntaxErrorException();	// If the infix notation ends with an operator.
			} else {
				Token ahead = tokens.get(j);
				if ((c.isOperator() && ahead.isOperator()) || (c.isOperand() && ahead.isOperand()) || c.type == Token._lpar && ahead.type == Token._rpar) throw new SyntaxErrorException();
			}
			if (c.isOperand())					// If the scanned character is an operand, add it to output.
				result.add(c);
			
			else if (c.type == Token._lpar)		// If the scanned character is an '(', push it to the stack.
				stack.push(c);
			
			else if (c.type == Token._rpar) {	// If the scanned character is an ')', pop and output from the stack until an '(' is encountered.
				if (stack.isEmpty() || !stack.contains(new Token(Token._lpar))) throw new SyntaxErrorException();
				while (!stack.isEmpty() && stack.peek().type != Token._lpar)
					result.add(stack.pop());
				stack.pop();
			}
			else if (c.isOperator()) {			// an operator is encountered
				if (c.type == Token._assgn) if(++assgnCounter > assgnOpCount) throw new SyntaxErrorException(); // '=' is not allowed more than the specified assignment count.
				while (!stack.isEmpty() && Prec(c) <= Prec(stack.peek()))
					result.add(stack.pop());
				stack.push(c);
			}
			else throw new SyntaxErrorException();	// Non operator/operand is encountered. An expression cannot contain tokens other than operations, operands & choose.
		}
		while (!stack.isEmpty()){				// pop all the operators from the stack
			if(stack.peek().type == Token._lpar) throw new SyntaxErrorException();	// If there is '(' left in the stack
			result.add(stack.pop());
		}
		return result;
	}
}