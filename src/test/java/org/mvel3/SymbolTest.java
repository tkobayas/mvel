package org.mvel3;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.Test;

public class SymbolTest {

    @Test
    public void test1() {
        String str =
            "package org.example;\n" +
            "import " + Person.class.getCanonicalName() + ";\n" +
            "class A{\n" +
            "    public void foo(Object param) {\n" +
            "        Person p;\n" +
            "        p = new Person();\n" +
            "        p.nicknamePublic = \"yoda\";\n" +
            "    } " +
            "}";

        System.out.println(str);

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser
                .getConfiguration()
                .setSymbolResolver(symbolSolver);
        CompilationUnit cu = StaticJavaParser.parse(str);

        cu.findAll(VariableDeclarator.class).forEach(
                vd -> {
                    ResolvedType resolvedType = vd.resolve().getType();
                    System.out.println(resolvedType);
//                    JavaParserFacade.get(typeSolver).solve(vd.getName());
//                    ReferenceType refType = vd.getType().asReferenceType();
//                    ResolvedType resType = refType.resolve();
//                    System.out.println(resType);
        });

        cu.findAll(FieldAccessExpr.class).forEach(
                vd -> {
                    ResolvedType resolvedType = vd.resolve().getType();
                    System.out.println(resolvedType);
//                    JavaParserFacade.get(typeSolver).solve(vd.getName());
//                    ReferenceType refType = vd.getType().asReferenceType();
//                    ResolvedType resType = refType.resolve();
//                    System.out.println(resType);
                });
    }
}
