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

import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.parser.printer.CoerceRewriter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.javaparser.Providers.provider;
import static org.assertj.core.api.Assertions.assertThat;

public class MVELTranspilerTest implements TranspilerTest {

    public List<String> getX() {
        return null;
    }

    @Test
    public void testAssignmentIncrement() {
        test(ctx -> ctx.addDeclaration("i", Integer.class),
             "i += 10;",
             "context.put(\"i\", i += 10);");
    }

    @Test
    public void testInlineCast1() {
        test(ctx -> ctx.addDeclaration("l", List.class),
             "l#ArrayList#removeRange(0, 10);",
             "((ArrayList)l).removeRange(0, 10);");
    }

    @Test
    public void testInlineCast2() {
        test(ctx -> ctx.addDeclaration("l", List.class),
             "l#java.util.ArrayList#removeRange(0, 10);",
             "((java.util.ArrayList)l).removeRange(0, 10);");
    }

    @Test
    public void testInlineCast3() {
        test(ctx -> ctx.addDeclaration("l", List.class),
             "l#ArrayList#[0];",
             "((ArrayList)l).get(0);");
    }

    @Test
    public void testInlineCoercion4() {
        test(ctx -> {
            ctx.addDeclaration("l", Long.class);
            ctx.addImport(java.util.Date.class.getCanonicalName());},
             "var x = l#Date#;",
             "var x = new java.util.Date(l);");
    }

    @Test
    public void testInlineCoercion5() {
        test(ctx -> {
                 ctx.addDeclaration("l", Long.class);
                 ctx.addImport(java.util.Date.class.getCanonicalName());},
             "var x = l#Date#getTime();",
             "var x = new java.util.Date(l).getTime();");
    }


    @Test
    public void testAssignmentIncrementInFieldWithPrimitive() {
        test(ctx -> ctx.addDeclaration("p", Person.class),
             "p.age += 10;",
             "p.setAge(p.getAge() + 10);");
    }


    @Test
    public void testConvertPropertyToAccessor() {
        String expectedJavaCode = "$p.getParent().getParent().getName();";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.parent.getParent().name;",
             expectedJavaCode);

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.getParent().parent.name;",
             expectedJavaCode);

        test(ctx ->
                 ctx.addDeclaration("$p", Person.class),
             "$p.parent.parent.name;",
             expectedJavaCode);

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.getParent().getParent().getName();",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorForEach() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "for(var a: $p.addresses){\n" +
             "  results.add(a.city);\n" +
             "}\n",
             "for (var a : $p.getAddresses()) {\n" +
             "  results.add(a.getCity());\n" +
             "}\n");
    }

    @Test
    public void testGenericsOnListAccess() {
        // The city rewrite wouldn't work, if it didn't know the generics
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.addresses[0].city + $p.addresses[1].city;",
             "$p.getAddresses().get(0).getCity() + $p.getAddresses().get(1).getCity();");
    }

    @Test
    public void testConvertIfConditionAndStatements() {
        String expectedJavaCode =  "if ($p.getAddresses() != null) {\n" +
                "  results.add($p.getName());\n" +
                "} else {\n" +
                "results.add($p.getAge());\n" +
                "}\n";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "if($p.addresses != null){\n" +
                     "  results.add($p.name);\n" +
                     "} else {\n " +
                     "  results.add($p.age);" +
                     "}",
             expectedJavaCode);
    }

    @Test
    public void testPrimitiveWithBigDecimal() {
        for(Primitive p : CoerceRewriter.floatPrimitives) {
            test("var x = 10B * " + p.toBoxedType() + ".MAX_VALUE;",
                 "var x = new java.math.BigDecimal(\"10\").multiply( BigDecimal.valueOf(" + p.toBoxedType() + ".MAX_VALUE), java.math.MathContext.DECIMAL128);");
        }
    }

    @Test
    public void testBoxTypeWithBigDecimal() {
        for(Primitive p : CoerceRewriter.floatPrimitives) {
            test("var x = 10B * " + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE);",
                 "var x = new java.math.BigDecimal(\"10\").multiply( BigDecimal.valueOf(" + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE)), java.math.MathContext.DECIMAL128);");
        }
    }

    @Test
    public void testBoxTypeWithBigInteger() {
        for(Primitive p : CoerceRewriter.integerPrimitives) {
            test(ctx -> {},
                 "var x = 10I * " + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE);",
                 "var x = new java.math.BigInteger(\"10\").multiply( BigInteger.valueOf(" + p.toBoxedType() + ".valueOf(" + p.toBoxedType() + ".MAX_VALUE)));");
        }
    }

    @Test
    public void testPrimitiveWithBigInteger() {
        for(Primitive p : CoerceRewriter.integerPrimitives) {
            test(ctx -> {},
                 "var x = 10I * " + p.toBoxedType() + ".MAX_VALUE;",
                 "var x = new java.math.BigInteger(\"10\").multiply( BigInteger.valueOf(" + p.toBoxedType() + ".MAX_VALUE));");
        }
    }

    @Test // I changed this, to avoid implicit narrowing (mdp)
    public void testPromoteBigDecimalToIntValueInsideIf() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("$m", BigDecimal.class);
             },
             "if($p.isEven($p.salary.intValue()) && $p.isEven($m.intValue())){}\n",
             "    if ($p.isEven($p.getSalary().intValue()) && $p.isEven($m.intValue())) {}\n");
    }

    @Test // I changed this, to avoid implicit narrowing (mdp)
    public void testPromoteBigDecimalToIntValueInsideIfWithStaticMethod() {
        test(ctx -> {
                 ctx.addDeclaration("$m", BigDecimal.class);
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addStaticImport(Person.class.getCanonicalName() + ".isEven");
             },
             "if(isEven($p.salary.intValue()) && isEven($m.intValue())){} ",
             "    if (isEven($p.getSalary().intValue()) && isEven($m.intValue())) {}\n");
    }


    @Test
    public void testConvertPropertyToAccessorWhile() {
        String expectedJavaCode =  "while ($p.getAddresses() != null) {\n" +
                "  results.add($p.getName());\n" +
                "}\n";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "while($p.addresses != null){" +
                     "  results.add($p.name);\n" +
                     "}",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorDoWhile() {
        String expectedJavaCode =  "do {\n" +
                "  results.add($p.getName());\n" +
                "} while ($p.getAddresses() != null);\n";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "do {\n" +
                     "  results.add($p.name);\n" +
                     "} while($p.addresses != null);",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorFor() {
        String expectedJavaCode =  "for (int i = 0; i < $p.getAddresses(); i++) {\n" +
                "  results.add($p.getName());\n" +
                "}";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "for(int i = 0; i < $p.addresses; i++) {\n" +
                     "  results.add($p.name);\n" +
                     "} ",
             expectedJavaCode);
    }

    @Test
    public void testConvertPropertyToAccessorSwitch() {
        String expectedJavaCode =
                "        switch($p.getName()) {\n" +
                "            case \"Luca\":\n" +
                "                results.add($p.getName());\n" +
                "}";

        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "        switch($p.name) {\n" +
                     "            case \"Luca\":\n" +
                     "                results.add($p.name);\n" +
                     "}",
             expectedJavaCode);
    }

    @Test
    public void testAccessorInArguments() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "insert(\"Modified person age to 1 for: \" + $p.name);",
             "insert(\"Modified person age to 1 for: \" + $p.getName());");
    }

    @Test
    public void testEnumField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "int key = $p.gender.getKey();",
             "int key = $p.getGender().getKey();");
    }

    @Test
    public void testEnumConstant() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "int key = Gender.FEMALE.getKey();",
             "int key = Gender.FEMALE.getKey();");
    }

    @Test
    public void testPublicField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.parentPublic.getParent().name;",
             "$p.parentPublic.getParent().getName();");

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.getParent().parentPublic.name;",
             "$p.getParent().parentPublic.getName();");
    }

    @Test
    public void testUncompiledMethod() {
        test("System.out.println(\"Hello World\");",
             "System.out.println(\"Hello World\");");
    }

    @Test
    public void testStringLength() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name.length;",
             "$p.getName().length();");
    }

    @Test
    public void testAssignment() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "Person np = $p; np = $p;",
             "Person np = $p; np = $p;");
    }

    @Test
    public void testAssignmentUndeclared() {
        // This use to test that it wuold add in a type declaration.
        // However this functionality has been dropped. Users must declare type of use 'var', for the first time a var is used.
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "np = $p;",
             "np = $p;");
    }

    @Test
    public void testSetter() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = \"Luca\";",
             "$p.setName(\"Luca\");");
    }

    @Test
    public void testBoxingSetter() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.ageAsInteger = 20;",
             "$p.setAgeAsInteger(20);");
    }

    @Test
    public void testSetterBigDecimal() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = $p.salary + 50000;",
             "$p.setSalary($p.getSalary().add(BigDecimal.valueOf(50000), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testSetterBigDecimalConstant() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = 50000;",
             "$p.setSalary(BigDecimal.valueOf(50000));");
    }

    @Test
    public void testSetterBigDecimalConstantFromLong() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary = 50000L;",
             "$p.setSalary(BigDecimal.valueOf(50000L));");
    }

    @Test @Ignore("String coercion")
    public void testSetterStringWithBigDecimal() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = BigDecimal.valueOf(1);",
             "$p.setName((BigDecimal.valueOf(1)).toString());");
    }

    @Test @Ignore("String coercion")
    public void testSetterStringWithBigDecimalFromField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.name = $p.salary;",
             "$p.setName(($p.getSalary()).toString());");
    }

    @Test @Ignore("String coercion")
    public void testSetterStringWithBigDecimalFromVariable() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("$m", BigDecimal.class);
             },
             "$p.name = $m;",
             "$p.setName(($m).toString());");
    }

    @Test @Ignore("String coercion")
    public void testSetterWithBigDecimalFromBigDecimalLiteral() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = 10000B;",
             "$p.setName((BigDecimal.valueOf(10000)).toString());");
    }

    @Test @Ignore("String coercion")
    public void testSetterStringWithBigDecimalFromBigDecimalLiteral() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = 10000B;",
             "$p.setName((BigDecimal.valueOf(10000)).toString());");
    }

    @Test @Ignore("String coercion")
    public void testSetterStringWithBigDecimalFromBigDecimalConstant() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = BigDecimal.ZERO;",
             "$p.setName((BigDecimal.ZERO).toString());");
    }

    @Test
    public void testSetterStringWithNull() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.name = null;",
             "$p.setName(null);");
    }

    @Test
    public void testSetterBigDecimalConstantModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { salary = 50000 };",
             "$p.setSalary(new java.math.BigDecimal(50000)); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testSetterBigDecimalLiteralModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { salary = 50000B };",
             "$p.setSalary(new java.math.BigDecimal(\"50000\")); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testSetterBigDecimalLiteralModifyNegative() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { salary = -50000B };",
             "$p.setSalary(new java.math.BigDecimal(\"-50000\")); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testBigDecimalModulo() {
        test(ctx -> ctx.addDeclaration("$b1", BigDecimal.class),
             "java.math.BigDecimal result = $b1 % 2;",
             "java.math.BigDecimal result = $b1.remainder(BigDecimal.valueOf(2), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalModuloPromotion() {
        test("BigDecimal result = 12 % 10;",
             "BigDecimal result = BigDecimal.valueOf(12 % 10);");
    }

    @Test
    public void testBigDecimalModuloWithOtherBigDecimal() {
        test(ctx -> {
                 ctx.addDeclaration("$b1", BigDecimal.class);
                 ctx.addDeclaration("$b2", BigDecimal.class);
             },
             "java.math.BigDecimal result = $b1 % $b2;",
             "java.math.BigDecimal result = $b1.remainder($b2, java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalModuloOperationSumMultiply() {
        test(ctx -> {
                 ctx.addDeclaration("bd1", BigDecimal.class);
                 ctx.addDeclaration("bd2", BigDecimal.class);
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.salary = $p.salary + (bd1.multiply(bd2));",
             "$p.setSalary($p.getSalary().add(bd1.multiply(bd2), java.math.MathContext.DECIMAL128));\n");
    }

    @Test
    public void testDoNotConvertAdditionInStringConcatenation() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                          "     list.add(\"before \" + $p + \", money = \" + $p.salary); " +
                          "     modify ( $p )  { salary = 50000 };  " +
                          "     list.add(\"after \" + $p + \", money = \" + $p.salary); ",
                         "      list.add(\"before \" + $p + \", money = \" + $p.getSalary()); " +
                         "      $p.setSalary(new java.math.BigDecimal(50000));" +
                         "      list.add(\"after \" + $p + \", money = \" + $p.getSalary()); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testBigIntegerOperatorsWithDeclareRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> ctx.addDeclaration("p", Person.class),
                 "var x = 10" + op + "p.ageAsBigInteger;",
                 "var x = p.getAgeAsBigInteger()." + method + "(BigInteger.valueOf(10));"
                );

        }
    }

    @Test
    public void testBigIntegerAssignOperatorsWithFieldAccessRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("p", Person.class);
                 },
                 "p.ageAsBigInteger " + op + "= 10;",
                 "p.setAgeAsBigInteger( p.getAgeAsBigInteger()." + method + "(BigInteger.valueOf(10)));"
                );

        }
    }

    @Test
    public void testBigIntegerAssignOperatorsWithMapRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("p", Person.class);
                 },
                 "p.bigIntegerMap[\"k1\"] " + op + "= 10;",
                 "p.getBigIntegerMap().put( \"k1\", p.getBigIntegerMap().get(\"k1\")." +
                 method + "(BigInteger.valueOf(10)));"
                );

        }
    }

    @Test
    public void testBigIntegerAssignOperatorsWithVarRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("b", BigDecimal.class);
                 },
                 "b " + op + "= 10;",
                 "context.put(\"b\", b = b." + method + "(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128));"
                );

        }
    }

    @Test
    public void testBigDecimalOperatorsWithRewrite() {
        for(String op : new String[] {"+", "-", "*", "/"} ) {
            String method = null;

            switch (op) {
                case "+" : method = "add"; break;
                case "-" : method = "subtract"; break;
                case "*" : method = "multiply"; break;
                case "/" : method = "divide"; break;
            }

            test(ctx -> {
                     ctx.addDeclaration("p", Person.class);
                 },
                 "var x = 10" + op + "p.salary;",
                 "var x = p.getSalary()." + method + "(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);"
                );
        }
    }

    @Test
    public void testDeclationExpressionBigIntegerLiteral() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
             },
             "$p.ageAsBigInteger = 10000I;",
             "$p.setAgeAsBigInteger(new java.math.BigInteger(\"10000\"));");
    }

    @Test
    public void testDeclationExpressionWithBigIntegerLiteral1() {
        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = 10*p.parentPublic.ageAsBigInteger;",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(BigInteger.valueOf(10));"
             );

        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = p.parentPublic.ageAsBigInteger*10;",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(BigInteger.valueOf(10));"
            );
    }

    @Test
    public void testDeclationExpressionWithBigIntegerLiteral2() {
        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = (10+10)*p.parentPublic.ageAsBigInteger;",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(BigInteger.valueOf(10+10));"
            );

        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = p.parentPublic.ageAsBigInteger*(10+10);",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(BigInteger.valueOf(10+10));"
            );
    }

    @Test
    public void testDeclationExpressionWithBigIntegerLiteral3() {
        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = 10I*p.parentPublic.ageAsBigInteger;",
             "var x = new java.math.BigInteger(\"10\").multiply(p.parentPublic.getAgeAsBigInteger());"
            );

        test(ctx -> {
                 ctx.addDeclaration("p", Person.class);
             },
             "var x = p.parentPublic.ageAsBigInteger*10I;",
             "var x = p.parentPublic.getAgeAsBigInteger().multiply(new java.math.BigInteger(\"10\"));"
            );
    }

    @Test
    public void testSetterPublicField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.nickName = \"Luca\";",
             "$p.nickName = \"Luca\";");
    }

    @Test @Ignore // ';' no longer optional
    public void withoutSemicolonAndComment() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "delete($person) // some comment\n" +
                     "delete($pet) // another comment\n",
             "delete($person);\n" +
                     "delete($pet);");
    }

    @Test
    public void testInitializerArrayAccess() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "var l = new ArrayList(); " +
                     "l.add(\"first\"); " +
                     "System.out.println(l[0]);",
             "var l = new ArrayList(); " +
                     "l.add(\"first\"); " +
                     "System.out.println(l.get(0));");
    }


    @Test
    public void testMapGet() {
        test(ctx -> ctx.addDeclaration("m", Map.class),
             "m[\"key\"];",
             "" +
                     "m.get(\"key\");\n" +
                     "");
    }

    @Test
    public void testMapGetAsField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key3\"];",
             "$p.getItems().get(\"key3\");");
    }

    @Test
    public void testMapGetInMethodCall() {
        test(ctx -> ctx.addDeclaration("m", Map.class),
             "System.out.println(m[\"key\"]);",
             "System.out.println(m.get(\"key\"));");
    }




    @Test
    public void testMapSet() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key3\"] = \"value3\";",
             "$p.getItems().put(\"key3\", \"value3\");");
    }

    @Test
    public void testListSet() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.streets[2] = \"value3\";",
             "$p.getStreets().set(2, \"value3\");");
    }

    @Test
    public void testArraySet() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.roads[2] = \"value3\";",
             "$p.getRoads()[2] = \"value3\";");
    }

    @Test
    public void testMapSetWithVariable() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "String key3 = \"key3\";\n" +
                     "$p.items[key3] = \"value3\";",
             "String key3 = \"key3\";\n" +
                     "$p.getItems().put(key3, \"value3\");");
    }

    @Test
    public void testMapSetWithConstant() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key3\"] = \"value3\";",
             "$p.getItems().put(\"key3\", \"value3\");");
    }

    @Test
    public void testMapSetWithVariableCoercionString() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key\"] = 2;",
             "$p.getItems().put(\"key\", java.lang.String.valueOf(2));");
    }

    @Test
    public void testMapPutWithVariableCoercionString() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.items[\"key\"] = 2;",
             "$p.getItems().put(\"key\", java.lang.String.valueOf(2));");
    }

    @Test
    public void testMapSetWithMapGetAsValue() {
        test(ctx -> {
                 ctx.addDeclaration("$p", Person.class);
                 ctx.addDeclaration("s", String.class);
                 ctx.addDeclaration("map", Map.class, "<String, Integer>");
             },
             "$p.items[\"key4\"] = map[s];",
             "$p.getItems().put(\"key4\", java.lang.String.valueOf(map.get(s)));");
    }

    @Test
    public void testMapSetToNewMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "Map<String, String> newhashmap = new HashMap<>();\n" +
                     "$p.items = newhashmap;\n",
                     "Map<String, String> newhashmap = new HashMap<>(); \n" +
                     "$p.setItems(newhashmap);");
    }

    @Test
    public void testInitializerMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "var m = new HashMap();\n" +
                     "m.put(\"key\", 2);\n" +
                     "System.out.println(m[\"key\"]);",
                     "var  m = new HashMap();\n" +
                     "m.put(\"key\", 2);\n" +
                     "System.out.println(m.get(\"key\"));");
    }

    @Test
    public void testMixArrayMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),

                     "    var m = new HashMap<String, List>();\n" +
                     "    var l = new ArrayList<String>();\n" +
                     "    l.add(\"first\");\n" +
                     "    m.put(\"content\", l);\n" +
                     "    System.out.println(m[\"content\"][0]);\n" +
                     "    list.add(m[\"content\"][0]);",

                     "    var m = new HashMap<String, List>();\n" +
                     "    var l = new ArrayList<String>();\n" +
                     "    l.add(\"first\");\n" +
                     "    m.put(\"content\", l);\n" +
                     "    System.out.println(m.get(\"content\").get(0));\n" +
                     "    list.add(m.get(\"content\").get(0));");
    }

    @Test
    public void testBigDecimal() {
        test(
                     "    BigDecimal sum = 0;\n" +
                     "    BigDecimal money = 10;\n" +
                     "    sum += money;\n" +
                     "    sum -= money;",

                     "   BigDecimal sum = BigDecimal.valueOf(0);\n" +
                     "   BigDecimal money =  BigDecimal.valueOf(10);\n" +
                     "   sum = sum.add(money, java.math.MathContext.DECIMAL128);\n" +
                     "   sum = sum.subtract(money, java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void bigDecimalLessThan() {
        test("    BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero < ten) {}",
             "BigDecimal zero = BigDecimal.valueOf(0);\n" +
                     "    BigDecimal ten = BigDecimal.valueOf(10);\n" +
                     "    if (zero.compareTo(ten) < 0) {}");
    }

    @Test
    public void bigDecimalLessThanOrEqual() {
        test("BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero <= ten) {}",
             "BigDecimal zero = BigDecimal.valueOf(0);\n" +
                     "    BigDecimal ten = BigDecimal.valueOf(10);\n" +
                     "    if (zero.compareTo(ten) <= 0) {}");
    }

    @Test
    public void bigDecimalGreaterThan() {
        test("BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero > ten) {}",
             "BigDecimal zero = BigDecimal.valueOf(0);\n" +
                     "    BigDecimal ten = BigDecimal.valueOf(10);\n" +
                     "    if (zero.compareTo(ten) > 0) {}\n");
    }

    @Test
    public void bigDecimalGreaterThanOrEqual() {
        test("BigDecimal zero = 0;\n" +
                     "    BigDecimal ten = 10;\n" +
                     "    if(zero >= ten) {}",
             "BigDecimal zero = BigDecimal.valueOf(0);\n" +
                     "    BigDecimal ten = BigDecimal.valueOf(10);\n" +
                     "    if (zero.compareTo(ten) >= 0) {}");
    }

    @Test
    public void bigDecimalEquals() {
        test("    BigDecimal zero = 0;\n" +
                     "    if(zero == 23) {}\n",
             "BigDecimal zero = BigDecimal.valueOf(0);\n" +
                     "    if (zero.compareTo(BigDecimal.valueOf(23)) == 0) {}");
    }

    @Test
    public void bigDecimalNotEquals() {
        test("BigDecimal zero = 0;\n" +
                     "    if(zero != 23) {}",
             "BigDecimal zero = BigDecimal.valueOf(0);\n" +
                     "    if (zero.compareTo(BigDecimal.valueOf(23)) != 0) {}");
    }

    @Test
    public void testBigDecimalCompoundOperatorOnField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary += 50000B;",
             "$p.setSalary($p.getSalary().add(new java.math.BigDecimal(\"50000\"), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testBigDecimalCompoundOperatorWithOnField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "$p.salary += $p.salary;",
             "$p.setSalary($p.getSalary().add($p.getSalary(), java.math.MathContext.DECIMAL128));");
    }

    @Test
    public void testBigDecimalArithmetic() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "java.math.BigDecimal operation = $p.salary + $p.salary;",
             "java.math.BigDecimal operation = $p.getSalary().add($p.getSalary(), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalArithmeticWithConversionLiteral() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "java.math.BigDecimal operation = $p.salary + 10B;",
             "java.math.BigDecimal operation = $p.getSalary().add(new java.math.BigDecimal(\"10\"), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalArithmeticWithConversionFromInteger() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "java.math.BigDecimal operation = $p.salary + 10;",
             "java.math.BigDecimal operation = $p.getSalary().add(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testBigDecimalPromotionAllFourOperations() {

        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "    BigDecimal result = 0B;" +
                     "    result += 50000;\n" +
                     "    result -= 10000;\n" +
                     "    result /= 10;\n" +
                     "    result *= 10;\n" +
                     "    result *= $p.salary;\n" +
                     "    $p.salary = result;",
             "BigDecimal result = new java.math.BigDecimal(\"0\");\n" +
                     "        result = result.add(BigDecimal.valueOf(50000), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.subtract(BigDecimal.valueOf(10000), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.divide(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.multiply(BigDecimal.valueOf(10), java.math.MathContext.DECIMAL128);\n" +
                     "        result = result.multiply($p.getSalary(), java.math.MathContext.DECIMAL128);\n" +
                     "        $p.setSalary(result);");
    }

    @Test
    public void testPromotionOfIntToBigDecimal() {
        test( "    BigDecimal result = 0B;" +
                     "    int anotherVariable = 20;" +
                     "    result += anotherVariable;",
             "BigDecimal result = new java.math.BigDecimal(\"0\");\n" +
                     "     int anotherVariable = 20;\n" +
                     "     result = result.add(BigDecimal.valueOf(anotherVariable), java.math.MathContext.DECIMAL128);");
    }

    @Test
    public void testPromotionOfIntToBigDecimalOnField() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "    int anotherVariable = 20;" +
                     "    $p.salary += anotherVariable;",
             "" +
                     "        int anotherVariable = 20;\n" +
                     "        $p.setSalary($p.getSalary().add(BigDecimal.valueOf(anotherVariable), java.math.MathContext.DECIMAL128));\n" +
                     "");
    }

    @Test
    public void testModify() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { name = \"Luca\", age = 35 };",
             "$p.setName(\"Luca\");\n $p.setAge(35);\n",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyMap() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify ( $p )  { items = $p2.items };",
             "$p.setItems($p2.getItems());\n",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p", "$p2"));
    }

    @Test
    public void testModifySemiColon() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify($p) { setAge(1); };",
             "{ $p.setAge(1); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyWithAssignment() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify($p) { age = $p.age+1 };",
             "{ $p.setAge($p.getAge() + 1); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyWithMethodCall() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
             "modify($p) { addresses.clear() };",
             "{ $p.getAddresses().clear(); ",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testAddCastToMapGet() {
        test(ctx -> ctx.addDeclaration("map", Map.class, "<String, java.util.Map>"),
             "Map pMap = map.get(\"whatever\");",
             "Map pMap = map.get(\"whatever\");");
    }

    @Test
    public void testAddCastToMapGetOfDeclaration() {
        test(ctx -> {
                 ctx.addDeclaration("map", Map.class, "<String, java.util.Map>");
                 ctx.addDeclaration("$p", Person.class);
             },
             "Map pMap = map.get( $p.name );",
             "Map pMap = map.get($p.getName() );");
    }

    @Test
    public void testSimpleVariableDeclaration() {
        test("int i;",
             "int i;");
    }

    @Test
    public void testModifyInsideIfBlock() {
        test(ctx -> ctx.addDeclaration("$p", Person.class),
                     "         if ($p.getParent() != null) {\n" +
                     "              $p.setName(\"with_parent\");\n" +
                     "         } else {\n" +
                         "         modify ($p) {\n" +
                         "            name = \"without_parent\"" +
                         "         }\n" +
                     "         " +
                     "      ",
                     "  if ($p.getParent() != null) { " +
                     "      $p.setName(\"with_parent\"); " +
                     "  } else {\n " +
                     "      {\n" +
                     "          $p.setName(\"without_parent\");\n" +
                     "      }\n",
             result -> assertThat(allUsedBindings(result)).containsExactlyInAnyOrder("$p"));
    }

    @Test
    public void testModifyOrdering() {
        test(ctx -> ctx.addDeclaration("$person", Person.class),
                     "        Address $newAddress = new Address();\n" +
                     "        $newAddress.setCity( \"Brno\" );\n" +
                     "        insert( $newAddress );\n" +
                     "        modify( $person ) {\n" +
                     "          setAddress( $newAddress )\n" +
                     "        }",
             "Addresss $newAddress = new Address(); " +
             "$newAddress.setCity(\"Brno\"); " +
             "insert($newAddress);\n" +
             "  $person.setAddress($newAddress);\n");
    }

    @Test
    public void testSetterOnVarRewrite() {
        test(
             "    Person p = new Person(\"yoda\");\n" +
             "    p.age = 100; \n",
             "    Person p = new Person(\"yoda\");\n" +
             "    p.setAge(100); \n"
            );
    }

    @Test
    public void testSetterOnInferredVarRewrite() {
        test("    var p = new Person(\"yoda\");\n" +
                "    p.age = 100; \n",
                "    var p = new Person(\"yoda\");\n" +
                "    p.setAge(100); \n"
            );
    }

    @Test
    public void testSetterOnMethodReturnRewrite() {
        test(
                MVELTranspilerTest.class.getCanonicalName() + ".createPerson(\"yoda\").age = 100;\n",
                 "    " + MVELTranspilerTest.class.getCanonicalName() + ".createPerson(\"yoda\").setAge(100);\n"
            );
    }

    @Test
    public void testGetterRewriteOnAssign() {
        test(
                "    var p = new Person(\"yoda\");\n" +
                "    p.age = 100; \n" +
                "    int a = p.age; \n",
                "    var p = new Person(\"yoda\");\n" +
                "    p.setAge(100); \n" +
                "    int a = p.getAge();\n"
            );
    }

    @Test
    public void testGetterRewriteInArgument() {
        test( "    var p = new Person(\"yoda\");\n" +
                "    p.age = 100; \n" +
                "    System.out.println(p.age); \n",
                "    var p = new Person(\"yoda\");\n" +
                "    p.setAge(100); \n" +
                "    System.out.println(p.getAge()); \n"
            );
    }

    public static Person createPerson(String name) {
        return new Person(name);
    }

    @Test
    public void forIterationWithSubtype() {
        // this wouldn't rewrite the property accessor if it didn't know the generics.
        test(ctx -> ctx.addDeclaration("$people", List.class, "<Person>"),
             "    for (var p : $people ) {\n" +
             "        System.out.println(\"Person's salary: \" + p.salary );\n" +
             "}",
             "    for (var p : $people) {\n" +
             "        System.out.println(\"Person's salary: \" + p.getSalary() );\n" +
             "    }\n"
            );
    }

    @Test @Ignore
    public void forIterationWithSubtype2() {
        // this wouldn't rewrite the property accessor if it didn't know the generics.
        test(ctx -> ctx.addDeclaration("$people", List.class, "<Person>"),
             "    for (var p : $people ) {\n" +
             "        System.out.println(\"Person's salary: \" + p.salary );\n" +
             "    }\n",
             "    for (var p : $people) {\n" +
             "        System.out.println(\"Person's salary: \" + p.getSalary() );\n" +
             "    }\n"
            );
    }

    @Test
    public void forIterationWithSubtypeNested() {
        // this wouldn't rewrite the property accessor if it didn't know the generics.
        test(ctx -> {
                 ctx.addDeclaration("$people", List.class, "<Person>");
                 ctx.addDeclaration("$addresses", List.class, "<Address>");
             },
                     "for (var p : $people ) {\n" +
                     "       System.out.println(\"Simple statement\");\n" +
                     "       for (var a : $addresses ) {\n" +
                     "           System.out.println(\"Person's salary: \" + p.salary + \" address's City: \" + a.city);\n" +
                     "       }\n" +
                     "}",
                     "for (var p : $people) {\n" +
                     "           System.out.println(\"Simple statement\");\n" +
                     "           for (var a : $addresses) {\n" +
             "           System.out.println(\"Person's salary: \" + p.getSalary() + \" address's City: \" + a.getCity());\n" +
                     "            }\n" +
                     "}"
        );
    }

    @Test
    public void testMultiLineStringLiteral() {
        test(" { java.lang.String s = \"\"\"\n string content\n \"\"\";",
             " { java.lang.String s = \"\"\"\n string content\n \"\"\";");
    }
}