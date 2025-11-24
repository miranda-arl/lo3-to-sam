package assignment3;

import java.util.List;
import java.util.Map;

public class NewExpr implements Expr {
    public final String className;
    public final List<Expr> arguments;

    public NewExpr(String className, List<Expr> arguments) {
        this.className = className;
        this.arguments = arguments;
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        Type classType = Type.userDefinedTypes.get(className);
        if (classType == null) {
            throw new CompileException("Unknown class type: " + className);
        }
        return classType;
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        StringBuilder code = new StringBuilder();

        if (classInfo == null) {
            throw new CompileException("Unknown class: " + className);
        }

        // List<FieldInfo> fieldInfos = classInfo.fields;
        ClassInfo currClassInfo = classTables.get(className); //+ "_new");
        // for (FieldInfo f: classInfo.fields) {
        //     System.out.println("NEW classInfo.fields="+f.name);
        // }
        // for (FieldInfo f: currClassInfo.fields) {
        //     System.out.println("NEW curr classInfo.fields="+f.name);
        // }
        int objectSize = currClassInfo.fields.size();

        Map<String, MethodInfo> methods = currClassInfo.methods;
        MethodInfo ctor = methods.get(className + "_new");

        // System.out.println("NewExpr: className=" + className + ", constructor args=" + methodInfo.paramTypes);
        int expectedArgs = 0; // default
        if (ctor != null) {
            expectedArgs = ctor.paramTypes.size();
            if (expectedArgs != arguments.size()) {
                throw new CompileException("Constructor for class " + className +
                " expects " + expectedArgs + " arguments but got " + arguments.size());
            }
        }
        
        // Space for return value (the object reference)
        code.append("PUSHIMM 0\n"); // space for rv
        code.append("LINK\n");//PUSHFBR\n"); // LINK\n"); (right)

        // Allocate the object
        code.append("PUSHIMM ").append(objectSize).append("\n"); // number of args (including this)
        code.append("MALLOC\n"); // create object and push reference on stack

        // Generate code for constructor arguments (if any)
        for (int i = 1; i < objectSize; i++) { // arguments.size()varguments.size(); i++) { //
            if (i < arguments.size()) {
                Expr arg = arguments.get(i);
                code.append(arg.generateCode(symbolTable, classInfo, classTables));
            } 
            else {
                System.out.println("empty field in constructor="+ currClassInfo.fields.get(i));
                code.append("PUSHIMM 0\n"); // empty for now
            }
        }

        // Push the object reference as the first argument to the constructor
        code.append("PUSHSP\n");
        code.append("PUSHIMM ").append(objectSize+1).append("\n"); //rv and this
        // append(objectSize+1).append("\n");
        //arguments.size()
        code.append("SUB\n");

        // Save current FBR and call constructor
        code.append("PUSHFBR\n"); 
        code.append("JSR ").append(className).append("_new\n");
        code.append("POPFBR\n");

        // Pop all constructor arguments, keep object reference in return slot
        code.append("ADDSP -").append(objectSize).append("\n"); //arguments.size()

        // Store object reference into the return slot
        code.append("STOREOFF -1\n");
        code.append("POPFBR\n");
        
        return code.toString();
    }
}