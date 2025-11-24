package assignment3;

import java.util.Map;

public interface Expr { 
    Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTable) throws CompileException;
    String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTable) throws CompileException;
}
