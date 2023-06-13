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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.context.MvelTranspilerContext;
import org.mvel3.util.ClassTypeResolver;
import org.mvel3.util.TypeResolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MvelTranspiler {

    private final PreprocessPhase preprocessPhase = new PreprocessPhase();
    private final StatementVisitor statementVisitor;
    private MvelTranspilerContext mvelTranspilerContext;

    public MvelTranspiler(MvelTranspilerContext mvelTranspilerContext) {
        this.statementVisitor = new StatementVisitor(mvelTranspilerContext);
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    public static TranspiledResult transpile(Object expression, Map<String, Class> types, ClassLoader classLoader) {

        String expressionString = expression.toString();

        Set<String> imports = new HashSet<>();
        imports.add("java.util.List");
        imports.add("java.util.ArrayList");
        imports.add("java.util.HashMap");
        imports.add("java.util.Map");
        imports.add("java.math.BigDecimal");
        imports.add("org.mvel3.Address");

        TypeResolver classTypeResolver = new ClassTypeResolver(imports, classLoader);
        MvelTranspilerContext context = new MvelTranspilerContext(classTypeResolver);

        for (Map.Entry<String, Class> o : types.entrySet()) {
            context.addDeclaration(o.getKey(), o.getValue());
        }

        MvelTranspiler mvelTranspiler = new MvelTranspiler(context);
        ConstraintTranspiler constraintTranspiler = new ConstraintTranspiler(context);

        if (isAStatement(expressionString)) {
            String expressionStringWithBraces = String.format("{%s}", expressionString);
            return mvelTranspiler.transpileStatement(expressionStringWithBraces);
        } else {
            return constraintTranspiler.compileExpression(expressionString);
        }
    }

    private static boolean isAStatement(String expressionString) {
        boolean hasSemiColon = expressionString.contains(";");
        List<String> statementOperators = Arrays.asList("+=", "-=", "/=", "*=");

        for(String s :statementOperators) {
            if(expressionString.contains(s)) {
                return true;
            }
        }
        return hasSemiColon;
    }

    public TranspiledBlockResult transpileStatement(String mvelBlock) {

        BlockStmt mvelExpression = MvelParser.parseBlock(mvelBlock);

        preprocessPhase.removeEmptyStmt(mvelExpression);

        Set<String> allUsedBindings = new HashSet<>();

        List<String> modifyUsedBindings = mvelExpression.findAll(ModifyStatement.class)
                .stream()
                .flatMap(this::transformStatementWithPreprocessing)
                .collect(toList());

        allUsedBindings.addAll(modifyUsedBindings);

        // Entry point of the compiler
        TypedExpression compiledRoot = mvelExpression.accept(statementVisitor, null);
        allUsedBindings.addAll(mvelTranspilerContext.getUsedBindings());

        Node javaRoot = compiledRoot.toJavaExpression();

        if(!(javaRoot instanceof BlockStmt)) {
            throw new MvelCompilerException("With a BlockStmt as a input I was expecting a BlockStmt output");
        }

        BlockStmt compiledBlockStatement = (BlockStmt) javaRoot;
        return new TranspiledBlockResult(compiledBlockStatement.getStatements())
                .setUsedBindings(allUsedBindings);
    }

    private Stream<String> transformStatementWithPreprocessing(Statement s) {
        PreprocessPhase.PreprocessPhaseResult invoke = preprocessPhase.invoke(s);
        s.remove();
        return invoke.getUsedBindings().stream();
    }

}
