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

import org.mvel3.EvaluatorBuilder.ContextInfoBuilder;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.transpiler.context.Declaration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MVEL {
    public <T, K, R> Evaluator<T, K, R> compile(EvaluatorInfo<T, K, R> evalInfo) {
        MVELCompiler compiler = new MVELCompiler();
        Evaluator<T, K, R> eval = compiler.compile(evalInfo);
        return eval;
    }

    private static MVEL instance;

    public static MVEL get() {
        if (instance == null) {
            instance = new MVEL();
        }

        return instance;
    }

    ClassManager clsManager = new ClassManager();

    public <T extends Map, Void, R> Evaluator<T, Void, R> compileMapEvaluator(final String providedExpr, final Class<R> outClass, final Set<String> imports, final Map<String, Type> types) {
        String actualExpression = maybeWrap(providedExpr);
        MVELCompiler MVELCompiler = new MVELCompiler();
        EvaluatorBuilder<T, Void, R> eval = new EvaluatorBuilder<>();
        eval.setClassManager(clsManager).setClassLoader(ClassLoader.getSystemClassLoader())
            .setExpression(actualExpression)
            .setImports(imports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(Map.class, "<String, Object>"))
                                               .setVars(Declaration.from(types)))
            .setOutType(Type.type(outClass));

        Evaluator<T, Void, R> evaluator = MVELCompiler.compile(eval.build());

        return  evaluator;
    }

    public <T extends List, Void, R> Evaluator<T, Void, R> compileListEvaluator(final String providedExpr, Class<R> outClass, final Set<String> imports, final Declaration[] types) {
        String actualExpression = maybeWrap(providedExpr);
        MVELCompiler MVELCompiler = new MVELCompiler();
        EvaluatorBuilder<T, Void, R> eval = new EvaluatorBuilder<>();
        eval.setClassManager(clsManager).setClassLoader(ClassLoader.getSystemClassLoader())
            .setExpression(actualExpression)
            .setImports(imports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(List.class, "<Object>"))
                                               .setVars(types))
            .setOutType(Type.type(outClass));

        Evaluator<T, Void, R> evaluator = MVELCompiler.compile(eval.build());

        return  evaluator;
    }

    public <T, K, R> Evaluator<T, K, R> compilePojoEvaluator(EvaluatorInfo<T, K, R> info) {
        MVELCompiler MVELCompiler = new MVELCompiler();
        Evaluator<T, K, R> evaluator = MVELCompiler.compile(info);

        return  evaluator;
    }

//    public <T, R> RootObjectEvaluator<T,R> compileRootObjectEvaluator(final RootObjectValues values) {
//        String actualExpression = maybeWrap(values.expression());
//        MVELCompiler MVELCompiler = new MVELCompiler();
//        RootObjectEvaluator evaluator = MVELCompiler.compileRootObjectEvaluator(clsManager, actualExpression,
//                                                                                values.rootClass(), values.rootGenerics(), values.outClass(), values.outGenerics(),
//                                                                                values.imports(), MVELCompiler.getClass().getClassLoader());
//
//        return  evaluator;
//    }

    public Object executeExpression(final String expression, Set<String> imports, final Map<String, Object> vars) {
        return executeExpression(expression, imports, vars, Object.class);
    }

    public <R> R executeExpression(final String expr, Set<String> imports, final Map<String, Object> vars,  Class<R> outClass) {
        return executeExpression(expr, imports, new HashMap<>(), Type.type(outClass), vars);
    }

    public <R> R executeExpression(final String expr, Set<String> imports,
                                    final Map<String, Type<Object>> types,
                                    Type<R> outType,
                                    final Map<String, Object> vars) {


        for (Map.Entry<String, Object> o : vars.entrySet()) {
            if ( types.keySet().contains(o.getKey())) {
                Class type = o.getValue() == null ? Object.class : o.getValue().getClass();
                types.put(o.getKey(), Type.type(type));
            }
        }

        MVELCompiler MVELCompiler = new MVELCompiler();
        EvaluatorBuilder<Map<String, Object>, Void, R> eval = new EvaluatorBuilder<>();

        eval.setClassManager(clsManager).setClassLoader(ClassLoader.getSystemClassLoader())
            .setExpression(expr)
            .setImports(imports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(Map.class, "<String, Object>"))
                                               .setVars(types.values().toArray(new Declaration[0])))
            .setOutType(outType);

        Evaluator<Map<String, Object>, Void, R> evaluator = MVELCompiler.compile(eval.build());

        return evaluator.eval(vars);
    }

    private static String maybeWrap(String providedExpr) {
        String actualExpr = providedExpr;
        if ( !providedExpr.contains(";")) { // @TODO this is aa very crude sniff. Personally I'd rather not sniff and instead have two different method calls (mdp)
            //actualExpr = "{ return " + providedExpr + ";}";
            actualExpr = "return " + providedExpr + ";";
        }
        return actualExpr;
    }

    public static <T> T putMap(Map map, String key, T v) {
        map.put(key, v);
        return v;
    }

    public static <T> T setList(List list, int index, T v) {
        list.set(index, v);
        return v;
    }
}
