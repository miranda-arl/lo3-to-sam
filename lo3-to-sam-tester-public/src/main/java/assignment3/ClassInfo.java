package assignment3;

import java.util.List;
import java.util.Map;

class ClassInfo {
    List<FieldInfo> fields;        // in declared order
    Map<String, MethodInfo> methods;
    // method name to list of argument types (including return type at index 0)
}