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
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.context.MvelTranspilerContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;

public class MVELTranspiler {

    private final PreprocessPhase preprocessPhase = new PreprocessPhase();

    private MvelTranspilerContext mvelTranspilerContext;

    public MVELTranspiler(MvelTranspilerContext mvelTranspilerContext) {
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    public static TranspiledResult transpile(Object expression, Map<String, Class> types) {
        return transpile(expression, types);
    }

    public static TranspiledResult transpile(Object expression, Map<String, Class> types, Consumer<MvelTranspilerContext> contextUpdates) {
        String expressionString = expression.toString();

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver solver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = new MvelParser(conf);

        MvelTranspilerContext context = new MvelTranspilerContext(parser, typeSolver);

        //  Some code provides var types via the contextUpdater and others via a list
        if (contextUpdates != null) {
            contextUpdates.accept(context);
        }

        for (Map.Entry<String, Class> o : types.entrySet()) {
            context.addDeclaration(o.getKey(), o.getValue());
        }

        MVELTranspiler mvelTranspiler = new MVELTranspiler(context);

        TranspiledResult transpiledResult = null;
        if (true) { //isAStatement(expressionString)) {
            String expressionStringWithBraces = String.format("{%s}", expressionString);
            transpiledResult =  mvelTranspiler.transpileStatement(expressionStringWithBraces);
        } else {
            //transpiledResult =  constraintTranspiler.compileExpression(expressionString);
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

    public static TranspiledResult transpile(String expression, Set<String> imports, Map<String, Class> types) {
        return transpile(expression, types, ctx -> {
            imports.stream().forEach(i -> ctx.addImport(i));
        });
    }

    public TranspiledBlockResult transpileStatement(String mvelBlock) {
        ParseResult<BlockStmt> result = mvelTranspilerContext.getParser().parseBlock(mvelBlock);
        if (!result.isSuccessful()) {
            throw new RuntimeException(result.getProblems().toString());
        }
        BlockStmt mvelExpression = result.getResult().get();

        VariableAnalyser analyser = new VariableAnalyser(mvelTranspilerContext.getDeclarations().keySet());
        mvelExpression.accept(analyser, null);

        analyser.getUsed().stream().forEach(v -> mvelTranspilerContext.addInput(v));


        preprocessPhase.removeEmptyStmt(mvelExpression);

        return new TranspiledBlockResult(mvelExpression.getStatements(), mvelTranspilerContext);
    }

    private Stream<String> transformStatementWithPreprocessing(Statement s) {
        PreprocessPhase.PreprocessPhaseResult invoke = preprocessPhase.invoke(s);
        s.remove();
        return invoke.getUsedBindings().stream();
    }
}
