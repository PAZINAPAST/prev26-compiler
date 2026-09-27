JAVA		= java
JAVAC		= javac --release 25 -encoding UTF8
ANTLR           = org.antlr.v4.Tool
ANTLRDIR	= lib/antlr-4.13.2-complete.jar
RM		= 'rm' -fr
FIND		= 'find'

.PHONY	: bin doc all clean

bin	:
	if [ -d src ] ; then $(MAKE) -C src ; fi
	$(JAVAC) --module-path $(ANTLRDIR) --source-path src -d bin src/prev26lang/Compiler.java
	@echo ":-) OK"

doc	:
	if [ -d doc ] ; then $(MAKE) -C doc ; fi

all	: bin doc

clean	:
	if [ -d src ] ; then $(MAKE) -C src clean ; fi
	if [ -d doc ] ; then $(MAKE) -C doc clean ; fi
	if [ -d prg ] ; then $(MAKE) -C prg clean ; fi
	$(FIND) . -type f -iname "*~" -exec $(RM) {} \;
	$(FIND) . -type f -iname "*.class" -exec $(RM) {} \;
	$(RM) bin
