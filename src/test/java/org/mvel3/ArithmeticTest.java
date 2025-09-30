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

import org.junit.Ignore;
import org.junit.Test;
import org.mvel3.transpiler.context.Declaration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Porting of ArithmeticTests from MVEL project.
 * 
 * Disabled tests are failing for various causes
 * 
 */

public class ArithmeticTest {

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

  public static Object executeExpression(final String expression, final Map<String, Object> vars) {
    MVEL mvel = new MVEL();
    return mvel.executeExpression(expression, getImports(),vars);
  }

  public static Object executeExpressionWithDefaultVariables(final String expression) {
    MVEL mvel = new MVEL();
    if (expression.indexOf(';') > 0) {
      Map<String, Object> vars = createTestMap();
      Map<String, Type<?>> types = MVEL.getTypeMap(vars);
      Declaration[] declrations = types.entrySet().stream().map( entry -> new Declaration(entry.getKey(), entry.getValue())).collect(Collectors.toList()).toArray(new Declaration[0]);
      Evaluator<Map<String, Object>, Void, Object>  runtime = MVEL.map(declrations).out(Type.OBJECT).block(expression).imports(getImports()).compile();
      return runtime.eval(vars);
    } else {
      return mvel.executeExpression(expression, getImports(), createTestMap());
    }
  }


  protected static Map<String, Object> createTestMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("foo", new Foo());
    map.put("a", null);
    map.put("b", null);
    map.put("c", "cat");
    map.put("BWAH", "");

    map.put("misc", new MiscTestClass());

    map.put("pi", "3.14");
    map.put("hour", 60);
    map.put("zero", 0);

    map.put("array", new String[]{"", "blip"});

    map.put("order", new Order());
    map.put("$id", 20);

    map.put("five", 5);

    map.put("testImpl",
            new TestInterface() {

              public String getName() {
                return "FOOBAR!";
              }

              public boolean isFoo() {
                return true;
              }
            });


    map.put("ipaddr", "10.1.1.2");

    map.put("dt1", new Date(currentTimeMillis() - 100000));
    map.put("dt2", new Date(currentTimeMillis()));
    return map;
  }

  @Test
  public void testMath() {
    String expression = "((double)pi) * hour"; // must cast pi to double, otherwise it uses the hour type, which is Integer for the coercion
    double result = 188.4;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath2() {
    assertThat(executeExpressionWithDefaultVariables("foo.number-1")).isEqualTo(3);
  }

  @Test
  public void testMath3() {
    String expression = "(10 * 5) * 2 / 3";
	var result = (10 * 5) * 2 / 3;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath4() {
	String expression = "(100 % 3) * 2 - 1 / 1 + 8 + (5 * 2)";
    var result = ((100 % 3) * 2 - 1 / 1 + 8 + (5 * 2));

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath4a() {
    String expression = "(100 % 90) * 20 - 15 / 16 + 80 + (50 * 21)";
    var result = (100 % 90) * 20 - 15 / 16 + 80 + (50 * 21);

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath5() {
    String expression = "300.5 / 5.3 / 2.1 / 1.5";
	double result = 300.5 / 5.3 / 2.1 / 1.5;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath5a() {
    String expression = "300.5 / 5.3 / 2.1 / 1.5";
    double result = 300.5 / 5.3 / 2.1 / 1.5;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath6() {
    String expression = "(300 * five + 1) + (100 / 2 * 2)";
    var result = (300 * 5 + 1) + 100 / 2 * 2;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath7() {
	String expression = "(100 % 3) * 2 - 1 / 1 + 8 + (5 * 2)";
    var result = ((100 % 3) * 2 - 1 / 1 + 8 + (5 * 2));

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath8() {
	String expression = "5 * (100.56 * 30.1)";
    double result = 5d * (100.56d * 30.1d);

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testPowerOf() {
    assertThat(executeExpressionWithDefaultVariables("5 ** 2")).isEqualTo(25);
  }

  @Test
  public void testSignOperator() {
    String expr = "int x = 15; return -x;";

    assertThat(executeExpressionWithDefaultVariables(expr)).isEqualTo(-15);
  }

  @Test
  public void testMath14() {
	String expression = "10-5*2 + 5*8-4";
    int result = 10 - 5 * 2 + 5 * 8 - 4;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath15() {
    String expression = "100-500*200 + 500*800-400";
    int result = 100 - 500 * 200 + 500 * 800 - 400;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath16() {
    String expression = "100-500*200*150 + 500*800-400";
    int result = 100 - 500 * 200 * 150 + 500 * 800 - 400;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath17() {
    String expression = "(100d * 50d) * 20d / 30d * 2d";
    double result = (100d * 50d) * 20d / 30d * 2d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath18() {
    String expression = "var a = 100d; var b = 50d; var c = 20d; var d = 30d; var e = 2d; return (a * b) * c / d * e;";
    var result = (100d * 50d) * 20d / 30d * 2d;

    assertThat(executeExpression(expression, Collections.emptyMap())).isEqualTo(result);
  }

  @Test
  public void testMath19() {
    String expression = "var a = 100; var b = 500; var c = 200; var d = 150; var e = 500; var f = 800; var g = 400; return a-b*c*d + e*f-g;";
    int result = 100 - 500 * 200 * 150 + 500 * 800 - 400;
    assertThat(executeExpression(expression, Collections.emptyMap())).isEqualTo(result);
  }

  @Test
  public void testMath32() {
    String expression = "var x = 20; var y = 10; var z = 5; return x-y-z;";
    int result = 20 - 10 - 5;

    assertThat(executeExpression(expression, Collections.emptyMap())).isEqualTo(result);
  }

  @Test
  public void testMath33() {
    String expression = "var x = 20; var y = 2; var z = 2; return x/y/z;";
    int result = 20 / 2 / 2;

    assertThat(executeExpression(expression, Collections.emptyMap())).isEqualTo(result);
  }

  @Test
  public void testMath20() {
    String expression = "10-5*7-3*8-6";
    int result = 10 - 5 * 7 - 3 * 8 - 6;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath21() {
    String expression = "100-50*70-30*80-60";
    int expected = 100 - 50 * 70 - 30 * 80 - 60;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(expected);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath22() {
    String expression = "(100-50)*70-30*(20-9)**3";
    int result = (int) ((100 - 50) * 70 - 30 * Math.pow(20 - 9, 3));

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath22b() {
    String expression = "a = 100; b = 50; c = 70; d = 30; e = 20; f = 9; g = 3; (a-b)*c-d*(e-f)**g";
    int result = (int) ((100 - 50) * 70 - 30 * Math.pow(20 - 9, 3));

    assertThat(executeExpression(expression, Collections.emptyMap())).isEqualTo(result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath23() {
    String expression = "10 ** (3)*10**3";
    int result = (int) (Math.pow(10, 3) * Math.pow(10, 3));

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath24() {
    String expression = "51 * 52 * 33 / 24 / 15 + 45 * 66 * 47 * 28 + 19";
    var result = 51 * 52 * 33 / 24 / 15 + 45 * 66 * 47 * 28 + 19;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath25() {
    String expression = "51 * (40 - 1000 * 50) + 100 + 50 * 20 / 10 + 11 + 12 - 80";
    var result = 51 * (40 - 1000 * 50) + 100 + 50 * 20 / 10 + 11 + 12 - 80;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath26() {
    String expression = "5 + 3 * 8 * 2 ** 2";
    int result = (int) (5d + 3d * 8d * Math.pow(2, 2));

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath27() {
    String expression = "50 + 30 * 80 * 20 ** 3 * 51";
    double result = 50 + 30 * 80 * Math.pow(20, 3) * 51;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo((int) result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath28() {
    String expression = "50 + 30 + 80 + 11 ** 2 ** 2 * 51";
    double result = 50 + 30 + 80 + Math.pow(Math.pow(11, 2), 2) * 51;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo((int) result);
  }

  @Test
  public void testMath29() {
    String expression = "10 + 20 / 4 / 4";
    var result = 10 + 20 / 4 / 4;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath30() {
    String expression = "40 / 20 + 10 + 60 / 21";
    var result = 40 / 20 + 10 + 60 / 21;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Ignore("DROOLS-6572 - Unable to parse **")
  @Test
  public void testMath31() {
    String expression = "40 / 20 + 5 - 4 + 8 / 2 * 2 * 6 ** 2 + 6 - 8";
    double result = 40f / 20f + 5f - 4f + 8f / 2f * 2f * Math.pow(6, 2) + 6f - 8f;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath34() {
    String expression = "a+b-c*d*x/y-z+10";
    int result = 200 + 100 - 150 * 2 * 400 / 300 - 75 + 10;

    Map<String, Object> map = new HashMap<>();
    map.put("a", 200);
    map.put("b", 100);
    map.put("c", 150);
    map.put("d", 2);
    map.put("x", 400);
    map.put("y", 300);
    map.put("z", 75);

    assertThat(executeExpression(expression, map)).isEqualTo(result);
  }

  @Test
  public void testMath34_Interpreted() {
    String expression = "a+b-c*x/y-z";
    int result = (int) 200 + 100 - 150 * 400 / 300 - 75;

    Map<String, Object> map = new HashMap<>();
    map.put("a", 200);
    map.put("b", 100);
    map.put("c", 150);
    map.put("x", 400);
    map.put("y", 300);
    map.put("z", 75);

    assertThat(executeExpression(expression, map)).isEqualTo(result);
  }

  @Test
  public void testMath35() {
    String expression = "b/x/b/b*y+a";
    double result = 20d / 40d / 20d / 20d * 50d + 10d;

    Map<String, Object> map = new HashMap<>();
    map.put("a", 10d);
    map.put("b", 20d);
    map.put("c", 30d);
    map.put("x", 40d);
    map.put("y", 50d);
    map.put("z", 60d);

    assertThat(executeExpression(expression, map)).isEqualTo(result);
  }

  @Test
  public void testMath35_Interpreted() {
    String expression = "b/x/b/b*y+a";
    double result = 20d / 40d / 20d / 20d * 50d + 10d;

    Map<String, Object> map = new HashMap<>();
    map.put("a", 10d);
    map.put("b", 20d);
    map.put("c", 30d);
    map.put("x", 40d);
    map.put("y", 50d);
    map.put("z", 60d);

    assertThat(executeExpression(expression, map)).isEqualTo(result);
  }

  @Test
  public void testMath36() {
    String expression = "b/x*z/a+x-b+x-b/z+y";
    double result = 20d / 40d * 60d / 10d + 40d - 20d + 40d - 20d / 60d + 50d;

    Map<String, Object> map = new HashMap<>();
    map.put("a", 10d);
    map.put("b", 20d);
    map.put("c", 30d);
    map.put("x", 40d);
    map.put("y", 50d);
    map.put("z", 60d);

    assertThat(executeExpression(expression, map)).isEqualTo(result);
  }

  @Test
  public void testMath37() {
    String expression = "x+a*a*c/x*b*z+x/y-b";
    double result = 2d + 10d * 10d * 30d / 2d * 20d * 60d + 2d / 2d - 20d;

    Map<String, Object> map = new HashMap<>();
    map.put("a", 10d);
    map.put("b", 20d);
    map.put("c", 30d);
    map.put("x", 2d);
    map.put("y", 2d);
    map.put("z", 60d);

    assertThat(executeExpression(expression, map)).isEqualTo(result);
  }

  @Test
  public void testMath38() {
    String expression = "100d + 200d - 300d + 400d - 500d + 105d / 205d - 405d + 305d * 206d";
    double result = 100d + 200d - 300d + 400d - 500d + 105d / 205d - 405d + 305d * 206d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath39() {
    String expression = "147d + 60d / 167d % 448d + 36d * 23d / 166d";
    double result = 147d + 60d / 167d % 448d + 36d * 23d / 166d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath40() {
    String expression = "228 - 338d % 375d - 103d + 260d + 412d * 177d + 121d";
    double result = 228d - 338d % 375d - 103d + 260d + 412d * 177d + 121d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath41() {
    String expression = "304d - 246d / 242d % 235d / 425d - 326d + 355d * 264d % 308d";
    double result = 304d - 246d / 242d % 235d / 425d - 326d + 355d * 264d % 308d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath42() {
    String expression = "11d - 7d / 3d * 18d % 14d * 8d * 11d - 2d - 11d / 13d + 14d";
    double result = 11d - 7d / 3d * 18d % 14d * 8d * 11d - 2d - 11d / 13d + 14d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath43() {
    String expression = "4d/3d*6d%8d*5d*8d+7d+9d*1d";
    double result = 4d / 3d * 6d % 8d * 5d * 8d + 7d + 9d * 1d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath44() {
    String expression = "6d+8d/9d*1d*9d*10d%4d*4d-4d*6d*3d";
    double result = 6d + 8d / 9d * 1d * 9d * 10d % 4d * 4d - 4d * 6d * 3d;

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(result);
  }

  @Test
  public void testMath44b() {
    String expression = "a+b/c*d*e*f%g*h-i*j*k";
    double result = 6d + 8d / 9d * 1d * 9d * 10d % 4d * 4d - 4d * 6d * 3d;

    Map<String, Object> vars = new HashMap<>();
    vars.put("a", 6d);
    vars.put("b", 8d);
    vars.put("c", 9d);
    vars.put("d", 1d);
    vars.put("e", 9d);
    vars.put("f", 10d);
    vars.put("g", 4d);
    vars.put("h", 4d);
    vars.put("i", 4d);
    vars.put("j", 6d);
    vars.put("k", 3d);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }

  @Test
  public void testOperatorPrecedence() {
    String expression = "var _x_001 = 500.2; var _x_002 = 200.8; var _r_001 = 701; return _r_001 == _x_001 + _x_002 || _x_001 == 500 + 0.1;";
    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(true);
  }

  //@Ignore("DROOLS-6572 - Unable to parse")
  @Test
  public void testOperatorPrecedence2() {
    String expression = "var _x_001 = 500.2; var _x_002 = 200.8; var _r_001 = 701; return _r_001 == _x_001 + _x_002 && _x_001 == 500 + 0.2;";
    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(true);
  }

  @Test
  public void testOperatorPrecedence3() {
    String expression = "var _x_001 = 500.2; var _x_002 = 200.9; var _r_001 = 701; return _r_001 == _x_001 + _x_002 && _x_001 == 500 + 0.2;";

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(false);
  }

  @Test
  public void testOperatorPrecedence4() {
    String expression = "var _x_001 = 500.2; var _x_002 = 200.9; var _r_001 = 701; return _r_001 == _x_001 + _x_002 || _x_001 == 500 + 0.2;";
    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(true);
  }

  @Test
  public void testOperatorPrecedence5() {
    String expression = "_x_001 == _x_001 / 2 - _x_001 + _x_001 + _x_001 / 2 && _x_002 / 2 == _x_002 / 2";

    Map<String, Object> vars = new HashMap<>();
    vars.put("_x_001", 500.2);
    vars.put("_x_002", 200.9);
    vars.put("_r_001", 701);

    assertThat(executeExpression(expression, vars)).isEqualTo(true);
  }

  @Test
  public void testModulus() {
    assertThat(executeExpressionWithDefaultVariables("38392 % 2")).isEqualTo(38392 % 2);
  }

  @Test
  public void testBitwiseOr1() {
    assertThat(executeExpressionWithDefaultVariables("2|4")).isEqualTo(6);
  }

  @Test
  public void testBitwiseOr2() {
    assertThat(executeExpressionWithDefaultVariables("(2 | 1) > 0")).isEqualTo(true);
  }

  @Test
  public void testBitwiseOr3() {
    assertThat(executeExpressionWithDefaultVariables("(2|1) == 3")).isEqualTo(true);
  }

  @Test
  public void testBitwiseOr4() {
    assertThat(executeExpressionWithDefaultVariables("2|five")).isEqualTo(2 | 5);
  }

  @Test
  public void testBitwiseAnd1() {
    assertThat(executeExpressionWithDefaultVariables("2 & 3")).isEqualTo(2);
  }

  @Test
  public void testBitwiseAnd2() {
    assertThat(executeExpressionWithDefaultVariables("five & 3")).isEqualTo(5 & 3);
  }

  @Test
  public void testShiftLeft() {
    assertThat(executeExpressionWithDefaultVariables("2 << 1")).isEqualTo(4);
  }

  @Test
  public void testShiftLeft2() {
    assertThat(executeExpressionWithDefaultVariables("five << 1")).isEqualTo(5 << 1);
  }

  @Ignore("DROOLS-6572 - Generates wrong code - unable to parse <<<")
  @Test
  public void testUnsignedShiftLeft() {
    assertThat(executeExpressionWithDefaultVariables("-2 <<< 0")).isEqualTo(2);
  }

  @Test
  public void testShiftRight() {
    assertThat(executeExpressionWithDefaultVariables("256 >> 1")).isEqualTo(128);
  }

  @Test
  public void testShiftRight2() {
    assertThat(executeExpressionWithDefaultVariables("five >> 1")).isEqualTo(5 >> 1);
  }

  @Test
  public void testUnsignedShiftRight() {
    assertThat(executeExpressionWithDefaultVariables("-5 >>> 1")).isEqualTo(-5 >>> 1);
  }

  @Test
  public void testUnsignedShiftRight2() {
    assertThat(executeExpressionWithDefaultVariables("(five - 10) >>> 1")).isEqualTo(-5 >>> 1);
  }

  @Test
  public void testShiftRightAssign() {
    assertThat(executeExpressionWithDefaultVariables("var _zZz = 5; return _zZz >>= 2;")).isEqualTo(5 >> 2);
  }

  @Test
  public void testShiftLeftAssign() {
    assertThat(executeExpressionWithDefaultVariables("var _yYy = 10; return _yYy <<= 2;")).isEqualTo(10 << 2);
  }

  @Test
  public void testUnsignedShiftRightAssign() {
    String expression = "var _xXx = -5; return _xXx >>>= 2;";

    assertThat(executeExpression(expression, Collections.emptyMap())).isEqualTo(-5 >>> 2);
  }

  @Test
  public void testXOR() {
    assertThat(executeExpressionWithDefaultVariables("1 ^ 2")).isEqualTo(3);
  }

  @Test
  public void testXOR2() {
    assertThat(executeExpressionWithDefaultVariables("five ^ 2")).isEqualTo(5 ^ 2);
  }

  @Test
  public void testInvert() {
    assertThat(executeExpressionWithDefaultVariables("~10")).isEqualTo(~10);
  }

  @Test
  public void testInvert2() {
    assertThat(executeExpressionWithDefaultVariables("~(10 + 1)")).isEqualTo(~(10 + 1));
  }

  @Test
  public void testInvert3() {
    assertThat(executeExpressionWithDefaultVariables("~10 + (1 + ~50)")).isEqualTo(~10 + (1 + ~50));
  }

  @Test
  public void testDeepPropertyAdd() {
    assertThat(executeExpressionWithDefaultVariables("foo.countTest+ 10")).isEqualTo(10);
  }

  @Test
  public void testDeepAssignmentIncrement() {
    String expression = "foo.countTest += 5; if (foo.countTest == 5) { foo.countTest = 0; return true; }" +
        " else { foo.countTest = 0; return false; }";

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(true);

    assertThat(executeExpressionWithDefaultVariables("foo.countTest += 5; if (foo.countTest == 5) { foo.countTest = 0; return true; }" +
              " else { foo.countTest = 0; return false; }")).isEqualTo(true);
  }

  //@Ignore("DROOLS-6572 - Generates wrong code - check for statements")
  @Test
  public void testDeepAssignmentWithBlock() {
    String expression = "with (foo) { countTest += 5; }; if (foo.countTest == 5) { foo.countTest = 0; return true; }" +
        " else { foo.countTest = 0; return false; }";

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(true);

    assertThat(executeExpressionWithDefaultVariables("with (foo) { countTest += 5; }; if (foo.countTest == 5) { foo.countTest = 0; return true; }" +
              " else { foo.countTest = 0; return false; }")).isEqualTo(true);
  }

  @Test
  public void testOperativeAssignMod() {
    int val = 5;
    assertThat(executeExpressionWithDefaultVariables("int val = 5; val %= 2; return val;")).isEqualTo(val %= 2);
  }

  @Test
  public void testOperativeAssignDiv() {
    int val = 10;
    assertThat(executeExpressionWithDefaultVariables("int val = 10; val /= 2; return val;")).isEqualTo(val /= 2);
  }

  @Test
  public void testOperativeAssignShift1() {
    int val = 5;
    assertThat(executeExpressionWithDefaultVariables("int val = 5; val <<= 2; return val;")).isEqualTo(val <<= 2);
  }

  @Test
  public void testOperativeAssignShift2() {
    int val = 5;
    assertThat(executeExpressionWithDefaultVariables("int val = 5; val >>= 2; return val;")).isEqualTo(val >>= 2);
  }

  @Test
  public void testOperativeAssignShift3() {
    int val = -5;
    assertThat(executeExpressionWithDefaultVariables("int val = -5; val >>>= 2; return val;")).isEqualTo(val >>>= 2);
  }

  @Test
  public void testAssignPlus() {
    assertThat(executeExpressionWithDefaultVariables("var xx0 = 5; xx0 += 4; return xx0 + 1;")).isEqualTo(10);
  }

  @Test
  public void testAssignPlus2() {
    var xx0 = 5; xx0 += 4;
    assertThat(xx0 += 1).isEqualTo(10);
    assertThat(executeExpressionWithDefaultVariables("var xx0 = 5; xx0 += 4; return xx0 += 1;")).isEqualTo(10);
  }

  //@Ignore("DROOLS-6572 - Rounding error")
  @Test
  public void testAssignDiv() {
    var xx0 = 20; xx0 /= 10;
    assertThat(xx0).isNotEqualTo(2.0);
    assertThat(xx0).isEqualTo(2);
    assertThat(executeExpressionWithDefaultVariables("var xx0 = 20; return (xx0 /= 10);")).isEqualTo(2);
  }

  @Test
  public void testAssignMult() {
    assertThat(executeExpressionWithDefaultVariables("var xx0 = 6; xx0 *= 6; return xx0;")).isEqualTo(36);
  }

  @Test
  public void testAssignSub() {
    assertThat(executeExpressionWithDefaultVariables("var xx0 = 15; xx0 -= 4; return xx0;")).isEqualTo(11);
  }

  @Test
  public void testAssignSub2() {
    var xx0 = 5;
    xx0 =- 100;
    assertThat(executeExpressionWithDefaultVariables("var xx0 = 5; return xx0 =- 100;")).isEqualTo(xx0);
  }

  @Test
  public void testBooleanStrAppend() {
    assertThat(executeExpressionWithDefaultVariables("\"foo\" + true")).isEqualTo("footrue");
  }

  
  @Ignore("DROOLS-6572 - Unable to parse")
  @Test
  public void testStringAppend() {
    String expression = "c + 'bar'";

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo("catbar");
  }

  @Test
  public void testNegation() {
    assertThat(executeExpressionWithDefaultVariables("-(-1)")).isEqualTo(1);
  }

  @Test
  public void testStrongTypingModeComparison() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("a", 0l);
    
    executeExpression("a==0", variables);
  }

  
  //@Ignore("DROOLS-6572 - Rounding error")
  @Test
  public void testJIRA158() {
    String expression = "(float) (4/2 + Math.sin(1))";

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo((float) (4 / 2 + Math.sin(1)));
  }

  @Test
  public void testJIRA162() {
    String expression = "1d - 2d + (3d * var1) * var1";
    double result = 1 - 2 + (3 * 1d) * 1;
    
    Map<String, Object> vars = new HashMap<>();
    vars.put("var1", 1d);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }

  @Test
  public void testJIRA161() {
    String expression = "1==-(-1)";

    assertThat(executeExpressionWithDefaultVariables(expression)).isEqualTo(1 == -(-1));
  }

  
  @Test
  public void testJIRA163() {
    String expression = "1d - 2d + (3d * 4d) * var1";
    double result = 1 - 2 + (3 * 4) * 1d;

    Map<String, Object> vars = new HashMap<>();
    vars.put("var1", 1d);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }

  //@Ignore("DROOLS-6572 - Wrong value calculated")
  @Test
  public void testJIRA164() {
    String expression = "1 / (var1 + var1) * var1";

    double var1 = 1d;
    double result = 1 / (var1 + var1) * var1;

    Map<String, Object> vars = new HashMap<>();
    vars.put("var1", var1);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);

  }

  //@Ignore("DROOLS-6572 - Wrong value calculated")
  @Test
  public void testJIRA164b() {
    String expression = "1 + 1 / (var1 + var1) * var1";

    double var1 = 1d;
    double result = 1 + 1 / (var1 + var1) * var1;
    Map<String, Object> vars = new HashMap<>();
    vars.put("var1", var1);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }

  //@Ignore("DROOLS-6572 - Wrong value calculated")
  @Test
  public void testJIRA164c() {
	double var1 = 1d;
    String expression = "1 + 1 / (var1 + var1 + 2 + 3) * var1";

    double result = 1 + 1 / (var1 + var1 + 2 + 3) * var1;
   
    Map<String, Object> vars = new HashMap<>();
    vars.put("var1", var1);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }

  //@Ignore("DROOLS-6572 - Wrong value calculated")
  @Test
  public void testJIRA164d() {
	double var1 = 1d;
    String expression = "1 + 1 + 1 / (var1 + var1) * var1";
    double result = 1 + 1 + 1 / (var1 + var1) * var1;
    Map<String, Object> vars = new HashMap<>();

    vars.put("var1", var1);

    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }

  //@Ignore("DROOLS-6572 - Wrong value calculated")
  @Test
  public void testJIRA164e() {
    String expression = "10 + 11 + 12 / (var1 + var1 + 51 + 71) * var1 + 13 + 14";
    Map<String, Object> vars = new HashMap<>();

    double var1 = 1d;
    vars.put("var1", var1);

    assertThat(((Double) executeExpression(expression, vars)).floatValue()).isCloseTo((float) (10 + 11 + 12 / (var1 + var1 + 51 + 71) * var1 + 13 + 14), within(0.01f));
  }

  //@Ignore("DROOLS-6572 - Wrong value calculated")
  @Test
  public void testJIRA164f() {
    String expression = "10 + 11 + 12 / (var1 + 1 + var1 + 51 + 71) * var1 + 13 + 14";
    double var1 = 1d;
    float result = (float) (10 + 11 + 12 / (var1 + 1 + var1 + 51 + 71) * var1 + 13 + 14);

    Map<String, Object> vars = new HashMap<>();

    vars.put("var1", var1);

    assertThat(((Double) executeExpression(expression, vars)).floatValue()).isCloseTo(result, within(0.01f));
  }


  @Test
  public void testJIRA164g() {
    String expression = "1 - 2 + (3 * var1) * var1";
    double var1 = 1d;
    float result = (float) (1 - 2 + (3 * var1) * var1);

    Map<String, Object> vars = new HashMap<>();
    vars.put("var1", var1);

    assertThat(((Double) executeExpression(expression, vars)).floatValue()).isCloseTo(result, within(0.01f));
  }

  @Test
  public void testJIRA164h() {
    String expression = "1 - var1 * (var1 * var1 * (var1 * var1) * var1) * var1";

    Map<String, Object> vars = new HashMap<>();

    double var1 = 2d;
    vars.put("var1", var1);

    assertThat(((Double) executeExpression(expression, vars)).floatValue()).isCloseTo((float) (1 - var1 * (var1 * var1 * (var1 * var1) * var1) * var1), within(0.01f));
  }

  @Test
  public void testJIRA180() {
    executeExpressionWithDefaultVariables("-Math.sin(0)");
  }

  @Test
  public void testJIRA208() {
    Map<String, Object> vars = new LinkedHashMap<>();
    vars.put("bal", 999);

    String[] testCases = {"bal - 80 - 90 - 30", "bal-80-90-30", "100 + 80 == 180", "100+80==180"};

    Object val1, val2;
    for (String expr : testCases) {
      val1 = executeExpression(expr, vars);
    assertThat(val1).isNotNull();
      val2 = executeExpression(expr, vars);
    assertThat(val2).isNotNull();
      assertThat(val2).as("expression did not evaluate correctly: " + expr).isEqualTo(val1);
    }
  }

  @Test
  public void testJIRA1208a() {
    Map<String, Object> bal = new HashMap<>();
    bal.put("bal", 999);
    assertThat(executeExpression("bal - 80 - 90 - 30", bal)).isEqualTo(799);
  }

  @Test
  public void testJIRA208b() {
    Map<String, Object> vars = new LinkedHashMap<>();
    vars.put("bal", 999);

    String[] testCases = {
                "bal + 80 - 80",
               "bal - 80 + 80", "bal * 80 / 80",
        "bal / 80 * 80"
    };

    
    for (String expr : testCases) {
      Object val1 = executeExpression(expr, vars);
    assertThat(val1).isNotNull();
      Object val2 = executeExpression(expr, vars);
    assertThat(val2).isNotNull();
      assertThat(val2).as("expression did not evaluate correctly: " + expr).isEqualTo(val1);
    }
  }

  @Test
  public void testJIRA210() {
    Map<String, Object> vars = new LinkedHashMap<>();
    vars.put("bal", new BigDecimal("999.99"));

    String[] testCases = {"bal - 1 + \"abc\"",};


    for (String expr : testCases) {
      Object val1 = executeExpression(expr, vars);
    assertThat(val1).isNotNull();
      Object val2 = executeExpression(expr, vars);
    assertThat(val2).isNotNull();
      assertThat(val2).as("expression did not evaluate correctly: " + expr).isEqualTo(val1);
    }
  }

  @Ignore("DROOLS-6572 - Generates wrong code - missing symbol")
  @Test
  public void testMathDec30() {
    Map<String, Object> params = new HashMap<>();
    params.put("value", 10);
    Map<String, Object> vars = new HashMap<>();
    vars.put("param", params);
    vars.put("param2", 10);

    assertThat(executeExpression("1 + 2 * param.value", vars)).isEqualTo(1 + 2 * 10);
  }

  @Test
  public void testJIRA99_Interpreted() {
    Map<String, Object> map = new HashMap<>();
    map.put("x", 20);
    map.put("y", 10);
    map.put("z", 5);

    assertThat(executeExpression("x - y - z", map)).isEqualTo(20 - 10 - 5);
  }

  @Test
  public void testJIRA99_Compiled() {
    Map<String, Object> map = new HashMap<>();
    map.put("x", 20);
    map.put("y", 10);
    map.put("z", 5);

    assertThat(executeExpression("x - y - z", map)).isEqualTo(20 - 10 - 5);
  }

  @Test()
  public void testModExpr() {
    String str = "$y % 4 == 0 && $y % 100 != 0 || $y % 400 == 0 ";
    Map<String, Object> vars = new HashMap<>();

    for (int i = 0; i < 500; i++) {
      int y = i;
      boolean expected = y % 4 == 0 && y % 100 != 0 || y % 400 == 0;
      vars.put("$y", y);

      assertThat(executeExpression(str, vars)).isEqualTo(expected);

    }
  }

  @Test
  public void testIntsWithDivision() {
    String expression = "0 == x - (y/2)";

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 50);
    vars.put("y", 100);
    Boolean result = (Boolean) executeExpression(expression, vars);
    assertThat(result).isTrue();
  }

  @Test
  public void testMathCeil() {
    String expression = "Math.ceil( x/3 ) == 1";
    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 4);
    Boolean result = (Boolean) executeExpression(expression, vars);
    assertThat(result).isTrue();
  }

  @Test
  public void testStaticMathCeil() {      
    int x = 4;
    int m = (int) Math.ceil( x/3 ); // demonstrating it's perfectly valid java

    String expression = "int m = (int) java.lang.Math.ceil( x/3 ); return m;";

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 4);
    assertThat(executeExpression(expression, vars)).isEqualTo(Integer.valueOf(1));
  }  

  @Test
  public void testStaticMathCeilWithJavaClassStyleLiterals() {            
	String expression = "java.lang.Math.ceil((double) x/3 )";
	double result = Math.ceil((double) 4 / 3);

	Map<String, Object> vars = new HashMap<>();
	vars.put("x", 4);
    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }
  
  @Test
  public void testMathCeilWithDoubleCast() {
    String expression = "Math.ceil( (double) x / 3 )";
    double result = Math.ceil((double) 4 / 3);

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 4);
    assertThat(executeExpression(expression, vars)).isEqualTo(result);
  }
  
  @Test
  public void testBigDecimalAssignmentIncrement() {
    String expression = "var s1=0B;s1+=1;s1+=1; return s1;";
    BigDecimal result = new BigDecimal(2);

    assertThat(executeExpression(expression, new HashMap<>())).isEqualTo(result);
  }
  
  /* https://github.com/mvel/mvel/issues/249
   * The following caused a ClassCastException because the compiler optimized for integers
   */
  @Test
  public void testIssue249() {
    String expression = "70 + 30 *  x1";
    Map<String, Object> expressionVars = new HashMap<>();
    expressionVars.put("x1", 128.33);

    assertThat(((Number) executeExpression(expression, expressionVars)).doubleValue()).isCloseTo(3919.9, within(0.01));
  }

}
