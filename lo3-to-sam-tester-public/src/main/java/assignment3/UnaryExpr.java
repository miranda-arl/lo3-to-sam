package assignment3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnaryExpr implements Expr {
    public final String operator;
    public final Expr operand;

    public UnaryExpr(String operator, Expr operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        String code = operand.generateCode(symbolTable, classInfo, classTables);
        String opCode;

        switch (operator) {
            case "!":
            case "~": // for string, reverse. identifier, bool
                Type operandType = operand.getType(symbolTable, classInfo, classTables);

                if (operandType == Type.STRING) {
                    initializeMethod("reverseString", classInfo);
                    Expr reverse = new CallExpr("reverseString", List.of(operand));
                    return reverse.generateCode(symbolTable, classInfo, classTables);
                } else if (operandType == Type.INT) {
                    return code + "PUSHIMM -1\nTIMES\n";
                } else {
                    opCode = "NOT\n";
                }
                break;
            default:
                throw new CompileException("Unknown unary operator: " + operator);
        }

        return code + opCode;
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        Type operandType = operand.getType(symbolTable, classInfo, classTables);
        switch (operator) {
            case "!":
                if (operandType != Type.BOOL) {
                    throw new CompileException("Type error: '!' operator requires a boolean operand.");
                }
                return Type.BOOL;
            case "~":
                if (operandType != Type.INT && operandType != Type.STRING) {
                    throw new CompileException("Type error: '~' operator requires an integer or string operand.");
                }
                return operandType;
            default:
                throw new CompileException("Unknown unary operator: " + operator);
        }
    }

    public void initializeMethod(String name, ClassInfo classInfo) {
        Map<String, MethodInfo> methods = classInfo.methods;
        if (!methods.containsKey(name)) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.returnType = "String";
            methodInfo.paramTypes = new ArrayList<>();
            methodInfo.paramTypes.add("String");
            methodInfo.paramNames = new ArrayList<>();
            methodInfo.paramNames.add("str");
            methodInfo.isConstructor = false;
            methods.put(name, methodInfo);
        }
    }
}
