grammar Arduinoml;


/******************
 ** Parser rules **
 ******************/

root            :   declaration bricks states EOF;

declaration     :   'application' name=IDENTIFIER;

bricks          :   (sensor|actuator)+;
    sensor      :   'sensor'   location ;
    actuator    :   'actuator' location ;
    location    :   id=IDENTIFIER ':' port=INT;

states          :   state+;
    state       :   initial? name=IDENTIFIER '{'  action* transitionList '}';
    action      :   receiver=IDENTIFIER '<=' value=SIGNAL;

    transitionList
                :   ( (connector=CONNECTOR '{' transition+ '}' ) | transition ) '=>' (next=IDENTIFIER | 'error' errorCode=INT);
    transition  :   trigger=IDENTIFIER 'is' value=SIGNAL ;
    initial     :   '->';

/*****************
 ** Lexer rules **
 *****************/

SIGNAL          :   'HIGH' | 'LOW';
CONNECTOR       :   'AND' | 'OR';
IDENTIFIER      :   LOWERCASE (LOWERCASE|UPPERCASE|NUMBER)+;
INT             :   NUMBER+;

/*************
 ** Helpers **
 *************/

fragment LOWERCASE  : [a-z];                                 // abstract rule, does not really exists
fragment UPPERCASE  : [A-Z];
fragment NUMBER     : [0-9];
NEWLINE             : ('\r'? '\n' | '\r')+      -> skip;
WS                  : ((' ' | '\t')+)           -> skip;     // who cares about whitespaces?
COMMENT             : '#' ~( '\r' | '\n' )*     -> skip;     // Single line comments, starting with a #
