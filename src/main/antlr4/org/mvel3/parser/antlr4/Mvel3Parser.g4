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
