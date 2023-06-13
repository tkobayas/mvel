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
import org.mvel3.javacompiler.KieMemoryCompiler;
import org.mvel3.parser.printer.PrintUtil;
import org.mvel3.transpiler.MvelTranspiler;
import org.mvel3.transpiler.TranspiledResult;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

public class MVELCompiler {


    public MapEvaluator compileAndLoad(ClassManager classManager,
                                       Object expression, Map<String, Class> types,
                                       ClassLoader classLoader) {
        CompilationUnit unit = compile((String) expression, types, classLoader);

        String javaFQN = evaluatorFullQualifiedName(unit);

        compileEvaluatorClass(classManager, classLoader, unit, javaFQN);

        Class<?> evaluatorDefinition = classManager.getClass(javaFQN);
        MapEvaluator evaluator = createMapEvaluatorInstance(evaluatorDefinition);
		return evaluator;
	}

    public CompilationUnit compile(String expression,
                                   Map<String, Class> types,
                                   ClassLoader classLoader) {
        TranspiledResult input = MvelTranspiler.transpile(expression, types, classLoader);

        return new CompilationUnitGenerator().createMapEvaluatorUnit(expression, input, types);
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

    private MapEvaluator createMapEvaluatorInstance(Class<?> evaluatorDefinition) {
        MapEvaluator evaluator;
        try {
            evaluator = (MapEvaluator) evaluatorDefinition.getConstructor().newInstance();
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
