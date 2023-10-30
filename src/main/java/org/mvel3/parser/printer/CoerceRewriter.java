package org.mvel3.parser.printer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.resolution.types.ResolvedType;
import org.mvel3.transpiler.context.TranspilerContext;

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

    public static final Primitive[] integerPrimitives = new Primitive[] { Primitive.CHAR,
                                                                          Primitive.SHORT,
                                                                          Primitive.INT,
                                                                          Primitive.LONG};

    public static final Primitive[] floatPrimitives = new Primitive[] { Primitive.CHAR,
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

            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("BigDecimal"), "valueOf", NodeList.nodeList(args));

            return methodCallExpr;
        };

        String bigDecimal = toObjectDescriptor(BigDecimal.class.getCanonicalName().replace(".", "/"));

        Arrays.stream(floatPrimitives).forEach(p -> {
            coercions.put(key(p.toDescriptor(), bigDecimal), toBigDecimal);
            coercions.put(key("Ljava/lang/" + p.toBoxedType() + ";", bigDecimal), toBigDecimal);
        });
    }

    public void bigIntegerCoercion() {
        Function<Expression, Expression> toBigInteger = e -> {
            List<Expression> args = getArgumentsWithUnwrap(e);

            MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("BigInteger"), "valueOf", NodeList.nodeList(args));

            return methodCallExpr;
        };


        String bigInteger = toObjectDescriptor(BigInteger.class.getCanonicalName().replace(".", "/"));

        Arrays.stream(integerPrimitives).forEach(p -> {
            coercions.put(key(p.toDescriptor(), bigInteger), toBigInteger);
            coercions.put(key("Ljava/lang/" + p.toBoxedType() + ";", bigInteger), toBigInteger);
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

        String date = toObjectDescriptor(Date.class.getCanonicalName().replace(".", "/"));

        Arrays.stream(integerPrimitives).forEach(p -> {
            coercions.put(key(p.toDescriptor(), date), integerToDate);
            coercions.put(key("Ljava/lang/" + p.toBoxedType() + ";", date), integerToDate);
        });
    }


    /**
     * As MVEL3 does not narrow types, Date can only be coerced to long and Long.
     */
    public void dateToLongCoercion() {
        String date = toObjectDescriptor(Date.class.getCanonicalName().replace(".", "/"));

        // This needs a function for each number time.
        Arrays.stream(new Primitive[] {Primitive.LONG}).forEach(p -> {
            Function<Expression, Expression> toNumber = e -> {
                FieldAccessExpr scope = new FieldAccessExpr(new FieldAccessExpr(new NameExpr("java"), "lang"),  p.toBoxedType().getNameAsString());
                MethodCallExpr valueOfCall = new MethodCallExpr(scope, "valueOf");
                MethodCallExpr getTimeCall = new MethodCallExpr(e, "getTime");

                valueOfCall.addArgument(getTimeCall);
                return valueOfCall;
            };

            coercions.put(key(date, p.toDescriptor()), toNumber);
            coercions.put(key(date, "Ljava/lang/" + p.toBoxedType() + ";"), toNumber);
        });
    }

    public void numberToStringCoercion() {
        Function<Expression, Expression> numberToDate = e -> {
            FieldAccessExpr stringScope = new FieldAccessExpr(new FieldAccessExpr(new NameExpr("java"), "lang"), "String");
            MethodCallExpr methodCallExpr = new MethodCallExpr(stringScope, "valueOf");
            methodCallExpr.addArgument(e);
            return methodCallExpr;
        };

        String string = toObjectDescriptor(String.class.getCanonicalName().replace(".", "/"));

        Arrays.stream(floatPrimitives).forEach(p -> {
            coercions.put(key(p.toDescriptor(), string), numberToDate);
            coercions.put(key("Ljava/lang/" + p.toBoxedType() + ";", string), numberToDate);
        });
    }

    public void stringToNumberCoercion() {
        String string = toObjectDescriptor(String.class.getCanonicalName().replace(".", "/"));

        // This needs a function for each number time.
        Arrays.stream(floatPrimitives).forEach(p -> {
            Function<Expression, Expression> toNumber = e -> {
                FieldAccessExpr scope = new FieldAccessExpr(new FieldAccessExpr(new NameExpr("java"), "lang"),  p.toBoxedType().getNameAsString());
                MethodCallExpr methodCallExpr = new MethodCallExpr(scope, "valueOf");
                methodCallExpr.addArgument(e);
                return methodCallExpr;
            };

            coercions.put(key(string, p.toDescriptor()), toNumber);
            coercions.put(key(string, "Ljava/lang/" + p.toBoxedType() + ";"), toNumber);
        });
    }

    private static String toObjectDescriptor(String canonicalType) {
        return "L" + canonicalType + ";";
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

//    public Expression coerce(ResolvedReferenceTypeDeclaration source, Expression sourceExpression, ResolvedReferenceTypeDeclaration target) {
//        CoercionKey key = key(source.toString()
//                              target.toDescriptor());
//        Function<Expression, Expression> coerce = coercions.get(key);
//
//        if (coerce != null) {
//            return coerce.apply(sourceExpression);
//        }
//
//        return null;
//    }

    public Expression coerce(ResolvedType source, Expression sourceExpression, ResolvedType target) {
        CoercionKey key = key(source.toDescriptor(),
                              target.toDescriptor());
        Function<Expression, Expression> coerce = coercions.get(key);

        if (coerce != null) {
            return coerce.apply(sourceExpression);
        }

        return null;
    }

}

