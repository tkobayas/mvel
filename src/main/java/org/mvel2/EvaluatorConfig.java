package org.mvel2;

import org.mvel3.ClassManager;

import java.util.Collections;
import java.util.Set;

public class EvaluatorConfig {

    public static abstract class BaseValuesBuilder<B extends BaseValuesBuilder> {
        private ClassLoader classLoader;

        private ClassManager classManager;

        private Set<String> imports = Collections.emptySet();

        public B classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return (B) this;
        }

        public B classManager(ClassManager classManager) {
            this.classManager = classManager;
            return (B) this;
        }

        public B imports(Set<String> imports) {
            this.imports = imports;
            return (B) this;
        }

        void build(BaseValues conf) {
            conf.classLoader = classLoader;
            conf.classManager = classManager;
            conf.imports = imports;
        }

    }

    public static abstract class BaseValues {
        private ClassLoader classLoader;

        private ClassManager classManager;

        private Set<String> imports;

        public ClassLoader classLoader() {
            return classLoader;
        }

        public ClassManager classManager() {
            return classManager;
        }

        public Set<String> imports() {
            return imports;
        }
    }

    //final String providedExpr, Set<String> imports, Class<T> contextClass, Class<R> returnClass, String... vars
    public static class ContextObjectValues extends BaseValues {
        private String expression;
        private Class contextClass;
        private String contextGenerics;
        private Class outClass;
        private String outGenerics;
        private String[] vars;

        public String expression() {
            return expression;
        }

        public Class contextClass() {
            return contextClass;
        }

        public String contextGenerics() {
            return contextGenerics;
        }

        public Class outClass() {
            return outClass;
        }

        public String outGenerics() {
            return outGenerics;
        }

        public String[] vars() {
            return vars;
        }
    }

    public static class ContextObjectValuesBuilder extends BaseValuesBuilder<ContextObjectValuesBuilder> {
        private static String[] EMPTY_VARS = new String[0];

        private String expression;
        private Class contextClass;
        String contextGenerics;
        private Class outClass;
        private String outGenerics;

        private String[] vars = EMPTY_VARS;

        public static ContextObjectValuesBuilder create() {
            return new ContextObjectValuesBuilder();
        }

        public ContextObjectValuesBuilder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public ContextObjectValuesBuilder context(Class contextClass, String contextGenerics) {
            this.contextClass = contextClass;
            this.contextGenerics = contextGenerics;
            return this;
        }

        public ContextObjectValuesBuilder contextClass(Class contextClass) {
            this.contextClass = contextClass;
            return this;
        }

        public ContextObjectValuesBuilder contextGenerics(String contextGenerics) {
            this.contextGenerics = contextGenerics;
            return this;
        }

        public ContextObjectValuesBuilder outClass(Class outClass) {
            this.outClass = outClass;
            return this;
        }

        public ContextObjectValuesBuilder outGenerics(String outGenerics) {
            this.outGenerics = outGenerics;
            return this;
        }

        public ContextObjectValuesBuilder vars(String... vars) {
            this.vars = vars;
            return this;
        }

        public ContextObjectValues build() {
            ContextObjectValues values = new ContextObjectValues();
            values.expression = expression;

            values.contextClass = contextClass;
            values.contextGenerics = contextGenerics;

            values.outClass = outClass;
            values.outGenerics = outGenerics;

            values.vars = vars;

            super.build(values);

            return values;
        }
    }

    public static class RootObjectConfig extends BaseValues {
        private String expression;
        private Class rootClass;
        String rootGenerics;
        private Class outClass;
        private String outGenerics;

        public String expression() {
            return expression;
        }

        public Class rootClass() {
            return rootClass;
        }

        public String rootGenerics() {
            return rootGenerics;
        }

        public Class outClass() {
            return outClass;
        }

        public String outGenerics() {
            return outGenerics;
        }
    }

    public static class RootObjectConfigBuilder extends BaseValuesBuilder<RootObjectConfigBuilder> {
        private String expression;
        private Class rootClass;
        String rootGenerics;
        private Class outClass;
        private String outGenerics;

        public static RootObjectConfigBuilder create() {
            return new RootObjectConfigBuilder();
        }

        public RootObjectConfigBuilder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public RootObjectConfigBuilder root(Class rootClass, String rootGenerics) {
            this.rootClass = rootClass;
            this.rootGenerics = rootGenerics;
            return this;
        }

        public RootObjectConfigBuilder rootClass(Class rootClass) {
            this.rootClass = rootClass;
            return this;
        }

        public RootObjectConfigBuilder rootGenerics(String rootGenerics) {
            this.rootGenerics = rootGenerics;
            return this;
        }

        public RootObjectConfigBuilder outClass(Class outClass) {
            this.outClass = outClass;
            return this;
        }

        public RootObjectConfigBuilder outGenerics(String outGenerics) {
            this.outGenerics = outGenerics;
            return this;
        }

        public RootObjectConfig build() {
            RootObjectConfig config = new RootObjectConfig();
            config.expression = expression;

            config.rootClass = rootClass;
            config.rootGenerics = rootGenerics;

            config.outClass = outClass;
            config.outGenerics = outGenerics;

            super.build(config);


            return config;

        }
    }


}
