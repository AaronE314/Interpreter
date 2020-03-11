import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Stack;

public class Interpreter {

    private static int nextID = 0;

    public static HashMap<Integer, SymbolValue> symbolTable = new HashMap<>();
    public static HashMap<String, Integer> symbolNameToID = new HashMap<>();

    public static LinkedHashMap<Integer, HashMap<Integer, SymbolValue>> symbolTables = new LinkedHashMap<>();
    public static HashMap<Integer, HashMap<String, Integer>> symbolNameToIDTables = new HashMap<>();

    public static final String KEYWORDS[] = { "while", "do", "od", "def", "fed", "if", "then", "else", "fi", "print", "return", "or", "and", "not"};
    public static final HashSet<String> TYPES = new HashSet<>(
        Arrays.asList(new String[] {"int", "double"}));

    public static boolean readNextChar = true;
    public static char prevChr = 0;

    public static Scanner sin;

    public static boolean printTables = false;
    public static boolean printTree = false;

    public static Stack<HashMap<Integer, SymbolValue>> executionStack = new Stack<>();
    public static Stack<ReturnVal> returnStack = new Stack<>();

    public static final int MAIN_SYMBOL_TABLE_ID = 0;
    public static int currentSymbolTable = MAIN_SYMBOL_TABLE_ID;

    public static HashMap<Integer, Node> functionParseTrees = new HashMap<>();

    public static String formatedCode = "";
    public static int indentNum = 0;

    public static int lineCount = 1;
    public static int charCount = 1;
    public static String currFName = "main";

    public static Token<?> lookahead;

    public static enum TOKEN_TYPE {
        KEYWORD, ID, CONSTANT, GLOBAL;
    }

    public static enum TOKENS {

        TYPE("TYPE", "#D67E5C"),
        ID("ID", "#E4B781"),
        OPERATOR("OPERATOR", "#16A3B6"),
        KEYWORD("KEYWORD", "#DF769B"),
        DOUBLE("DOUBLE", "#7060EB"),
        INTEGER("INTEGER", "#7060EB"),
        ERROR("ERROR", "red"),
        BRACKET("BRACKET", "#FFD700"),
        FORMATING("FORMATING", "black"),

        COMMA("COMMA", "#B2CACD", ","),
        SEMICOLON("SEMICOLON", "#B2CACD", ";"),
        DOT("DOT", "#B2CACD", ".");
        
        private String token;
        private String symbol;

        private TOKENS(String token, String color, String symbol) {
            this.token = token;
            this.symbol = symbol;
        }

        private TOKENS(String token, String color) {
            this.token = token;
            this.symbol = "";
        }

        private String getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    public static class ReturnVal {
        public Number val = null;
    }

    public static enum NODE_TYPE {

        ID, TYPE, ASSIGN, CONSTANT_INT, CONSTANT_DOUBLE, OP, BOOL, KEYWORD, FUNCTION, PARAMS, DECL;

    }

    public static class Node {
        
        private NODE_TYPE type;
        private Object value;
        private Token<?> token;
        private Number computedValue;

        private ArrayList<Node> children;

        public Node(NODE_TYPE type, Object value) {
            this.type = type;
            this.value = value;
            this.token = null;
            this.children = new ArrayList<Node>();
        }

        public Node(NODE_TYPE type, Token<?> token) {
            this.type = type;
            this.value = token.value;
            this.token = token;
            this.children = new ArrayList<Node>();
        }

        public void addChild(Node node) {
            if (node != null) {
                this.children.add(node);
            }
        }

        public void addFirstChild(Node node) {
            if (node != null) {
                this.children.add(0, node);
            }
        }

        public void setValue(Object val) {
            this.value = val;
        }

        public void setCValue(Number val) {
            this.computedValue = val;
        }

        public void setType(NODE_TYPE type) {
            this.type = type;
        }
        
        public ArrayList<Node> getChilren() {
            return this.children;
        }

        public NODE_TYPE getType() {
            return this.type;
        }

        public Object getValue() {
            return this.value;
        }

        public Token<?> getToken() {
            return this.token;
        }

        public String infoString() {
            return this.value + ", " + this.type;
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder(50);
            print(buffer, "", "");
            return buffer.toString();
        }
    
        private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
            buffer.append(prefix);
            if (type != null) {
                buffer.append(type + ": ");
            }
            buffer.append(value);
            buffer.append('\n');
            for (Iterator<Node> it = children.iterator(); it.hasNext();) {
                Node next = it.next();
                if (it.hasNext()) {
                    // next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
                    next.print(buffer, childrenPrefix + "|-- ", childrenPrefix + "|   ");
                } else {
                    // next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
                    next.print(buffer, childrenPrefix + "|__ ", childrenPrefix + "    ");
                }
            }
        }

    }

    public static class ParseException extends Exception {
        
        private static final long serialVersionUID = 356363453L; 

        public ParseException(String message) {
            super(message);
        }
    }

    public static class Token<T> implements Serializable {

        private static final long serialVersionUID = 12345467643L; 

        private T value;
        private TOKENS token;
        private String idType;
        private int constId;
        private Object idValue;
        private Class<T> type;

        @SuppressWarnings("unchecked")
        public Token(T value, TOKENS token) {
            this.value = value;
            this.token = token;

            if (token == TOKENS.INTEGER) {
                this.type = (Class<T>) Integer.class;
            } else if (token == TOKENS.DOUBLE) {
                this.type = (Class<T>) Double.class;
            } else {
                this.type = null;
            }
        }

        public Class<T> getType() {
            return this.type;
        }

        public void flipSign() {
            Object oVal = this.value;

            if (oVal instanceof Integer) {
                int val = (int) oVal;

                val *= -1;

                this.value = type.cast(val);

            } else if (oVal instanceof Double) {
                double val = (double) oVal;

                val *= -1;

                this.value = type.cast(val);
            }
        }

        public T getValue() {
            return value;
        }

        public void setIdType(String type) {
            this.idType = type;
        }

        public void setValue(T val) {
            this.value = val;
        }

        public void setConstId(int id) {
            this.constId = id;
        }

        public int getConstId() {
            return this.constId;
        }

        public String getIdType() {
            return this.idType;
        }

        public String getValueString() {
            return (value != null) ? value.toString() : token.getSymbol();
        }

        public TOKENS getToken() {
            return token;
        }

        @Override
        public String toString() {
            if (value != null && token != TOKENS.ERROR) {
                return "<" + token + "," + value + ">";
            } else {
                return "<" + token + ">";
            }
        }
    }

    public static class SymbolValue implements Serializable {

        private static final long serialVersionUID = 129348938L; 

        private int id;
        private Token<?> token;
        private TOKEN_TYPE type;
        private boolean global;
        
        public SymbolValue(int id, Token<?> token, TOKEN_TYPE type) {
            this.id = id;
            this.token = token;
            this.type = type;
            this.global = false;
        }

        public SymbolValue(int id, Token<?> token, TOKEN_TYPE type, boolean global) {
            this.id = id;
            this.token = token;
            this.type = type;
            this.global = global;
        }

        public boolean isGlobal() {return this.global;}
        public int getId() { return this.id;}
        public Object getValue() { return this.token.getValue();}
        public String getName() { return this.token.toString();}
        public Token<?> getToken() { return this.token;}
        public TOKEN_TYPE getType() { return this.type;}

        @Override
        public String toString() {
            return token.toString();
        }
    }

    public static String sinNext() throws NoSuchElementException {
        charCount += 1;
        return sin.next();
    }

    public static void main(String[] args) {

        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-s")) {
                    printTables = true;
                }
                if (args[i].equals("-t")) {
                    printTree = true;
                }
                if (args[i].equals("-h")) {
                    System.out.println("-s: to print symbol tables");
                    System.out.println("-t: to print parse tree");
                }
            }
        }
        
        sin = new Scanner(System.in);
        
        sin.useDelimiter("");

        // Make Main symbol table
        symbolTables.put(MAIN_SYMBOL_TABLE_ID, symbolTable);
        symbolNameToIDTables.put(MAIN_SYMBOL_TABLE_ID, symbolNameToID);

        installKeywords();


        try {
            lookahead = getNextToken();
        } catch(Exception e) {
            System.err.println("Error: Code empty");
            return;
        }

        // Call to parse
        Node root = parse();

        if (root != null) {
            if (printTables || printTree) {
                System.out.println("Program Output:");
            }

            executionStack.push(symbolTables.get(MAIN_SYMBOL_TABLE_ID));
            returnStack.push(new ReturnVal());
            try {
                Eval(root);
            } catch (RuntimeException e) {
                if (e.getMessage() != null) {
                    System.out.println("RuntimeException: " + e.getMessage());
                } else {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sin.close();

    }

    /**
     * Adds all the keywords to the main symbol table
     */
    public static void installKeywords() {

        int tableId = currentSymbolTable;

        currentSymbolTable = MAIN_SYMBOL_TABLE_ID;
        for (String keyword: KEYWORDS) {
            installSymbol(new Token<String>(keyword, TOKENS.KEYWORD), TOKEN_TYPE.KEYWORD);
        }

        currentSymbolTable = tableId;
    }

    public static int newSymbolTable(int id) {

        symbolTables.put(id, new HashMap<>());
        symbolNameToIDTables.put(id, new HashMap<>());

        currentSymbolTable = id;

        return id;

    }

    public static <E> int installSymbol(Token<?> name, TOKEN_TYPE type) {
        
        int id = -1;

        HashMap<Integer, SymbolValue> currentTable = symbolTables.get(currentSymbolTable);
        HashMap<String, Integer> currentSymbolNameToIdTable = symbolNameToIDTables.get(currentSymbolTable);

        if (symbolNameToID.containsKey(name.getValueString())) {
            id = symbolNameToID.get(name.getValueString());

            if (!currentSymbolNameToIdTable.containsKey(name.getValueString())) {
                currentSymbolNameToIdTable.put(name.getValueString(), id);
                currentTable.put(id, new SymbolValue(id, name, type, true));
            }

        } else if (currentSymbolNameToIdTable.containsKey(name.getValueString())) {
            id = currentSymbolNameToIdTable.get(name.getValueString());
        } else {
            id = nextID++;

            currentSymbolNameToIdTable.put(name.getValueString(), id);
            currentTable.put(id, new SymbolValue(id, name, type));
        }
        return id;

    }

    public static boolean is_keyword(String name) {

        if (symbolNameToID.containsKey(name)) {

            int id = symbolNameToID.get(name);

            if (symbolTable.containsKey(id)) {

                SymbolValue value = symbolTable.get(id);

                if (value.type == TOKEN_TYPE.KEYWORD) {
                    return true;
                }

            }

        }

        return false;

    }

    public static Token<?> getNextToken() throws Exception {

        char chr = (readNextChar) ? sinNext().charAt(0) : prevChr;
        readNextChar = true;

        if (chr >= '0' && chr <= '9') {

            return getNum(chr);

        } else if (chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z') {

            return getSymbol(chr);
        } else {

            switch(chr) {

                case '<':
                case '>':
                case '=':
                    String operator = "" + chr;
                    chr = sinNext().charAt(0);

                    if (( operator.equals("<") && chr == '>') || chr == '=') {
                        return new Token<String>(operator + chr, TOKENS.OPERATOR);
                    } else {
                        readNextChar = false;
                        prevChr = chr;
                        return new Token<String>(operator, TOKENS.OPERATOR);
                    }
                case '+':
                case '-':
                case '*':
                case '/':
                case '%':
                    return new Token<String>("" + chr, TOKENS.OPERATOR);
                case '(':
                case ')':
                    return new Token<String>("" + chr, TOKENS.BRACKET);
                case ',':
                    return new Token<String>(null, TOKENS.COMMA);
                case ';':
                    return new Token<String>(null, TOKENS.SEMICOLON);
                case '.':
                    return new Token<String>(null, TOKENS.DOT);
                case '\n':
                    lineCount += 1;
                    charCount = 0;
                case '\r':
                case '\t':
                case ' ':
                    return getNextToken();
                default:
                    return new Token<Void>(null, TOKENS.ERROR);
            }

        }

    }

    public static Token<?> getNum(char chr) {

        int intValue = chr - '0';
        double doubleValue = 0.0;
        int exponent = 0;
        double pow = 10;

        boolean typeDouble = false;

        chr = sinNext().charAt(0);

        while (sin.hasNext() && chr >= '0' && chr <= '9') {

            intValue = (int) pow * intValue + chr - '0';

            chr = sinNext().charAt(0);

        }

        if (chr == '.') {

            typeDouble = true;

            pow = 0.1;

            if (sin.hasNext()) {
                
                chr = sinNext().charAt(0);
                while (chr >= '0' && chr <= '9') {

                    doubleValue += pow * (chr - '0');
                    
                    if (!sin.hasNext()) {
                        break;
                    }
                    chr = sinNext().charAt(0);
                    pow /= 10;
                    
                }

            }

        }
        
        if (chr == 'e') {

            pow = 10;

            chr = sinNext().charAt(0);

            int sign = 1;
            if (chr == '-') {
                sign = -1;
                typeDouble = true;
                chr = sinNext().charAt(0);
            }


            while (sin.hasNext() && chr >= '0' && chr <= '9') {

                exponent = (int) pow * exponent + chr - '0';
    
                chr = sinNext().charAt(0);
    
            }

            exponent *= sign;

        }

        boolean error = ((chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z'));

        readNextChar = error;
        prevChr = chr;

        if (!typeDouble) {

            int finalValue = intValue * (int) Math.pow(10, exponent);

            if (!error) {
                Token<Integer> token = new Token<Integer>(finalValue, TOKENS.INTEGER);
                int id = installSymbol(token, TOKEN_TYPE.CONSTANT);
                token.setConstId(id);
                return token;
            } else {
                return new Token<String>(String.valueOf(finalValue) + chr, TOKENS.ERROR);
            } 
        } else {

            double finalValue = (intValue + doubleValue) * Math.pow(10, exponent);

            if (!error) {
                Token<Double> token = new Token<Double>(finalValue, TOKENS.DOUBLE);
                int id = installSymbol(token, TOKEN_TYPE.CONSTANT);
                token.setConstId(id);
                return token;
            } else {
                return new Token<String>(String.valueOf(finalValue) + chr, TOKENS.ERROR);
            } 

        }

    }

    public static Token<?> getSymbol(char chr) {

        String name = "";

        do {

            name += chr;

            chr = sinNext().charAt(0);

        } while (chr >= '0' && chr <= '9' || chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z');

        readNextChar = false;
        prevChr = chr;

        if (TYPES.contains(name)) {

            return new Token<String>(name, TOKENS.TYPE);
        } else if (is_keyword(name)) {

            return new Token<String>(name, TOKENS.KEYWORD);
        } else {

            return new Token<Integer>(installSymbol(new Token<String>(name, TOKENS.ID), TOKEN_TYPE.ID), TOKENS.ID);
        }

    }

    public static void changeIndent(int amount) {
        indentNum += amount;

        if (indentNum < 0) {
            indentNum = 0;
        }

        if (formatedCode.endsWith("   ")) {
            formatedCode = formatedCode.substring(0, formatedCode.length() - 3);
        }
    }

    public static void newLineInFormat() {

        if (!formatedCode.endsWith("\n")) {
            formatedCode += "\n";

            for (int i = 0; i < indentNum; i++) {
                formatedCode += "   ";
            }
        }
    }

    public static void addToFormat(Token<?> val) {

        HashMap<Integer, SymbolValue> currentTable = symbolTables.get(currentSymbolTable);

        if (val.getValueString().equals("then")) {
            formatedCode += " ";
        }

        if (formatedCode.endsWith(" ") && val.getToken() == TOKENS.SEMICOLON) {
            formatedCode = formatedCode.substring(0, formatedCode.length() - 1);
        }

        if (val.getToken() == TOKENS.ID) {

            SymbolValue value = symbolTable.get(val.getValue());

            if (value == null) {
                value = currentTable.get(val.getValue());
            }

            formatedCode += value.getToken().getValue();
        } else {
            formatedCode += val.getValueString();
        }

        if (val.getToken() == TOKENS.KEYWORD || val.getToken() == TOKENS.TYPE || val.getToken() == TOKENS.COMMA) {
            formatedCode += " ";
        }


    }

    public static boolean hasReturn() {
        return returnStack.peek().val != null;
    }

    public static String formatCenter(String str, int width) {

        int padSize = width - str.length();
        int padStart = str.length() + padSize / 2;

        str = String.format("%" + padStart + "s", str);
        return String.format("%-" + width + "s", str);
    }

    public static void initIDValue(int id) {
        SymbolValue sv = executionStack.peek().get(id);

        if (sv == null) {
            sv = symbolTables.get(MAIN_SYMBOL_TABLE_ID).get(id);
        }

        if (sv != null) {

            Token<?> token = sv.getToken();

            if (token.idValue != null) {
                throw new RuntimeException(token.value + " has already been declared");
            }
            
            if (token.idType.equals("INT")) {
                token.idValue = (int) 0;
            } else if (token.idType.equals("DOUBLE")) {
                token.idValue = (double) 0.0;
            } else {
                throw new RuntimeException(token.idValue + " is an unknown type");
            }

        } else {
            throw new RuntimeException(id + "not found");
        }
    }

    public static void setIDValue(int id, Object value) {

        SymbolValue sv = executionStack.peek().get(id);

        if (sv == null) {
            sv = symbolTables.get(MAIN_SYMBOL_TABLE_ID).get(id);
        }

        if (sv != null) {

            Token<?> token = sv.getToken();

            if (token.idValue == null) {
                throw new RuntimeException(token.value + " has not been declared");
            }
            
            if (token.idType.equals("INT")) {
                if (value instanceof Integer) {
                    token.idValue = (int) value;
                } else {
                    throw new RuntimeException(value + " is not of type INT");
                }
            } else if (token.idType.equals("DOUBLE")) {
                if (value instanceof Number) {
                    token.idValue = ((Number) value).doubleValue();
                } else {
                    throw new RuntimeException(value + " is an unknown type");
                }
            } else {
                throw new RuntimeException(token.idValue + " is an unknown type");
            }

        } else {
            throw new RuntimeException(id + "not found");
        }

    }

    public static Number getNodeValue(Node node) {

        if (node.type == NODE_TYPE.ID) {

            return (Number) executionStack.peek().get((int) node.value).getToken().idValue;

        } else if (node.type == NODE_TYPE.CONSTANT_DOUBLE || node.type == NODE_TYPE.CONSTANT_INT) {
            return (Number) node.value;
        } else {
            if (node.computedValue != null) {
                return node.computedValue;
            } else {
                return (Number) node.value;
            }
        }

    }

    public static void printSymbolTables() {

        symbolTables.entrySet().forEach(entry -> {

            int key = entry.getKey();

            String name = (key == 0) ? "Main Table" : symbolTable.get(key).getToken().getValue().toString();
            
            System.out.println();
            System.out.println("---------------------------");
            System.out.println(String.format("| %-24s|", name));
            System.out.println("|-------------------------|");
            System.out.println("|" + formatCenter("ID", 4) + "|" + String.format("%-10s%10s", "Value", "Properties") + "|");
            System.out.println(String.format("|%4s+%20s|", "-", "-").replace(" ", "-"));

            HashMap<Integer, SymbolValue> table = entry.getValue();

            table.entrySet().forEach(entry2 -> {

                if (entry2.getValue().getType() != TOKEN_TYPE.KEYWORD) {
                    SymbolValue sv = entry2.getValue();

                    System.out.println(
                        "|" + formatCenter(entry2.getKey().toString(), 4) + "|"
                        + String.format("%-10s%10s", sv.getToken().getValue().toString() + ((sv.getToken().idValue != null) ? ", " + sv.getToken().idValue : "")
                        , (sv.isGlobal() ? TOKEN_TYPE.GLOBAL + ", " : "") + entry2.getValue().getType() + ((sv.getToken().getIdType() != null) ? ", " + sv.getToken().getIdType() : "")) + "|"
                    );
                }

            });

            System.out.println("---------------------------");


        });

    }

    public static Node parse() {

        try {

            Node root = program();

            if (root != null) {

                if (printTree) {
                    System.out.println(root.toString());

                    for (Node func : functionParseTrees.values()) {
                        System.out.println(func.toString());
                    }
                }

                if (printTables) {
                    printSymbolTables();
                }

                return root;

            } else {
            System.out.println(formatedCode);
                System.err.println("Error Invalid program format");
            }
        } catch (ParseException e) {

            System.out.println(formatedCode);
            System.err.println("ParseException " + e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public static boolean match(String t) throws Exception {

        if (lookahead.getValueString().equals(t)) {

            if (t == ".") {
                return true;
            } 
            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                if (!lookahead.getValueString().equals(".")) {
                    error(t, lookahead.getValueString());
                    return false;
                }
            }
            return true;
        } else {
            error(t, lookahead.getValueString());
            return false;
        }

    }


    public static void error(String expected, String received) throws Exception {

        throw new ParseException(lineCount + ":" + charCount + ": Expected " + expected + " but received " + received);

    }

    public static Node program() throws Exception {

        Node root = new Node(null, "root");

        // Node fdecs =
        fdecls();

        // if (fdecs != null) {

        // root.addChild(fdecs);

        Node decs = declarations();

        // if (decs != null) {

        root.addChild(decs);

        Node satements = statement_seq();

        root.addChild(satements);

            // if (satements != null) {
        match(TOKENS.DOT.getSymbol());
        return root;
            // } else {
            //     error("id, if, while, print or return", lookahead.getValueString());
            // }
        // } else {
        //     error("int or double", lookahead.getValueString());
        // }
        // } else {
        //     error("def", lookahead.getValueString());
        // }

        // return null;

    }

    public static Node fdecls() throws Exception {

        Node func = fdec();

        if (func != null) {
            match(TOKENS.SEMICOLON.getSymbol());
            newLineInFormat();
            // func.addChild(fdecls());
            functionParseTrees.put((int) func.value, func);
            fdecls();
            return func;
        } else {
            return null;
        }

    }

    public static Node fdec() throws Exception {

        Node func = new Node(NODE_TYPE.FUNCTION, "");

        if (lookahead.getValueString().equals("def")) {

            match("def");

            func.addChild(type());

            Node id = fname();
            newSymbolTable((int) id.value);
            func.setValue(id.value);

            match("(");
            func.addChild(params());
            match(")");

            changeIndent(1);
            newLineInFormat();

            func.addChild(declarations());
            func.addChild(statement_seq());

            changeIndent(-1);
            
            currentSymbolTable = MAIN_SYMBOL_TABLE_ID;
            match("fed");

            return func;

        }

        return null;

    }

    public static Node params() throws Exception {

        Node p = new Node(NODE_TYPE.PARAMS, "params");
        Node t = type();
        p.addChild(t);
        if (t != null) {
            p.addChild(var((String) t.value));
            p.addChild(params_rest());
            return p;
        } else {
            return p;
        }

    }

    public static Node params_rest() throws Exception {

        if (lookahead.getValueString().equals(",")) {

            match(",");
            return params();
        } else {
            return null;
        }

    }

    public static Node fname() throws Exception {

        return id(null);
    }

    public static Node declarations() throws Exception {

        Node decls = new Node(null, "decls");

        Node decl = decl();

        decls.addChild(decl);

        if (decl != null) {
            match(TOKENS.SEMICOLON.getSymbol());

            newLineInFormat();
            decls.addChild(declarations());
            return decls;
        } else {
            return null;
        }

    }

    public static Node decl() throws Exception {

        Node decl = new Node(NODE_TYPE.DECL, "decl");

        Node t = type();

        if (t == null) {
            return null;
        }

        decl.addChild(t);

        Node vl = varlist((String) t.value);

        decl.addChild(vl);

        return (t != null && vl != null) ? decl : null;

    }

    public static Node type() throws Exception {
        
        if (lookahead.getValueString().equals("int")) {
            match("int");
            return new Node(NODE_TYPE.TYPE, "INT");
        } else if (lookahead.getValueString().equals("double")){
            match("double");
            return new Node(NODE_TYPE.TYPE, "DOUBLE");
        }

        // error("int or double", lookahead.getValueString());
        return null;

    }

    public static Node varlist(String t) throws Exception {

        Node vl = new Node(null, "vl");

        Node v = var(t);
        vl.addChild(v);
        Node vr = varlist_rest(t);
        vl.addChild(vr);

        return (vr == null) ? v : vl;
    }

    public static Node varlist_rest(String t) throws Exception {

        if (lookahead.getValueString().equals(",")) {
            match(",");
            return varlist(t);
        } else {
            return null;
        }
    }

    public static Node statement_seq() throws Exception {

        Node sateSeq = new Node(null, "sateSeq");

        Node s = statement();
        sateSeq.addChild(s);

        Node ssr = statement_seq_rest();
        sateSeq.addChild(ssr);

        return (s != null) ? ((ssr == null) ? s : sateSeq) : null;
    }

    public static Node statement_seq_rest() throws Exception {

        if (lookahead.getValueString().equals(";")) {
            match(";");
            newLineInFormat();
            
            return statement_seq();
        } else {
            newLineInFormat();
            return null;
        }
    }

    public static Node statement() throws Exception {

        Node v = var(null);
        if (v != null) {
            Node statement = new Node(NODE_TYPE.ASSIGN, "=");
            statement.addChild(v);
            match("=");
            statement.addChild(expr());
            return statement;
        } else if (lookahead.getValueString().equals("if")) {
            Node statement = new Node(NODE_TYPE.KEYWORD, "if");
            match("if");
            statement.addChild(bexpr());
            match("then");

            changeIndent(1);
            newLineInFormat();

            statement.addChild(statement_seq());
            // changeIndent(-1);
            statement.addChild(if_rest());
            return statement;
        } else if (lookahead.getValueString().equals("while")) {
            Node statement = new Node(NODE_TYPE.KEYWORD, "while");
            match("while");
            statement.addChild(bexpr());
            match("do");

            changeIndent(1);
            newLineInFormat();

            statement.addChild(statement_seq());

            changeIndent(-1);

            match("od");
            return statement;
        } else if (lookahead.getValueString().equals("print")) {
            Node statement = new Node(NODE_TYPE.KEYWORD, "print");
            match("print");
            statement.addChild(expr());
            return statement;
        } else if (lookahead.getValueString().equals("return")) {
            Node statement = new Node(NODE_TYPE.KEYWORD, "return");
            match("return");
            statement.addChild(expr());
            return statement;
        }

        return null;
    }

    public static Node if_rest() throws Exception {

        if (lookahead.getValueString().equals("fi")) {

            match("fi");

            return null;
        } else if (lookahead.getValueString().equals("else")) {
            match("else");

            changeIndent(1);
            newLineInFormat();

            Node el = new Node(NODE_TYPE.KEYWORD, "else");

            el.addChild(statement_seq());

            changeIndent(-1);

            match("fi");
            return el;
        }

        error("fi or else", lookahead.getValueString());
        return null;
    }

    public static Node expr() throws Exception {

        Node ex = new Node(null, "expr");

        Node t = term();
        ex.addChild(t);

        Node er = expr_rest();

        if (t == null) {
            return null;
        }

        if (er == null) {
            return t;
        }

        er.addFirstChild(t);
        // ex.addChild(er);

        return er;
        // return (t != null) ? ((er == null) ? t : ex) : null;

    }

    public static Node expr_rest() throws Exception {

        if (lookahead.getValueString().equals("+")) {

            Node plus = new Node(NODE_TYPE.OP, "+");

            match("+");

            Node t = term();
            

            if (t == null) {
                return null;
            }

            Node er = expr_rest();
            plus.addChild(er);
            
            if (er != null) {
                er.addChild(t);
            } else {
                plus.addChild(t);
            }

            return plus;

        } else if (lookahead.getValueString().equals("-")) {

            Node minus = new Node(NODE_TYPE.OP, "-");

            match("-");

            Node t = term();

            if (t == null) {
                return null;
            }

            Node er = expr_rest();
            minus.addChild(er);
            
            if (er != null) {
                er.addChild(t);
            } else {
                minus.addChild(t);
            }

            return minus;
        }

        return null;
    }

    public static Node term() throws Exception {

        // Node t = new Node(null, "term");

        Node f = factor();
        // t.addChild(f);

        if (f == null) {
            return null;
        }

        Node tr = term_rest();

        if (tr == null) {
            return f;
        }

        tr.addFirstChild(f);

        return tr;
        // t.addChild(tr);

        // return (f != null) ? ((tr == null) ? f : t) : null;

    }

    public static Node term_rest() throws Exception {

        if (lookahead.getValueString().equals("*")) {

            Node mul = new Node(NODE_TYPE.OP, "*");

            match("*");

            Node f = factor();
            
            if (f == null) {
                return null;
            }
            
            Node tr = term_rest();
            mul.addChild(tr);
            
            if (tr != null) {
                tr.addChild(f);
            } else {
                mul.addChild(f);
            }

            return mul;

        } else if (lookahead.getValueString().equals("/")) {

            Node div = new Node(NODE_TYPE.OP, "/");

            match("/");

            Node f = factor();

            if (f == null) {
                return null;
            }
            
            Node tr = term_rest();
            div.addChild(tr);
            
            if (tr != null) {
                tr.addChild(f);
            } else {
                div.addChild(f);
            }

            return div;

        } else if (lookahead.getValueString().equals("%")) {

            Node div = new Node(NODE_TYPE.OP, "%");

            match("%");

            Node f = factor();
            
            if (f == null) {
                return null;
            }
            
            Node tr = term_rest();
            div.addChild(tr);
            
            if (tr != null) {
                tr.addChild(f);
            } else {
                div.addChild(f);
            }

            return div;
        }

        return null;
    }

    public static Node factor() throws Exception {

        Node id = id(null);
        if (id != null) {
            Node fact = new Node(NODE_TYPE.FUNCTION, "Func Call");
            fact.addChild(id);

            Node rest = factor_rest();
            fact.addChild(rest);
            return (rest == null) ? id : fact;
        }
        Node num = number();
        if (num != null) {
            return num;
        } else if (lookahead.getValueString().equals("(")) {
            match("(");
            Node exp = expr();
            match(")");
            return exp;
        } else if (lookahead.getValueString().equals("-")) {
            match("-");
            num = number();
            Token<?> val = num.getToken();
            if (val != null) {
                Token<?> val2 = symbolTables.get(currentSymbolTable).get(val.getConstId()).getToken();
                val2.flipSign();
            }
            return num;
        }

        error("id, number or -", lookahead.getValueString());
        return null;
    }

    public static Node factor_rest() throws Exception {
        
        if (lookahead.getValueString().equals("(")) {
            match("(");
            Node expSeq = exprseq();
            match(")");
            return expSeq;
        } 
        Node vr = var_rest();
        if (vr != null) {
            return vr;
        }

        return null;
    }

    public static Node exprseq() throws Exception {

        Node exseq = new Node(null, "exprseq");

        Node exp = expr();
        exseq.addChild(exp);

        Node er = exprseq_rest();
        exseq.addChild(er);

        return (exp != null) ? ((er == null) ? exp : exseq) : null;

    }

    public static Node exprseq_rest() throws Exception {

        if (lookahead.getValueString().equals(",")) {
            match(",");
            return exprseq();
        }

        return null;
    }

    public static Node bexpr() throws Exception {
        
        Node bexpr = new Node(null, "bexpr");

        Node bt = bterm();

        if (bt == null) {
            return null;
        }
        bexpr.addChild(bt);

        Node ber = bexpr_rest();

        if (ber == null) {
            return bt;
        }

        ber.addFirstChild(bt);

        return ber;

    }

    public static Node bexpr_rest() throws Exception {

        if (lookahead.getValueString().equals("or")) {

            Node ber = new Node(NODE_TYPE.BOOL, "or");

            match("or");

            Node bt = bterm();

            if (bt == null) {
                return null;
            }

            
            Node ber2 = bexpr_rest();
            
            if (ber2 != null) {
                ber2.addFirstChild(bt);
            } else {
                ber.addFirstChild(bt);
            }
            ber.addChild(ber2);
            return ber;
        }

        return null;
    }

    public static Node bterm() throws Exception {

        Node bt = new Node(null, "bterm");

        Node bf = bfactor();
        
        if (bf == null) {
            return null;
        }
        bt.addChild(bf);

        Node btr = bterm_rest();

        if (btr == null) {
            return bf;
        }
        btr.addFirstChild(bf);

        return btr;
    }

    public static Node bterm_rest() throws Exception {

        if (lookahead.getValueString().equals("and")) {

            Node btr = new Node(NODE_TYPE.BOOL, "and");

            match("and");

            Node bf = bfactor();

            if (bf == null) {
                return null;
            }

            
            Node btr2 = bterm_rest();
            
            if (btr2 != null) {
                btr2.addFirstChild(bf);
            } else {
                btr.addFirstChild(bf);
            }
            btr.addChild(btr2);
            return btr;
        }

        return null;
    }

    public static Node bfactor() throws Exception {

        if (lookahead.getValueString().equals("(")) {
            match("(");
            Node bfr = bfactor_rest();
            match(")");
            return bfr;
        } else if (lookahead.getValueString().equals("not")) {
            Node not = new Node(NODE_TYPE.BOOL, "not");
            match("not");
            Node bf = bfactor();
            not.addChild(bf);
            return not;
        }

        return null;
    }

    public static Node bfactor_rest() throws Exception {

        Node bexpr = bexpr();
        if (bexpr != null) {
            return bexpr;
        }
        Node ex = expr();
        if (ex != null) {
            Node cmp = comp();

            // ex.addChild(comp());
            cmp.addChild(ex);
            cmp.addChild(expr());
            return cmp;
        }

        error("id, number, ( or not", lookahead.getValueString());
        return null;
    }

    public static Node comp() throws Exception {

        Node cmp = new Node(NODE_TYPE.BOOL, "");

        switch (lookahead.getValueString()) {

            case "<":
                match("<");
                cmp.setValue("<");
                return cmp;
            case ">":
                match(">");
                cmp.setValue(">");
                return cmp;
            case "==":
                match("==");
                cmp.setValue("==");
                return cmp;
            case "<=":
                match("<=");
                cmp.setValue("<=");
                return cmp;
            case ">=":
                match(">=");
                cmp.setValue(">=");
                return cmp;
            case "<>":
                match("<>");
                cmp.setValue("<>");
                return cmp;
            default:
                error("<, >, ==, <=, >= or <>", lookahead.getValueString());
                return null;
        }

    }

    public static Node var(String type) throws Exception {

        if (lookahead.getToken() == TOKENS.ID) {
            Token<?> token = null;
            if (lookahead.getIdType() == null && type != null) {
                token = symbolTables.get(currentSymbolTable).get(lookahead.getValue()).getToken();
                token.setIdType(type);
            }

            if (token == null) {
                token = lookahead;
            }

            Node var = new Node(NODE_TYPE.ID, (int) ((Object) lookahead.value));

            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                error("id", lookahead.getValueString());
            }

            var.addChild(var_rest());

            return var;
        }

        return null;
    }

    public static Node var_rest() throws Exception{

        if (lookahead.getValueString().equals("[")) {
            match("[");
            Node exp = expr();
            match("]");
            return exp;
        }

        return null;
    }

    public static Node id(Node type) throws Exception {

        if (lookahead.getToken() == TOKENS.ID) {
            if (lookahead.getIdType() == null && type != null) {
                symbolTables.get(currentSymbolTable).get(lookahead.getValue()).getToken().setIdType((String) type.value);
            }
            Object value = lookahead.getValue();
            int symbolIndex = (int) value;
            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                error("id", lookahead.getValueString());
            }
            return new Node(NODE_TYPE.ID, symbolIndex);
        }

        return null;
    }

    public static Node number() throws Exception {

        if (lookahead.getToken() == TOKENS.DOUBLE || lookahead.getToken() == TOKENS.INTEGER) {

            Token<?> num = lookahead;

            NODE_TYPE type = (lookahead.getToken() == TOKENS.DOUBLE) ? NODE_TYPE.CONSTANT_DOUBLE : NODE_TYPE.CONSTANT_INT;

            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                error("number", lookahead.getValueString());
            }
            return new Node(type, num);
        }

        return null;
    }

    public static void Eval(Node root) throws Exception {

        if (hasReturn()) {
            return;
        }

        if (root.type != null) {
            switch (root.type) {

                case ASSIGN:
                    eval_assign(root);
                    break;
                case OP:
                    eval_op(root);
                    break;
                case KEYWORD:
                    eval_keyword(root);
                    break;
                case FUNCTION:
                    eval_Function(root);
                    break;
                case DECL:
                    eval_decl(root);
                    break;
                default:
    
                    for (Node child : root.getChilren()) {
                        Eval(child);
                    }
            }
        
        } else {
            for (Node child : root.getChilren()) {
                Eval(child);
            }
        }


    }

    public static void eval_decl(Node node) {
        
        ArrayList<Node> children = node.getChilren();

        // Node type = children.get(0);

        if (children.get(1).type == NODE_TYPE.ID) {
            initIDValue((int) children.get(1).value);
        } else if (children.get(1).value.equals("vl")){
            eval_var_list(children.get(1));
        }

    }

    public static void eval_var_list(Node node) {

        ArrayList<Node> children = node.getChilren();

        initIDValue((int) children.get(0).value);

        if (children.get(1).value.equals("vl")) {
            eval_var_list(children.get(1));
        } else if (children.get(1).type == NODE_TYPE.ID) {
            initIDValue((int) children.get(1).value);
        }

    }

    public static void eval_assign(Node node) throws Exception {

        ArrayList<Node> children = node.getChilren();

        Eval(children.get(0));
        Eval(children.get(1));

        if (hasReturn()) {
            return;
        }

        setIDValue((int) children.get(0).value, getNodeValue(children.get(1)) );

    }

    public static void updateCurrFunc(int id) {
        if (id == MAIN_SYMBOL_TABLE_ID) {
            currFName = "main";
        } else {
            currFName = symbolTables.get(MAIN_SYMBOL_TABLE_ID).get(id).token.value.toString();
        }
    }
    
    public static Object deepCopy(Object object) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStrm = new ObjectOutputStream(outputStream);
            outputStrm.writeObject(object);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            ObjectInputStream objInputStream = new ObjectInputStream(inputStream);
            return objInputStream.readObject();
        }
        catch (Exception e) {
			e.printStackTrace();
			return null;
        }
    }

    public static HashMap<Integer, SymbolValue> getStackFrame(int id) {

        HashMap<Integer, SymbolValue> table = symbolTables.get(id);

        HashMap<Integer, SymbolValue> clonedTable = new HashMap<>();

        for (Map.Entry<Integer, SymbolValue> mapElement : table.entrySet()) { 

            clonedTable.put(mapElement.getKey(), (SymbolValue) deepCopy(mapElement.getValue()));

        }

        return clonedTable;

    }

    public static void eval_Function(Node node) throws Exception {

        ArrayList<Node> children = node.getChilren();

        updateCurrFunc((int) children.get(0).value);

        // Set values of params in table

        ArrayList<Number> passedParams = new ArrayList<>();

        if (children.size() > 1) {

            if (!children.get(1).value.equals("exprseq")) {
                Eval(children.get(1));
                passedParams.add(getNodeValue(children.get(1)));
            } else if (children.get(1).value.equals("exprseq")) {
                Node curr = children.get(1);
                while (curr.type == null) {
                    Eval(curr.getChilren().get(0));
                    passedParams.add(getNodeValue(curr.getChilren().get(0)));
                    curr = curr.getChilren().get(1);
                }
    
                Eval(curr);
                passedParams.add(getNodeValue(curr));
            }
        }

        // Push function code to stack
        Node functionRoot = functionParseTrees.get((int) children.get(0).value);

        if (functionRoot == null) {
            throw new RuntimeException(currFName + " is not a function");
        }

        executionStack.push(getStackFrame((int) children.get(0).value));

        returnStack.push(new ReturnVal());

        // Can be up to length of 4 in the order (Type, params, decls, stateSeq)
        ArrayList<Node> functionChildren = functionRoot.getChilren();

        Node returnType = null;

        int i = 0;

        if (functionChildren.get(i).type == NODE_TYPE.TYPE) {
            returnType = functionChildren.get(i);
            i++;
        }

        if (functionChildren.get(i).type == NODE_TYPE.PARAMS) {
            eval_params(functionChildren.get(i), passedParams, 0);
            i++;
        }

        for (;i < functionChildren.size(); i++) {
            Eval(functionChildren.get(i));
        }

        if (hasReturn()) {
            
            if ((returnType.value.equals("INT") && returnStack.peek().val instanceof Integer)
                || returnType.value.equals("DOUBLE")) {

                    node.computedValue = returnStack.peek().val;
            }

        }

        updateCurrFunc(MAIN_SYMBOL_TABLE_ID);
        returnStack.pop();
        executionStack.pop();

    }

    public static void eval_params(Node node, ArrayList<Number> passedParams, int i) throws Exception {

        ArrayList<Node> children = node.getChilren();

        Node type = children.get(0);
        Node id = children.get(1);

        if (i < 0 || i >= passedParams.size()) {
            throw new RuntimeException("To few Params passed to function " + currFName);
        }

        Number value = passedParams.get(i);
        if ((value instanceof Integer && type.value.equals("INT")) 
            || (type.value.equals("DOUBLE"))) {
            initIDValue((int) id.value);
            setIDValue((int) id.value, value);
        } else {
            throw new RuntimeException(node.value + " Type Error");
        }
        
        if (children.size() > 2 && children.get(2).type == NODE_TYPE.PARAMS) {
            eval_params(children.get(2), passedParams, i + 1);
        } else if (i < passedParams.size() - 1) {
            throw new RuntimeException("To Many Params passed to function " + currFName);
        }

    }

    public static void eval_op(Node node) throws Exception {

        if (node.value instanceof String) {

            String val = (String) node.value;

            ArrayList<Node> children = node.getChilren();

            Eval(children.get(0));
            Eval(children.get(1));

            if (hasReturn()) {
                return;
            }

            Number val1 = getNodeValue(children.get(0));
            Number val2 = getNodeValue(children.get(1));

            switch (val) {

                case "+":

                    if ((val1 instanceof Integer) && (val2 instanceof Integer)) {
                        int val3 = val1.intValue() + val2.intValue();
                        node.setCValue(val3);
                    } else {
                        double val3 = val1.doubleValue() + val2.doubleValue();
                        node.setCValue(val3);
                    }
                    break;

                case "-":
                    if ((val1 instanceof Integer) && (val2 instanceof Integer)) {
                        int val3 = val1.intValue() - val2.intValue();
                        node.setCValue(val3);
                    } else {
                        double val3 = val1.doubleValue() - val2.doubleValue();
                        node.setCValue(val3);
                    }
                    break;

                case "/":
                    if ((val1 instanceof Integer) && (val2 instanceof Integer)) {
                        int val3 = val1.intValue() / val2.intValue();
                        node.setCValue(val3);
                    } else {
                        double val3 = val1.doubleValue() / val2.doubleValue();
                        node.setCValue(val3);
                    }
                    break;
                
                case "%":
                    if ((val1 instanceof Integer) && (val2 instanceof Integer)) {
                        int val3 = val1.intValue() % val2.intValue();
                        node.setCValue(val3);
                    } else {
                        double val3 = val1.doubleValue() % val2.doubleValue();
                        node.setCValue(val3);
                    }
                    break;
                
                case "*":
                    if ((val1 instanceof Integer) && (val2 instanceof Integer)) {
                        int val3 = val1.intValue() * val2.intValue();
                        node.setCValue(val3);
                    } else {
                        double val3 = val1.doubleValue() * val2.doubleValue();
                        node.setCValue(val3);
                    }
                    break;

            }

        }

    }

    public static boolean eval_bool(Node node) throws Exception {

        if (hasReturn()) {
            return false;
        }

        if (node.value instanceof String) {

            String val = (String) node.value;
            ArrayList<Node> children = node.getChilren();

            if (val.equals("or") || val.equals("and") || val.equals("not")) {

                boolean val1 = eval_bool(children.get(0));
                boolean val2 = false;
                if (!val.equals("not")) {
                    val2 = eval_bool(children.get(1));
                }

                switch (val) {

                    case "or":
                        return val1 || val2;
                    case "and":
                        return val1 && val2;
                    case "not":
                        return !val1;
                }

            } else {

                Eval(children.get(0));
                Eval(children.get(0));

                Number val1 = getNodeValue(children.get(0));
                Number val2 = getNodeValue(children.get(1));

                switch (val) {

                    case "<":
                        return val1.doubleValue() < val2.doubleValue();
                    case ">":
                        return val1.doubleValue() > val2.doubleValue();
                    case "==":
                        return val1.doubleValue() == val2.doubleValue();
                    case "<=":
                        return val1.doubleValue() <= val2.doubleValue();
                    case ">=":
                        return val1.doubleValue() <= val2.doubleValue();
                    case "<>":
                        return val1.doubleValue() != val2.doubleValue();
                }
            }
        }

        return false;
    }

    public static void eval_keyword(Node node) throws Exception {

        if (hasReturn()) {
            return;
        }

        if (node.value instanceof String) {

            String val = (String) node.value;
            ArrayList<Node> children = node.getChilren();

            switch (val) {

                case "while":

                    while (eval_bool(children.get(0))) {
                        Eval(children.get(1));
                    }
                    break;

                case "if":
                    
                    boolean evalBool = eval_bool(children.get(0));
                    if (evalBool) {
                        Eval(children.get(1));
                    }
                    
                    // else
                    if (children.size() > 2 && children.get(2).value == "else") {

                        if (!evalBool) {

                            ArrayList<Node> elseChildren = children.get(2).getChilren();

                            Eval(elseChildren.get(0));
                        }
                    }

                    break;
                case "print":
                    Eval(children.get(0));
                    System.out.println(getNodeValue(children.get(0)));
                    break;

                case "return":
                    Eval(children.get(0));

                    returnStack.peek().val = getNodeValue(children.get(0));

                    break;


            }
        }

    }


}