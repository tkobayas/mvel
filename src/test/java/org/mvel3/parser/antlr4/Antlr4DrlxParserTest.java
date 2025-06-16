/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.parser.antlr4;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Antlr4DrlxParserTest {

    @Test
    public void testParseSimpleExpr() {
        String expr = "name == \"Mark\"";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        Mvel3Parser.EqualityExpressionContext eqCtx = getEqualityExpressionContext((Mvel3Parser.MvelStartContext) tree);

        // Should have two relational expressions (left and right of ==)
        assertThat(eqCtx.relationalExpression().size()).isEqualTo(2);
        
        // Left side should be "name"
        String leftSide = eqCtx.relationalExpression(0).getText();
        assertThat(leftSide).isEqualTo("name");
        
        // Right side should be "Mark" 
        String rightSide = eqCtx.relationalExpression(1).getText();
        assertThat(rightSide).isEqualTo("\"Mark\"");
    }

    private static Mvel3Parser.EqualityExpressionContext getEqualityExpressionContext(Mvel3Parser.MvelStartContext tree) {
        Mvel3Parser.MvelExpressionContext exprCtx = tree.mvelExpression();
        Mvel3Parser.ConditionalExpressionContext condCtx = exprCtx.conditionalExpression();
        Mvel3Parser.LogicalOrExpressionContext orCtx = condCtx.logicalOrExpression();
        Mvel3Parser.LogicalAndExpressionContext andCtx = orCtx.logicalAndExpression(0);
        return andCtx.equalityExpression(0);
    }
}