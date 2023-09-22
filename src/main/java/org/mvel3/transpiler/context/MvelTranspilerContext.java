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

package org.mvel3.transpiler.context;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.ast.RootTypeThisExpr;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.util.OptionalUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MvelTranspilerContext {

    private final Map<String, Declaration> declarations = new HashMap<>();

    private final Set<String> imports = new HashSet<>();

    private final Set<String> staticImports = new HashSet<>();

    private final Set<String> inputs = new HashSet<>();

    private final MvelParser parser;

    private TypeSolver typeSolver;

    private JavaSymbolSolver symbolResolver;

    private ParserConfiguration parserConfiguration;

    private JavaParserFacade facade;

    // Used in ConstraintParser
    private Optional<Class<?>> rootPattern = Optional.empty();
    private Optional<String> rootPrefix = Optional.empty();

    public MvelTranspilerContext(MvelParser parser, TypeSolver typeSolver) {
        this.parser = parser;
        this.typeSolver = typeSolver;
        this.parserConfiguration = parser.getParserConfiguration();
        this.symbolResolver = (JavaSymbolSolver) parserConfiguration.getSymbolResolver().get();
        this.facade = JavaParserFacade.get(typeSolver);
    }

    public MvelParser getParser() {
        return parser;
    }

    public TypeSolver getTypeSolver() {
        return typeSolver;
    }

    public JavaParserFacade getFacade() {
        return facade;
    }

    public ParserConfiguration getParserConfiguration() {
        return parserConfiguration;
    }

    public JavaSymbolSolver getSymbolResolver() {
        return symbolResolver;
    }

    public MvelTranspilerContext addDeclaration(String name, Class<?> clazz) {
        declarations.put(name, new Declaration(name, clazz));
        return this;
    }

    public MvelTranspilerContext addDeclaration(String name, Class<?> clazz, String annotations) {
        declarations.put(name, new Declaration(name, clazz, annotations));
        return this;
    }

    public Map<String, Declaration> getDeclarations() {
        return declarations;
    }

    public Optional<Declaration> findDeclarations(String name) {
        Declaration d = declarations.get(name);
        return Optional.ofNullable(d);
    }

    public MvelTranspilerContext addStaticImport(String name) {
        this.staticImports.add(name);
        return this;
    }

    public MvelTranspilerContext addImport(String name) {
        this.imports.add(name);
        return this;
    }


    public MvelTranspilerContext addInput(String name) {
        this.inputs.add(name);
        return this;
    }

    public Set<String> getImports() {
        return imports;
    }

    public Set<String> getStaticImports() {
        return staticImports;
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public void setRootPatternPrefix(Class<?> rootPattern, String rootPrefix) {
        this.rootPattern = Optional.of(rootPattern);
        this.rootPrefix = Optional.of(rootPrefix);
    }

    public Optional<Class<?>> getRootPattern() {
        return rootPattern;
    }

    public Optional<String> getRootPrefix() {
        return rootPrefix;
    }

    public Optional<TypedExpression> createRootTypePrefix() {
        return OptionalUtils.map2(rootPattern, rootPrefix, RootTypeThisExpr::new);
    }
}
