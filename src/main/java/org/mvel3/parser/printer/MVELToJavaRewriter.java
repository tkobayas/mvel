package org.mvel3.parser.printer;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionEnumDeclaration;
import com.github.javaparser.utils.Pair;
import org.mvel3.parser.MvelParser;
import org.mvel3.transpiler.context.MvelTranspilerContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MVELToJavaRewriter {
    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private ResolvedType mapType;

    private ResolvedType listType;


    private BinaryExpr rootBinaryExpr;
    private BinaryExpr lastBinaryExpr;

    Map<BinaryExpr, BinaryExprTypes> nodeMap;

    List<Expression> nodes = new ArrayList<>();

    private CoerceRewriter coercer = CoerceRewriter.INSTANCE;

    //private MvelParser mvelParser;

    private MvelTranspilerContext context;

    public MVELToJavaRewriter(MvelTranspilerContext context) {
        this.context = context;
        bigDecimalType = new ReferenceTypeImpl(context.getTypeSolver().solveType(BigDecimal.class.getCanonicalName().toString()));
        bigIntegerType = new ReferenceTypeImpl(context.getTypeSolver().solveType(BigInteger.class.getCanonicalName().toString()));
        mapType = new ReferenceTypeImpl(context.getTypeSolver().solveType(Map.class.getCanonicalName().toString()));
        listType = new ReferenceTypeImpl(context.getTypeSolver().solveType(List.class.getCanonicalName().toString()));

        nodeMap = new IdentityHashMap<>();
    }

    private void rewriteNode(Node node) {
        BinaryExpr binExpr = null;
        switch (node.getClass().getSimpleName())  {
            case "AssignExpr":
                processAssignExpr((AssignExpr) node);
                break;
            case "FieldAccessExpr":
                maybeRewriteToGetter((FieldAccessExpr) node);
                break;
            case "ArrayAccessExpr":
                rewriteArrayAccessExpr((ArrayAccessExpr) node);
                break;
            case "BinaryExpr":
                binExpr = (BinaryExpr) node;
                if (rootBinaryExpr == null) {
                    rootBinaryExpr = binExpr;
                }

                lastBinaryExpr = null;
                rewriteNode( binExpr.getLeft());
                if (lastBinaryExpr == null) {
                    processBinaryExpr(binExpr, binExpr.getLeft());
                }

                lastBinaryExpr = null;
                rewriteNode( binExpr.getRight());
                if (lastBinaryExpr == null) {
                    processBinaryExpr(binExpr, binExpr.getRight());
                }
                break;
            case "MethodCallExpr":
                MethodCallExpr methodCall = (MethodCallExpr) node;
                if (methodCall.getScope().isPresent()) {
                    rewriteNode(methodCall.getScope().get());
                }
                for (Expression arg : methodCall.getArguments()) {
                    rewriteNode(arg);
                }
                break;
            case "VariableDeclarationExpr":
                VariableDeclarationExpr declrExpr = (VariableDeclarationExpr) node;
                VariableDeclarator declr = declrExpr.getVariable(0);

                if ( declr.getInitializer().isPresent()) {
                    Expression initializer = declr.getInitializer().get();
                    rewriteNode(initializer);

                    initializer = declr.getInitializer().get(); // get again, incase it was written

                    // Don't coerce 'var' types, as they will be assigned to what ever the return type is
                    Type type = declr.getType();
                    if (!(initializer instanceof TextBlockLiteralExpr) &&
                        !(type.isClassOrInterfaceType() && type.asClassOrInterfaceType().getNameAsString().equals("var"))) {
                        Optional<Expression> result =  coercer.coerce(initializer.calculateResolvedType(), initializer, declr.getType().resolve());
                        result.ifPresent( i -> declr.setInitializer(i));
                    }


                }

                break;
            default:
                rewriteChildren(node);
                break;
        }

        if ( binExpr != null) {
            lastBinaryExpr = binExpr;
        }

    }

    private void processAssignExpr(AssignExpr node) {
        AssignExpr assignExpr = node;
        Expression target = assignExpr.getTarget();

        rewriteNode(assignExpr.getValue());

        if (target instanceof ArrayAccessExpr){
            ArrayAccessExpr arrayAccessor = (ArrayAccessExpr) target;
            rewriteNode(arrayAccessor.getName());

            ResolvedType resolvedType = arrayAccessor.getName().calculateResolvedType();

            boolean isMap = resolvedType.isAssignableBy(mapType);
            MethodUsage putSet = !resolvedType.isArray() ? getMethod(isMap ? "put" : "set", arrayAccessor.getName(), 2) : null;

            Expression value = assignExpr.getValue();
            if (assignExpr.getOperator() != Operator.ASSIGN) {

                Expression left = target;
                Expression right = value;

                // I don't like the getMethod being used in different ways (mdp)
                MethodUsage getter = !resolvedType.isArray() ? getMethod("get", arrayAccessor.getName(), 1) : null;
                ResolvedType leftType = null;
                if (getter != null) {
                    MethodCallExpr method = new MethodCallExpr(getter.getName(), arrayAccessor.getIndex());
                    method.setScope(arrayAccessor.getName());
                    left = method;

                    if (!resolvedType.isArray()) {
                        int index = isMap ? 1 : 0; // else List
                        Pair<ResolvedTypeParameterDeclaration, ResolvedType> pair =  resolvedType.asReferenceType().getTypeParametersMap().get(index);
                        leftType = pair.b;
                    }
                } else {
                    leftType = target.calculateResolvedType();
                }

                ResolvedType rightType = right.calculateResolvedType();

                if (isBigNumber(leftType)) {
                    Expression methodCallExpr = rewriteBigNumberOperator(left, leftType, right, rightType, getOperator(assignExpr));

                    value = methodCallExpr;
                } else if (isBigNumber(rightType)) {
                    Expression methodCallExpr = rewriteBigNumberOperator(right, rightType, left, leftType, getOperator(assignExpr));
                    value = methodCallExpr;
                } else {
                    BinaryExpr binaryExpr = new BinaryExpr(left, right, getOperator(assignExpr));
                    value = binaryExpr;
                }
            }

            if (putSet != null) {
                MethodCallExpr method = new MethodCallExpr(putSet.getName(), arrayAccessor.getIndex(), value);
                method.setScope(arrayAccessor.getName());
                assignExpr.replace(method);
            } else if (assignExpr.getValue() != value) {
                assignExpr.setValue(value);
            }
        } else if (target instanceof  FieldAccessExpr) {
            FieldAccessExpr fieldAccessor = (FieldAccessExpr) target;
            rewriteNode(fieldAccessor.getScope());

            Expression value = assignExpr.getValue();

            ResolvedType valueType;
            if (assignExpr.getOperator() != Operator.ASSIGN) {
                Expression left = target;
                Expression right = value;

                MethodUsage getter = getMethod("get", fieldAccessor, 0);
                ResolvedType leftType;
                if (getter != null) {
                    MethodCallExpr method = new MethodCallExpr(getter.getName());
                    method.setScope(fieldAccessor.getScope().clone());
                    left = method;
                    leftType = getter.returnType();;
                } else {
                    leftType = target.calculateResolvedType();
                }

                ResolvedType rightType = right.calculateResolvedType();

                if (isBigNumber(leftType)) {
                    Expression methodCallExpr = rewriteBigNumberOperator(left, leftType, right, rightType, getOperator(assignExpr));
                    value = methodCallExpr;
                    valueType = leftType;
                } else if (isBigNumber(rightType)) {
                    Expression methodCallExpr = rewriteBigNumberOperator(right, rightType, left, leftType, getOperator(assignExpr));
                    value = methodCallExpr;
                    valueType = rightType;
                } else {
                    BinaryExpr binaryExpr = new BinaryExpr(left, right, getOperator(assignExpr));
                    value = binaryExpr;
                    valueType = leftType;
                }
            } else {
                valueType = value.calculateResolvedType();
            }

            MethodUsage setter = getMethod("set", fieldAccessor, 1);




            if (setter != null) {
                Optional<Expression> coerced = coercer.coerce(valueType, value, setter.getParamType(0));
                if (coerced.isPresent()) {
                    value = coerced.get();
                }

                MethodCallExpr method = new MethodCallExpr(setter.getName(), value);
                method.setScope(((FieldAccessExpr) target).getScope());
                assignExpr.replace(method);
            } else if (assignExpr.getValue() != value) {
                Optional<Expression> coerced = coercer.coerce(valueType, value, fieldAccessor.calculateResolvedType());
                if (coerced.isPresent()) {
                    value = coerced.get();
                }

                assignExpr.setValue(value);
            }
        } else if (target instanceof NameExpr){
            if (assignExpr.getOperator() != Operator.ASSIGN) {
                Expression left = assignExpr.getTarget();
                Expression right = assignExpr.getValue();

                ResolvedType leftType = left.calculateResolvedType();
                ResolvedType rightType = right.calculateResolvedType();

                if (isBigNumber(leftType)) {
                    Expression methodCallExpr = rewriteBigNumberOperator(left, leftType, right, rightType, getOperator(assignExpr));
                    assignExpr.setValue(methodCallExpr);
                    assignExpr.setOperator(Operator.ASSIGN);
                } else if (isBigNumber(rightType)) {
                    Expression methodCallExpr = rewriteBigNumberOperator(right, rightType, left, leftType, getOperator(assignExpr));
                    assignExpr.setValue(methodCallExpr);
                    assignExpr.setOperator(Operator.ASSIGN);
                }
            }
        }
    }

    private BinaryExpr rewriteAssignExprBinaryExpression(AssignExpr n) {
        BinaryExpr binExpr = new BinaryExpr(n.getTarget().clone(), n.getValue().clone(), getOperator(n));
        n.setValue(binExpr);

        return binExpr;
    }

    private static BinaryExpr.Operator getOperator(AssignExpr n) {
        String opStr = n.getOperator().asString();
        opStr = opStr.substring(0, opStr.length()-1);
        BinaryExpr.Operator op = null;
        switch(opStr) {
            case "+": op = BinaryExpr.Operator.PLUS; break;
            case "-": op = BinaryExpr.Operator.MINUS; break;
            case "*": op = BinaryExpr.Operator.MULTIPLY; break;
            case "/": op = BinaryExpr.Operator.DIVIDE; break;
            case "%": op = BinaryExpr.Operator.REMAINDER; break;
        }
        return op;
    }

    public void rewriteChildren(Node n) {
        List<Node> children = n.getChildNodes();
        if (children.isEmpty()) {
            return;
        }

        // The list must be cloned, because children are replaced as the tree is processed.
        for (Node child : new ArrayList<>(children)) {
            rewriteNode( child);
        }
    }

    public void processBinaryExpr(BinaryExpr binExpr, Node node) {
        BinaryExprTypes types = nodeMap.computeIfAbsent(binExpr, k -> new BinaryExprTypes(k));

        if ( node == binExpr.getLeft() ) {
            types.setLeft(binExpr.getLeft());
        } else if ( node == binExpr.getRight() ) {
            types.setRight(binExpr.getRight());
        }

        if (types.getLeftType() != null && types.getRightType() != null) {
            Node parent = binExpr.getParentNode().get();

            nodeMap.remove(binExpr);
            rewrite(types);

            if (binExpr != rootBinaryExpr) {
                Node current = parent;
                Node prev = binExpr;
                while (current.getClass() != BinaryExpr.class) {
                    prev = current;
                    current = current.getParentNode().get();
                }
                processBinaryExpr((BinaryExpr) current, prev);
            }
        }
    }

    public Expression rewrite(BinaryExprTypes binExprTypes) {
        Expression e1 = null;
        Expression e2 = null;
        if (binExprTypes.binaryExpr.getLeft().getClass() != NullLiteralExpr.class &&
            isBigNumber(binExprTypes.leftType)) {
            e1 = binExprTypes.left;
            e2 = binExprTypes.right;
        } else if (binExprTypes.binaryExpr.getRight().getClass() != NullLiteralExpr.class &&
                   isBigNumber(binExprTypes.rightType)) {
            e1 = binExprTypes.right;
            e2 = binExprTypes.left;
        }

        Expression methodCallExpr = rewrite(binExprTypes.getBinaryExpr(), e1, e2);

        return methodCallExpr;
    }

    private Expression rewrite(BinaryExpr binExpr, Expression e1, Expression e2) {
        Expression methodCallExpr = null;
        if (e1 != null && e2 != null) {
            BinaryExpr.Operator opEnum = binExpr.getOperator();
            methodCallExpr = rewriteBigNumberOperator(e1, e1.calculateResolvedType(), e2, e2.calculateResolvedType(), opEnum);

            binExpr.replace(methodCallExpr);
        }
        return methodCallExpr;
    }

    boolean isBigNumber(ResolvedType type) {
        if (bigDecimalType.isAssignableBy(type) || bigIntegerType.isAssignableBy(type)) {
            return true;
        }

        return false;
    }

    private Expression rewriteBigNumberOperator(Expression e1, ResolvedType e1Type, Expression e2, ResolvedType e2Type, BinaryExpr.Operator opEnum) {
        String op = null;
        Expression result = null;

        switch (opEnum) {
            case MULTIPLY : op = "multiply"; break;
            case DIVIDE : op = "divide"; break;
            case PLUS : op = "add"; break;
            case MINUS : op = "subtract"; break;
            case REMAINDER : op = "remainder"; break;
            case EQUALS : op = "compareTo"; break;
            case NOT_EQUALS : op = "compareTo"; break;
            case GREATER : op = "compareTo"; break;
            case GREATER_EQUALS : op = "compareTo"; break;
            case LESS : op = "compareTo"; break;
            case LESS_EQUALS : op = "compareTo"; break;
        }

        MethodCallExpr methodCallExpr;

        Optional<Expression> coerceResult = coercer.coerce(e2Type, e2, e1Type);
        if (coerceResult.isPresent()) {
            e2 = coerceResult.get();
        }

        Expression mathContext = context.getParser().parseExpression("java.math.MathContext.DECIMAL128").getResult().get();

        List<Expression> children = getArgumentsWithUnwrap(e2);
        if (!op.equals("compareTo") && e1Type.equals(bigDecimalType)) {
            children.add(mathContext);
        }

        methodCallExpr = new MethodCallExpr(op, children.toArray( new Expression[0]));
        methodCallExpr.setScope(e1);
        result = methodCallExpr;

        if (op.equals("compareTo")) {
            result = new BinaryExpr(methodCallExpr, new IntegerLiteralExpr("0"), opEnum);
        }

        return result;
    }

    public static List<Expression> getArgumentsWithUnwrap(Expression e) {
        List<Expression> children = new ArrayList<>();
        if (!e.isEnclosedExpr()) {
            children.add(e);
        } else {
            // unwrap unneeded ()
            e.getChildNodes().stream()
              .map(Expression.class::cast)
              .forEach(children::add);
        }
        return children;
    }

    public Node maybeRewriteToGetter(FieldAccessExpr n) {
        rewriteNode(n.getScope());

        MethodUsage getter = getMethod("get", n, 0);

        if (getter != null) {
            MethodCallExpr methodCallExpr = new MethodCallExpr(getter.getName());
            methodCallExpr.setScope(n.getScope());
            n.replace(methodCallExpr);

            return methodCallExpr;
        }

        return null;
    }

    public Node rewriteArrayAccessExpr(ArrayAccessExpr n) {
        rewriteNode(n.getName());

        MethodCallExpr methodCallExpr = null;
        if ( !(n.getParentNode().get() instanceof  AssignExpr)) {
            ResolvedType resolvedType = n.getName().calculateResolvedType();


            if (resolvedType.isArray()) {
                return null;
            }


            methodCallExpr = new MethodCallExpr("get", n.getIndex());
            methodCallExpr.setScope(n.getName());

            n.replace(methodCallExpr);

        }
        return methodCallExpr;
    }


    private static MethodUsage getMethod(String name, Expression e, int x) {
        if (e instanceof  MethodCallExpr) {
            return getMethod(name, (MethodCallExpr)e, x );
        } else if (e instanceof  FieldAccessExpr) {
            return getMethod(name, (MethodCallExpr)e, x );
        }

        return null;
    }

    private MethodUsage getMethod(String getterSetter, FieldAccessExpr n, int x) {
        ResolvedReferenceTypeDeclaration d;

        try {
            ResolvedType type = n.getScope().calculateResolvedType();
            d = type.asReferenceType().getTypeDeclaration().get();
        } catch(Exception e) {
            // scope not resolvable, most likely a package
            return null;
        }

        try {
            if (d.getField(n.getName().asString()).accessSpecifier() == AccessSpecifier.PUBLIC) {
                // do not rewrite if it allows Public access.
                return null;
            }
        } catch (UnsolvedSymbolException e) {
           // swallow
        }

        String getterTarget = getterSetter(getterSetter, n.getName().asString());
        String valueTarget = n.getNameAsString().toLowerCase();
        for (MethodUsage candidate : d.getAllMethods()) {
            String methodName = candidate.getName();
            if (!candidate.getDeclaration().isStatic() && candidate.getNoParams() == x &&
                (methodName.equals(getterTarget) || methodName.equals(valueTarget))) {
                return candidate;
            }
        }

        throw new UnsolvedSymbolException("The node has neither a public field or a getter method for : " + n);
    }

    private static MethodUsage getMethod(String name, MethodCallExpr n, int x) {
        ResolvedType type = n.calculateResolvedType();
        ResolvedReferenceTypeDeclaration d = type.asReferenceType().getTypeDeclaration().get();

        for (MethodUsage candidate : d.getAllMethods()) {
            String methodName = candidate.getName();
            if (!candidate.getDeclaration().isStatic() && candidate.getNoParams() == x && methodName.equals(name)) {
                return candidate;
            }
        }

        throw new IllegalStateException("The node has neither a public field or a getter method for : " + n);
    }

    private static String getterSetter(String getterSetter, String name) {
        return getterSetter + name.substring(0, 1).toUpperCase() + name.substring(1);
    }



    public static class BinaryExprTypes {
        private BinaryExpr binaryExpr;
        ResolvedType leftType;
        ResolvedType rightType;

        Expression left;
        Expression right;

        public BinaryExprTypes(BinaryExpr binaryExpr) {
            this.binaryExpr = binaryExpr;
        }

        public BinaryExpr getBinaryExpr() {
            return binaryExpr;
        }

        public Expression getLeft() {
            return left;
        }

        public void setLeft(Expression left) {
            this.left = left;
            this.leftType = left.calculateResolvedType();
        }

        public Expression getRight() {
            return right;
        }

        public void setRight(Expression right) {
            this.right = right;
            this.rightType = right.calculateResolvedType();
        }

        public ResolvedType getLeftType() {
            return leftType;
        }

        public ResolvedType getRightType() {
            return rightType;
        }
    }
}

