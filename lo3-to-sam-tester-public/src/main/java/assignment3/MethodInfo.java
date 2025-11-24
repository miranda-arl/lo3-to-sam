package assignment3;
import java.util.List;

class MethodInfo {
    String returnType;
    List<String> paramTypes;     // ["int", "Counter", ...]
    List<String> paramNames;     // ["x", "y", ...]
    boolean isConstructor;       // true if name == className && returnType == "void"
}