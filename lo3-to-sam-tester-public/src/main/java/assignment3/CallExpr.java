package assignment3;

import java.util.List;
import java.util.Map;

public class CallExpr implements Expr {
    public final String name;
    public final List<Expr> arguments;

    public CallExpr(String name, List<Expr> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {    
        StringBuilder code = new StringBuilder();

        //  last value in the stack was the reference to ‘this’
        // code.append("PUSHOFF 0\n"); // Push the 'this' reference
        code.append("PUSHIMM 0\n"); // Push a dummy return address
        for (Expr arg : this.arguments) {
            code.append(arg.generateCode(symbolTable, classInfo, classTables));
        }

        code.append("LINK\n"); // (fbr added to top of stack) stack[sp] = fbr, fbr = sp, sp = sp + 1,
        code.append("JSR ").append(name).append("\n"); // saves pc + 1 to stack and jumps to label
        code.append("POPFBR\n"); // sp = sp-1, sp = stack[fbr]; // ???sets pc+1 to be new 0-index, sp = pc+1
        code.append("ADDSP -").append(arguments.size()).append("\n");

        return code.toString();
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        MethodInfo methodInfo = classInfo.methods.get(name);
        List<String> params = methodInfo.paramTypes;
        if (params.isEmpty()) {
            throw new CompileException("Unknown method: " + name);
        }
        if (arguments.size() != params.size() - 1) {
            throw new CompileException("Wrong number of arguments for method: " + name);
        }

        for (int i = 0; i < arguments.size(); i++) {
            Type expectedType = Type.fromString(params.get(i));
            Type actualType = arguments.get(i).getType(symbolTable, classInfo, classTables);

            if (expectedType != actualType) {
                throw new CompileException("Argument type mismatch in method '" + name + 
                    "': expected " + expectedType + " but got " + actualType);
            }
        }
        String returnType = methodInfo.returnType; // globalSymbolTable.getReturnType(name);
        return Type.fromString(returnType);
    }
}