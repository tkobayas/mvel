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

    public static class ContextWithInts {
        private int a;
        private int b;
        private int c;
        private int d;

        public ContextWithInts(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        public int getC() {
            return c;
        }

        public void setC(int c) {
            this.c = c;
        }

        public int getD() {
            return d;
        }

        public void setD(int d) {
            this.d = d;
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
        MapEvaluator evaluator = mvel.compileMapEvaluator("foo.getName() + bar.getName()", types, "foo", "bar");
        assertThat((String) evaluator.eval(vars)).isEqualTo("xxxyyy");
    }

    @Test
    public void testMapEvaluatorReturns() {
        Map<String, Class> types = new HashMap<>();
        types.put("a", int.class);
        types.put("b", int.class);
        types.put("c", int.class);
        types.put("d", int.class);

        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);
        vars.put("c", 3);
        vars.put("d", -1);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("a = 4; b = 5; c = 6; d = a + b + c;", types, "a", "d");
        assertThat((int) evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars.get("a")).isEqualTo(4); // updated
        assertThat(vars.get("b")).isEqualTo(2); // not updated
        assertThat(vars.get("c")).isEqualTo(3); // not updated
        assertThat(vars.get("d")).isEqualTo(15); // updated
    }

    @Test
    public void testMapEvalutorInputs() {
        Map<String, Class> types = new HashMap<>();
        types.put("a", int.class);
        types.put("b", int.class);
        //types.put("d", int.class);

        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("a = 4; b = 5; int c = 6; int d = a + b + c; return d;", types, "a", "d");
        assertThat((int) evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars.get("a")).isEqualTo(4); // updated
        assertThat(vars.get("b")).isEqualTo(2); // not updated
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
    public void testArrayEvaluatorReturns() {
        Declaration[] types = new Declaration[] {
            new Declaration("a", int.class),
            new Declaration("b", int.class),
            new Declaration("c", int.class),
            new Declaration("d", int.class)
        };

        Object[] vars =new Object[] { 1, 2, 3, -1};

        MVEL mvel = new MVEL();
        ArrayEvaluator evaluator = mvel.compileArrayEvaluator("a = 4; b = 5; c = 6; d = a + b + c;", types, "a", "d");
        assertThat((int) evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars[0]).isEqualTo(4); // updated
        assertThat(vars[1]).isEqualTo(2); // not updated
        assertThat(vars[2]).isEqualTo(3); // not updated
        assertThat(vars[3]).isEqualTo(15); // updated
    }

    @Test
    public void testPojoContextCamelCaseEvaluator() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextCamelCase, String> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()",
                                                                                       ContextCamelCase.class, String.class, new String[] {"foo", "bar"});

        ContextCamelCase context = new ContextCamelCase(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextRecordEvaluator() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextRecord, String> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()",
                                                                                   ContextRecord.class, String.class, new String[] {"foo", "bar"});

        ContextRecord context = new ContextRecord(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextMixed() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextMixed, String> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()",
                                                                                    ContextMixed.class, String.class, "foo", "bar");

        ContextMixed context = new ContextMixed(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoEvaluatorReturns() {
//        ContextWithInts context = new ContextWithInts(1, 2, 3, -4);
//
//
//        MVEL mvel = new MVEL();
//        PojoEvaluator<ContextWithInts, Integer> evaluator = mvel.compilePojoEvaluator("a = 4; b = 5; c = 6; d = a + b + c;",
//                                                                                      ContextWithInts.class, Integer.class,
//                                                                                      "a", "d");
//        assertThat((int) evaluator.eval(context)).isEqualTo(15);
//
//        assertThat(context.getA()).isEqualTo(4); // updated
//        assertThat(context.getB()).isEqualTo(2); // not updated
//        assertThat(context.getC()).isEqualTo(3); // not updated
//        assertThat(context.getD()).isEqualTo(15); // updated
    }
}
