package org.mvel3.parser.antlr4;

public class ParserTestUtil {

    static Mvel3Parser.EqualityExpressionContext getEqualityExpressionContext(Mvel3Parser.MvelStartContext tree) {
        Mvel3Parser.MvelExpressionContext exprCtx = tree.mvelExpression();
        Mvel3Parser.ConditionalExpressionContext condCtx = exprCtx.conditionalExpression();
        Mvel3Parser.LogicalOrExpressionContext orCtx = condCtx.logicalOrExpression();
        Mvel3Parser.LogicalAndExpressionContext andCtx = orCtx.logicalAndExpression(0);
        return andCtx.equalityExpression(0);
    }

    static Mvel3Parser.LogicalOrExpressionContext getLogicalOrExpressionContext(Mvel3Parser.MvelStartContext tree) {
        // For parenthesized expressions, we need to navigate through the primary expression
        Mvel3Parser.PrimaryExpressionContext primaryCtx = getPrimaryExpressionContext(tree);
        // The parenthesized expression contains another expression
        Mvel3Parser.ExpressionContext innerExprCtx = primaryCtx.expression();
        return innerExprCtx.conditionalExpression().logicalOrExpression();
    }

    static Mvel3Parser.PrimaryExpressionContext getPrimaryExpressionContext(Mvel3Parser.MvelStartContext tree) {
        Mvel3Parser.MvelExpressionContext exprCtx = tree.mvelExpression();
        Mvel3Parser.ConditionalExpressionContext condCtx = exprCtx.conditionalExpression();
        Mvel3Parser.LogicalOrExpressionContext orCtx = condCtx.logicalOrExpression();
        Mvel3Parser.LogicalAndExpressionContext andCtx = orCtx.logicalAndExpression(0);
        Mvel3Parser.EqualityExpressionContext eqCtx = andCtx.equalityExpression(0);
        Mvel3Parser.RelationalExpressionContext relCtx = eqCtx.relationalExpression(0);
        Mvel3Parser.AdditiveExpressionContext addCtx = relCtx.additiveExpression(0);
        Mvel3Parser.MultiplicativeExpressionContext mulCtx = addCtx.multiplicativeExpression(0);
        Mvel3Parser.UnaryExpressionContext unaryCtx = mulCtx.unaryExpression(0);
        Mvel3Parser.PostfixExpressionContext postfixCtx = unaryCtx.postfixExpression();
        Mvel3Parser.PrimaryExpressionContext primaryCtx = postfixCtx.primaryExpression();
        return primaryCtx;
    }

    static Mvel3Parser.LogicalAndExpressionContext getLogicalAndExpressionContext(Mvel3Parser.MvelStartContext tree) {
        // For parenthesized expressions with AND, we need similar navigation
        Mvel3Parser.PrimaryExpressionContext primaryCtx = getPrimaryExpressionContext(tree);
        // The parenthesized expression contains another expression
        Mvel3Parser.ExpressionContext innerExprCtx = primaryCtx.expression();
        return innerExprCtx.conditionalExpression().logicalOrExpression().logicalAndExpression(0);
    }
}
