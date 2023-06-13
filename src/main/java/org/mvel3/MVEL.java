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

import java.util.HashMap;
import java.util.Map;

public class MVEL {
    ClassManager clsManager = new ClassManager();

    public MapEvaluator compileToMapEvaluator(final String expression, final Map<String, Class> types) {
        MVELCompiler MVELCompiler = new MVELCompiler();
        MapEvaluator mapEvaluator = MVELCompiler.compileAndLoad(clsManager, expression,
                                                                types, MVELCompiler.getClass().getClassLoader());

        return  mapEvaluator;
    }

    public <T> T executeExpression(final String expression, final Map<String, Object> vars) {
        Map<String, Class> types = new HashMap<>();
        for (Map.Entry<String, Object> o : vars.entrySet()) {
            Class type = o.getValue() == null ? Object.class : o.getValue().getClass();
            types.put(o.getKey(), type);
        }

        MapEvaluator mapEvaluator = compileToMapEvaluator(expression, types);

        return (T) mapEvaluator.eval(vars);
    }

}
