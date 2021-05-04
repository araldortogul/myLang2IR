all: Main.class Token.class Choose.class SyntaxErrorException.class
	jar cfm mylang2ir Manifest.txt Main.class Token.class Choose.class SyntaxErrorException.class
Main.class: Main.java
	javac -cp . Main.java

Token.class: Token.java
	javac -cp . Token.java

Choose.class: Choose.java
	javac -cp . Choose.java

SyntaxErrorException.class: SyntaxErrorException.java
	javac -cp . SyntaxErrorException.java
