package assignment3;

import java.util.Map;

public class ThisExpr implements Expr {
    public final String currentClassName;

    public ThisExpr(String currentClassName) {
        this.currentClassName = currentClassName;
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        // Assuming 'this' refers to the current class type
        Type currentClassType = Type.fromString(currentClassName);
        if (currentClassType == null) {
            throw new CompileException("'this' used outside of class context");
        }
        return currentClassType;
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        // Generate code for 'this' expression
        // object reference is typically stored in a known location, e.g., in a register or at a specific stack offset
        // ClassInfo classInfo = classTables.get(currentClassName);
        FieldInfo fieldInfo = classInfo.fields.get(0);
        System.out.println("field this with offset=" +fieldInfo.offset);
        // 'this' refers to the first field
        int index = fieldInfo.offset;
        if (currentClassName.equals("Main")) {
            return  "PUSHOFF "+ (index +1)+ "\n";
        } else {
            return  "PUSHOFF "+ (index +1)+ "\n";
        }
        // "DUP\n"; // 0\n";// +
    }
}