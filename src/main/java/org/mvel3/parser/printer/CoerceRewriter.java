package org.mvel3.parser.printer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.resolution.Solver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.Streams;
import org.mvel3.transpiler.MVELTranspiler;
import org.mvel3.transpiler.context.TranspilerContext;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.mvel3.parser.printer.MVELToJavaRewriter.getArgumentsWithUnwrap;

public class CoerceRewriter {

    private Map<CoercionKey, Function<Expression, Expression>> coercions = new HashMap<>();

    public static final Primitive[] INTEGER_PRIMITIVES = new Primitive[] {Primitive.CHAR,
                                                                          Primitive.SHORT,
                                                                          Primitive.INT,
                                                                          Primitive.LONG};

    public static final Primitive[] FLOAT_PRIMITIVES = new Primitive[] {Primitive.CHAR,
                                                                        Primitive.SHORT,
                                                                        Primitive.INT,
                                                                        Primitive.LONG,
                                                                        Primitive.FLOAT,
                                                                        Primitive.DOUBLE};

    TranspilerContext transpilerContext;

    public CoerceRewriter(TranspilerContext transpilerContext) {
        this.transpilerContext = transpilerContext;

        bigDecimalCoercion();
        bigIntegerCoercion();

        integerToDateCoercion();
        dateToLongCoercion();

        numberToStringCoercion();
        stringToNumberCoercion();

    }

    public void bigDecimalCoercion() {
        Function<Expression, Expression> toBigDecimal = e -> {
            List<Expression> args = getArgumentsWithUnwrap(e);

            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(BigDecimal.class.getSimpleName()), "valueOf", NodeList.nodeList(args));

            return methodCallExpr;
        };

        String bigDecimal = BigDecimal.class.getCanonicalName();

        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), bigDecimal), toBigDecimal);
            coercions.put(key("java.lang." + p.toBoxedType(), bigDecimal), toBigDecimal);
        });
    }

    public void bigIntegerCoercion() {
        Function<Expression, Expression> toBigInteger = e -> {
            List<Expression> args = getArgumentsWithUnwrap(e);

            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(BigInteger.class.getSimpleName()), "valueOf", NodeList.nodeList(args));

            return methodCallExpr;
        };


        String bigInteger = BigInteger.class.getCanonicalName();

        Arrays.stream(INTEGER_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), bigInteger), toBigInteger);
            coercions.put(key("java.lang." + p.toBoxedType(), bigInteger), toBigInteger);
        });

    }

    public void integerToDateCoercion() {
        ParseResult<ClassOrInterfaceType> result = transpilerContext.getParser().parseClassOrInterfaceType("java.util.Date");
        if (!result.isSuccessful()) {
            throw new RuntimeException("Cannot resolve type:" + result.getProblems());
        }

        final ClassOrInterfaceType dateType = result.getResult().get();

        Function<Expression, Expression> integerToDate = e -> {
            ClassOrInterfaceType type = dateType.clone();

            ObjectCreationExpr expr = new ObjectCreationExpr(null, type, NodeList.nodeList(e));

            return expr;
        };

        String date = Date.class.getCanonicalName();

        Arrays.stream(INTEGER_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), date), integerToDate);
            coercions.put(key("java.lang." + p.toBoxedType(), date), integerToDate);
        });
    }


    /**
     * As MVEL3 does not narrow types, Date can only be coerced to long and Long.
     */
    public void dateToLongCoercion() {
        String date = Date.class.getCanonicalName();

        Function<Expression, Expression> toLong = e -> {
            MethodCallExpr getTimeCall = new MethodCallExpr(e, "getTime");
            return getTimeCall;
        };

        Arrays.stream(new Primitive[] {Primitive.LONG}).forEach(p -> {
            coercions.put(key(date, p.name().toUpperCase()), toLong);
            coercions.put(key(date, "java.lang." + p.toBoxedType()), toLong);
        });

    }

    public void numberToStringCoercion() {
        Function<Expression, Expression> numberToString = e -> {
            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("String"), "valueOf");
            methodCallExpr.addArgument(e);
            return methodCallExpr;
        };

        String string = String.class.getCanonicalName();

        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            coercions.put(key(p.name().toUpperCase(), string), numberToString);
            coercions.put(key("java.lang." + p.toBoxedType(), string), numberToString);
        });

        Arrays.stream(new String[] {BigDecimal.class.getCanonicalName(), BigInteger.class.getCanonicalName()}).forEach(p -> {
            Function<Expression, Expression> toNumber = e -> {
                MethodCallExpr methodCallExpr = new MethodCallExpr(e, "toString");
                return methodCallExpr;
            };

            coercions.put(key(p, string), toNumber);
        });

    }

    public void stringToNumberCoercion() {
        String string = String.class.getCanonicalName();

        // This class will preserve the coercion for target's type of primitive or Number Object wrapper.

        // This needs a function for each number type.
        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            Function<Expression, Expression> toNumber = e -> {
                MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(p.toBoxedType().getNameAsString()), "valueOf");
                methodCallExpr.addArgument(e);
                return methodCallExpr;
            };

            coercions.put(key(string, "java.lang." + p.toBoxedType()), toNumber);
        });

        // This needs a function for each number type.
        // ignore Char
        Arrays.stream(FLOAT_PRIMITIVES).forEach(p -> {
            if ( p != Primitive.CHAR) {
                Function<Expression, Expression> toNumber = e -> {
                    String         primName       = p.name().toLowerCase();
                    String         parseName      = "parse" + primName.substring(0, 1).toUpperCase() + primName.substring(1);
                    MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr(p.toBoxedType().getNameAsString()), parseName);
                    methodCallExpr.addArgument(e);
                    return methodCallExpr;
                };

                coercions.put(key(string, p.name().toUpperCase()), toNumber);
            }
        });

        // Noq add Char
        // This does not work, so always coerce to char
        //Character x = Character.valueOf("a".charAt(0)); x += Character.valueOf("a".charAt(0));
        Function<Expression, Expression> toNumber = e -> {
            MethodCallExpr methodCallExpr = new MethodCallExpr(e, "charAt");
            methodCallExpr.addArgument(new IntegerLiteralExpr(0));
            return methodCallExpr;
        };

        coercions.put(key(string, Primitive.CHAR.name().toUpperCase()), toNumber);
        coercions.put(key(string, "java.lang." + Primitive.CHAR.toBoxedType()), toNumber);

        Arrays.stream(new Class[] {BigDecimal.class, BigInteger.class}).forEach(p -> {
            Function<Expression, Expression> toBigNumber = e -> {
                ClassOrInterfaceType bigNumberClass = MVELTranspiler.handleParserResult(transpilerContext.getParser().parseClassOrInterfaceType(p.getSimpleName()));
                return new ObjectCreationExpr(null, bigNumberClass, NodeList.nodeList(e));
            };

            coercions.put(key(string, p.getCanonicalName()), toBigNumber);
        });
    }

    CoercionKey key(String sourceType, String targetType) {
        return CoercionKey.create(sourceType, targetType);
    }

    public static class CoercionKey {
        private String sourceType;
        private String targetType;

        public static CoercionKey create(String sourceType, String targetType) {
            return new CoercionKey(sourceType, targetType);
        }

        public CoercionKey(String sourceType, String targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public String getSourceType() {
            return sourceType;
        }

        public String getTargetType() {
            return targetType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CoercionKey that = (CoercionKey) o;

            if (!sourceType.equals(that.sourceType)) {
                return false;
            }
            return targetType.equals(that.targetType);
        }

        @Override
        public int hashCode() {
            int result = sourceType.hashCode();
            result = 31 * result + targetType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CoercionKey{" +
                   "sourceType='" + sourceType + '\'' +
                   ", targetType='" + targetType + '\'' +
                   '}';
        }
    }

    public Expression coerce(ResolvedType source, Expression sourceExpression, ResolvedType target) {
        CoercionKey key = key(describe(source),
                              describe(target));
        Function<Expression, Expression> coerce = coercions.get(key);

        if (coerce != null) {
            return coerce.apply(sourceExpression);
        }

        return null;
    }

    public String describe(ResolvedType type) {
        return type.isPrimitive() ? type.describe().toUpperCase() : type.describe();
    }

}

