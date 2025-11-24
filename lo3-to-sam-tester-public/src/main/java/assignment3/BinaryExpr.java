package assignment3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BinaryExpr implements Expr {
    public final String operator;
    public final Expr left, right;

    public BinaryExpr(Expr left, String operator, Expr right) {
        this.left = left;
        this.operator = operator;
        this.right = right;

        if (left == null) {
            throw new CompileException("Left hand side is null");
        }
        if (right == null) {
            throw new CompileException("Right hand side is null");
        }
        if (!List.of("+", "-", "*", "/", "%", "<", ">", "=", "&", "|").contains(operator)) {
            throw new CompileException("Unsupported binary operator: " + operator);
        }
    }

    @Override
    public Type getType(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        Type l = left.getType(symbolTable, classInfo, classTables);
        Type r = right.getType(symbolTable, classInfo, classTables);

        if (l == Type.STRING && r == Type.INT) {
            if (!operator.equals("*")) {
                throw new CompileException("Operator " + operator + " not supported for types " + l + " " + r);
            }
            return Type.STRING;
        }

        if (!l.equals(r) && r != Type.VOID) {
            throw new CompileException("Type mismatch: " + l + " vs " + r + " for operator " + operator);
        }

        if (operator.equals("+")) {
            if (l == Type.INT || l == Type.STRING) return l;
        } else if(operator.equals("<") || operator.equals(">") || operator.equals("=")) {
            if (l == Type.INT || l == Type.STRING) { 
                return Type.BOOL;
            } else if (r == Type.VOID) {
                return Type.BOOL;
            }
        } else if (List.of("-", "*", "/", "%").contains(operator)) {
            if (l != Type.INT) throw new CompileException("Operator " + operator + " not supported for type " + l);
            return Type.INT;
        } else if (List.of("&", "|").contains(operator)) {
            if (l != Type.BOOL) throw new CompileException("Operator " + operator + " not supported for type " + l);
            return Type.BOOL;
        }
        throw new CompileException("Unknown binary operator: " + operator);
    }

    @Override
    public String generateCode(SymbolTable symbolTable, ClassInfo classInfo, Map<String, ClassInfo> classTables) throws CompileException {
        Type l = left.getType(symbolTable, classInfo, classTables);
        Type r = right.getType(symbolTable, classInfo, classTables);

        String lCode = left.generateCode(symbolTable, classInfo, classTables);
        String rCode = right.generateCode(symbolTable, classInfo, classTables);

        if (l == Type.STRING && operator.equals("*") && r == Type.INT) {
            initializeMethod("repeatString", classInfo, "int");
            Expr concat = new CallExpr("repeatString", List.of(left, right));
            return concat.generateCode(symbolTable, classInfo, classTables);
        } else if (l == Type.STRING && operator.equals("+") && r == Type.STRING) {
            initializeMethod("concatString", classInfo, "String");
            Expr concat = new CallExpr("concatString", List.of(left, right));
            return concat.generateCode(symbolTable, classInfo, classTables);
        } else if (l == Type.STRING && operator.equals("=") && r == Type.STRING) {
            initializeMethod("equalString", classInfo, "String");
            Expr concat = new CallExpr("equalString", List.of(left, right));
            return concat.generateCode(symbolTable, classInfo, classTables);
        }  else if (l == Type.STRING && operator.equals("<") && r == Type.STRING) {
            initializeMethod("lessString", classInfo, "String");
            Expr concat = new CallExpr("lessString", List.of(left, right));
            return concat.generateCode(symbolTable, classInfo, classTables);
        }  else if (l == Type.STRING && operator.equals(">") && r == Type.STRING) {
            initializeMethod("greaterString", classInfo, "String");
            Expr concat = new CallExpr("greaterString", List.of(left, right));
            return concat.generateCode(symbolTable, classInfo, classTables);
        }
        return lCode + rCode + opcode();
    }

    private String opcode() {
        switch (operator) {
            case "+": return "ADD\n";
            case "-": return "SUB\n";
            case "*": return "TIMES\n";
            case "/": return "DIV\n";
            case "%": return "MOD\n";
            case "<": return "LESS\n";
            case ">": return "GREATER\n";
            case "=": return "EQUAL\n";
            case "&": return "AND\n";
            case "|": return "OR\n";
            default: throw new CompileException("Unknown op: " + operator);
        }
    }

    public String lenString(String conditionLabel, String jumpLabel) {
        return 
            "DUP\nPUSHIND\nISNIL\n"+
            "JUMPC "+conditionLabel+"\n"+
            "SWAP\nPUSHIMM 1\nADD\nSWAP\nPUSHIMM 1\nADD\n"+
            "JUMP "+jumpLabel+"\n";
    }

    public void initializeMethod(String name, ClassInfo classInfo, String param2) {
        Map<String, MethodInfo> methods = classInfo.methods;
        if (!methods.containsKey(name)) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.returnType = "String";

            methodInfo.paramTypes = new ArrayList<>();
            methodInfo.paramTypes.add("String");
            methodInfo.paramTypes.add(param2);

            methodInfo.paramNames = new ArrayList<>();
            methodInfo.paramNames.add("p1");
            methodInfo.paramNames.add("p2");

            methodInfo.isConstructor = false;
            methods.put(name, methodInfo);
        }
    }
}

