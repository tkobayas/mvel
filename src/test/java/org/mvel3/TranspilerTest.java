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

import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.TranspiledResult;
import org.mvel3.transpiler.context.MvelTranspilerContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

interface TranspilerTest {

    default void test(Consumer<MvelTranspilerContext> contextUpdater,
                      String inputExpression,
                      String expectedResult,
                      Consumer<TranspiledResult> resultAssert) {
        Consumer<MvelTranspilerContext> outerContextUpdater = ctx -> {
            contextUpdater.accept(ctx);

            ctx.addImport(java.util.List.class.getCanonicalName());
            ctx.addImport(java.util.ArrayList.class.getCanonicalName());
            ctx.addImport(java.util.HashMap.class.getCanonicalName());
            ctx.addImport(java.util.Map.class.getCanonicalName());
            ctx.addImport(BigDecimal.class.getCanonicalName());
            ctx.addImport(BigInteger.class.getCanonicalName());
            ctx.addImport(Address.class.getCanonicalName());
            ctx.addImport(Person.class.getCanonicalName());
            ctx.addImport(Gender.class.getCanonicalName());            
        };

        TranspiledResult compiled = MVELTranspiler.transpile(inputExpression, Collections.emptyMap(), outerContextUpdater);

        verifyBodyWithBetterDiff(expectedResult, compiled.asString());
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
                      Consumer<TranspiledResult> resultAssert) {
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

    default Collection<String> allUsedBindings(TranspiledResult result) {
        return new ArrayList<>(result.getInputs());
    }
}
