package org.mvel3.parser.antlr4;

public class ParserTestUtil {

    // Navigate to equality expression from root for simple expressions
    static Mvel3Parser.EqualityExpressionContext getEqualityExpressionContext(Mvel3Parser.MvelStartContext tree) {
        Mvel3Parser.MvelExpressionContext exprCtx = tree.mvelExpression();
        Mvel3Parser.ConditionalExpressionContext condCtx = exprCtx.conditionalExpression();
        Mvel3Parser.ConditionalOrExpressionContext orCtx = condCtx.conditionalOrExpression();
        Mvel3Parser.ConditionalAndExpressionContext andCtx = orCtx.conditionalAndExpression();
        Mvel3Parser.InclusiveOrExpressionContext incOrCtx = andCtx.inclusiveOrExpression();
        Mvel3Parser.ExclusiveOrExpressionContext excOrCtx = incOrCtx.exclusiveOrExpression();
        Mvel3Parser.AndExpressionContext andExprCtx = excOrCtx.andExpression();
        return andExprCtx.equalityExpression();
    }
}