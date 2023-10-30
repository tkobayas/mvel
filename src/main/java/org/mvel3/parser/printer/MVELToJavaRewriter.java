package org.mvel3.parser.printer;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Solver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.CompilationUnitContext;
import com.github.javaparser.utils.Pair;
import org.mvel3.MVEL;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.transpiler.context.TranspilerContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.github.javaparser.ast.type.PrimitiveType.Primitive.byTypeName;
import static org.mvel3.transpiler.MVELTranspiler.handleParserResult;

public class MVELToJavaRewriter {
    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private ResolvedType stringType;

    private ResolvedType mapType;

    private ResolvedType listType;

    private MethodUsage mapPut;

    private MethodUsage listSet;

    private ResolvedType contextObjectType;

    private ResolvedType rootObjectType;

    private BinaryExpr   rootBinaryExpr;
    private BinaryExpr   lastBinaryExpr;

    Map<BinaryExpr, BinaryExprTypes> nodeMap;

    private CoerceRewriter coercer;

    private Set<String>    declaredVars;

    private TranspilerContext context;

    private CompilationUnitContext unitContext;

    private MethodUsage mapGetMethod;

    Expression mathContext;

    public MVELToJavaRewriter(TranspilerContext context) {
        this.context = context;
        unitContext = (CompilationUnitContext) JavaParserFactory.getContext(context.getUnit(), context.getTypeSolver());
        Solver solver = context.getFacade().getSymbolSolver();
        coercer = context.getCoercer();

        declaredVars = new HashSet<>();

        bigDecimalType = solver.classToResolvedType(BigDecimal.class);
        bigIntegerType = solver.classToResolvedType(BigInteger.class);
        stringType = solver.classToResolvedType(String.class);
        mapType = solver.classToResolvedType(Map.class);
        listType = solver.classToResolvedType(List.class);

        mathContext = handleParserResult(context.getParser().parseExpression("java.math.MathContext.DECIMAL128"));

        if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid()) {
            rootObjectType = solver.classToResolvedType(context.getEvaluatorInfo().rootDeclaration().type().getClazz());
        }

        contextObjectType = solver.classToResolvedType(context.getEvaluatorInfo().variableInfo().type().getClazz());

        nodeMap = new IdentityHashMap<>();

        ResolvedReferenceTypeDeclaration d = mapType.asReferenceType().getTypeDeclaration().get();
        mapGetMethod = findGetterSetter("get", "", 1, d);
    }

    private void rewriteNode(Node node) {
        BinaryExpr binExpr = null;

        switch (node.getClass().getSimpleName()) {
            case "DrlNameExpr":
            case "NameExpr":
                NameExpr nameExpr = (NameExpr) node;
                String name = nameExpr.getNameAsString();
                if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid() && context.getEvaluatorInfo().rootDeclaration().name().equals(name)) {
                    // do not rewrite at root objects.
                    return;
                }

                if (!declaredVars.contains(name)) {
                    node = rewriteNameToContextObject(nameExpr);
                }
                break;
            case "MethodCallExpr":
                // This attempts to only rewrite methods that are not called against a variable
                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                methodCallExpr.getArguments().stream().forEach( a -> rewriteNode(a));

                if (!methodCallExpr.hasScope() && !methodCallExpr.getNameAsString().contains(".")) {
                    node = rewriteMethodToContextObject(methodCallExpr);
                }
                break;
        }

        switch (node.getClass().getSimpleName())  {
            case "CastExpr":
                processInlineCastExpr((CastExpr) node);
                break;
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
  //              ResolvedMethodDeclaration resolved = methodCall.resolve();

//                ResolvedParameterDeclaration resolvedParameterDeclaration = resolved.getParam(0);


//                ResolvedType resolvedType = methodCall.getScope().get().calculateResolvedType();

                if (methodCall.getScope().isPresent()) {
                    rewriteNode(methodCall.getScope().get());
                }
                for (Expression arg : methodCall.getArguments()) {
                    rewriteNode(arg);
                }

                maybeCoerceArguments(methodCall);
                break;
            case "VariableDeclarationExpr":
                VariableDeclarationExpr declrExpr = (VariableDeclarationExpr) node;
                VariableDeclarator declr = declrExpr.getVariable(0);

                declaredVars.add(declr.getNameAsString());

                if ( declr.getInitializer().isPresent()) {
                    Expression initializer = declr.getInitializer().get();
                    rewriteNode(initializer);

                    initializer = declr.getInitializer().get(); // get again, incase it was written

                    // Don't coerce 'var' types, as they will be assigned to what ever the return type is
                    Type type = declr.getType();
                    if (!(initializer instanceof TextBlockLiteralExpr) &&
                        !(type.isClassOrInterfaceType() && type.asClassOrInterfaceType().getNameAsString().equals("var"))) {
                        // This may not resolve, there is invalid syntax or types do not match parameters.
                        ResolvedType initType = initializer.calculateResolvedType();
                        Expression result =  coercer.coerce(initType, initializer, declr.getType().resolve());
                        if (result != null) {
                            declr.setInitializer(result);
                        }
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

    private void maybeCoerceArguments(MethodCallExpr methodCall) {
        // Get the Method declaration and it's resolved types.
        ResolvedType scope = context.getFacade().getType(methodCall.getScope().get());
        ResolvedType resolvedScope = methodCall.getScope().get().calculateResolvedType();

        List<ResolvedMethodDeclaration> methods = scope.asReferenceType().getAllMethods();
        List<ResolvedType> argTypes = Arrays.asList(new ResolvedType[methodCall.getArguments().size()]);
        for (int i = 0; i < methodCall.getArguments().size(); i++ ) {
            argTypes.set(i, methodCall.getArguments().get(i).calculateResolvedType());
        }

        methodLoop:
        for ( ResolvedMethodDeclaration methodDeclr : methods) {
            // Find the method with same name and number of arguments
            // Then find the first subset of those, that all arguments either match or
            // can be coerced
            if (methodDeclr.getNumberOfParams() == methodCall.getArguments().size() &&
                methodDeclr.getName().equals(methodCall.getNameAsString())) {
                // copy the list of original args, coerced ones will replace this.
                // only when we have a full match of args, will it replace the coerced ones in the methodCall
                List<Expression> newArgs = new ArrayList<>(methodCall.getArguments());

                // check each arg, see if not assignable, see it can coerce.
                for (int i = 0, size = methodDeclr.getNumberOfParams(); i < size; i++) {
                    ResolvedType            paramType         = methodDeclr.getParam(i).getType();

                    if (paramType.isTypeVariable()) {
                        ResolvedTypeVariable typeVariable = paramType.asTypeVariable();
                        String typeName = typeVariable.asTypeParameter().getName();
                        for ( Pair<ResolvedTypeParameterDeclaration, ResolvedType> pair : resolvedScope.asReferenceType().getTypeParametersMap() ) {
                            if (typeName.equals(pair.a.asTypeParameter().getName())) {
                                paramType = pair.b;
                                break;
                            }
                        }
                    }

                    if (!((paramType).isAssignableBy(argTypes.get(i)))) {
                        // else try coercion
                        Expression result = coercer.coerce(argTypes.get(i), methodCall.getArguments().get(i), paramType);
                        if (result == null) {
                            // cannot be assiged and also cannot be coerced, so the method does not match.
                            continue methodLoop;
                        }
                        newArgs.set(i, result);
                    }
                }
                // We have a matched method name and matched arguments, use this one.
                for (int j = 0; j < newArgs.size(); j++) {
                    if (newArgs.get(j) != methodCall.getArgument(j)) {
                        // the arg was replaced via coercion, replace it in the actual methodCall
                        methodCall.setArgument(j, newArgs.get(j));
                    }
                }
            }
        }
    }

    private Expression processInlineCastExpr(CastExpr node) {
        Expression expr;

        // This is assuming the name used is a reference type. What if it was a primitive or an array? // @TODO support other ResolvedTypes (mdp)
        ResolvedType targetType = context.getFacade().convertToUsage(node.getType());

        ResolvedType sourceType = node.getExpression().calculateResolvedType();
        if (sourceType.isAssignableBy(targetType)) {
            // have to put into an () enclosure, as this was not in the original grammar due to #....#
            EnclosedExpr enclosure = new EnclosedExpr();
            node.replace(enclosure);
            enclosure.setInner(node);
            expr = enclosure; // a normal casts suffices
        } else {
            // else try coercion
            Expression result = coercer.coerce(sourceType, node.getExpression(), targetType);
            if (result == null) {
                throw new RuntimeException("Cannot be cast or coerced: " + node);
            }
            expr = result;

            node.replace(expr);
        }

        return expr;
    }

    public boolean isPublicField(ResolvedReferenceTypeDeclaration d, String name) {
        for (ResolvedFieldDeclaration f : d.getAllFields()) {
            if (f.accessSpecifier() == AccessSpecifier.PUBLIC && f.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private Expression rewriteNameToContextObject(NameExpr nameExpr) {
        String name = nameExpr.getNameAsString();
        Expression expr = nameExpr;
        if (rootObjectType != null) {
            NameExpr scope = new NameExpr(context.getEvaluatorInfo().rootDeclaration().name());

            ResolvedReferenceTypeDeclaration d = rootObjectType.asReferenceType().getTypeDeclaration().get();
            FieldAccessExpr fieldAccessExpr = new FieldAccessExpr(scope, name);

            if (isPublicField(d, name)) {
                // public field exists, so use that.
                nameExpr.replace(fieldAccessExpr);
                expr = fieldAccessExpr;
                //rewriteNode(expr);
            } else {
                // temporary swap, or type and thus method resolving will not work
                nameExpr.replace(fieldAccessExpr);

                Node result = maybeRewriteToGetter(fieldAccessExpr);
                if (result != null) {
                    expr = (Expression) result;
                } else {
                    // no getter either, so revert
                    fieldAccessExpr.replace(nameExpr);
                }
            }
        }

        return expr;
    }

    private Expression rewriteMethodToContextObject(MethodCallExpr methodCallExpr) {
        Expression expr = methodCallExpr;
        if (rootObjectType != null) {
            // clone this, so we can try and resolve it against the root pattern as scope.
            // Note if this is a static import, the root will take priority.
            MethodCallExpr cloned = methodCallExpr.clone();
            cloned.setScope(new NameExpr(context.getEvaluatorInfo().rootDeclaration().name()));

            methodCallExpr.replace(cloned); // temporary swap, so type resolving works
            MethodUsage methodUsage = null;
            try {
                methodUsage = context.getFacade().solveMethodAsUsage(cloned);
            } catch (RuntimeException e) {
                // swallow, we check null anyway. I's dumb this is a runtime exception, as no solving is valid.
            }

            if (methodUsage != null) {
                expr = cloned;
            } else {
                // swap back again.
                cloned.replace(methodCallExpr);
            }
        }

        return expr;
    }

    private void processAssignExpr(AssignExpr node) {
        AssignExpr assignExpr = node;
        Expression target = assignExpr.getTarget();

        rewriteNode(assignExpr.getValue());

        if (target instanceof ArrayAccessExpr) {
            ArrayAccessExpr arrayAccessor = (ArrayAccessExpr) target;
            rewriteNode(arrayAccessor.getName());

            ResolvedType resolvedType = arrayAccessor.getName().calculateResolvedType();

            MethodUsage putSet = null;
            boolean isMap = resolvedType.isAssignableBy(mapType);
            boolean isList = resolvedType.isAssignableBy(listType);
            putSet = getPutSet(isMap, isList);

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
                Expression coerced = coercer.coerce(valueType, value, setter.getParamType(0));
                if (coerced != null) {
                    value = coerced;
                }

                MethodCallExpr method = new MethodCallExpr(setter.getName(), value);
                method.setScope(((FieldAccessExpr) target).getScope());
                assignExpr.replace(method);
            } else if (assignExpr.getValue() != value) {
                Expression coerced = coercer.coerce(valueType, value, fieldAccessor.calculateResolvedType());
                if (coerced != null) {
                    value = coerced;
                }

                assignExpr.setValue(value);
            }
        } else if (target instanceof NameExpr){
            NameExpr nameExpr = (NameExpr) target;

            // If this updates a var that exists in the context, make sure the result is assigned back there too.
            if ( context.getInputs().contains(nameExpr.getNameAsString())) {
                Class contextClass = context.getEvaluatorInfo().variableInfo().type().getClazz();
                if (contextClass.isAssignableFrom(Map.class)) {

                    if (assignExpr.getParentNode().get() instanceof ExpressionStmt) {
                        // This is its own statement, so no need to wrap the assignment and return the new value
                        // a  = 5 becomes context.put("a", a = 5);
                        MethodCallExpr putMethod = new MethodCallExpr(new NameExpr(new SimpleName("context")), "put");
                        assignExpr.replace(putMethod);
                        putMethod.setArguments(NodeList.nodeList(new StringLiteralExpr(nameExpr.getNameAsString()),
                                                                 assignExpr));
                    } else {
                        // This assigment is part of some expression, so wrap the asssignment and return the new value
                        // return a = 5 becomes return org.mvel3.MVEL.putMap(context, "a", a = 5);
                        Expression scope = handleParserResult(context.getParser().parseExpression(MVEL.class.getCanonicalName()));

                        MethodCallExpr putMethod = new MethodCallExpr( scope,"putMap");
                        assignExpr.replace(putMethod);
                        putMethod.addArgument(new NameExpr("context"));
                        putMethod.addArgument(new StringLiteralExpr(nameExpr.getNameAsString()));
                        putMethod.addArgument(assignExpr);
                    }
                } else if (contextClass.isAssignableFrom(List.class)) {
                    if (assignExpr.getParentNode().get() instanceof ExpressionStmt) {
                        // This is its own statement, so no need to wrap the assignment and return the new value
                        // a = 5 becomes context.set(i, a = 5);
                        MethodCallExpr setMethod = new MethodCallExpr(new NameExpr(new SimpleName("context")), "set");
                        assignExpr.replace(setMethod);
                        setMethod.setArguments(NodeList.nodeList(new IntegerLiteralExpr(context.getEvaluatorInfo().variableInfo().indexOf(nameExpr.getNameAsString())),
                                                                 assignExpr));
                    } else {
                        // This assigment is part of some expression, so wwrap the asssignment and return the new value
                        // return a = 5 becomes return org.mvel3.MVEL.setList(context, 3, a = 5);
                        Expression scope = handleParserResult(context.getParser().parseExpression(MVEL.class.getCanonicalName()));

                        MethodCallExpr setMethod = new MethodCallExpr( scope,"setList");
                        assignExpr.replace(setMethod);
                        setMethod.addArgument("context");
                        setMethod.addArgument(new IntegerLiteralExpr(context.getEvaluatorInfo().variableInfo().indexOf(nameExpr.getNameAsString())));
                        setMethod.addArgument(assignExpr);
                    }
                } else {
                    // pojo
                    // @TOOD I need to call the generated method below. But ideally only if it's part of some parent.
                    addSetterMethod(nameExpr);
                    MethodCallExpr setMethod = new MethodCallExpr( "contextSet" + nameExpr.getNameAsString());
                    assignExpr.replace(setMethod);
                    setMethod.addArgument(new NameExpr("context"));
                    setMethod.addArgument(assignExpr);
                }
            }

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

    public void addSetterMethod(NameExpr nameExpr) {
        MethodDeclaration methodDeclr = context.getClassDeclaration().addMethod( "contextSet" + nameExpr);
        methodDeclr.setStatic(true);
        methodDeclr.setPublic(true);


        ResolvedType propertyResolvedType = nameExpr.calculateResolvedType();
        Type propertyType = resolvedTypeToType(propertyResolvedType);
        Type contextType = resolvedTypeToType(contextObjectType);

        Parameter c = new Parameter(contextType, "context" );
        Parameter v = new Parameter(propertyType, "v" );
        methodDeclr.setParameters(NodeList.nodeList(c, v));

        methodDeclr.setType(propertyType.clone());

        ResolvedReferenceTypeDeclaration d = contextObjectType.asReferenceType().getTypeDeclaration().get();

        MethodUsage methodUsage = findGetterSetter("set", nameExpr.getNameAsString(), 1, d);

        MethodCallExpr setMethod = new MethodCallExpr(new NameExpr(new SimpleName("context")), methodUsage.getName());
        setMethod.addArgument(new NameExpr("v"));

        ReturnStmt returnStmt = new ReturnStmt(new NameExpr("v"));

        BlockStmt blockStmt = new BlockStmt();
        blockStmt.addStatement(setMethod);
        blockStmt.addStatement(returnStmt);
        methodDeclr.setBody(blockStmt);
    }

    private MethodUsage getPutSet(boolean isMap, boolean isList) {
        MethodUsage putSet = null;
        Set<MethodUsage> methods;

        if (isMap) {
            if (mapPut != null) {
                return mapPut;
            }
            methods = mapType.asReferenceType().getTypeDeclaration().get().getAllMethods();
            for (MethodUsage m : methods) {
                if (m.getName().equals("put")) {
                    putSet = m;
                    break;
                }
            }
            mapPut = putSet;
        } else if (isList) {
            if (listSet != null) {
                return listSet;
            }
            methods = listType.asReferenceType().getTypeDeclaration().get().getAllMethods();
            for (MethodUsage m : methods) {
                if (m.getName().equals("set")) {
                    putSet = m;
                    break;
                }
            }
            listSet = putSet;
        }


        return putSet;
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
            ResolvedType e1Type = e1.calculateResolvedType();
            ResolvedType e2Type = e2.calculateResolvedType();

            if (binExpr.getOperator() == BinaryExpr.Operator.PLUS &&
                (stringType.isAssignableBy(e1Type) || stringType.isAssignableBy(e2Type))) {
                // Do not rewrite this, because it's a string concatenation, due to JVM operator overloading.
                return null;
            }

            BinaryExpr.Operator opEnum = binExpr.getOperator();
            methodCallExpr = rewriteBigNumberOperator(e1, e1Type, e2, e2Type, opEnum);

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

        Expression coerceResult = coercer.coerce(e2Type, e2, e1Type);
        if (coerceResult != null) {
            e2 = coerceResult;
        }

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

        ResolvedType type;
        try {
            type = n.getScope().calculateResolvedType();
        } catch (Exception e) {
            // It cannot be known if 'n' is a package which cannot be resolved or a package.
            // This is a ugly way to simply do nothing if it doesn't resolve and it's assumed (maybe wrongly) it was a
            // package, instead of some other failure.
            return n;
        }
        Expression arg = null;
        MethodUsage getter = null;
        if ( mapType.isAssignableBy(type)) {
            getter = mapGetMethod;
            arg = new StringLiteralExpr(n.getNameAsString());
        } else {
            getter = getMethod("get", n, 0);
        }

        MethodCallExpr methodCallExpr = createGetterMethodCallExpr(n, getter, arg);
        if (methodCallExpr != null) {
            return methodCallExpr;
        }

        return null;
    }

    private static MethodCallExpr createGetterMethodCallExpr(FieldAccessExpr n, MethodUsage getter, Expression arg) {
        if (getter != null) {
            MethodCallExpr methodCallExpr = new MethodCallExpr(getter.getName());
            if (arg != null) {
                methodCallExpr.addArgument(arg);
            }
            methodCallExpr.setScope(n.getScope());
            n.replace(methodCallExpr);

            return methodCallExpr;
        }
        return null;
    }

    public Node rewriteArrayAccessExpr(ArrayAccessExpr n) {
        if (n.getParentNode().get() instanceof  AssignExpr && ((AssignExpr)n.getParentNode().get()).getTarget() == n) {
            // do not rewrite the setter part of the ArrayAccessExpr, but getting is fine.
            return n;
        }

        rewriteNode(n.getName());

        MethodCallExpr methodCallExpr = null;
        ResolvedType resolvedType = n.getName().calculateResolvedType();


        if (resolvedType.isArray()) {
            return null;
        }

        methodCallExpr = new MethodCallExpr("get", n.getIndex());
        methodCallExpr.setScope(n.getName());

        n.replace(methodCallExpr);
        return methodCallExpr;
    }


    private static MethodUsage getMethod(String name, Expression e, int x) {
        if (e instanceof  MethodCallExpr) {
            return getMethod((MethodCallExpr)e, name, x);
        } else if (e instanceof  FieldAccessExpr) {
            return getMethod((MethodCallExpr)e, name, x);
        } else if (e instanceof  NameExpr) {
            return getMethod(name, e.calculateResolvedType(), ((NameExpr)e).getNameAsString(), x );
        }

        return null;
    }

    public  static MethodUsage getMethod(String getterSetter, FieldAccessExpr n, int x) {
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

        MethodUsage candidate = findGetterSetter(getterSetter, n.getNameAsString(), x, d);
        if (candidate != null) {
            return candidate;
        }

        throw new UnsolvedSymbolException("The node has neither a public field or a getter method for : " + n);
    }

    public  static MethodUsage getMethod(String getterSetter, ResolvedType type, String name, int x) {
        ResolvedReferenceTypeDeclaration d;

        try {
            d = type.asReferenceType().getTypeDeclaration().get();
        } catch(Exception e) {
            // scope not resolvable, most likely a package
            return null;
        }

        MethodUsage candidate = findGetterSetter(getterSetter, name, x, d);
        if (candidate != null) {
            return candidate;
        }

        throw new UnsolvedSymbolException("The node has neither a public field or a getter method for : " + name);
    }

    public static MethodUsage findGetterSetter(String getterSetter, String name, int x, ResolvedReferenceTypeDeclaration d) {
        String getterTarget = getterSetter(getterSetter, name);
        for (MethodUsage candidate : d.getAllMethods()) {
            String methodName = candidate.getName();
            if (candidate.getDeclaration().accessSpecifier() == AccessSpecifier.PUBLIC &&
                !candidate.getDeclaration().isStatic() &&
                candidate.getNoParams() == x &&
                (methodName.equals(getterTarget) || methodName.equals(name))) {
                return candidate;
            }
        }
        return null;
    }

    private static MethodUsage getMethod(MethodCallExpr n, String name, int x) {
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
        if ( name == null || name.isEmpty()) {
            return getterSetter;
        } else {
            return getterSetter + name.substring(0, 1).toUpperCase() + name.substring(1);
        }
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
            short a = 10;
            Short.valueOf(a);
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

    Type resolvedTypeToType(ResolvedType resolved) {
        if(resolved.isPrimitive()) {
            ResolvedPrimitiveType asPrimitive = resolved.asPrimitive();
            switch(asPrimitive.describe().toLowerCase()) {
                case "byte": return new PrimitiveType(Primitive.BYTE);
                case "short": return new PrimitiveType(Primitive.SHORT);
                case "char": return new PrimitiveType(Primitive.CHAR);
                case "int": return new PrimitiveType(Primitive.INT);
                case "long": return new PrimitiveType(Primitive.LONG);
                case "boolean": return new PrimitiveType(Primitive.BOOLEAN);
                case "float": return new PrimitiveType(Primitive.FLOAT);
                case "double": return new PrimitiveType(Primitive.DOUBLE);
            }
        }
        return handleParserResult(context.getParser().parseClassOrInterfaceType(resolved.describe()));
    }
}

