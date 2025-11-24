package assignment3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Type can be INT, BOOL, STRING, VOID, or any user-defined class type
public class Type {
    public static final Type INT = new Type("int", true);
    public static final Type BOOL = new Type("bool", true);
    public static final Type STRING = new Type("String", true);
    public static final Type VOID = new Type("void", true);

    private final String name;
    private final boolean isBuiltin;

    public static final Map<String, Type> userDefinedTypes = new ConcurrentHashMap<>();

    // Constructor is private for built-in types, public for user-defined
    private Type(String name, boolean isBuiltin) {
        this.name = name;
        this.isBuiltin = isBuiltin;
    }

    public Type(String name) {
        this(name, false);
        userDefinedTypes.put(name, this);
    }

    public String getName() {
        return name;
    }

    public boolean isBuiltin() {
        return isBuiltin;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Type type = (Type) obj;
        return name.equals(type.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public static Type fromString(String s) {
        switch (s) {
            case "int": return INT;
            case "bool": return BOOL;
            case "String": return STRING;
            case "void": return VOID;
            // User-defined types can be handled here if needed
            default: 
                if (userDefinedTypes.containsKey(s)) {
                    return userDefinedTypes.get(s);

                } else {
                    throw new RuntimeException("Unknown type: " + s);
                }
        }
    }

    public void registerUserDefinedType(String name) {
        userDefinedTypes.put(name, this);
    }
}