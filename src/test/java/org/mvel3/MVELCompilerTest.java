package org.mvel3;

import org.junit.Test;
import org.mvel2.ParserContext;
import org.mvel3.MVEL.Type;
import org.mvel3.transpiler.context.Declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MVELCompilerTest {

    public static class ContextCamelCase {
        private Foo foo;
        private Bar bar;

        private List<Foo> foos;

        public ContextCamelCase(Foo foo, Bar bar, Foo... foos) {
            this.foo = foo;
            this.bar = bar;
            this.foos = new ArrayList<>();
            this.foos.addAll(Arrays.asList(foos));
        }

        public Foo getFoo() {
            return foo;
        }

        public Bar getBar() {
            return bar;
        }

        public List<Foo> getFoos() {
            return foos;
        }
    }

    public static class ContextRecord {
        private Foo foo;
        private Bar bar;

        private List<Foo> foos;

        public ContextRecord(Foo foo, Bar bar, Foo... foos) {
            this.foo = foo;
            this.bar = bar;
            this.foos = new ArrayList<>();
            this.foos.addAll(Arrays.asList(foos));
        }

        public Foo foo() {
            return foo;
        }

        public Bar bar() {
            return bar;
        }

        public List<Foo> foos() {
            return foos;
        }
    }

    public static class ContextMixed {
        private Foo foo;
        private Bar bar;

        private List<Foo> foos;

        public ContextMixed(Foo foo, Bar bar, Foo... foos) {
            this.foo = foo;
            this.bar = bar;
            this.foos = new ArrayList<>();
            this.foos.addAll(Arrays.asList(foos));
        }

        public Foo foo() {
            return foo;
        }

        public Bar getBar() {
            return bar;
        }

        public List<Foo> foos() {
            return foos;
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
        Map<String, Type> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("foo.getName() + bar.getName()", getImports(), types, "foo", "bar");
        assertThat((String) evaluator.eval(vars)).isEqualTo("xxxyyy");
    }

    @Test
    public void testMapEvaluatorWithGenerics() {
        Map<String, Type> types = new HashMap<>();
        types.put("foos", Type.type(List.class, "<Foo>"));

        Foo foo1 = new Foo();
        foo1.setName("foo1");

        Foo foo2 = new Foo();
        foo2.setName("foo2");

        List<Foo> foos = new ArrayList<>();
        foos.add(foo1);
        foos.add(foo2);

        Map<String, Object> vars = new HashMap<>();
        vars.put("foos", foos);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("foos[0].name + foos[1].name", getImports(), types, "foos");
        assertThat((String) evaluator.eval(vars)).isEqualTo("foo1foo2");
    }

    @Test
    public void testMapEvaluatorReturns() {
        Map<String, Type> types = new HashMap<>();
        types.put("a", Type.type(int.class));
        types.put("b", Type.type(int.class));
        types.put("c", Type.type(int.class));
        types.put("d", Type.type(int.class));

        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);
        vars.put("c", 3);
        vars.put("d", -1);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("a = 4; b = 5; c = 6; d = a + b + c;", getImports(), types, "a", "d");
        assertThat((int) evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars.get("a")).isEqualTo(4); // updated
        assertThat(vars.get("b")).isEqualTo(2); // not updated
        assertThat(vars.get("c")).isEqualTo(3); // not updated
        assertThat(vars.get("d")).isEqualTo(15); // updated
    }

    @Test
    public void testMapEvalutorInputs() {
        Map<String, Type> types = new HashMap<>();
        types.put("a", Type.type(int.class));
        types.put("b", Type.type(int.class));
        //types.put("d", int.class);

        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);

        MVEL mvel = new MVEL();
        MapEvaluator evaluator = mvel.compileMapEvaluator("a = 4; b = 5; int c = 6; int d = a + b + c; return d;", getImports(), types, "a", "d");
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
        ArrayEvaluator evaluator = mvel.compileArrayEvaluator("foo.getName() + bar.getName()", getImports(), types);
        assertThat((String) evaluator.eval(new Object[]{foo, bar})).isEqualTo("xxxyyy");
    }

    @Test
    public void testArrayEvaluatorWithGenerics() {
        Declaration[] types = new Declaration[] {new Declaration("foos", List.class, "<Foo>")};

        Foo foo1 = new Foo();
        foo1.setName("foo1");

        Foo foo2 = new Foo();
        foo2.setName("foo2");

        List<Foo> foos = new ArrayList<>();
        foos.add(foo1);
        foos.add(foo2);

        MVEL mvel = new MVEL();
        ArrayEvaluator evaluator = mvel.compileArrayEvaluator("foos[0].name", getImports(), types);
        assertThat((String) evaluator.eval(new Object[]{foos})).isEqualTo("foo1");
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
        ArrayEvaluator evaluator = mvel.compileArrayEvaluator("a = 4; b = 5; c = 6; d = a + b + c;", getImports(), types, "a", "d");
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
        PojoEvaluator<ContextCamelCase, String> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()", getImports(),
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
        PojoEvaluator<ContextRecord, String> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()", getImports(),
                                                                                   ContextRecord.class, String.class, new String[] {"foo", "bar"});

        ContextRecord context = new ContextRecord(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextRecordEvaluatorWithGenerics() {
        Foo foo1 = new Foo();
        foo1.setName("foo1");

        Foo foo2 = new Foo();
        foo2.setName("foo2");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextRecord, String> evaluator = mvel.compilePojoEvaluator("foos[0].name + foos[1].name", getImports(),
                                                                                   ContextRecord.class, String.class, "foos");

        ContextRecord context = new ContextRecord(null, null, foo1, foo2);
        assertThat(evaluator.eval(context)).isEqualTo("foo1foo2");
    }

    @Test
    public void testPojoContextMixed() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        PojoEvaluator<ContextMixed, String> evaluator = mvel.compilePojoEvaluator("foo.getName() + bar.getName()", getImports(),
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


    public static Set<String> getImports() {

        Set<String> imports = new HashSet<>();
        imports.add("java.util.List");
        imports.add("java.util.ArrayList");
        imports.add("java.util.HashMap");
        imports.add("java.util.Map");
        imports.add("java.math.BigDecimal");
        imports.add(org.mvel3.Address.class.getCanonicalName());
        imports.add(Foo.class.getCanonicalName());

        return imports;
    }
}
