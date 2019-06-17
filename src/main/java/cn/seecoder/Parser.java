package cn.seecoder;

import java.util.ArrayList;

public class Parser {
    Lexer lexer;

    public Parser(Lexer l){
        lexer = l;
    }

    public AST parse(){

        AST ast = term(new ArrayList<>());
        lexer.match(TokenType.EOF);
        return ast;
    }

    // term ::= LAMBDA LCID DOT term
    //        | application
    private AST term(ArrayList<String> ctx){
        if (lexer.next(TokenType.LAMBDA)) {
            lexer.match(TokenType.LCID);
            String name = lexer.tokenvalue;
            lexer.match(TokenType.DOT);
            ctx.add(0,lexer.tokenvalue);
            String value =String.valueOf(ctx.indexOf(name));
            AST term = term(ctx);
            ctx.remove(ctx.indexOf(name));
            return new Abstraction(new Identifier(name,value), term);
        } else {
            return this.application(ctx);
        }
    }

    //左递归 Application ::= Application Atom| Atom 变右递归
    // application ::= atom application'
    // application' ::= atom application'
    //                | ε
    private AST application(ArrayList<String> ctx){
        AST lhs=atom(ctx);
        while (true) {
            AST rhs = atom(ctx);
            if (rhs==null) {
                return lhs;
            } else {
                lhs = new Application(lhs, rhs);
            }
        }
    }

    // atom ::= LPAREN term RPAREN
    //        | LCID
    //        | ε
    private AST atom(ArrayList<String> ctx){
        if (this.lexer.next(TokenType.LPAREN)) {
            AST term = term(ctx);
            this.lexer.match(TokenType.RPAREN);
            return term;
        } else if (this.lexer.next(TokenType.LCID)) {
            return new Identifier(lexer.tokenvalue,String.valueOf(ctx.indexOf(lexer.tokenvalue)));
        } else {
            return null;
        }
    }

}

