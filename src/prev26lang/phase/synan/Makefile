JAVA		= java
ANTLR		= org.antlr.v4.Tool
ANTLRDIR	= ../../../../lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr

parser	: Prev26Parser.java Prev26Parser.interp Prev26Parser.tokens 
Prev26Parser.java Prev26Parser.interp Prev26Parser.tokens : Prev26Parser.g4 ../lexan/Prev26Lexer.java ../lexan/Prev26Lexer.interp ../lexan/Prev26Lexer.tokens
	$(JAVA) -cp $(ANTLRDIR):$(CLASSPATH) $(ANTLR) -no-listener -no-visitor -lib ../lexan $<
	mv Prev26Parser.java Prev26Parser.java-orig
	sed 's/@SuppressWarnings({"all"/@SuppressWarnings({"doclint:missing", "all"/' < Prev26Parser.java-orig > Prev26Parser.java
	$(RM) Prev26Parser.java-orig ../../../../bin/compiler/phase/synan/Prev26Parser.class

../lexan/Prev26Lexer.java ../lexan/Prev26Lexer.interp ../lexan/Prev26Lexer.tokens :
	$(MAKE) -C ../lexan

.PHONY	: clean
clean	:
	$(RM) *~
	$(RM) Prev26*.java *interp *tokens
