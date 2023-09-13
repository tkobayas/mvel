/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.parser.printer.MVELToJavaRewriter;
import org.mvel3.transpiler.context.MvelTranspilerContext;

import java.util.List;
import java.util.Set;

import static org.mvel3.parser.printer.PrintUtil.printNode;

public class TranspiledBlockResult implements TranspiledResult {

    private List<Statement> statements;

    private MvelTranspilerContext context;

    public TranspiledBlockResult(List<Statement> statements, MvelTranspilerContext context) {
        this.statements = statements;
        this.context = context;
    }

    public String asString() {

        CompilationUnit unit = new CompilationUnit();

        context.getImports().stream().forEach(s -> unit.addImport(s));
        context.getStaticImports().stream().forEach(s -> unit.addImport(s, true, false));

        ClassOrInterfaceDeclaration cls = unit.addClass("DummyClass");

        context.getInputs().stream().forEach( var -> cls.addPrivateField(context.getDeclarations().get(var).getClazz(), var));

        MethodDeclaration method = cls.addMethod("dummyMethod");
        BlockStmt stmt = statementResults();
        method.setBody(stmt);

        context.getSymbolResolver().inject(unit);

        MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);


        rewriter.rewriteChildren(stmt);

//        MVELToJavaVisitor1 mvelToJava1 = new MVELToJavaVisitor1(StaticMvelParser.getStaticTypeSolver());
//        mvelToJava1.visit(stmt, null);
//
//        MVELToJavaVisitor2 mvelToJava = new MVELToJavaVisitor2(StaticMvelParser.getStaticTypeSolver());
//        mvelToJava.findAndRewriteBinExpr(stmt);
//        mvelToJava.visit(stmt, null);

        return printNode(stmt, context.getTypeSolver());
    }

    @Override
    public BlockStmt statementResults() {
        if (statements.size() == 1 && statements.get(0) instanceof BlockStmt) {
            return (BlockStmt) statements.get(0);
        } else {
            NodeList<Statement> nodeList = NodeList.nodeList(statements);
            return new BlockStmt(nodeList);
        }
    }

    @Override
    public Set<String> getInputs() {
        return context.getInputs();
    }

    @Override
    public String toString() {
        return "ParsingResult{" +
               "statements='" + asString() + '\'' +
               '}';
    }
}
