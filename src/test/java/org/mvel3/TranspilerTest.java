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

import org.mvel3.transpiler.TranspiledBlockResult;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.context.MvelTranspilerContext;
import org.mvel3.util.ClassTypeResolver;
import org.mvel3.util.TypeResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

interface TranspilerTest {

    default void test(Consumer<MvelTranspilerContext> testFunction,
                      String inputExpression,
                      String expectedResult,
                      Consumer<TranspiledBlockResult> resultAssert) {
        Set<String> imports = new HashSet<>();
        imports.add("java.util.List");
        imports.add("java.util.ArrayList");
        imports.add("java.util.HashMap");
        imports.add("java.util.Map");
        imports.add("java.math.BigDecimal");
        imports.add("org.mvel3.Address");
        imports.add(Person.class.getCanonicalName());
        imports.add(Gender.class.getCanonicalName());
        TypeResolver typeResolver = new ClassTypeResolver(imports, this.getClass().getClassLoader());
        MvelTranspilerContext mvelTranspilerContext = new MvelTranspilerContext(typeResolver);
        testFunction.accept(mvelTranspilerContext);
        TranspiledBlockResult compiled = new MVELTranspiler(mvelTranspilerContext).transpileStatement(inputExpression);
        verifyBodyWithBetterDiff(expectedResult, compiled.resultAsString());
        resultAssert.accept(compiled);
    }

    default void verifyBodyWithBetterDiff(Object expected, Object actual) {
        try {
            assertThat(actual).asString().isEqualToIgnoringWhitespace(expected.toString());
        } catch (AssertionError e) {
            assertThat(actual).isEqualTo(expected);
        }
    }

    default void test(String inputExpression,
                      String expectedResult,
                      Consumer<TranspiledBlockResult> resultAssert) {
        test(id -> {
        }, inputExpression, expectedResult, resultAssert);
    }

    default void test(Consumer<MvelTranspilerContext> testFunction,
                      String inputExpression,
                      String expectedResult) {
        test(testFunction, inputExpression, expectedResult, t -> {
        });
    }

    default void test(String inputExpression,
                      String expectedResult) {
        test(d -> {
        }, inputExpression, expectedResult, t -> {
        });
    }

    default Collection<String> allUsedBindings(TranspiledBlockResult result) {
        return new ArrayList<>(result.getInputs());
    }
}
