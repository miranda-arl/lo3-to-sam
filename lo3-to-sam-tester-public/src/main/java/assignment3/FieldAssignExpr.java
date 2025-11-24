package assignment3;

import java.util.List;
import java.util.Map;

public class FieldAssignExpr implements Expr {
    public final Expr object;
    public final String fieldName;
    public final Expr value; // rhs

    public FieldAssignExpr(Expr object, String fieldName, Expr value) {
        this.object = object;
        this.fieldName = fieldName;
        this.value = value;
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
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        System.out.println("field assigning="+fieldName);
        StringBuilder code = new StringBuilder();
        String className = ((IdentifierExpr) object).name;

        ClassInfo currClassInfo = classTables.get(className);
        MethodInfo methodInfo = currClassInfo.methods.get(className+"_new");

        List<String> paramNames = methodInfo.paramNames;
        List<String> paramTypes = methodInfo.paramTypes;
        List<FieldInfo> fields = currClassInfo.fields;
        List<String> fieldNames = new java.util.ArrayList<>();
        List<String> fieldTypes = new java.util.ArrayList<>();

        for (FieldInfo field : fields) {
            fieldNames.add(field.name);
            fieldTypes.add(field.type);
            // System.out.println("fieldName: "+field.name);
            // System.out.println("fieldType: "+field.type);
        }

        // System.out.println("paramNames: "+paramNames);
        // System.out.println("paramTypes: "+paramTypes);
        System.out.println("Generating code for field assignment '"+fieldName+"' in class "+((IdentifierExpr) object).name);

        int index = fieldNames.indexOf(fieldName);
        FieldInfo fieldInfo = fields.get(index);
        int offset = fieldInfo.offset;

        // write value to memory (this.x = rval)
        code.append("PUSHOFF ").append(1).append("\n"); // offset of correct obj // adding 1
        code.append("PUSHIMM ").append(offset).append("\n"); //xxx
        code.append("ADD\n"); //SUB\n");
        code.append("PUSHOFF ").append(offset+1).append("\n"); // value.generateCode(symbolTable, classInfo, classTables));
        code.append("STOREIND\n");
        return code.toString();
    }
}