package org.mvel3.parser.printer;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.resolution.types.ResolvedType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.mvel3.parser.printer.MVELToJavaRewriter.getArgumentsWithUnwrap;

public class CoerceRewriter {

    private Map<CoercionKey, Function<Expression, Expression>> coercions = new HashMap<>();


    private static String[] integerPrimitives = new String[] { Primitive.CHAR.toDescriptor(),
                                                               Primitive.SHORT.toDescriptor(),
                                                               Primitive.INT.toDescriptor(),
                                                               Primitive.LONG.toDescriptor()};

    private static String[] floatPrimitives = new String[] { Primitive.CHAR.toDescriptor(),
                                                             Primitive.SHORT.toDescriptor(),
                                                             Primitive.INT.toDescriptor(),
                                                             Primitive.LONG.toDescriptor(),
                                                             Primitive.FLOAT.toDescriptor(),
                                                             Primitive.DOUBLE.toDescriptor()};

    public void populate() {
        Function<Expression, Expression> toBigDecimal = new Function<>() {
            @Override
            public Expression apply(Expression e) {
                List<Expression> args = getArgumentsWithUnwrap(e);

                MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("BigDecimal"), "valueOf", NodeList.nodeList(args));

                return methodCallExpr;
            }
        };

        String bigDecimal = String.format("L%s;", BigDecimal.class.getCanonicalName().replace(".", "/"));


        Arrays.stream(floatPrimitives).forEach(s -> coercions.put(key(s, bigDecimal),
                                                                  toBigDecimal));

        Function<Expression, Expression> toBigInteger = new Function<>() {
            @Override
            public Expression apply(Expression e) {
                List<Expression> args = getArgumentsWithUnwrap(e);

                MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("BigInteger"), "valueOf", NodeList.nodeList(args));

                return methodCallExpr;
            }
        };

        String bigInteger = String.format("L%s;", BigInteger.class.getCanonicalName().replace(".", "/"));

        Arrays.stream(integerPrimitives).forEach(s -> coercions.put(key(s, bigInteger),
                                                                    toBigInteger));
//
//        Function<Expression, Void> intToBigDecimal = new Function<Expression, Void>() {
//            @Override
//            public Void apply(Expression expression) {
//                Node node = expression.getParentNode().get();
//                MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("BigInteger"), "valueOf");
//                methodCallExpr.setParentNode(node);
//                methodCallExpr.addArgument(expression);
//                return null;
//            }
//        };
//
//        Arrays.stream(integerPrimitives).forEach(s -> coercions.put(key(s, BigInteger.class.getCanonicalName()),
//                                                                    intToBigDecimal));
//
//        Function<Expression, Void> intWithCastToBigDecimal = new Function<Expression, Void>() {
//            @Override
//            public Void apply(Expression expression) {
//                Node node = expression.getParentNode().get();
//                MethodCallExpr methodCallExpr = new MethodCallExpr(new NameExpr("BigInteger"), "valueOf");
//                methodCallExpr.setParentNode(node);
//                methodCallExpr.addArgument(new CastExpr(PrimitiveType.longType(), expression));
//                return null;
//            }
//        };
//
//        Arrays.stream(new String[] {float.class.getCanonicalName(), int.class.getCanonicalName()}).forEach(s -> {
//            coercions.put(key(s, BigInteger.class.getCanonicalName()),
//                          intWithCastToBigDecimal);
//        });
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




    public CoerceRewriter() {
        populate();
    }

    public static final CoerceRewriter INSTANCE = new CoerceRewriter();

    public Optional<Expression> coerce(ResolvedType source, Expression sourceExpression, ResolvedType target) {
        CoercionKey key = key(source.toDescriptor(),
                              target.toDescriptor());
        Function<Expression, Expression> coerce = coercions.get(key);

        if (coerce != null) {
            return Optional.of(coerce.apply(sourceExpression));
        }

        return Optional.empty();

    }

}

