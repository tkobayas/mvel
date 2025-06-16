package org.mvel3.parser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import org.mvel3.parser.ast.visitor.DrlGenericVisitor;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;

import java.util.List;

public class WithStatement extends Statement {

    private final Expression modifyObject;
    private final NodeList<Statement> expressions;

    public WithStatement(TokenRange tokenRange, Expression modifyObject, NodeList<Statement> expressions) {
        super(tokenRange);
        this.modifyObject = modifyObject;
        this.expressions = expressions;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return ((DrlGenericVisitor<R, A>)v).visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        ((DrlVoidVisitor<A>)v).visit(this, arg);
    }

    public NodeList<Statement> getExpressions() {
        return expressions;
    }

    public Expression getWithObject() {
        return modifyObject;
    }

}
