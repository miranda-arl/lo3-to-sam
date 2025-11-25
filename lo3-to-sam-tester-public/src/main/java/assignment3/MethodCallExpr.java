package assignment3;

import java.util.List;
import java.util.Map;

public class MethodCallExpr implements Expr {
    public final Expr objName;
    public final String methodName;
    public final List<Expr> arguments;

    public MethodCallExpr(Expr objName, String methodName, List<Expr> arguments) {
        this.objName = objName;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        String classInstanceNameStr = ((IdentifierExpr) objName).name;
        System.out.println("classInstanceNameStr="+classInstanceNameStr);
        System.out.println("methodName="+methodName);

        Type classType = objName.getType(symbolTable, classInfo, classTables);
        
        if (classType.toString().equals(classInstanceNameStr)) {
            throw new CompileException("Cannot call instance method statically");
        }

        StringBuilder code = new StringBuilder();

        code.append("PUSHIMM 0\n"); // Push a dummy return address
        // code.append(new ThisExpr(((IdentifierExpr) objName).name).generateCode(symbolTable, globalSymbolTable, classTables)); // add 'this' as first actual

        for (Expr arg : this.arguments) {
            // new FieldAssignExpr(arg, )
            System.out.println("arg="+arg+ " type="+arg.getType(symbolTable, classInfo, classTables));
            code.append(arg.generateCode(symbolTable, classInfo, classTables));
        }

        code.append("LINK\n"); // (fbr added to top of stack) stack[sp] = fbr, fbr = sp, sp = sp + 1,
        code.append("JSR ").append(methodName).append("\n"); // saves pc + 1 to stack and jumps to label
        code.append("POPFBR\n"); // sp = sp-1, sp = stack[fbr]; // ???sets pc+1 to be new 0-index, sp = pc+1
        code.append("ADDSP -").append(arguments.size()).append("\n");

        return code.toString();
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        String classInstanceNameStr = ((IdentifierExpr) objName).name;
        System.out.println("classInstanceNameStr="+classInstanceNameStr);
        System.out.println("methodName="+methodName);
        Type classType = objName.getType(symbolTable, classInfo, classTables);
        
        if (classType.toString().equals(classInstanceNameStr)) {
            throw new CompileException("Cannot call instance method statically");
        }

        ClassInfo localClassInfo = classTables.get(classType.toString());

        // System.out.println("methodName="+methodName);
        // System.out.println("classType="+classType.toString());
         Map<String, MethodInfo> methods = localClassInfo.methods;
        if (!methods.containsKey(methodName)) {
            throw new CompileException("Unknown method: " + methodName);
        }

        MethodInfo methodInfo = methods.get(methodName);
        List<String> paramTypes = methodInfo.paramTypes;
        System.out.println("types for "+methodName+": "+paramTypes.toString());
        if (arguments.size() != paramTypes.size()) {
            throw new CompileException("Wrong number of arguments for method: " + methodName);
        }
        
        for (int i = 0; i < arguments.size(); i++) {
            Type expectedType = Type.fromString(paramTypes.get(i));
            Type actualType = arguments.get(i).getType(symbolTable, classInfo, classTables);

            if (expectedType != actualType) {
                throw new CompileException("Argument type mismatch in method '" + methodName +
                    "': expected " + expectedType + " but got " + actualType);
            }
        }
        String returnType = methodInfo.returnType;
        return Type.fromString(returnType);
    }
}