JAVA		= java
ANTLR		= org.antlr.v4.Tool
ANTLRDIR	= ../../../../lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr

lexer	: Prev26Lexer.java Prev26Lexer.interp Prev26Lexer.tokens
Prev26Lexer.java Prev26Lexer.interp Prev26Lexer.tokens : Prev26Lexer.g4
	$(JAVA) -cp $(ANTLRDIR):$(CLASSPATH) $(ANTLR) $<
	mv Prev26Lexer.java Prev26Lexer.java-orig
	sed 's/@SuppressWarnings({"all"/@SuppressWarnings({"doclint:missing", "all"/' < Prev26Lexer.java-orig > Prev26Lexer.java
	$(RM) Prev26Lexer.java-orig ../../../../bin/compiler/phase/lexan/Prev26Lexer.class

.PHONY	: clean
clean	:
	$(RM) *~
	$(RM) Prev26*.java* *interp *tokens
