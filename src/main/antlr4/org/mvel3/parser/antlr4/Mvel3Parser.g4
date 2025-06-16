// MVEL3 Parser - minimal MVEL expression parser

parser grammar Mvel3Parser;

import Java20Parser; // Import Java 20 parser for basic types and structures

options {
    tokenVocab = Mvel3Lexer;
}

// Start rule for MVEL expressions
mvelStart
    : mvelExpression EOF
    ;

// MVEL expression - start with simple expression support
mvelExpression
    : conditionalExpression
    ;

// Conditional expression: expr ? expr : expr
conditionalExpression
    : logicalOrExpression (QUESTION expression COLON conditionalExpression)?
    ;

// Logical expressions
logicalOrExpression
    : logicalAndExpression (OR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (AND equalityExpression)*
    ;

// Equality and relational expressions (including MVEL operators)
equalityExpression
    : relationalExpression ((EQUAL | NOTEQUAL) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LT | GT | LE | GE | INSTANCEOF | IN | CONTAINS | MATCHES | SOUNDSLIKE | STRSIM) additiveExpression)*
    ;

// Arithmetic expressions
additiveExpression
    : multiplicativeExpression ((ADD | SUB) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((MUL | DIV | MOD) unaryExpression)*
    ;

// Unary expressions
unaryExpression
    : (ADD | SUB | BANG | TILDE) unaryExpression
    | postfixExpression
    ;

// Postfix expressions (field access, method calls, array access, etc.)
postfixExpression
    : primaryExpression postfixOp*
    ;

postfixOp
    : DOT identifier                                    // field access
    | DOT identifier LPAREN argumentList? RPAREN       // method call
    | LBRACK expression RBRACK                          // array access
    | PROJECTOR expression RBRACE                       // MVEL projection
    | SELECTFIRST expression RBRACE                     // MVEL select first
    | SELECTLAST expression RBRACE                      // MVEL select last
    ;

// Primary expressions
primaryExpression
    : literal
    | identifier
    | THIS
    | LPAREN expression RPAREN                          // parenthesized expression
    | mvelMapLiteral                                    // MVEL map literal
    | mvelListLiteral                                   // MVEL list literal
    | NEW identifier LPAREN argumentList? RPAREN       // object creation
    ;

// MVEL map literal: [key: value, key2: value2]
mvelMapLiteral
    : LBRACK mvelMapEntry (COMMA mvelMapEntry)* RBRACK
    | LBRACK COLON RBRACK  // empty map
    ;

mvelMapEntry
    : expression COLON expression
    ;

// MVEL list literal: [element1, element2, element3]
mvelListLiteral
    : LBRACK expression (COMMA expression)* RBRACK
    | LBRACK RBRACK  // empty list
    ;

// Expression (for recursive rules)
expression
    : conditionalExpression
    ;

// Arguments for method calls
argumentList
    : expression (COMMA expression)*
    ;

// Literals
literal
    : IntegerLiteral
    | FloatingPointLiteral
    | BooleanLiteral
    | CharacterLiteral
    | StringLiteral
    | NullLiteral
    ;

// Identifier
identifier
    : Identifier
    ;