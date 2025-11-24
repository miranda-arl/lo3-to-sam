package assignment3;

import java.util.Map;

public class LiteralExpr implements Expr {
    public final Object value;

    public LiteralExpr(Object value) {
        this.value = value;
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        if (value instanceof Integer) return Type.INT;
        if (value instanceof Boolean) return Type.BOOL;
        if (value instanceof String) return Type.STRING;
        if (value == null) return Type.VOID;
        throw new CompileException("Unknown literal type: " + value);
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        if (value instanceof Integer) return "PUSHIMM " + value + "\n";
        if (value instanceof Boolean) return "PUSHIMM " + ((boolean)value ? 1 : 0) + "\n";
        if (value instanceof String) return "PUSHIMMSTR \"" + value + "\"\n";
        if (value == null) return "PUSHIMM 0\n"; // Added code generation for null literal. pushimm -1?
        throw new CompileException("Cannot generate code for literal: " + value);
    }
}