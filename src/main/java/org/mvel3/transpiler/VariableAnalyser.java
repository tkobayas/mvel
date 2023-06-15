package org.mvel3.transpiler;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import java.util.HashSet;
import java.util.Set;

public class VariableAnalyser extends GenericVisitorAdapter<Boolean, Void> {
    private Set<String> inputs = new HashSet<>();

    private Set<String> declared = new HashSet<>();

    public VariableAnalyser() {
    }

    public Boolean visit(NameExpr n, Void arg) {
        if (!declared.contains(n.getNameAsString())) {
            inputs.add(n.getNameAsString());
        }
        return null;
    }

    @Override
    public Boolean visit(VariableDeclarator n, Void arg) {
        declared.add(n.getNameAsString());

        return null;
    }

    public Set<String> getInputs() {
        return inputs;
    }
}
