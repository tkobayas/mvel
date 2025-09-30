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

import org.mvel3.transpiler.TranspiledResult;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public interface TranspilerTest {

    default void  test(Consumer<MVELBuilder<Map<String, Object>, Void,Object>> contextUpdater,
                      String inputExpression,
                      String expectedResult,
                      Consumer<TranspiledResult> resultAssert) {
        MVELBuilder<Map<String, Object>, Void, Object> builder = MVEL.map()
                                                                     .out(Type.OBJECT)
                                                                     .block(inputExpression)
                                                                     .classManager(new ClassManager())
                                                                     .classLoader(ClassLoader.getSystemClassLoader())
                                                                     .generatedSuperName(BaseExecutorClass.class.getName());

        builder.addImport(java.util.List.class.getCanonicalName());
        builder.addImport(java.util.ArrayList.class.getCanonicalName());
        builder.addImport(java.util.HashMap.class.getCanonicalName());
        builder.addImport(java.util.Map.class.getCanonicalName());
        builder.addImport(BigDecimal.class.getCanonicalName());
        builder.addImport(BigInteger.class.getCanonicalName());
        builder.addImport(Address.class.getCanonicalName());
        builder.addImport(Person.class.getCanonicalName());
        builder.addImport(Gender.class.getCanonicalName());
        contextUpdater.accept(builder);

//        // @FIXME
//        if (!builder.getRootDeclaration().type().isVoid()) {
//            // set a root within the context, if one is used
//            builder.getVariableInfo().setVars(builder.getRootDeclaration());
//        }

        TranspiledResult compiled = new MVELCompiler().transpile(builder.build());

        verifyBodyWithBetterDiff(expectedResult, compiled.methodBodyAsString());
        resultAssert.accept(compiled);
    }

    class BaseExecutorClass {
        public void insert(String string) {

        }
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

    default <K,R> void test(Consumer<MVELBuilder<Map<String, Object>, Void, Object>> testFunction,
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
