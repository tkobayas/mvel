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

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.parser.printer.MVELToJavaRewriter;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.MvelTranspilerContext;

import java.util.List;
import java.util.Set;

import static org.mvel3.parser.printer.PrintUtil.printNode;

public class TranspiledBlockResult implements TranspiledResult {

    private List<Statement> statements;

    private BlockStmt rewrittenStmt;
    private NodeList<ImportDeclaration> imports;

    private MvelTranspilerContext context;

    public TranspiledBlockResult(List<Statement> statements, MvelTranspilerContext context) {
        this.statements = statements;
        this.context = context;
    }

    public String asString() {
        getBlock();

        return printNode(rewrittenStmt, context.getTypeSolver());
    }

    @Override
    public BlockStmt getBlock() {
        rewriteBlock();
        return rewrittenStmt;
    }

    public void rewriteBlock() {
        if (rewrittenStmt == null) {

            CompilationUnit unit = new CompilationUnit();

            context.getImports().stream().forEach(s -> unit.addImport(s));
            context.getStaticImports().stream().forEach(s -> unit.addImport(s, true, false));

            imports = unit.getImports();

            ClassOrInterfaceDeclaration cls = unit.addClass("DummyClass");

            context.getInputs().stream().forEach(var -> {
                Declaration declr = context.getDeclarations().get(var);
                FieldDeclaration f = cls.addPrivateField(declr.getClazz().getCanonicalName() + declr.getGenerics(), var);
                System.out.println(f);
//                if (declr.getGenerics() != null) {
//                    ParseResult<AnnotationExpr> result = context.getParser().parseAnnotation(declr.getGenerics());
//                    if (result.isSuccessful()) {
//                        AnnotationExpr expr = result.getResult().get();
//                        f.setAnnotations(NodeList.nodeList(expr));
//                    } else {
//                        throw new RuntimeException("Unable to parser annotation expression '" + declr.getGenerics() + "' for input var: '" + var + "'");
//                    }
//                }
            });

            MethodDeclaration method = cls.addMethod("dummyMethod");

            if (statements.size() == 1 && statements.get(0) instanceof BlockStmt) {
                rewrittenStmt = (BlockStmt) statements.get(0);
            } else {
                NodeList<Statement> nodeList = NodeList.nodeList(statements);
                rewrittenStmt = new BlockStmt(nodeList);
            }

            method.setBody(rewrittenStmt);

            context.getSymbolResolver().inject(unit);

            MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);


            rewriter.rewriteChildren(rewrittenStmt);
        }
    }

    public NodeList<ImportDeclaration> getImports() {
        return imports;
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
