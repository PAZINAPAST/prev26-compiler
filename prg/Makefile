JAVA            = java
ANTLRDIR        = ../lib/antlr-4.13.2-complete.jar
XSLTPROC	= xsltproc
RM              = 'rm' -fr

PHASE		= all
TARGETPHASE	?= $(PHASE)
LOGGEDPHASE	?= $(PHASE)

.PHONY	: %
%	: %.p26
	$(RM) -fr $@-*.xml $@-*.html
	if $(JAVA) -cp ../bin:../src:$(ANTLRDIR) prev26lang.Compiler --xsl=../lib/xsl/ --logged-phase=$(LOGGEDPHASE) --target-phase=$(TARGETPHASE) $(OPTIONS) $< ; then for XML in `'find' . -iname $@"-*.xml"` ; do $(XSLTPROC) $$XML -o `basename $$XML xml`html; done ; fi

.PHONY  : clean
clean   :
	$(RM) *~
	$(RM) *xml *html
