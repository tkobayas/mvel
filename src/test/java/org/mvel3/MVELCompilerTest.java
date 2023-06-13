package org.mvel3;

import org.junit.Test;
import org.mvel2.ParserContext;
import org.mvel3.transpiler.context.Declaration;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MVELCompilerTest {

    public static class ContextCamelCase {
        private Foo foo;
        private Bar bar;

        public ContextCamelCase(Foo foo, Bar bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public Foo getFoo() {
            return foo;
        }

        public Bar getBar() {
            return bar;
        }
    }

    public static class ContextRecord {
        private Foo foo;
        private Bar bar;

        public ContextRecord(Foo foo, Bar bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public Foo foo() {
            return foo;
        }

        public Bar bar() {
            return bar;
        }
    }

    public static class ContextMixed {
        private Foo foo;
        private Bar bar;

        public ContextMixed(Foo foo, Bar bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public Foo foo() {
            return foo;
        }

        public Bar getBar() {
            return bar;
        }
    }

    @Test
    public void testMapEvaluator() {
        Map<String, Class> types = new HashMap<>();
        types.put("foo", Foo.class);
        types.put("bar", Bar.class);

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("foo.getName() + bar.getName()", types);
        assertThat((String) evaluator.eval(vars)).isEqualTo("xxxyyy");
    }

    @Test
    public void testArrayEvaluator() {
        Declaration[] types = new Declaration[] {new Declaration("foo", Foo.class),
                                                 new Declaration("bar", Bar.class)};

        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        ArrayEvaluator evaluator = mvel.compileArrayEvaluator("foo.getName() + bar.getName()", types);
        assertThat((String) evaluator.eval(new Object[]{foo, bar})).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextCamelCaseEvaluator() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextCamelCase> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()",
                                                                              ContextCamelCase.class, new String[] {"foo", "bar"});

        ContextCamelCase context = new ContextCamelCase(foo, bar);
        assertThat((String) evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextRecordEvaluator() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextRecord> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()",
                                                                              ContextRecord.class, new String[] {"foo", "bar"});

        ContextRecord context = new ContextRecord(foo, bar);
        assertThat((String) evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextMixed() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextMixed> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()",
                                                                           ContextMixed.class, new String[] {"foo", "bar"});

        ContextMixed context = new ContextMixed(foo, bar);
        assertThat((String) evaluator.eval(context)).isEqualTo("xxxyyy");
    }
}
