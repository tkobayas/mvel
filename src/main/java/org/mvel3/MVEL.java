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
 * 
 * Borrowed from MVEL, under the ASL2.0 license.
 *  
 */

package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MVEL {
    public static class Type {
        private final Class clazz;

        private final String generics;

        public Type(Class clazz, String generics) {
            this.clazz = clazz;
            this.generics = generics;
        }

        public static Type type(Class clazz) {
            return new Type(clazz,"");
        }
        public static Type type(Class clazz, String generics) {
            return new Type(clazz, generics);
        }

        public Class getClazz() {
            return clazz;
        }

        public String getGenerics() {
            return generics;
        }
    }

    ClassManager clsManager = new ClassManager();

    public MapEvaluator compileMapEvaluator(final String providedExpr, final Set<String> imports, final Map<String, Type> types, String... returnVars) {
        String actualExpression = maybeWrap(providedExpr);
        MVELCompiler MVELCompiler = new MVELCompiler();
        MapEvaluator evaluator = MVELCompiler.compileMapEvaluator(clsManager, actualExpression,
                                                                  types, imports, MVELCompiler.getClass().getClassLoader(), returnVars);

        return  evaluator;
    }

    public ArrayEvaluator compileArrayEvaluator(final String providedExpr, final Set<String> imports, final Declaration[] types, String... returnVars) {
        String actualExpression = maybeWrap(providedExpr);
        MVELCompiler MVELCompiler = new MVELCompiler();
        ArrayEvaluator evaluator = MVELCompiler.compileArrayEvaluator(clsManager, actualExpression,
                                                                      types, imports, MVELCompiler.getClass().getClassLoader(),
                                                                      returnVars);

        return  evaluator;
    }

    public <T, R> PojoEvaluator<T,R> compilePojoEvaluator(final String providedExpr, Set<String> imports, Class<T> contextClass, Class<R> returnClass, String... vars) {
        String actualExpression = maybeWrap(providedExpr);
        MVELCompiler MVELCompiler = new MVELCompiler();
        PojoEvaluator evaluator = MVELCompiler.compilePojoEvaluator(clsManager, actualExpression,
                                                                    contextClass, returnClass, vars, imports,
                                                                    MVELCompiler.getClass().getClassLoader());

        return  evaluator;
    }

    public Object executeExpression(final String expression, Set<String> imports, final Map<String, Object> vars) {
        return executeExpression(expression, imports, vars, Object.class);
    }

    public <T> T executeExpression(final String providedExpr, Set<String> imports, final Map<String, Object> vars,  Class<T> returnClass) {
        String actualExpr = maybeWrap(providedExpr);

        Map<String, Type> types = new HashMap<>();
        for (Map.Entry<String, Object> o : vars.entrySet()) {
            Class type = o.getValue() == null ? Object.class : o.getValue().getClass();
            types.put(o.getKey(), Type.type(type));
        }

        MapEvaluator mapEvaluator = compileMapEvaluator(actualExpr, imports, types);

        return (T) mapEvaluator.eval(vars);
    }

    private static String maybeWrap(String providedExpr) {
        String actualExpr = providedExpr;
        if ( !providedExpr.contains(";")) { // @TODO this is aa very crude sniff. Personally I'd rather not sniff and instead have two different method calls (mdp)
            actualExpr = "{ return " + providedExpr + ";}";
        }
        return actualExpr;
    }
}
