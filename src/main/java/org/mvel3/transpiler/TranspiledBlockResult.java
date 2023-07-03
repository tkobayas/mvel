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
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.context.Declaration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mvel3.parser.printer.PrintUtil.printNode;

public class TranspiledBlockResult implements TranspiledResult {

    private List<Statement> statements;

    private Set<String> inputs;

    private Map<String, Declaration> declarations;

    private Set<String> imports;

    public TranspiledBlockResult(List<Statement> statements, Map<String, Declaration> declarations, Set<String> inputs, Set<String> imports) {
        this.statements = statements;
        this.declarations = declarations;
        this.inputs = inputs;
        this.imports = imports;
    }

    public String resultAsString() {

        CompilationUnit unit = new CompilationUnit();
        imports.stream().forEach(s -> unit.addImport(s));

        ClassOrInterfaceDeclaration cls = unit.addClass("DummyClass");

        inputs.stream().forEach( var -> cls.addPrivateField(declarations.get(var).getClazz(), var));
        //cls.addField()

        MethodDeclaration method = cls.addMethod("dummyMethod");
        BlockStmt stmt = statementResults();
        method.setBody(stmt);

        MvelParser.getStaticConfiguration().getSymbolResolver().ifPresent( c -> ((JavaSymbolSolver)c).inject(unit));

        return printNode(stmt);
    }

    @Override
    public BlockStmt statementResults() {
        return new BlockStmt(NodeList.nodeList(statements));
    }

    @Override
    public Set<String> getInputs() {
        return inputs;
    }

    @Override
    public String toString() {
        return "ParsingResult{" +
                "statements='" + resultAsString() + '\'' +
                '}';
    }
}
