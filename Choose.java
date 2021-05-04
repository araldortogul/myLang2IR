import java.util.ArrayList;
/**
 * A child of Token class, Choose is a function in the form {@code choose(expr1,expr2,expr3,expr4)} which returns {@code expr2} if {@code expr1} is equal to 0, returns {@code expr3} if {@code expr1} is positive and returns {@code expr4} if {@code expr1} is negative.
 * @author Aral Dortogul
 */
public class Choose extends Token{
	/**
	 * An ArrayList of the list of tokens for the 4 arguments of the choose token.
	 */
	public ArrayList<ArrayList<Token>> tokens_of_arg;
	/**
	 * Constructs a new choose token with the given token type
	 * @param type the token type (always Token._choose)
	 */
	public Choose(int type) {
		super(type);
		tokens_of_arg = new ArrayList<ArrayList<Token>>();
		for (int i = 0; i < 4; i++)
			tokens_of_arg.add(new ArrayList<Token>());
	}
}
