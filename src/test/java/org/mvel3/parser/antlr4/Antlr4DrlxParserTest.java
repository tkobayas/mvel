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

import com.github.javaparser.ast.expr.Expression;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.parser.DrlxParser.parseExpression;
import static org.mvel3.parser.antlr4.ParserTestUtil.getEqualityExpressionContext;
import static org.mvel3.parser.printer.PrintUtil.printNode;

public class Antlr4DrlxParserTest {

    @Test
    public void testParseSimpleExpr() {
        String expr = "name == \"Mark\"";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        // Get the equality expression context - this demonstrates proper AST node access
        Mvel3Parser.EqualityExpressionContext eqCtx = getEqualityExpressionContext((Mvel3Parser.MvelStartContext) tree);

        // Assert on the actual AST nodes
        assertThat(eqCtx.EQUAL()).isNotNull();
        assertThat(eqCtx.EQUAL().getText()).isEqualTo("==");

        // Should have relational expression on the right (for "Mark")
        assertThat(eqCtx.relationalExpression()).isNotNull();
        assertThat(eqCtx.relationalExpression().getText()).isEqualTo("\"Mark\"");

        // Verify the complete expression text
        assertThat(eqCtx.getText()).isEqualTo("name==\"Mark\"");
    }

    @Test
    public void testBinaryWithNewLine() {
        String orExpr = "(addresses == 2 ||\n" +
                "                   addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);

        // Verify parsing succeeded and whitespace was handled correctly
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) orTree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("(addresses==2||addresses==3)");

        String andExpr = "(addresses == 2 &&\n addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);
        Mvel3Parser.MvelStartContext andStartCtx = (Mvel3Parser.MvelStartContext) andTree;
        assertThat(andStartCtx).isNotNull();
        assertThat(andStartCtx.mvelExpression().getText()).isEqualTo("(addresses==2&&addresses==3)");
    }

    @Test
    public void testBinaryWithWindowsNewLine() {
        String orExpr = "(addresses == 2 ||\r\n" +
                "                   addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) orTree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("(addresses==2||addresses==3)");

        String andExpr = "(addresses == 2 &&\r\n addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);
        Mvel3Parser.MvelStartContext andStartCtx = (Mvel3Parser.MvelStartContext) andTree;
        assertThat(andStartCtx).isNotNull();
        assertThat(andStartCtx.mvelExpression().getText()).isEqualTo("(addresses==2&&addresses==3)");
    }

    @Test
    public void testBinaryWithNewLineBeginning() {
        String orExpr = "(" + System.lineSeparator() + "addresses == 2 || addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) orTree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("(addresses==2||addresses==3)");

        String andExpr = "(" + System.lineSeparator() + "addresses == 2 && addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);
        Mvel3Parser.MvelStartContext andStartCtx = (Mvel3Parser.MvelStartContext) andTree;
        assertThat(andStartCtx).isNotNull();
        assertThat(andStartCtx.mvelExpression().getText()).isEqualTo("(addresses==2&&addresses==3)");
    }

    @Test
    public void testBinaryWithNewLineEnd() {
        String orExpr = "(addresses == 2 || addresses == 3 " + System.lineSeparator() + ")";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) orTree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("(addresses==2||addresses==3)");

        String andExpr = "(addresses == 2 && addresses == 3 " + System.lineSeparator() + ")";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);
        Mvel3Parser.MvelStartContext andStartCtx = (Mvel3Parser.MvelStartContext) andTree;
        assertThat(andStartCtx).isNotNull();
        assertThat(andStartCtx.mvelExpression().getText()).isEqualTo("(addresses==2&&addresses==3)");
    }

    @Test
    public void testBinaryWithNewLineBeforeOperator() {
        String andExpr = "(addresses == 2" + System.lineSeparator() + "&& addresses == 3  )";
        ParseTree andTree = Antlr4DrlxParser.parseExpression(andExpr);
        Mvel3Parser.MvelStartContext andStartCtx = (Mvel3Parser.MvelStartContext) andTree;
        assertThat(andStartCtx).isNotNull();
        assertThat(andStartCtx.mvelExpression().getText()).isEqualTo("(addresses==2&&addresses==3)");

        String orExpr = "(addresses == 2" + System.lineSeparator() + "|| addresses == 3  )";
        ParseTree orTree = Antlr4DrlxParser.parseExpression(orExpr);
        Mvel3Parser.MvelStartContext orStartCtx = (Mvel3Parser.MvelStartContext) orTree;
        assertThat(orStartCtx).isNotNull();
        assertThat(orStartCtx.mvelExpression().getText()).isEqualTo("(addresses==2||addresses==3)");
    }

    @Test
    public void testParseSafeCastExpr() {
        // Test the original expression now that we use full Java grammar
        String expr = "this instanceof Person && ((Person) this).name == \"Mark\"";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        // Verify that the expression parses successfully and get the top-level AND context
        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("thisinstanceofPerson&&((Person)this).name==\"Mark\"");
    }

    @Ignore("Inline Cast is DRLX specific, not MVEL")
    @Test
    public void testParseInlineCastExpr() {
        String expr = "this#Person.name == \"Mark\"";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("this#Person.name==\"Mark\"");
    }

    @Ignore("Inline Cast is DRLX specific, not MVEL")
    @Test
    public void testParseInlineCastExpr2() {
        String expr = "address#com.pkg.InternationalAddress.state.length == 5";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("address#com.pkg.InternationalAddress.state.length==5");
    }

    @Ignore("Inline Cast is DRLX specific, not MVEL")
    @Test
    public void testParseInlineCastExpr3() {
        String expr = "address#org.mvel3.compiler.LongAddress.country.substring(1)";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("address#org.mvel3.compiler.LongAddress.country.substring(1)");
    }

    @Ignore("Inline Cast is DRLX specific, not MVEL")
    @Test
    public void testParseInlineCastExpr4() {
        String expr = "address#com.pkg.InternationalAddress.getState().length == 5";
        ParseTree tree = Antlr4DrlxParser.parseExpression(expr);

        Mvel3Parser.MvelStartContext startCtx = (Mvel3Parser.MvelStartContext) tree;
        assertThat(startCtx).isNotNull();
        assertThat(startCtx.mvelExpression().getText()).isEqualTo("address#com.pkg.InternationalAddress.getState().length==5");
    }


}