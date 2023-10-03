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
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.mvel3.MVEL.Type;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.context.TranspilerContext;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;

public class MVELTranspiler {

    private final PreprocessPhase preprocessPhase = new PreprocessPhase();

    private TranspilerContext mvelTranspilerContext;

    public MVELTranspiler(TranspilerContext mvelTranspilerContext) {
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    public static TranspiledResult transpile(Object expression, Map<String, Type> types, Consumer<TranspilerContext> contextUpdates) {
        String expressionString = expression.toString();

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver solver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = new MvelParser(conf);

        TranspilerContext context = new TranspilerContext(parser, typeSolver);



        //  Some code provides var types via the contextUpdater and others via a list
        if (contextUpdates != null) {
            contextUpdates.accept(context);
        }

        for (Map.Entry<String, Type> o : types.entrySet()) {
            context.addDeclaration(o.getKey(), o.getValue().getClazz(), o.getValue().getGenerics());
        }

        if (context.getRootObject().isPresent()) {
            context.addDeclaration(context.getRootPrefix().get(), context.getRootObject().get(), context.getRootGenerics().get());
        }


        MVELTranspiler mvelTranspiler = new MVELTranspiler(context);


        String expressionStringWithBraces = String.format("{%s}", expressionString);
        TranspiledResult transpiledResult =  mvelTranspiler.transpileStatement(expressionStringWithBraces);

        return transpiledResult;
    }

    public static TranspiledResult transpile(String expression, Set<String> imports, Map<String, Type> types) {
        TranspiledResult result = transpile(expression, types, ctx -> {
            imports.stream().forEach(i -> ctx.addImport(i));
        });

        result.getBlock();

        return result;
    }

    public TranspiledBlockResult transpileStatement(String mvelBlock) {
        System.out.println(mvelBlock);
        ParseResult<BlockStmt> result = mvelTranspilerContext.getParser().parseBlock(mvelBlock);
        if (!result.isSuccessful()) {
            throw new RuntimeException(result.getProblems().toString());
        }
        BlockStmt mvelExpression = result.getResult().get();

        VariableAnalyser analyser = new VariableAnalyser(mvelTranspilerContext.getDeclarations().keySet());
        mvelExpression.accept(analyser, null);

        if (mvelTranspilerContext.getRootPrefix().isPresent()) {
            analyser.getUsed().add(mvelTranspilerContext.getRootPrefix().get());
        }

        analyser.getUsed().stream().forEach(v -> mvelTranspilerContext.addInput(v));

        preprocessPhase.removeEmptyStmt(mvelExpression);

        return new TranspiledBlockResult(mvelExpression.getStatements(), mvelTranspilerContext);
    }
}
