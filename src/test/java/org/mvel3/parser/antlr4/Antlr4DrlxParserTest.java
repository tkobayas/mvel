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
import static org.mvel3.parser.antlr4.ParserTestUtil.getEqualityExpressionContext;
import static org.mvel3.parser.antlr4.ParserTestUtil.getLogicalAndExpressionContext;
import static org.mvel3.parser.antlr4.ParserTestUtil.getLogicalOrExpressionContext;

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

    @Test
    public void testBinaryWithNewLine() {
        String orExpr = "(addresses == 2 ||\n" +
                "                   addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);

        Mvel3Parser.LogicalOrExpressionContext orCtx = getLogicalOrExpressionContext((Mvel3Parser.MvelStartContext) orTree);
        assertThat(orCtx.logicalAndExpression().size()).isEqualTo(2);
        assertThat(orCtx.logicalAndExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(orCtx.logicalAndExpression(1).getText()).isEqualTo("addresses==3");

        String andExpr = "(addresses == 2 &&\n addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);

        Mvel3Parser.LogicalAndExpressionContext andCtx = getLogicalAndExpressionContext((Mvel3Parser.MvelStartContext) andTree);
        assertThat(andCtx.equalityExpression().size()).isEqualTo(2);
        assertThat(andCtx.equalityExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(andCtx.equalityExpression(1).getText()).isEqualTo("addresses==3");
    }

    @Test
    public void testBinaryWithWindowsNewLine() {
        String orExpr = "(addresses == 2 ||\r\n" +
                "                   addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);

        Mvel3Parser.LogicalOrExpressionContext orCtx = getLogicalOrExpressionContext((Mvel3Parser.MvelStartContext) orTree);
        assertThat(orCtx.logicalAndExpression().size()).isEqualTo(2);
        assertThat(orCtx.logicalAndExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(orCtx.logicalAndExpression(1).getText()).isEqualTo("addresses==3");

        String andExpr = "(addresses == 2 &&\r\n addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);

        Mvel3Parser.LogicalAndExpressionContext andCtx = getLogicalAndExpressionContext((Mvel3Parser.MvelStartContext) andTree);
        assertThat(andCtx.equalityExpression().size()).isEqualTo(2);
        assertThat(andCtx.equalityExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(andCtx.equalityExpression(1).getText()).isEqualTo("addresses==3");
    }

    @Test
    public void testBinaryWithNewLineBeginning() {
        String orExpr = "(" + System.lineSeparator() + "addresses == 2 || addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);

        Mvel3Parser.LogicalOrExpressionContext orCtx = getLogicalOrExpressionContext((Mvel3Parser.MvelStartContext) orTree);
        assertThat(orCtx.logicalAndExpression().size()).isEqualTo(2);
        assertThat(orCtx.logicalAndExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(orCtx.logicalAndExpression(1).getText()).isEqualTo("addresses==3");

        String andExpr = "(" + System.lineSeparator() + "addresses == 2 && addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);

        Mvel3Parser.LogicalAndExpressionContext andCtx = getLogicalAndExpressionContext((Mvel3Parser.MvelStartContext) andTree);
        assertThat(andCtx.equalityExpression().size()).isEqualTo(2);
        assertThat(andCtx.equalityExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(andCtx.equalityExpression(1).getText()).isEqualTo("addresses==3");
    }

    @Test
    public void testBinaryWithNewLineEnd() {
        String orExpr = "(addresses == 2 || addresses == 3 " + System.lineSeparator() + ")";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);

        Mvel3Parser.LogicalOrExpressionContext orCtx = getLogicalOrExpressionContext((Mvel3Parser.MvelStartContext) orTree);
        assertThat(orCtx.logicalAndExpression().size()).isEqualTo(2);
        assertThat(orCtx.logicalAndExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(orCtx.logicalAndExpression(1).getText()).isEqualTo("addresses==3");

        String andExpr = "(addresses == 2 && addresses == 3 " + System.lineSeparator() + ")";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);

        Mvel3Parser.LogicalAndExpressionContext andCtx = getLogicalAndExpressionContext((Mvel3Parser.MvelStartContext) andTree);
        assertThat(andCtx.equalityExpression().size()).isEqualTo(2);
        assertThat(andCtx.equalityExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(andCtx.equalityExpression(1).getText()).isEqualTo("addresses==3");
    }

    @Test
    public void testBinaryWithNewLineBeforeOperator() {
        String andExpr = "(addresses == 2" + System.lineSeparator() + "&& addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);

        Mvel3Parser.LogicalAndExpressionContext andCtx = getLogicalAndExpressionContext((Mvel3Parser.MvelStartContext) andTree);
        assertThat(andCtx.equalityExpression().size()).isEqualTo(2);
        assertThat(andCtx.equalityExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(andCtx.equalityExpression(1).getText()).isEqualTo("addresses==3");

        String orExpr = "(addresses == 2" + System.lineSeparator() + "|| addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);

        Mvel3Parser.LogicalOrExpressionContext orCtx = getLogicalOrExpressionContext((Mvel3Parser.MvelStartContext) orTree);
        assertThat(orCtx.logicalAndExpression().size()).isEqualTo(2);
        assertThat(orCtx.logicalAndExpression(0).getText()).isEqualTo("addresses==2");
        assertThat(orCtx.logicalAndExpression(1).getText()).isEqualTo("addresses==3");
    }
}