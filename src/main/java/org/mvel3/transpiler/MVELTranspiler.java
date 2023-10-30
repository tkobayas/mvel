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

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.parser.MvelParser;
import org.mvel3.parser.printer.MVELToJavaRewriter;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.context.TranspilerContext;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_15;

public class MVELTranspiler {

    private final PreprocessPhase preprocessPhase = new PreprocessPhase();

    private TranspilerContext context;

    public MVELTranspiler(TranspilerContext context) {
        this.context = context;
    }

//    public static TranspiledResult transpile(String expression, EvaluatorInfo<T, K, R> info) {
//        TranspiledResult result = transpile(expression, varTypes, rootVarTypes, ctx -> {
//            imports.stream().forEach(i -> ctx.addImport(i));
//        });
//
//        result.getBlock();
//
//        return result;
//    }

    public static <T, K, R>  TranspiledResult transpile(EvaluatorInfo<T, K, R> evalInfo, EvalPre evalPre) {

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver solver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(JAVA_15);
        conf.setSymbolResolver(solver);

        MvelParser parser = new MvelParser(conf);
//        if (context.getRootObject().isPresent()) {
//            context.addDeclaration(context.getRootPrefix().get(), context.getRootObject().get(), context.getRootGenerics().get());
//        }

        TranspilerContext context = new TranspilerContext(parser, typeSolver, evalInfo);

        //  Some code provides var types via the contextUpdater and others via a list


//        Arrays.stream(info.variableInfo().vars()).forEach( d -> context.addDeclaration(d));
//
//        Arrays.stream(info.rootInfo().vars()).forEach( d -> context.addDeclaration(d));

        MVELTranspiler mvelTranspiler = new MVELTranspiler(context);

        TranspiledResult transpiledResult =  mvelTranspiler.transpileBlock(evalInfo.expression(), evalPre);

        return transpiledResult;
    }

    public static <T> T handleParserResult(ParseResult<T> result) {
        if (result.isSuccessful()) {
            return result.getResult().get();
        } else {
            throw new ParseProblemException(result.getProblems());
        }
    }

    public TranspiledBlockResult transpileBlock(String mvelBlock, EvalPre evalPre) {
        // wrap as expression/block may or may not have {}, then unwrap latter.
        ParseResult<BlockStmt> result = context.getParser().parseBlock("{" + mvelBlock + "}");

        if (!result.isSuccessful()) {
            throw new RuntimeException(result.getProblems().toString());
        }
        BlockStmt mvelExpression = result.getResult().get();

        VariableAnalyser analyser = new VariableAnalyser(context.getEvaluatorInfo().allVars().keySet());
        mvelExpression.accept(analyser, null);

        if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid()) {
            analyser.getUsed().add(context.getEvaluatorInfo().rootDeclaration().name());
        }

        analyser.getUsed().stream().forEach(v -> context.addInput(v));

        preprocessPhase.removeEmptyStmt(mvelExpression);

        CompilationUnit unit = new CompilationUnit("org.mvel3");
        context.setUnit(unit);

        EvaluatorInfo<?, ?, ?> evalInfo = context.getEvaluatorInfo();

        evalInfo.imports().stream().forEach(s -> unit.addImport(s));

        evalInfo.staticImports().stream().forEach(s -> unit.addImport(s, true, false));

        ClassOrInterfaceDeclaration classDeclaration = unit.addClass("GeneratorEvaluaor__");
        context.setClassDeclaration(classDeclaration);
        classDeclaration.addImplementedType(getClassOrInterfaceType(org.mvel3.Evaluator.class.getCanonicalName()) + "<" +
                                            getClassOrInterfaceType(context.getEvaluatorInfo().variableInfo().type().getCanonicalGenericsName()) + ", " +
                                            getClassOrInterfaceType(context.getEvaluatorInfo().rootDeclaration().type().getCanonicalGenericsName()) + ", " +
                                            getClassOrInterfaceType(context.getEvaluatorInfo().outType().getCanonicalGenericsName()) + "> ");

        MethodDeclaration method = classDeclaration.addMethod("eval");
        method.setPublic(true);

        org.mvel3.Type outType = context.getEvaluatorInfo().outType();
        if ( !outType.isVoid()) {
            method.setType(handleParserResult(context.getParser().parseType(outType.getCanonicalGenericsName())));
        }

        method.addParameter(handleParserResult(context.getParser().parseType(evalInfo.variableInfo().type().getCanonicalGenericsName())), "context");

        NodeList<Statement> tempStmts = evalPre.evalPre(evalInfo, context, mvelExpression.getStatements());
        mvelExpression.setStatements(tempStmts);

        // post to add in returns
//
//        if (statements.size() == 1 && statements.get(0) instanceof BlockStmt) {
//            BlockStmt blockStmt = (BlockStmt) statements.get(0);
//            tempStmts.stream().forEach( s -> blockStmt.addStatement(0, s));
//
//            rewrittenStmt = (BlockStmt) statements.get(0);
//        } else {
//            tempStmts.addAll(statements);
//            NodeList<Statement> nodeList = NodeList.nodeList(tempStmts);
//            rewrittenStmt = new BlockStmt(nodeList);
//        }

        if (mvelExpression.getStatements().size() == 1 && mvelExpression.getStatement(0).isBlockStmt()) {
            method.setBody(mvelExpression.getStatement(0).asBlockStmt());
        } else {
            method.setBody(mvelExpression);
        }

        context.getSymbolResolver().inject(unit);

        MVELToJavaRewriter rewriter = new MVELToJavaRewriter(context);

        rewriter.rewriteChildren(method.getBody().get());

        // Inject the "return" if one is needed and it's missing and it's a statement expression.
        // This will not check branchs of an if statement or for loop, those need explicit returns
        Statement stmt = method.getBody().get().getStatements().getLast().get();
        if (!evalInfo.outType().isVoid() && stmt.isExpressionStmt() && method.getType() != null) {
            ReturnStmt returnStmt = new ReturnStmt(stmt.asExpressionStmt().getExpression());
            stmt.replace(returnStmt);
        }

        System.out.println(PrintUtil.printNode(unit));

        return new TranspiledBlockResult(unit, classDeclaration, method, context);
    }

    public ClassOrInterfaceType getClassOrInterfaceType(String fqn) {
        switch (fqn) {
            case "boolean":
                fqn = Boolean.class.getCanonicalName();
                break;
            case "char":
                fqn = Character.class.getCanonicalName();
                break;
            case "short":
                fqn = Short.class.getCanonicalName();
                break;
            case "int":
                fqn = Integer.class.getCanonicalName();
                break;
            case "long":
                fqn = Long.class.getCanonicalName();
                break;
            case "float":
                fqn = Float.class.getCanonicalName();
                break;
            case "double":
                fqn = Double.class.getCanonicalName();
                break;
        }
        ClassOrInterfaceType clsType = handleParserResult(context.getParser().parseClassOrInterfaceType(fqn));
        if (clsType.isPrimitiveType()) {
            clsType = clsType.asPrimitiveType().toBoxedType();
        }
        return clsType;

    }
}
