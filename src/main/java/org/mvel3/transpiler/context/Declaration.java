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

import java.util.Objects;

public class Declaration {
    private String name;
    private Class<?> clazz;

    private String generics = "";

    public Declaration(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
        this.generics = "";
    }

    public Declaration(String name, Class<?> clazz, String generics) {
        this.name = name;
        this.clazz = clazz;
        this.generics = (generics != null) ? generics : "";
    }

    public String getName() {
        return name;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getGenerics() {
        return generics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Declaration that = (Declaration) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (!clazz.equals(that.clazz)) {
            return false;
        }
        return Objects.equals(generics, that.generics);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + clazz.hashCode();
        result = 31 * result + (generics != null ? generics.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Declaration{" +
               "name='" + name + '\'' +
               ", clazz=" + clazz +
               ", annotations='" + generics + '\'' +
               '}';
    }
}
