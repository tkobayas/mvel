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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Name;
import org.mvel2.EvaluatorConfig.BaseValues;
import org.mvel2.EvaluatorConfig.ContextObjectValues;
import org.mvel3.MVEL.Type;
import org.mvel3.javacompiler.KieMemoryCompiler;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.TranspiledResult;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MVELCompiler {


    public MapEvaluator compileMapEvaluator(ClassManager classManager,
                                            String expression, Map<String, Type> types,
                                            Set<String> imports,
                                            ClassLoader classLoader,
                                            String... returnVars) {
        CompilationUnit unit = compileMapEvaluatorNoLoad(expression, types, imports, returnVars);

		return compileEvaluator(classManager, classLoader, unit);
	}
    public ArrayEvaluator compileArrayEvaluator(ClassManager classManager, String expression, Declaration[] types,
                                                Set<String> imports,
                                                ClassLoader classLoader, String... returnVars) {
        CompilationUnit unit = compileArrayEvaluatorNoLoad(expression, types, imports, returnVars);

        return compileEvaluator(classManager, classLoader, unit);
    }

    public PojoEvaluator compilePojoEvaluator(ClassManager classManager, String expression, Class contextClass, Class outClass, String[] vars,
                                              Set<String> imports, ClassLoader classLoader) {
        CompilationUnit unit = compilePojoEvaluatorNoLoad(expression, contextClass, outClass, vars, imports);

        return compileEvaluator(classManager, classLoader, unit);
    }

    public PojoEvaluator compilePojoEvaluator(String expr, ContextObjectValues config) {
        CompilationUnit unit = compilePojoEvaluatorNoLoad(expr, config);

        return compileEvaluator(unit, config);
    }

    private CompilationUnit compilePojoEvaluatorNoLoad(String expr, ContextObjectValues config) {
        CompilationUnit unit = compilePojoEvaluatorNoLoad(expr, config);

        return unit;
    }

    public PojoEvaluator compileRootObjectEvaluator(ClassManager classManager, String expression,
                                                    Class rootClass, String rootGenerics,
                                                    Class outClass, String outGenerics,
                                                    Set<String> imports, ClassLoader classLoader) {
        CompilationUnit unit = compileRootObjectEvaluatorNoLoad(expression,
                                                                rootClass, rootGenerics,
                                                                outClass, outGenerics,
                                                                imports);

        return compileEvaluator(classManager, classLoader, unit);
    }

    private <T> T compileEvaluator(CompilationUnit unit, BaseValues baseConfig) {
        return compileEvaluator(baseConfig.classManager(), baseConfig.classLoader(), unit);
    }
    private <T> T compileEvaluator(ClassManager classManager, ClassLoader classLoader, CompilationUnit unit) {
        String javaFQN = evaluatorFullQualifiedName(unit);

        compileEvaluatorClass(classManager, classLoader, unit, javaFQN);

        Class<T> evaluatorDefinition = classManager.getClass(javaFQN);
        T evaluator = createEvaluatorInstance(evaluatorDefinition);
        return evaluator;
    }

    public CompilationUnit compileMapEvaluatorNoLoad(String expression,
                                                     Map<String, Type> types,
                                                     Set<String> imports,
                                                     String... returnVars) {
        TranspiledResult input = MVELTranspiler.transpile(expression, imports, types);

        return new CompilationUnitGenerator(input.getTranspilerContext().getParser()).createMapEvaluatorUnit(expression, input, types, returnVars);
    }

    public CompilationUnit compileArrayEvaluatorNoLoad(String expression,
                                                       Declaration[] types,
                                                       Set<String> imports,
                                                       String... returnVars) {

        Map<String, Type> typeMap = Arrays.stream(types).collect(Collectors.toMap(v -> v.getName(), v -> Type.type(v.getClazz(), v.getGenerics())));
        TranspiledResult input = MVELTranspiler.transpile(expression, imports, typeMap);

        return new CompilationUnitGenerator(input.getTranspilerContext().getParser()).createArrayEvaluatorUnit(expression, input, types, returnVars);
    }

    public CompilationUnit compilePojoEvaluatorNoLoad(String expression,
                                                      Class contextClass,
                                                      Class outClass,
                                                      String[] vars,
                                                      Set<String> imports) {
        Map<String, Type> map = new HashMap<>();
        Map<String, Method> methods = new HashMap<>();
        for (String var : vars) {
            Method method = getMethod(contextClass, var);

            if (method == null) {
                throw new RuntimeException("Unable to determine type for variable '" + var + "'");
            }
            methods.put(var, method);
            int nameEnd = method.getReturnType().getCanonicalName().length();
            map.put(var, Type.type(method.getReturnType(), method.getGenericReturnType().getTypeName().substring(nameEnd)));
        }

        TranspiledResult input = MVELTranspiler.transpile(expression, imports, map);

        return new CompilationUnitGenerator(input.getTranspilerContext().getParser()).createPojoEvaluatorUnit(expression, input, contextClass, outClass, methods);
    }

    public CompilationUnit compileRootObjectEvaluatorNoLoad(String expression,
                                                            Class rootClass,
                                                            String rootGenerics,
                                                            Class outClass,
                                                            String outGenerics,
                                                            Set<String> imports) {
        Map<String, Type> types = new HashMap<>();
        types.put("___this", Type.type(rootClass, rootGenerics));

        TranspiledResult input = MVELTranspiler.transpile(expression, imports, types);

        return new CompilationUnitGenerator(input.getTranspilerContext().getParser()).createRootObjectEvaluatorUnit(expression, input, rootClass,
                                                                                                                    rootGenerics, outClass, outGenerics);
    }

    public Method getMethod(Class contextClass, String var)  {
        Method method = null;
        try {
            String getterName = "get" + StringUtils.ucFirst(var);
            method = contextClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // swallow
        }

        try {
            method = contextClass.getMethod(var);
        } catch (NoSuchMethodException e) {
            // swallow
        }

        return method;
    }


    private String evaluatorFullQualifiedName(CompilationUnit evaluatorCompilationUnit) {
        ClassOrInterfaceDeclaration evaluatorClass = evaluatorCompilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("class expected"));

        String evaluatorClassName = evaluatorClass.getNameAsString();
        Name packageName = evaluatorCompilationUnit.getPackageDeclaration().map(PackageDeclaration::getName)
                .orElseThrow(() -> new RuntimeException("No package in template"));
        return String.format("%s.%s", packageName, evaluatorClassName);
    }

    private <T> T createEvaluatorInstance(Class<T> evaluatorDefinition) {
        T evaluator;
        try {
            evaluator = (T) evaluatorDefinition.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return evaluator;
    }

    private void compileEvaluatorClass(ClassManager classManager, ClassLoader classLoader, CompilationUnit compilationUnit, String javaFQN) {
        Map<String, String> sources = Collections.singletonMap(
                javaFQN,
                PrintUtil.printNode(compilationUnit)
        );
        KieMemoryCompiler.compile(classManager, sources, classLoader);
    }
}
