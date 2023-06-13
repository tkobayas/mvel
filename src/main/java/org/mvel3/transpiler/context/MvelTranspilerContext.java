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

import org.mvel3.transpiler.MvelCompilerException;
import org.mvel3.transpiler.ast.RootTypeThisExpr;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.util.TypeResolver;
import org.mvel3.transpiler.util.OptionalUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mvel3.util.StringUtils.isEmpty;

public class MvelTranspilerContext {

    private final Map<String, Declaration> declarations = new HashMap<>();
    private final Map<String, StaticMethod> staticMethods = new HashMap<>();
    private final Map<String, DeclaredFunction> declaredFunctions = new HashMap<>();

    private final TypeResolver typeResolver;
    private final String scopeSuffix;
    private final Set<String> usedBindings = new HashSet<>();

    // Used in ConstraintParser
    private Optional<Class<?>> rootPattern = Optional.empty();
    private Optional<String> rootPrefix = Optional.empty();

    public MvelTranspilerContext(TypeResolver typeResolver) {
        this(typeResolver, null);
    }

    public MvelTranspilerContext(TypeResolver typeResolver, String scopeSuffix) {
        this.typeResolver = typeResolver;
        this.scopeSuffix = isEmpty( scopeSuffix ) ? null : scopeSuffix;
    }

    public MvelTranspilerContext addDeclaration(String name, Class<?> clazz) {
        declarations.put(name, new Declaration(name, clazz));
        return this;
    }

    public Optional<Declaration> findDeclarations(String name) {
        Declaration d = declarations.get(name);
        if (d == null && scopeSuffix != null) {
            d = declarations.get( name + scopeSuffix );
        }
        return Optional.ofNullable(d);
    }

    public Optional<Class<?>> findEnum(String name) {
        try {
            return Optional.of(typeResolver.resolveType(name));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public Class<?> resolveType(String name) {
        try {
            return typeResolver.resolveType(name);
        } catch (ClassNotFoundException e) {
            throw new MvelCompilerException(e);
        }
    }

    public MvelTranspilerContext addStaticMethod(String name, Method method) {
        staticMethods.put(name, new StaticMethod(name, method));
        return this;
    }

    public Optional<Method> findStaticMethod(String name) {
        return Optional.ofNullable(staticMethods.get(name)).map(StaticMethod::getMethod);
    }

    public MvelTranspilerContext addDeclaredFunction(String name, String returnType, List<String> arguments) {
        declaredFunctions.put(name, new DeclaredFunction(this.typeResolver, name, returnType, arguments));
        return this;
    }

    public Optional<DeclaredFunction> findDeclaredFunction(String name) {
        return Optional.ofNullable(declaredFunctions.get(name));
    }

    public void setRootPatternPrefix(Class<?> rootPattern, String rootPrefix) {
        this.rootPattern = Optional.of(rootPattern);
        this.rootPrefix = Optional.of(rootPrefix);
    }

    public Optional<Class<?>> getRootPattern() {
        return rootPattern;
    }

    public Optional<TypedExpression> createRootTypePrefix() {
        return OptionalUtils.map2(rootPattern, rootPrefix, RootTypeThisExpr::new);
    }

    public void addUsedBinding(String s) {
        usedBindings.add(s);
    }

    public Set<String> getUsedBindings() {
        return usedBindings;
    }
}
