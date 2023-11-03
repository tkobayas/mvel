package org.mvel3.transpiler;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.Statement;
import org.mvel2.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.transpiler.context.TranspilerContext;

import java.util.List;

public interface EvalPre {
    NodeList<Statement> evalPre(EvaluatorInfo<?, ?, ?> evalInfo, TranspilerContext<?, ?, ?> context, NodeList<Statement> statements);
}
