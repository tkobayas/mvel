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

package org.mvel3;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.TranspiledResult;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.util.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CompilationUnitGenerator {

    private CompilationUnit template;
    private ClassOrInterfaceDeclaration evaluatorClass;
    private MethodDeclaration methodDeclaration;
    private BlockStmt methodBody;
    private BlockStmt bindingAssignmentBlock;
    private BlockStmt mvelExecutionBlock;
    private BlockStmt repopulateMapBlock;
    private Statement lastMVELStatement;
    private Statement lastBodyStatement;

    private static final String RESULT_VALUE_REFERENCE_NAME = "___resultValue";

    public CompilationUnit createMapEvaluatorUnit(String originalExpression, TranspiledResult input, Map<String, MVEL.Type> types, String... returnVars) {
        loadTemplate("MapEvaluatorTemplate");
        renameTemplateClass(originalExpression, "MapEvaluatorTemplate");
        clearExamples();

        createMapContextVariableAssignments(types.entrySet(), input.getInputs(), returnVars);

        addImports(input);
        rewriteBody(input, Object.class);

        return template;
    }

    public CompilationUnit createArrayEvaluatorUnit(String originalExpression, TranspiledResult input,
                                                    Declaration[] types, String... returnVars) {
        loadTemplate("ArrayEvaluatorTemplate");
        renameTemplateClass(originalExpression, "ArrayEvaluatorTemplate");

        clearExamples();

        createArrayContextVariableAssignments(types, returnVars);

        addImports(input);
        rewriteBody(input, Object.class);

        return template;
    }

    private void rewriteBody(TranspiledResult input, Class<Object> returnClass) {
        BlockStmt compiledMVELBlock = input.getBlock();
        mvelExecutionBlock.replace(compiledMVELBlock);

        lastMVELStatement = lastStatementOfABlock(compiledMVELBlock);
        lastBodyStatement = lastStatementOfABlock(methodBody);

        defineLastStatement(compiledMVELBlock, returnClass);

        logGenerateClass();
    }

    public CompilationUnit createPojoEvaluatorUnit(String originalExpression, TranspiledResult input,
                                                   Class contextClass, Class returnClass, Map<String, Method> types) {
        loadTemplate("PojoEvaluatorTemplate");
        renameTemplateClass(originalExpression, "PojoEvaluatorTemplate");

        clearExamples();

        Type contextType = new ClassOrInterfaceType(contextClass.getCanonicalName());
        Type returnType = new ClassOrInterfaceType(returnClass.getCanonicalName());
        evaluatorClass.getImplementedTypes(0).setTypeArguments(contextType, returnType);
        methodDeclaration.getParameter(0).setType(contextType);
        methodDeclaration.setType(returnClass.getCanonicalName());


        for (Map.Entry<String, Method> type : types.entrySet()) {
            createPojoContextVariableAssignments(type);
        }

        addImports(input);
        rewriteBody(input, returnClass);

        return template;
    }

    private void addImports(TranspiledResult input) {
        NodeList<ImportDeclaration> imports = input.getImports();
        if (imports == null) {
            throw new RuntimeException();
        }
        for (ImportDeclaration i : imports) {
            template.addImport(i.clone());
        }
    }

    private Statement lastStatementOfABlock(BlockStmt mvelBlock) {
        NodeList<Statement> statements = mvelBlock.getStatements();
        return statements.get(statements.size() - 1);
    }

    private void clearExamples() {
        for (VariableDeclarationExpr variableDeclarationExpr : methodBody.findAll(VariableDeclarationExpr.class)) {
            variableDeclarationExpr.getParentNode().ifPresent(Node::remove);
        }
    }

    private void renameTemplateClass(String originalExpression, String templateName) {
        String newName = String.format("Evaluator%s", StringUtils.md5Hash(originalExpression));
        replaceSimpleNameWith(evaluatorClass, templateName, newName);
    }

    private void logGenerateClass() {
        System.out.println(PrintUtil.printNode(template));
        //LOG.debug(PrintUtil.printNode(template));
    }

    // Simulate "Last expression is a return statement"
    private void defineLastStatement(BlockStmt mvelBlock, Class returnClass) {
        Expression expression;
        if (lastMVELStatement.isReturnStmt()) {
            expression = ((ReturnStmt)lastMVELStatement).getExpression().get();
        } else {
            expression = lastMVELStatement.asExpressionStmt().getExpression();
        }

        addResultReference(returnClass);

        if(expression.isMethodCallExpr() && expression.asMethodCallExpr().getNameAsString().startsWith("set")) {
            lastStatementIsGetter(mvelBlock, expression);
        } else {
            final Expression assignExpr = new AssignExpr(new NameExpr(RESULT_VALUE_REFERENCE_NAME), expression, AssignExpr.Operator.ASSIGN);
            mvelBlock.replace(lastMVELStatement, new ExpressionStmt(assignExpr));
        }

        returnResult();
    }

    private void lastStatementIsGetter(BlockStmt mvelBlock, Expression expression) {
        MethodCallExpr methodCallExprClone = expression.asMethodCallExpr().clone();
        String getterName = methodCallExprClone.getName().asString().replace("set", "get");
        MethodCallExpr getter = new MethodCallExpr(getterName);
        methodCallExprClone.getScope().ifPresent(getter::setScope);
        final Expression assignExpr = new AssignExpr(new NameExpr(RESULT_VALUE_REFERENCE_NAME), getter, AssignExpr.Operator.ASSIGN);
        mvelBlock.addStatement(assignExpr);
    }

    private void returnResult() {
        ReturnStmt node = new ReturnStmt(new NameExpr(RESULT_VALUE_REFERENCE_NAME));
        lastBodyStatement.replace(node);
    }

    private void addResultReference(Class returnClass) {
        VariableDeclarationExpr returnVariable = new VariableDeclarationExpr(StaticJavaParser.parseType(returnClass.getCanonicalName()),
                                                                             RESULT_VALUE_REFERENCE_NAME);
        methodBody.addStatement(0, returnVariable);
    }

    private void createMapContextVariableAssignments(Set<Entry<String, MVEL.Type>> entries, Set<String> inputs, String[] returnVars) {
        Set<String> returnSet = new HashSet<>(Arrays.asList(returnVars));

        for (Map.Entry<String, MVEL.Type> entry : entries) {
            String variable = entry.getKey();
            if (!inputs.contains(variable)) {
                continue;
            }
            
            MVEL.Type contextVarType = entry.getValue();
            if (contextVarType.getClazz().getCanonicalName() != null) {
                Type type = StaticJavaParser.parseType(contextVarType.getClazz().getCanonicalName() + contextVarType.getGenerics());
                VariableDeclarationExpr varDecl = new VariableDeclarationExpr(type, variable);
                Expression indexMethodExpression = new CastExpr(type, new MethodCallExpr(new NameExpr("map"), "get", NodeList.nodeList(new StringLiteralExpr(variable))));
                methodBody.addStatement(0, varDecl);

                final Expression expr = new AssignExpr(new NameExpr(variable), indexMethodExpression, AssignExpr.Operator.ASSIGN);
                bindingAssignmentBlock.addStatement(expr);

                if (returnSet.contains(variable)) {
                    MethodCallExpr putExpr = new MethodCallExpr(new NameExpr("map"), "put", NodeList.nodeList(new StringLiteralExpr(variable), new NameExpr(variable)));
                    repopulateMapBlock.addStatement(putExpr);
                }
            }
        }
    }

    private void createArrayContextVariableAssignments(Declaration[] types, String... returnVars) {
        Set<String> returnSet = new HashSet<>(Arrays.asList(returnVars));

        for (int i = 0; i < types.length; i++) {
            Class<?> contextVarClass = types[i].getClazz();
            String binding = types[i].getName();
            if (contextVarClass.getCanonicalName() != null) {
                Type type = StaticJavaParser.parseType(types[i].getClazz().getCanonicalName() + types[i].getGenerics());
                VariableDeclarationExpr variable = new VariableDeclarationExpr(type, binding);

                Expression indexMethodExpression = new CastExpr(type, new ArrayAccessExpr(new NameExpr("array"), new IntegerLiteralExpr(i)));

                methodBody.addStatement(0, variable);


                final Expression expr = new AssignExpr(new NameExpr(binding), indexMethodExpression, AssignExpr.Operator.ASSIGN);
                bindingAssignmentBlock.addStatement(expr);

                if (returnSet.contains(binding)) {
                    AssignExpr putExpr = new AssignExpr(new ArrayAccessExpr(new NameExpr("array"), new IntegerLiteralExpr(i)),
                                                        new NameExpr(binding),
                                                        Operator.ASSIGN);

                    repopulateMapBlock.addStatement(putExpr);
                }
            }
        }
    }

    private void createPojoContextVariableAssignments(Map.Entry<String, Method> entry) {

        String binding = entry.getKey();
        Method method = entry.getValue();

        Class<?> contextVarClass = method.getReturnType();
        if (contextVarClass.getCanonicalName() != null) {
            Type type = StaticJavaParser.parseType(method.getGenericReturnType().getTypeName());
            VariableDeclarationExpr variable = new VariableDeclarationExpr(type, binding);
            Expression indexMethodExpression = new CastExpr(type, new MethodCallExpr(new NameExpr("pojo"), method.getName()));
            methodBody.addStatement(0, variable);

            final Expression expr = new AssignExpr(new NameExpr(binding), indexMethodExpression, AssignExpr.Operator.ASSIGN);
            bindingAssignmentBlock.addStatement(expr);
        }
    }

    private void loadTemplate(String templateName) {
        template = getMethodTemplate(templateName);
        evaluatorClass = template.getClassByName(templateName)
                .orElseThrow(() -> new RuntimeException("Cannot find class"));
        methodDeclaration = evaluatorClass.findFirst(MethodDeclaration.class)
                .orElseThrow(() -> new RuntimeException("cannot find Method"));
        methodBody = methodDeclaration.findFirst(BlockStmt.class)
                .orElseThrow(() -> new RuntimeException("cannot find method body"));

        bindingAssignmentBlock = findBlock(" binding assignment");
        mvelExecutionBlock = findBlock(" execute MVEL here");
        repopulateMapBlock = findBlock(" repopulate map");
    }

    private BlockStmt findBlock(String comment) {
        BlockStmt block = methodBody.findFirst(BlockStmt.class, b -> blockHasComment(b, comment))
                .orElseThrow(() -> new RuntimeException(comment + " not found"));
        block.getStatements().clear();
        return block;
    }

    private CompilationUnit getMethodTemplate(String templateName) {
        InputStream resourceAsStream = this.getClass()
                .getResourceAsStream("/org/mvel3/" + templateName + ".java");
        return new JavaParser().parse(resourceAsStream).getResult().get();
    }

    public static void replaceSimpleNameWith(Node source, String oldName, String newName) {
        source.findAll(SimpleName.class, ne -> ne.toString().equals(oldName))
                .forEach(r -> r.replace(new SimpleName(newName)));
    }

    private static boolean blockHasComment(BlockStmt block, String comment) {
        return block.getComment().filter(c -> comment.equals(c.getContent()))
                .isPresent();
    }
}
