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
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.parser.printer.MVELToJavaRewriter;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;

import java.util.List;
import java.util.Set;

import static org.mvel3.parser.printer.PrintUtil.printNode;

public class TranspiledBlockResult implements TranspiledResult {

    private List<Statement> statements;

    private BlockStmt rewrittenStmt;
    private NodeList<ImportDeclaration> imports;

    private TranspilerContext context;

    public TranspiledBlockResult(List<Statement> statements, TranspilerContext context) {
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
            context.setUnit(unit);

            context.getImports().stream().forEach(s -> unit.addImport(s));
            context.getStaticImports().stream().forEach(s -> unit.addImport(s, true, false));

            imports = unit.getImports();

            ClassOrInterfaceDeclaration cls = unit.addClass("DummyClass");

            context.getInputs().stream().forEach(var -> {
                Declaration declr = context.getDeclarations().get(var);
                cls.addPrivateField(declr.getClazz().getCanonicalName() + declr.getGenerics(), var);
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

    public TranspilerContext getTranspilerContext() {
        return context;
    }

    @Override
    public String toString() {
        return "ParsingResult{" +
               "statements='" + asString() + '\'' +
               '}';
    }
}
