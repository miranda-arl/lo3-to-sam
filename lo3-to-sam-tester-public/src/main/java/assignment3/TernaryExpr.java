package assignment3;

import java.util.Map;
import java.util.UUID;

public class TernaryExpr implements Expr {
    public final Expr condition;
    public final Expr thenBranch;
    public final Expr elseBranch;

    public TernaryExpr(Expr condition, Expr thenBranch, Expr elseBranch) throws CompileException {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;

        if (condition == null) {
            throw new CompileException("Missing condition in ternary operator");
        }
        if (thenBranch == null) {
            throw new CompileException("Missing expression after '?' in ternary operator");
        }
        if (elseBranch == null) {
            throw new CompileException("Missing expression after ':' in ternary operator");
        }
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        String thenLabel = "then_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String endLabel = "end_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String conditionCode = condition.generateCode(symbolTable, classInfo, classTables);
        String thenCode = thenBranch.generateCode(symbolTable, classInfo, classTables);
        String elseCode = elseBranch.generateCode(symbolTable, classInfo, classTables);

        return conditionCode +
               "JUMPC " + thenLabel + "\n" +
               elseCode +
               "JUMP " + endLabel + "\n" +
               thenLabel + ":\n" +
               thenCode +
               endLabel + ":\n";
    }
    
    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        Type condType = condition.getType(symbolTable, classInfo, classTables);
        Type thenType = thenBranch.getType(symbolTable, classInfo, classTables);
        Type elseType = elseBranch.getType(symbolTable, classInfo, classTables);

        if (!condType.equals(Type.BOOL)) {
            throw new CompileException("Condition must be boolean");
        }
        if (!thenType.equals(elseType)) {
            throw new CompileException("Branches must have same type");
        }
        return thenType;
    }
}
