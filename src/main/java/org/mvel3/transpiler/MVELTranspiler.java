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

public class MVELTranspiler {

    private final PreprocessPhase preprocessPhase = new PreprocessPhase();
    //private final StatementVisitor statementVisitor;
    private MvelTranspilerContext mvelTranspilerContext;

    public MVELTranspiler(MvelTranspilerContext mvelTranspilerContext) {
        //this.statementVisitor = new StatementVisitor(mvelTranspilerContext);
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

        MVELTranspiler mvelTranspiler = new MVELTranspiler(context);
        ConstraintTranspiler constraintTranspiler = new ConstraintTranspiler(context);

        TranspiledResult transpiledResult;
        if (isAStatement(expressionString)) {
            String expressionStringWithBraces = String.format("{%s}", expressionString);
            transpiledResult =  mvelTranspiler.transpileStatement(expressionStringWithBraces);
        } else {
            transpiledResult =  constraintTranspiler.compileExpression(expressionString);
        }

        return transpiledResult;
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

        VariableAnalyser analyser = new VariableAnalyser(mvelTranspilerContext.getDeclarations().keySet());
        mvelExpression.accept(analyser, null);

        preprocessPhase.removeEmptyStmt(mvelExpression);

//        mvelExpression.findAll(ModifyStatement.class)
//                      .stream()
//                      .flatMap(this::transformStatementWithPreprocessing)
//                      .collect(toList());
//
//        // Entry point of the compiler
//        TypedExpression compiledRoot = mvelExpression.accept(statementVisitor, null);
//
//        Node javaRoot = compiledRoot.toJavaExpression();
//
//        if(!(javaRoot instanceof BlockStmt)) {
//            throw new MVELTranspilerException("With a BlockStmt as a input I was expecting a BlockStmt output");
//        }
//
//        BlockStmt compiledBlockStatement = (BlockStmt) javaRoot;

        //return new TranspiledBlockResult(compiledBlockStatement.getStatements(), analyser.getUsed());
        return new TranspiledBlockResult(mvelExpression.getStatements(), mvelTranspilerContext.getDeclarations(), analyser.getUsed(), mvelTranspilerContext.getTypeResolver().getImports());
    }

    private Stream<String> transformStatementWithPreprocessing(Statement s) {
        PreprocessPhase.PreprocessPhaseResult invoke = preprocessPhase.invoke(s);
        s.remove();
        return invoke.getUsedBindings().stream();
    }

}
