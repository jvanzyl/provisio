grammar Provisio;

options {
    output = AST;
}

tokens {
  RUNTIME;
  VERSIONMAP;
  ARTIFACTSET;
  ARTIFACT;
  ACTION;
  ASSIGNMENT;
  LIST;
  MAP;
}

@header {
package io.provis.parser.antlr;
}

@lexer::members {
public static Set<String> keywords = new HashSet<String>() {{
    add("true");
    add("false");
}};
}

@lexer::header {
package io.provis.parser.antlr;

import java.util.Set;
import java.util.HashSet;
}

runtime
  : 'runtime' IDENTIFIER '{' versionMap* artifactSet* action* '}' EOF -> ^(RUNTIME IDENTIFIER versionMap* artifactSet* action*)
  ;

versionMap
  : 'versionMap' '=>' LITERAL -> ^(VERSIONMAP LITERAL)
  ;

artifactSet
  : IDENTIFIER '{' artifact* action* '}' -> ^(ARTIFACTSET IDENTIFIER artifact* action*)
  ;

artifact
  : COORDINATE ( '{' action* '}' )* -> ^(ARTIFACT COORDINATE action*)
  ;
    
action
  : IDENTIFIER '(' assignment (COMMA assignment)* ')' -> ^(ACTION IDENTIFIER assignment+ )
  ;

assignment
  : IDENTIFIER '=>' expression -> ^(ASSIGNMENT IDENTIFIER expression)
  ;
    
expression
  : BOOLEAN
  | LITERAL
  | DIGIT
  | '[' expression (list_separator expression)* ']'-> ^(LIST expression (expression)* )
  | '{' LITERAL '=>' expression (list_separator LITERAL '=>' expression)* '}' -> ^(MAP LITERAL expression (LITERAL expression)* )
  ;

//
// expressions should encompass all types
//

BOOLEAN
  : 'true' 
  | 'false' ;

//
// <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
//
COORDINATE  
  : ( IDENTIFIER ':' IDENTIFIER (':' VERSION)? ) // G:A:V 
  | ( IDENTIFIER ':' IDENTIFIER ':' IDENTIFIER (':' VERSION)? ) // G:A:E:V
  | ( IDENTIFIER ':' IDENTIFIER ':' IDENTIFIER ':' IDENTIFIER (':' VERSION)? ) // G:A:E:C:V
  ;

VERSION
  : DIGIT (LETTER | DIGIT | '.' | '_' | '-' )*
  ;

assignment_separator
  : COMMA 
  ;

list_separator
  : COMMA 
  ;

LITERAL
  : (('"' ~'"'* '"') | ('\'' ~'\''* '\'')) { setText(getText().substring(1, getText().length() - 1)); }
  ;

IDENTIFIER
  : (LETTER | '_' ) (LETTER | DIGIT | '.' | '_' | '-' | '/' )* {!keywords.contains($text)}?
  ;

COMMA
  : ','
  ;

fragment LETTER
  : 'A'..'Z' | 'a'..'z'
  ;

fragment DIGIT
  : '0'..'9'
  ;

WS
  : (' ' | '\t' | '\r' '\n' | '\n')+ { $channel = HIDDEN; }
  ;

COMMENT
  : '/*' (options {greedy=false;} : .)* '*/' { $channel = HIDDEN; }
  | ('//' | '#') (~'\n')* { $channel = HIDDEN; }
  ;
