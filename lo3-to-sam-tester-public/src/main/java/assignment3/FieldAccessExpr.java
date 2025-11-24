package assignment3;

import java.util.List;
import java.util.Map;

public class FieldAccessExpr implements Expr {
    public final Expr object;
    public final String fieldName;

    public FieldAccessExpr(Expr object, String fieldName) {
        this.object = object;
        this.fieldName = fieldName;
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        // ClassInfo classInfo = classTables.get(((IdentifierExpr) object).name);
        List<FieldInfo> fields = classInfo.fields; 
        if (fields.indexOf(fieldName) == -1) {
            throw new CompileException("Unknown field: " + fieldName);
        }
        int index = fields.indexOf(fieldName);
        FieldInfo fieldInfo = fields.get(index);
        return Type.fromString(fieldInfo.type);
    }

    @Override
    public String generateCode(SymbolTable symbolTable,ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        StringBuilder code = new StringBuilder();

        String className = ((IdentifierExpr) object).name;
        ClassInfo currClassInfo = classTables.get(className);
        List<FieldInfo> fields = currClassInfo.fields; 

        int index = -1; 
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo f = fields.get(i);
		System.out.println("fieldName in ACCESS="+f.name);
            if (fieldName.equals(f.name)) {
                index = i; 
            }
        }
        System.out.println("fieldName="+fieldName);
        // int index = fields.indexOf(fieldName);
        System.out.println("index="+index);
        FieldInfo fieldInfo = fields.get(index);
        int offset = fieldInfo.offset;

        System.out.println("Accessing fields for class---------"+ className+" fieldName="+fieldName);
        // read value from memory (obj.x)
        code.append("PUSHOFF ").append(1).append("\n"); // offset of correct obj
        code.append("PUSHIMM ").append(offset).append("\n");
        code.append("ADD\n"); //SUB
        code.append("PUSHIND\n");
        return code.toString();
    }
}