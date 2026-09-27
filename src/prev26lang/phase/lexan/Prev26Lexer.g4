lexer grammar Prev26Lexer;

@header {
	package prev26lang.phase.lexan;

	import prev26lang.common.report.*;
}

@members {
    @Override
	public LexAn.LocLogToken nextToken() {
		return (LexAn.LocLogToken) super.nextToken();
	}

	void err(String s) {
		int l = getLine();
		int colStart = (getCharPositionInLine() - getText().length()) + 1;
		int colEnd = colStart + getText().length() - 1;

		Location loc = new Location(l, colStart, l, colEnd);

		throw new Report.Error(loc, s);
	}
}


//-------------------------------
// Rezervirane besede
//-------------------------------
AND: 'and'; 
AS: 'as' ;
BOOL: 'bool';
DO: 'do' ;
CHAR: 'char';
ELSE: 'else';
END:  'end';
FALSE: 'false';
FUN: 'fun';
IF: 'if';
IN: 'in';
INT: 'int';
LET: 'let';
NIL: 'nil';
NONE: 'none';
NOT: 'not';
OR: 'or';
SIZEOF: 'sizeof';
THEN: 'then';
TRUE: 'true';
TYP: 'typ';
VAR: 'var'; 
VOID: 'void';
WHILE: 'while'; 

//---------------------------------------------------------------------
// Komentarji, njih skipamo dokler ne pridemo do ASCI znaka LF(\u000A)
//---------------------------------------------------------------------
COMMENT: '//' ~[\u000A]* -> skip;

//---------------------------------------------------------------------
// Simboli
//---------------------------------------------------------------------
DOT: '.';
COMMA: ',';
COLON: ':';
ASS: '=';
PLUS: '+';
MINUS: '-';
MUL: '*';
DIV: '/';
MOD: '%';
EQ: '==';
NEQ: '!=';
LEQ: '<=';
GEQ: '>=';
LT: '<';
GT: '>';
LP: '(';
RP: ')';
LBRCK: '[';
RBRCK: ']';
LBRCE: '{';
RBRCE: '}';
CARET: '^';

//------------------------------------------------------------------------
// Nevalidne celoštevilske konstante, to so konstante z vodilnimi ničlami
//------------------------------------------------------------------------
INVNUM: IDIG { err(getText());};
IDIG: '0'+DIG; 


//------------------------------------------------------------------------------------------------------
// Celoštevilske konstante, zaporedje desetiških števk (od 0 do 9), pred katerimi je lahko znak + ali -
//------------------------------------------------------------------------------------------------------
NUM: DIG;
DIG: '0' 
	| [1-9][0-9]* ;

//--------------------------------------------------------------------------------------------------------------
// Imena, neprazno zaporedje črk od A-Z,a-z,stevk(0-9) in podčrtajev(_), ne sme se začeti z števko in ni reswrd	
//--------------------------------------------------------------------------------------------------------------	
NAME: ([A-Za-z]+[0-9]* | '_'[0-9]* )+;

//---------------------------------------------------------------------------------------------------------------------------------
// Znakovna konstanta (Lahko je ASCII znak npr. 'a', če sta ' ali \ sta zapisana '\\' in '\'', lahko tudi šestajtiški zapis '\xA9')
//----------------------------------------------------------------------------------------------------------------------------------
CHARCONST: '\'' ([\u0020-\u0026] | [\u0028-\u005B] | [\u005D-\u007E]) '\'' 
	| '\'' ASCII '\'' 
	| '\'' '\u005C\u0027' '\'' 
	| '\'' '\u005C\u005C' '\'' ;
ASCII: '\\x' [0-9A-Fa-f][0-9A-Fa-f];

//-------------------------------------------------------------------------------------------------------------------
// Belo besedilo jih skipam to so  HT, LF in CR
//-------------------------------------------------------------------------------------------------------------------
WHITESPACE: ([\t\r\n]+ | ' ') -> skip;

//-----------------------------------------------------------------------------------------------------------------------------------
// Konstanten niz, je lahko tudi prazno zaporedje znakovnih konsatant enala pravila le da tukaj moramo zapisati " z "\""
//-----------------------------------------------------------------------------------------------------------------------------------
STRING: '"' (([\u0020-\u0021] | [\u0023-\u005B] | [\u005D-\u007E]) | ASCII | '\u005C\u005C' | '\u005C\u0022')* '"' ;

//------------------------------------------------------
// vse ostalo je napaka
//------------------------------------------------------
ERROR: . { err(getText());};
