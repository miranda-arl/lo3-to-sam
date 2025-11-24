package assignment3;

import java.util.Map; 

public class IdentifierExpr implements Expr {
    public final String name;

    public IdentifierExpr(String name) {
        this.name = name;
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables)  throws CompileException {
        // 1. Local variables
        if (symbolTable != null && symbolTable.containsKey(name)) {
            String type = symbolTable.getReturnType(name);
            return Type.fromString(type);
        }

        // 2. Class fields
        if (symbolTable != null && symbolTable.currentClass != null) {
            // ClassInfo currentClassInfo = classTables.get(symbolTable.currentClass);
            for (FieldInfo field : classInfo.fields) {
                if (field.name.equals(name)) {
                    return Type.fromString(field.type);
                }
            }
        }
        
        // 4. Is it a class name?
        if (classInfo != null) {
            return Type.fromString(name);
        }

        // 5. Not found → error
        throw new CompileException("Undeclared variable: " + name);
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        // System.out.println("identifier expr name="+name);
        
        if (symbolTable != null && symbolTable.containsKey(name)) {
            int offset = Integer.parseInt(symbolTable.getLocation(name));
            // System.out.println("inside symbol id+++++++++"+name);
            return "PUSHOFF " + offset + "\n";
        }
        
        if (symbolTable != null && symbolTable.currentClass != null) {
            for (FieldInfo field : classInfo.fields) {
                // System.out.println("OFFSET="+field.offset);
                if (field.name.equals(name)) {
                    return "PUSHOFF -1\n" +// -(field.offset) + "\n" +
                    "PUSHIMM " + field.offset + "\n" +
                    "ADD\n" +
                    "PUSHIND\n";
                    // return "PUSHOFF " + field.offset + "\n";
                }
            }
        }

        if (classInfo.fields != null) {
            for (FieldInfo field : classInfo.fields) {
                if (field.name.equals(name)) {
                    System.out.println("name in identifier expr="+name+ " offset="+field.offset);
                    return "PUSHOFF " + field.offset + "\n";
                }
            }
        }
        
        throw new CompileException("Undeclared variable: " + name);
    }

    public String getName() {
        return name;
    }
}