package assignment3;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.utexas.cs.sam.io.SamTokenizer;
import edu.utexas.cs.sam.io.Tokenizer;
import edu.utexas.cs.sam.io.Tokenizer.TokenType;

public class LiveOak3Compiler
{
	static SymbolTable globalSymbolTable = new SymbolTable();
	// static SymbolTable classSymbolTable = new SymbolTable();
	static Map<String, ClassInfo> classTable = new HashMap<>();
	static ClassInfo currClassInfo = new ClassInfo();

	// static Map<String, SymbolTable> globalSymbolTables = new java.util.HashMap<>();
	//static List<String> methodNames = new ArrayList<>();
	static String currentClassName = "";
	static String currentClassEndLabel = "";
	static String currentMethodName = "";
	static String currentMethodEndLabel = "";
	static Deque<String> breakLabelStack = new ArrayDeque<>();

	static List<String> classDeclList = new java.util.ArrayList<>();
	static List<String> formalsList = new java.util.ArrayList<>();
	static int formalsCount = 0; // number of formals in current method
	static int localVarCount = 0; // number of local variables in current method
	static int stackPointer = 0;
	static LabelGenerator labelGen = new LabelGenerator();

	static Set<String> reservedWords = Set.of(
		"class", "int", "void", "return", "if", "else", "while","break", "new", 
		"this", "true", "false", "null");

	public static void main(String[] args) throws IOException {
		if (args.length != 2) 
		{
			System.err.println("usage: java LiveOak3Compiler <source-file>");
			return;
		}

		String fileName = args[0];
		String pgm = compiler(fileName);
		// write program to a new file
		try (PrintWriter out = new PrintWriter(new FileWriter(args[1]))) {
			out.print(pgm);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error writing to output file");
			System.err.println("Error writing to output file");
		}
	}

	static String compiler(String fileName) {
		//returns SaM code for program in file
		try 
		{
			System.out.println("Compiling " + fileName);
			SamTokenizer f1 = new SamTokenizer(fileName, SamTokenizer.TokenizerOptions.PROCESS_STRINGS);
			SamTokenizer f2 = new SamTokenizer(fileName, SamTokenizer.TokenizerOptions.PROCESS_STRINGS);
			SamTokenizer f3 = new SamTokenizer(fileName, SamTokenizer.TokenizerOptions.PROCESS_STRINGS);

			String pgm = getProgram(f1, f2, f3);
			return pgm;
		} 
		catch (IOException e) 
		{
			System.out.println("Error message: " + e.getMessage());
			return "ERROR\n";
		}
		catch (Error e)
		{
			System.out.println("Error message: " + e.getMessage());
			System.err.println("Failed to compile " + fileName);
			System.out.println("Failed to compile " + fileName);
			throw e;
		}
	}

	static String getProgram(SamTokenizer f1, SamTokenizer f2, SamTokenizer f3) throws CompileException {
		try {
			String pgm="";
			pgm += "start:\nJUMP Main_main\n";

			globalSymbolTable = new SymbolTable();
			classTable = new HashMap<>();

			// First pass: collect class declarations
			while (f1.peekAtKind() != TokenType.EOF) {
				if (f1.peekAtKind().equals(TokenType.WORD)) {
					// classSymbolTable = new SymbolTable();
					parseClassSignature(f1);
				} else {
					throw new CompileException("Expected class declaration");
				}
			}

			if (classTable.isEmpty() || !classTable.containsKey("Main")) {
				throw new CompileException("Program must contain a Main class");
			}

			// Second pass: collect method declarations
			while (f2.peekAtKind() != TokenType.EOF) {
				if (f2.peekAtKind().equals(TokenType.WORD)) {
					f2.getWord(); // class
					String className = f2.getWord(); // name
					if (!f2.check('(')) {
						throw new CompileException("Method must follow proper syntax");
					}

					while (!f2.check(')')) {
						f2.getWord(); // rT
						f2.getWord(); // name
						while (!f2.check(';')) {
							f2.check(',');
							f2.getWord(); // name
						}
					}
					f2.check('{');
					while (!f2.test('}')) {
						// Skip method signatures and bodies
						if (f2.peekAtKind().equals(TokenType.WORD)) {
							parseMethodSignature(f2, className);
							f2.check('{');
							int braceCount = 1;
							while (braceCount > 0) {
								if (f2.peekAtKind() == TokenType.EOF) {
									throw new CompileException("unexpected end of file");
								}
								if (f2.check('{')) {
									braceCount++;
								} else if (f2.check('}')) {
									braceCount--;
								} else {
									f2.skipToken(); // consume other tokens
								}
							}
						} else {
							throw new CompileException("Unexpected token in class body");
						}
					}
					f2.check('}');
					pgm += addImplicitConstructor(className); // only adds if missing
				}
			}

			// Third pass: generate code and validate function calls
			
			// Add code to jump to main
			while (f3.peekAtKind() != TokenType.EOF) {
				if (f3.peekAtKind().equals(TokenType.WORD)) {
					pgm += getClass(f3);
				} else {
					throw new CompileException("Expected class declaration");
				}
			}
			System.out.println("pgm="+pgm);
			return pgm;
		} catch(Error e) {
			throw e;
		}		
	}

	static void parseClassSignature(SamTokenizer f) throws CompileException {
		if (!f.peekAtKind().equals(TokenType.WORD) || !f.getWord().equals("class")) {
			throw new CompileException("Expected 'class' keyword");
		}

		if (!f.peekAtKind().equals(TokenType.WORD)) {
			throw new CompileException("Invalid class name");
		}

		stackPointer = 0; // starts at 0 for each class
		// ⟨ClassDecl⟩ → class ⟨ClassName⟩ ( (⟨VarDecl⟩)* ) { (⟨MethodDecl⟩)* } 
		String className = f.getWord();
		
		if (classTable.containsKey(className)) {
			throw new CompileException("Class " + className + " is already defined");
		}

		if (reservedWords.contains(className)) {
			throw new CompileException("Cannot use reserved word as class name: " + className);
		}
		currentClassName = className;
		Type classType = new Type(className);
		classType.registerUserDefinedType(className);

		f.check('(');
		classDeclList = new java.util.ArrayList<>(); // types

		List<FieldInfo> fieldInfos = new ArrayList<>();
		FieldInfo fInfo = new FieldInfo();
		fInfo.name = "this";
		fInfo.type = className;
		fInfo.offset = stackPointer++;// this at offset 0
		classDeclList.add(className);

		fieldInfos.add(fInfo);
		while (!f.test(')')) {
			fInfo = new FieldInfo();
			String attrType = f.getWord();
			String attrName = f.getWord(); // attrName
			classDeclList.add(attrType);

			fInfo.name = attrName;
			fInfo.type = attrType;
			fInfo.offset = stackPointer++;
			fieldInfos.add(fInfo);

			while (f.check(',')) {
				attrName = f.getWord();
				if (attrName.equals(attrType)) {
					throw new CompileException("Invalid formal syntax for class variables");
				}
				classDeclList.add(attrType);
				fInfo = new FieldInfo();
				fInfo.name = attrName;
				fInfo.type = attrType;
				fInfo.offset = stackPointer++;
				fieldInfos.add(fInfo);
			}
			f.check(';');
		}
		f.check(')');

		currClassInfo = new ClassInfo();
		currClassInfo.fields = fieldInfos;
		currClassInfo.methods = new HashMap<>();
		
		classTable.put(className, currClassInfo);

		f.check('{');
		
		while (!f.test('}')) {
			// Skip method signatures and bodies
			if (f.peekAtKind().equals(TokenType.WORD)) {
				f.getWord(); // rT
				f.getWord(); // methodName
				if (!f.check('(')) {
					throw new CompileException("Method must follow proper syntax");
				}
				while (!f.test(')')) {
					f.getWord(); // pRtype
					f.getWord(); // pName
					if (!f.check(',')) {
						break;
					}
				}
				f.check(')');
				f.check('{');
				int braceCount = 1;
				while (braceCount > 0) {
					if (f.peekAtKind() == TokenType.EOF) {
						throw new CompileException("unexpected end of file");
					}
					if (f.check('{')) {
						braceCount++;
					} else if (f.check('}')) {
						braceCount--;
					} else {
						f.skipToken(); // consume other tokens
					}
				}
			} else {
				throw new CompileException("Unexpected token in class body");
			}
		}
		f.check('}');
	}

	static String addImplicitConstructor(String className) {
		// Skip Main class; it does not need a constructor
		if (className.equals("Main")) return "";

		// if no explicit constructor
		ClassInfo classInfo = classTable.get(className);
		if (!classInfo.methods.containsKey(className+"_"+"new")) {
			// System.out.println("class with implicit con: "+className);
			List<String> fieldNames = new ArrayList<>();
			for (FieldInfo field : classInfo.fields) {
					fieldNames.add(field.name);
			}

			MethodInfo methodInfo = new MethodInfo();
			methodInfo.returnType = "void";
			methodInfo.paramTypes = classDeclList;
			methodInfo.paramNames = fieldNames;	
			methodInfo.isConstructor = true;

			// String[] attr = new String[classDeclList.size()+1];
			// attr[0] = "void";
			// for (int i = 0; i < classDeclList.size(); i++) {
			// 	attr[i+1] = classDeclList.get(i); 
			// }
			// classSymbolTable.enter(className+"_new", attr);
			classInfo.methods.put(className+"_new", methodInfo);
			// System.out.println("No explicit constructor. Added one="+className+"_new");
			// System.out.println("classInfo: "+classInfo.methods.get(className+"_new").paramNames);
			return generateImplicitConstructorCode(className, classInfo);
		}
		return "";
	}

	static String generateImplicitConstructorCode(String className, ClassInfo classInfo) {
		StringBuilder code = new StringBuilder();
		code.append(className).append("_new:\n");

		code.append("ADDSP 1\n"); // 0

		// Save frame pointer
		// code.append("PUSHFBR\n"); // //LINK\n");

		// Offset for fields inside object
		int offset = 0;

		// For each field, initialize with default value
		for (FieldInfo field : classInfo.fields) {
			// System.out.println("field in gen implicit="+field.name);
			// push 'this' reference
			code.append("PUSHOFF ").append(offset+1).append("\n"); 
			// push field offset
			code.append("PUSHIMM ").append(offset).append("\n");

			// adjust address
			code.append("ADD\n");

			// push default value
			code.append("PUSHIMM 0\n");// store into heap object
			code.append("STOREIND\n");
			offset++;
		}
		
		// Pop frame pointer
		code.append("POPFBR\n");

		// Implicit return (constructor doesn't return explicitly)
		code.append("JUMPIND\n");

		return code.toString();
	}


	static String getClass(SamTokenizer f) throws CompileException {
		// stackPointer = 0; // starts at 0 for each class

		String pgm = "";

		f.getWord(); // consume 'class'
		currentClassName = f.getWord();
		currClassInfo = classTable.get(currentClassName);
		currentClassEndLabel = newLabel("end_" + currentClassName);

		f.check('(');
		getClassVarDeclarations(f, currentClassName);
		f.check(')');

		f.check('{');
		while (!f.test('}')) {
			pgm += getMethod(f);

		}
		f.check('}');

		Map<String, MethodInfo> classMethods = classTable.get(currentClassName).methods;
		if (classMethods.containsKey("concatString")) {
			pgm += concatString();
		}
		if (classMethods.containsKey("repeatString")) {
			pgm += repeatString();
		}
		if (classMethods.containsKey("reverseString")) {
			pgm += reverseString();
		}
		if (classMethods.containsKey("equalString")) {
			pgm += equalString();
		}
		if (classMethods.containsKey("lessString")) {
			pgm += lessString();
		}
		if (classMethods.containsKey("greaterString")) {
			pgm += greaterString();
		}
		return pgm;
	}

	static void getClassVarDeclarations(SamTokenizer f, String className) throws CompileException {
		while (!f.test(')')) {
			f.getWord(); // attrType
			f.getWord(); // attrName 
			while (f.check(',')) {
				f.getWord(); // attrName
			}
			f.check(';');
		}
	}

	static void parseMethodSignature(SamTokenizer f, String className) throws Error {
		formalsList = new java.util.ArrayList<>();
		List<String> formalNames = new ArrayList<>();

		String returnType = f.getWord();
		if (!returnType.equals("int") && !returnType.equals("String") && !returnType.equals("bool") && !returnType.equals("void")
		&& !Type.userDefinedTypes.containsKey(returnType)) {
			throw new CompileException("invalid return type");
		}

		if (!f.peekAtKind().equals(TokenType.WORD)) {
			throw new CompileException("invalid method name");
		}

		String methodName = f.getWord();
		if (!f.test('(')) {
			throw new CompileException("expected '(' after method name");
		}

		if (reservedWords.contains(methodName)) {
			throw new CompileException("Cannot use reserved word as method name: " + methodName);
		}

		f.check('(');

		int paramCount = 1; // 'this' is implicit first formal

		formalsList.add(className); // implicit 'this' as first formal
		formalNames.add("this");

		while (!f.test(')')) {
			if (!f.peekAtKind().equals(TokenType.WORD)) {
				throw new CompileException("invalid token type for parameter");
			}
			String paramType = f.getWord(); // parameter type
			if (!paramType.equals("int") && !paramType.equals("String") && !paramType.equals("bool") && !Type.userDefinedTypes.containsKey(paramType)) {
				throw new CompileException("invalid method parameter type");
			}
			String paramName = f.getWord(); // parameter name
			formalNames.add(paramName);
			formalsList.add(paramType);
			paramCount++;

			if (!f.check(',')) {
				break;
			}
		}

		f.check(')');

		if (methodName.equals("main") && paramCount != 1) { // this is 1
			throw new CompileException("main method cannot have formals");
		}

		MethodInfo methodInfo = new MethodInfo();
		methodInfo.returnType = returnType;
		methodInfo.paramTypes = formalsList;
		methodInfo.paramNames = formalNames;
		if (className.equals(methodName)) {
			methodName = "new";
			methodInfo.isConstructor = true;
		} else {
			methodInfo.isConstructor = false;
		}

		currClassInfo = classTable.get(className);

		currClassInfo.methods.put(className+"_"+methodName, methodInfo);
	}

	static String getMethod(SamTokenizer f) throws CompileException {
		SymbolTable methodSymbolTable = new SymbolTable();
		methodSymbolTable.currentClass = currentClassName;
		formalsCount = 1; // 'this' is implicit first formal
		localVarCount = 0;
		currentMethodName = "";

		currClassInfo = classTable.get(currentClassName);

		try {
			String returnType = f.getWord();
			String methodName = f.getWord(); 

			if (currentClassName.equals(methodName)) {
				methodName = "new";
			}

			currentMethodName = currentClassName+"_"+methodName;
			System.out.println("currentMethodName="+currentMethodName);
			currentMethodEndLabel = newLabel("end_" + currentMethodName);

			f.match('(');
			String formals = getFormals(f, methodSymbolTable);
			f.match(')');

			if (!f.check('{')) {
				throw new CompileException("missing opening brace for body");
			}

			String declarations = "";
			while (f.peekAtKind().equals(TokenType.WORD)) {
				declarations = getDeclarations(f, methodSymbolTable);
			}

			if (!f.check('{')) {
				throw new CompileException("missing opening brace for block");
			}

			String statements = "";
			while (!f.test('}')) {
				StatementResult statementResult = getStatements(f, methodSymbolTable);
				if (!returnType.equals("void") && !statementResult.guaranteesReturn) { // needs to be at the very end
					throw new CompileException("Method '" + currentMethodName + "' is missing a return statement");
				}
				statements += statementResult.code;
			}

			if (!f.check('}')) {
				throw new CompileException("missing closing brace for statements");
			}

			if (!f.check('}')) { 
				throw new CompileException("missing closing brace for method");
			}

			String prologue = "ADDSP ";
			if (currentMethodName.equals("Main_main")) {
				System.out.println("In Main_main ADDSP ="+(localVarCount + 2));
				prologue += (localVarCount + 2) + "\n"; //1
			} else {
				prologue += (localVarCount) + "\n";
			}

			int rvIndex = 0;
			if (!currentMethodName.equals("Main_main")) {
				rvIndex = rvIndex -(formalsCount+1); // (sp+1) -(formalsCount+1), 1 for rv and 1 for 'this'
			}

			String epilogue = 
			currentMethodEndLabel + ":\n" + 
			"STOREOFF " + rvIndex + "\n" + 
			"ADDSP -" + localVarCount + "\n";

			if (currentMethodName.equals("Main_main")) {
				epilogue = currentMethodEndLabel + ":\n" + 
					"STOREOFF " + rvIndex + "\n" + 
					"ADDSP -" + (localVarCount + 1) + "\n";
			}

			if (methodName.equals("new")) {
				prologue = "ADDSP " + (localVarCount + 1) + "\n";
				String middle = initializeInstanceFields(methodSymbolTable, currClassInfo);
				epilogue = currentMethodEndLabel + ":\n" + "POPFBR\nJUMPIND\n";
				return currentMethodName + ":\n" + prologue + formals+ statements + middle + epilogue;
			}

			String result = currentMethodName + ":\n" + prologue + formals + declarations + statements + epilogue;
			if (currentMethodName.equals("Main_main")) {
				result += "STOP\n";
			} else {
				result += "JUMPIND\n"; // sp = sp-1, pc = stack[sp]
			}
			return result;
		} catch (Error e) {
			throw e;
		}
	}

	static String initializeInstanceFields(SymbolTable symbolTable, ClassInfo classInfo) {
		StringBuilder code = new StringBuilder();
		
		// For each field, initialize with default value
			for (int i = 0; i < classInfo.fields.size(); i++) {
				FieldInfo field = classInfo.fields.get(i);
				Expr obj = new IdentifierExpr(currentClassName);
				Expr emptyVal = new LiteralExpr(0);
				Expr instanceExpr = new FieldAssignExpr(obj, field.name, emptyVal);

				code.append(instanceExpr.generateCode(symbolTable, classInfo, classTable));
			}
		return code.toString();
	}

	static Expr parseExpr(SamTokenizer f, SymbolTable symbols) throws CompileException {
		return parseTernary(f, symbols);
	}

	static Expr parseTernary(SamTokenizer f, SymbolTable symbols) throws CompileException {
		Expr condition = parseOr(f, symbols);
		if (f.check('?')) {
			Expr thenExpr = parseExpr(f, symbols);
			f.match(':');
			Expr elseExpr = parseExpr(f, symbols);
			
			return new TernaryExpr(condition, thenExpr, elseExpr);
		}
		return condition;
	}

	static Expr parseOr(SamTokenizer f, SymbolTable symbols) throws CompileException {
		Expr left = parseAnd(f, symbols);
		while (f.check('|')) {
			Expr right = parseAnd(f, symbols);
			left = new BinaryExpr(left, "|", right);
			if (!f.test(')')) {
				throw new CompileException("BinaryExpr not enclosed in parenthesis");
			}
		}
		return left;
	}

	static Expr parseAnd(SamTokenizer f, SymbolTable symbols) throws CompileException {
		Expr left = parseEquality(f, symbols);
		while (f.check('&')) {
			Expr right = parseEquality(f, symbols);
			left = new BinaryExpr(left, "&", right);
			if (!f.test(')')) {
				throw new CompileException("BinaryExpr not enclosed in parenthesis");
			}
		}
		return left;
	}

	static Expr parseEquality(SamTokenizer f, SymbolTable symbols) throws CompileException {
		Expr left = parseAdd(f, symbols);
		while (f.test('=') || f.test('<') || f.test('>')) {
			String op = "" + f.getOp();
			Expr right = parseAdd(f, symbols);
			left = new BinaryExpr(left, op, right);
			if (!f.test(')')) {
				throw new CompileException("BinaryExpr not enclosed in parenthesis");
			}
		}
		return left;
	}

	static Expr parseAdd(SamTokenizer f, SymbolTable symbols) throws CompileException {
		Expr left = parseMul(f, symbols);
		while (f.test('+') || f.test('-')) {
			String op = "" + f.getOp();
			Expr right = parseMul(f, symbols);
			left = new BinaryExpr(left, op, right);
			if (!f.test(')')) {
				throw new CompileException("BinaryExpr not enclosed in parenthesis");
			}
		}
		return left;
	}

	static Expr parseMul(SamTokenizer f, SymbolTable symbols) throws CompileException {
		Expr left = parseUnary(f, symbols);
		while (f.test('*') || f.test('/') || f.test('%')) {
			String op = "" + f.getOp();
			Expr right = parseUnary(f, symbols);
			left = new BinaryExpr(left, op, right);
			if (!f.test(')')) {
				throw new CompileException("BinaryExpr not enclosed in parenthesis");
			}
		}
		return left;
	}

	static Expr parseUnary(SamTokenizer f, SymbolTable symbols) throws CompileException {		
		if (f.test('!') || f.test('~')) {
			String op = "" + f.getOp();
			Expr right = parseUnary(f, symbols);
			return new UnaryExpr(op, right);
		}
		return parsePrimary(f, symbols);
	}

	static Expr parsePrimary(SamTokenizer f, SymbolTable symbols) throws CompileException {
		switch (f.peekAtKind()) {
			case INTEGER:
				return new LiteralExpr(f.getInt());
			case STRING:
				return new LiteralExpr(f.getString());
			case WORD:
				String word = f.getWord();
				if (word.equals("true")) return new LiteralExpr(true);
				if (word.equals("false")) return new LiteralExpr(false);

				// this
				if (word.equals("this")) {
					if (f.test('.')) {
						throw new CompileException("Cannot access field or method on this");
					}
					return new ThisExpr(currentClassName);
				}
				// null
				if (word.equals("null")) {
					if (f.test('.')) {
						throw new CompileException("Cannot access field or method on null");
					}
					return new LiteralExpr(null);
				}

				// create new object
				if (word.equals("new")) {
					String className = f.getWord();
					if (f.check('(')) {
						List<Expr> actuals = new ArrayList<>();
						actuals.add(new ThisExpr(className)); // implicit 'this' as first actual
						if (!f.test(')')) {
							actuals = parseActuals(f, actuals, symbols);
						}
						f.match(')');
					    // System.out.println("parse: new className="+className);

						if (f.test('.')) {
							throw new CompileException("Chain of calls after new not supported");
						}
						return new NewExpr(className, actuals);
					}
				}  

				// identifier (could be var OR start of obj.method OR obj.field)
        		Expr base = new IdentifierExpr(word);
				// loop to consume .field or .method chains
				if (f.check('.')) {
					String methodName = f.getWord();
					String methodClassName = base.getType(symbols, currClassInfo, classTable).toString();

					if (f.check('(')) {
						List<Expr> actuals = new ArrayList<>();
						actuals.add(new ThisExpr(methodClassName)); // implicit 'this' as first actual
						if (!f.test(')')) {
							actuals = parseActuals(f, actuals, symbols);
						}
						f.match(')');
						// word is the object, class name
						base = new MethodCallExpr(base, methodClassName+"_"+methodName, actuals);
					} else {
						// field access
						String fieldName = methodName;
						base = new FieldAccessExpr(base, fieldName);
					}
					
				}
				
				if (f.check('(')) {
					if (classTable.containsKey(word)) {
						throw new CompileException("Missing new in constructor call");
					} else {
						throw new CompileException("Function calls not supported");
					}
				}
				return base;
			case OPERATOR:
				if (f.check('(')) {
					Expr parsePrimary = parseExpr(f, symbols);
					if (!f.check(')')) {
						throw new CompileException("Expected closing ')' after expression");
					}
					return parsePrimary;
				}
			default:
				return null;
		}
	}

	static String getFormals(SamTokenizer f, SymbolTable symbolTable) throws CompileException {
		StringBuilder code = new StringBuilder();
		Map<String, MethodInfo> methods;
		MethodInfo methodInfo;

		if (currClassInfo != null) {
			methods = currClassInfo.methods;
			if (methods.containsKey(currentMethodName)){
				methodInfo = methods.get(currentMethodName);

				int offset = (methodInfo.paramTypes.size()) - (formalsCount + 1);

				symbolTable.enter("this", new String[] { currentClassName, Integer.toString(offset), ""}); // 'this' at offset 0?
				while (true) {
					if (f.peekAtKind() == Tokenizer.TokenType.WORD) {
						String type = f.getWord();
						if (!type.equals("int") && !type.equals("bool") && !type.equals("String") && !Type.userDefinedTypes.containsKey(type)) {
							throw new CompileException("Unknown type in formals: " + type);
						}

						if (f.peekAtKind() != Tokenizer.TokenType.WORD) {
							throw new CompileException("Expected identifier after type in formals");
						}

						String id = f.getWord();
						if (reservedWords.contains(id)) {
							throw new CompileException("Cannot use reserved word as identifier: " + id);
						}

						offset = -(formalsCount + 1);
						if (methods.containsKey(currentMethodName)){
							if (!methods.get(currentMethodName).isConstructor) {
								offset = (methodInfo.paramTypes.size()) - (formalsCount + 2);
								System.out.println("IS NOT CONSTRUCTOR formal offset="+offset);
							} else {
								offset = formalsCount + 1; 
								System.out.println("IS CONSTRUCTOR formal offset="+offset);
							}
						} 

						symbolTable.enter(id, new String[]{type, Integer.toString(offset), ""});
						formalsCount++;

						// If comma, continue parsing formals
						if (f.check(',')) {
							continue;
						}

						// If closing paren, done
						if (f.test(')')) {
							break;
						}

						throw new CompileException("Expected ',' or ')' after formal parameter");
					} else if (f.test(')')) {
						break;
					} else {
						throw new CompileException("Unexpected token in formals: " + f.peekAtKind());
					}
				}
			}
		}
		return code.toString();
	}

	static String getDeclarations(SamTokenizer f, SymbolTable symbolTable) throws CompileException {
		StringBuilder code = new StringBuilder();


		while (f.peekAtKind() == Tokenizer.TokenType.WORD) {
			String type = f.getWord();

			if (!type.equals("int") && !type.equals("bool") && !type.equals("String")
			&& !Type.userDefinedTypes.containsKey(type)) {
				throw new CompileException("Unknown type in declaration: " + type);
			}

			do {
				if (f.peekAtKind() != Tokenizer.TokenType.WORD) {
					throw new CompileException("Expected identifier after type");
				}
				String varName = f.getWord();
				if (reservedWords.contains(varName)) {
					throw new CompileException("Cannot use reserved word as identifier: " + varName);
				}

				if (symbolTable.containsKey(varName)) {
					throw new CompileException("Variable '" + varName + "' already declared");
				}

				// Compute stack offset: 1 (rv) + formalsCount + localVarCount
				// one for link, one for fbr
				int offset = 2  + localVarCount;
				if (currentMethodName.equals("Main_main")) {
					offset = 1 + localVarCount; // one for rv
				}
				
				symbolTable.enter(varName, new String[] { type, Integer.toString(offset), "" });
				localVarCount++;

			} while (f.check(',')); // handle multiple vars in one declaration

			f.match(';'); // ensures semicolon ends the declaration
		}

		return code.toString();
	}

	static StatementResult getStatements(SamTokenizer f, SymbolTable symbolTable) throws CompileException {
		StringBuilder code = new StringBuilder();
		boolean guaranteesReturn = false;

		while (true) {
			try {
				switch (f.peekAtKind()) {
					case WORD: {
						String word = f.getWord();

						// --- IF Statement ---
						if (word.equals("if")) {
							f.match('(');
							Expr condition = parseExpr(f, symbolTable);
							condition.getType(symbolTable, currClassInfo, classTable);
							String condCode = condition.generateCode(symbolTable, currClassInfo, classTable);
							f.match(')');
							f.match('{');
							StatementResult thenResult = getStatements(f, symbolTable);
							String thenCode = thenResult.code;
							f.match('}');
							f.match("else");
							f.match('{');

							StatementResult elseResult = getStatements(f, symbolTable);
							String elseCode = elseResult.code;
							f.match('}');

							String thenLabel = LabelGenerator.newLabel("then");
							String elseLabel = LabelGenerator.newLabel("else");
							String nextLabel = LabelGenerator.newLabel("next");

							code.append(condCode);
							code.append("JUMPC ").append(thenLabel).append("\n");
							code.append(elseLabel).append(":\n");
							code.append(elseCode);
							code.append("JUMP ").append(nextLabel).append("\n");
							code.append(thenLabel).append(":\n");
							code.append(thenCode);
							code.append(nextLabel).append(":\n");

							if (thenResult.guaranteesReturn && elseResult.guaranteesReturn) {
								guaranteesReturn = true;
							}
							continue;
						}

						// --- WHILE Statement ---
						if (word.equals("while")) {
							f.match('(');
							String condLabel = newLabel("cond");
							String bodyLabel = newLabel("body");
							String endLabel = newLabel("endwhile");

							// Push loop labels onto stacks
							breakLabelStack.push(endLabel);

							Expr condition = parseExpr(f, symbolTable);
							condition.getType(symbolTable, currClassInfo, classTable);
							String condCode = condition.generateCode(symbolTable, currClassInfo, classTable);
							f.match(')');

							f.match('{');
							StatementResult bodyResult = getStatements(f, symbolTable);
							String bodyCode = bodyResult.code;
							f.match('}');

							// Pop loop labels off after loop
							breakLabelStack.pop();

							code.append("JUMP ").append(condLabel).append("\n");
							code.append(bodyLabel).append(":\n");
							code.append(bodyCode);
							code.append(condLabel).append(":\n");
							code.append(condCode);
							code.append("JUMPC ").append(bodyLabel).append("\n");
							code.append(endLabel).append(":\n");
							continue;
						}

						// --- BREAK Statement ---
						if (word.equals("break")) {
							f.match(';');
							if (breakLabelStack.isEmpty()) {
								throw new CompileException("break used outside of loop");
							}
							code.append("JUMP ").append(breakLabelStack.peek()).append("\n");
							continue;
						}

						// --- RETURN Statement ---
						if (word.equals("return")) {
							Expr returnExpr = parseExpr(f, symbolTable);

							Type returnType = returnExpr.getType(symbolTable, currClassInfo, classTable);
							String returnCode = returnExpr.generateCode(symbolTable, currClassInfo, classTable);
							f.match(';');

							ClassInfo classInfo = classTable.get(currentClassName);
							MethodInfo methodInfo = classInfo.methods.get(currentMethodName);

							String declaredReturnType = methodInfo.returnType;
							Type t = Type.fromString(declaredReturnType);
							if (!t.toString().equals(returnType.toString())) {
								throw new CompileException("Return type mismatch in method " + currentMethodName);
							}

							code.append(returnCode);
							code.append("JUMP ").append(currentMethodEndLabel).append("\n");

							guaranteesReturn = true; // return found
							continue;
						}

						// --- Assignment ---
						if (f.test('=')) {
							f.getOp(); // consume '='
							System.out.println("===== parsing assignment to "+word);
							Expr expr = parseExpr(f, symbolTable);
							expr.getType(symbolTable, currClassInfo, classTable);

							String exprCode = expr.generateCode(symbolTable, currClassInfo, classTable);
							if (!f.check(';')) {
								throw new CompileException("Extra parenthesis");
							}

							String[] varAttr = symbolTable.lookup(word);
							if (varAttr == null) { // not found in method scope
								List<FieldInfo> fields = classTable.get(currentClassName).fields;
								for (FieldInfo field : fields) {
									if (field.name.equals(word)) {
										varAttr = new String[2];
										varAttr[0] = field.type;

										System.out.println("is Constructor?="+classTable.get(currentClassName).methods.get(currentMethodName).isConstructor);
										if (classTable.get(currentClassName).methods.get(currentMethodName).isConstructor) {
											System.out.println("offset for cons="+(field.offset+1));
											varAttr[1] = Integer.toString(field.offset+1);
										} else {
											
											varAttr[1] = Integer.toString(field.offset);
										}
										break;
									}
								}
								if (varAttr == null) {
									throw new CompileException("Unknown variable '" + word + "' not found in method or class scope");
								}
							}

							code.append(exprCode);
							code.append("STOREOFF ").append(varAttr[1]).append("\n"); // varAttr[1] is offset
							continue;
						} 
						
						if (f.test('.')) {
							// method call or field access
							f.pushBack();
							Expr accessBase = parseExpr(f, symbolTable);
							if (!f.check(';')) {
								throw new CompileException("Expected ';' after statement");
							}
							code.append(accessBase.generateCode(symbolTable, currClassInfo, classTable));
							continue;
						}

						throw new CompileException("Unrecognized statement starting with word: " + word);
					}

					case OPERATOR: {
						if (f.check(';')) {
							// empty statement
							continue;
						} else if (f.test('}')) {
							// end of block
							return new StatementResult(code.toString(), guaranteesReturn);
						}
						throw new CompileException("Unexpected operator in statement.");
					}
					default:
						
						throw new CompileException("Unrecognized start of statement: " + f.peekAtKind());
				}
			} catch(CompileException e) {
				throw e;
			}
		}
	}

	static List<Expr> parseActuals(SamTokenizer f, List<Expr> actuals, SymbolTable symbols) throws CompileException {
		// List<Expr> actuals = new ArrayList<>();
		actuals.add(parseExpr(f, symbols));
		while (f.check(',')) {
			actuals.add(parseExpr(f, symbols));
		}
		return actuals;
	}

	static String newLabel(String base) {
		return labelGen.newLabel(base);
	}

    static String reverseString() {
        String str_rev_loop = "str_rev_loop";
        String str_rev_middle = "str_rev_middle";
        String str_rev_ending = "str_rev_ending";
        String str_rev_loop2 = "str_rev_loop2";

		// rv = -2, 
		// (in func) 0 = (in stack) -1, 1 = 2, 2 = 3
		// in stack: 0 (link), 1 (fbr)
		// rv = -2, 
        return
			"reverseString:\n"+
			"PUSHOFF "+ (-1) +"\nPUSHIMM 0\nSWAP\n"+
			str_rev_loop + ":\n"+
				"DUP\nPUSHIND\nISNIL\n"+
				"JUMPC " + str_rev_middle + "\n"+
				"SWAP\nPUSHIMM 1\nADD\nSWAP\nPUSHIMM 1\nADD\n"+        
				"JUMP " + str_rev_loop + "\n"+
			str_rev_middle + ":\n"+
				"PUSHOFF "+ (3) +"\n"+
				"PUSHIMM -1\nADD\nSTOREOFF "+ (3) +"\n"+
				"PUSHOFF "+ (2) +"\n"+
				"PUSHIMM 1\nADD\nMALLOC\n"+
			str_rev_loop2 + ":\n"+
				"DUP\nPUSHOFF "+ (3) +"\n"+
				"PUSHIND\nSTOREIND\nPUSHIMM 1\nADD\n"+
				"PUSHOFF "+ (3) +"\n"+
				"PUSHIMM -1\nADD\n"+
				"STOREOFF "+ (3) +"\n"+
				"PUSHOFF "+ (3) +"\n"+
				"PUSHOFF "+ (-1) +"\n"+
				"LESS\n"+
				"JUMPC " + str_rev_ending + "\n"+
				"JUMP " + str_rev_loop2 + "\n"+
			str_rev_ending + ":\n"+
				"PUSHOFF "+ (2) +"\n"+
				"PUSHIMM -1\nTIMES\nADD\n"+
				"STOREOFF "+ (-2) +"\n"+
				"ADDSP -2\n"+
				"JUMPIND\n";
    }

	static String concatString() {
        String str_concat_beginning = "str_concat_beginning";
        String str_concat_loop1 = "str_concat_loop1";
        String str_concat_middle = "str_concat_middle";
        String str_concat_loop2 = "str_concat_loop2";
        String str_concat_concatPrep = "str_concat_concatPrep";
        String str_concat_concat1 = "str_concat_concat1";
        String str_concat_concat2Prep = "str_concat_concat2Prep";
        String str_concat_concat2 = "str_concat_concat2";
        String str_concat_ending = "str_concat_ending";
		// rv = -3, 
		// (in func) 0 = (in stack) -2, 1 = -1, 2 = 2 = rv, 3 = 3, 4 = 4
		// in stack: 0 (link), 1 (fbr)
        return
            "concatString:\n"+
            //"ADDSP 2\n"+ // local var
            str_concat_beginning + ":\n"+
                "PUSHOFF "+ (-2) +"\nPUSHIMM 0\nSWAP\n"+
            str_concat_loop1 + ":\n"+
                lenString(str_concat_middle, str_concat_loop1)+
            str_concat_middle + ":\n"+ 
                "PUSHOFF "+ (-1) +"\nPUSHIMM 0\nSWAP\n"+
                "JUMP " + str_concat_loop2 + "\n"+
            str_concat_loop2 + ":\n"+
                lenString(str_concat_concatPrep, str_concat_loop2)+
            str_concat_concatPrep + ":\n"+
                "SWAP\nDUP\nPUSHOFF 2\n"+
                "ADD\nPUSHIMM 1\nADD\n"+
                "MALLOC\nPUSHOFF "+ (-2) +"\n"+
            str_concat_concat1 + ":\n"+
                "DUP\nPUSHIND\nISNIL\n"+
                "JUMPC " + str_concat_concat2Prep + "\n"+
                "DUP\nPUSHIND\nPUSHOFF 6\nSWAP\n"+
                "STOREIND\nPUSHIMM 1\nADD\nPUSHOFF 6\n"+
                "PUSHIMM 1\nADD\nSTOREOFF 6\n"+
                "JUMP " + str_concat_concat1 + "\n"+
            str_concat_concat2Prep + ":\n"+
                "PUSHOFF "+ (-1) +"\n"+
            str_concat_concat2 + ":\n"+
                "DUP\nPUSHIND\nISNIL\n"+
                "JUMPC " + str_concat_ending + "\n"+
                "DUP\nPUSHIND\nPUSHOFF 6\n"+
                "SWAP\nSTOREIND\nPUSHIMM 1\nADD\n"+
                "PUSHOFF 6\nPUSHIMM 1\nADD\nSTOREOFF 6\n"+
                "JUMP " + str_concat_concat2 + "\n"+
            str_concat_ending + ":\n"+
                "PUSHOFF 6\nPUSHOFF 2\nPUSHOFF 5\n"+
                "ADD\nSUB\nSTOREOFF "+ (-3) +"\nADDSP -7\n"+
                "JUMPIND\n";
            //"POPFBR\n";//PUSHIMM 0\n
    }

	static String repeatString() {
        String str_repeat_beginning = "str_repeat_beginning";
        String str_repeat_prepare = "str_repeat_prepare";
        String str_repeat_allocate = "str_repeat_allocate";
        String str_repeat_pos_prologue = "str_repeat_pos_prologue";
        String str_repeat_neg_prologue = "str_repeat_neg_prologue";
        String str_repeat_setUpForCopy = "str_repeat_setUpForCopy";
        String str_repeat_copy = "str_repeat_copy";

		// rv = -3, 
		// (in func) 0 = (in stack) -2, 1 = -1, 2 = 2 = rv, 3 = 3, 4 = 4
		// in stack: 0 (link), 1 (fbr)
		// int rvIndex = 2; 
		// int offset = 3; // offset of first local var after rv
		
        return
		"repeatString:\n"+
            "PUSHIMM 0\nPUSHOFF "+(-2)+"\n"+
        str_repeat_beginning + ":\n"+ 
            lenString(str_repeat_prepare, str_repeat_beginning)+
        str_repeat_prepare + ":\n"+
            "PUSHOFF "+ (-1)+ "\nPUSHOFF "+ (2)+ "\nTIMES\nPUSHIMM 1\n"+
            "ADD\nDUP\nDUP\nISPOS\n"+
            "JUMPC " + str_repeat_allocate + "\n"+
            "ADDSP -1\nPUSHIMM 1\n"+
        str_repeat_allocate + ":\n"+
            "MALLOC\nPUSHIMM 1\nPUSHOFF "+ (4) + "\n"+
            "CMP\nPUSHIMM 1\nLESS\n"+
            "JUMPC " + str_repeat_neg_prologue + "\n"+
            "DUP\nPUSHOFF "+ (-2) + "\n"+
        str_repeat_copy + ":\n"+
            "DUP\nPUSHIND\nISNIL\n"+
            "JUMPC " + str_repeat_setUpForCopy + "\n"+
            "DUP\nPUSHIND\nPUSHOFF "+ (5) + "\nSWAP\n"+
            "STOREIND\nPUSHOFF "+ (5) + "\nPUSHIMM 1\n"+
            "ADD\nSTOREOFF "+ (5) + "\nPUSHIMM 1\nADD\n"+
            "JUMP " + str_repeat_copy + "\n"+
        str_repeat_setUpForCopy + ":\n"+
            "STOREOFF "+ (3) + "\nPUSHOFF "+ (6) + "\nPUSHOFF "+ (4) + "\nPUSHIMM -1\n"+
            "ADD\nADD\nPUSHOFF "+ (5) + "\nEQUAL\n"+
            "JUMPC " + str_repeat_pos_prologue + "\n"+
            "PUSHOFF "+ (-2) + "\n"+
            "JUMP " + str_repeat_copy + "\n"+
		str_repeat_neg_prologue + ":\n"+
            "STOREOFF "+ (-3) + "\nADDSP -3\n"+
			"JUMPIND\n"+
        str_repeat_pos_prologue + ":\n"+
            "STOREOFF "+ (-3) + "\nADDSP -4\n"+
			"JUMPIND\n";
    }

    static String compareString() {
        String str_cmp_loop = LabelGenerator.newLabel("str_cmp_loop");
        String str_cmp_secIsBigger = LabelGenerator.newLabel("str_cmp_secIsBigger");
        String str_cmp_firstIsBigger = LabelGenerator.newLabel("str_cmp_firstIsBigger");
        String str_cmp_prologue = LabelGenerator.newLabel("str_cmp_prologue");
        String str_cmp_test = LabelGenerator.newLabel("str_cmp_test");
        String str_cmp_equal = LabelGenerator.newLabel("str_cmp_equal");
        String str_cmp_end = LabelGenerator.newLabel("str_cmp_end");

		// rv = -3, 
		// (in func) 0 = (in stack) -2, 1 = -1, 2 = 2 = rv, 3 = 3, 4 = 4
		// in stack: 0 (link), 1 (fbr)
        return
			"PUSHIMM 0\nPUSHIMM 0\nPUSHIMM 0\n"+
			str_cmp_loop + ":\n"+
				"PUSHOFF "+ (-2) +"\nPUSHIND\nDUP\nISNIL\n"+
				"JUMPC " + str_cmp_secIsBigger + "\n"+
				"PUSHOFF "+ (-1) +"\nPUSHIND\nDUP\nISNIL\n"+
				"JUMPC " + str_cmp_firstIsBigger + "\n"+
				"CMP\nDUP\nISNEG\n"+
				"JUMPC " + str_cmp_test + "\n"+
				"DUP\nISPOS\n"+
				"JUMPC " + str_cmp_test + "\n"+
				"ADDSP -1\n"+
				"PUSHOFF "+ (-2) +"\nPUSHIMM 1\nADD\nSTOREOFF "+ (-2) +"\n"+
				"PUSHOFF "+ (-1) +"\nPUSHIMM 1\nADD\nSTOREOFF "+ (-1) +"\n"+
				"PUSHOFF 3\nPUSHIMM 1\nADD\nSTOREOFF 3\n"+
				"PUSHOFF 4\nPUSHIMM 1\nADD\nSTOREOFF 4\n"+
				"JUMP " + str_cmp_loop + "\n"+
			str_cmp_secIsBigger + ":\n"+
				"ADDSP -1\nPUSHOFF "+ (-1) +"\nPUSHIND\nISNIL\n"+
				"JUMPC " + str_cmp_equal + "\n"+
				"PUSHIMM 1\nSTOREOFF 2\n"+
				"JUMP " + str_cmp_prologue + "\n"+
			str_cmp_firstIsBigger + ":\n"+
				"ADDSP -1\nPUSHIMM -1\nSTOREOFF 2\n"+
				"JUMPC " + str_cmp_prologue + "\n"+
			str_cmp_prologue + ":\n"+
				"PUSHOFF "+ (-2) +"\nPUSHOFF 3\nSUB\nADDSP -1\n"+
				"PUSHOFF "+ (-1) +"\nPUSHOFF 4\nSUB\nADDSP -1\n"+
				"ADDSP -2\n"+
				"JUMP " + str_cmp_end + "\n"+
			str_cmp_test + ":\n"+
				"STOREOFF 2\n"+
				"JUMP " + str_cmp_prologue + "\n"+
			str_cmp_equal + ":\n"+
				"PUSHIMM 0\n"+
				"JUMP " + str_cmp_test + "\n"+
			str_cmp_end + ":\n";
				//"STOREOFF "+ (-3) +"\nADDSP -1\n";
    }

	static String compareStringOp(int expected) {
        return compareString() + 
           "PUSHIMM " + expected + "\n" +
           "EQUAL\nSTOREOFF" +(-3)+ "\nJUMPIND\n";
    }
	
	static String equalString() {
        return "equalString:\n" + compareStringOp(0);
    }
	
	static String lessString() {
        return "lessString:\n" + compareStringOp(1);
    }

    static String greaterString() {
        return "greaterString:\n" + compareStringOp(-1);
    }

	static String lenString(String conditionLabel, String jumpLabel) {
		return 
		"DUP\nPUSHIND\nISNIL\n"+
		"JUMPC "+conditionLabel+"\n"+
		"SWAP\nPUSHIMM 1\nADD\nSWAP\nPUSHIMM 1\nADD\n"+
		"JUMP "+jumpLabel+"\n";
    }

}


