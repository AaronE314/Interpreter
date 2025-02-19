import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Parser {

    private static int nextID = 0;

    public static HashMap<Integer, SymbolValue> symbolTable = new HashMap<>();
    public static HashMap<String, Integer> symbolNameToID = new HashMap<>();

    public static LinkedHashMap<Integer, HashMap<Integer, SymbolValue>> symbolTables = new LinkedHashMap<>();
    public static HashMap<Integer, HashMap<String, Integer>> symbolNameToIDTables = new HashMap<>();

    public static final String KEYWORDS[] = { "while", "do", "od", "def", "fed", "if", "then", "else", "fi", "print", "return", "or", "and"};
    public static final HashSet<String> TYPES = new HashSet<>(
        Arrays.asList(new String[] {"int", "double"}));

    public static boolean readNextChar = true;
    public static char prevChr = 0;

    public static Scanner sin;

    public static final int MAIN_SYMBOL_TABLE_ID = 0;
    public static int currentSymbolTable = MAIN_SYMBOL_TABLE_ID;

    public static String formatedCode = "";
    public static int indentNum = 0;

    public static int lineCount = 1;
    public static int charCount = 1;

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

    public static class Token<T> {

        private T value;
        private TOKENS token;
        private String idType;
        private int constId;
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

    public static class SymbolValue {

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
        parse();

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

    public static String formatCenter(String str, int width) {

        int padSize = width - str.length();
        int padStart = str.length() + padSize / 2;

        str = String.format("%" + padStart + "s", str);
        return String.format("%-" + width + "s", str);
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
                        + String.format("%-10s%10s", sv.getToken().getValue().toString()
                        , (sv.isGlobal() ? TOKEN_TYPE.GLOBAL + ", " : "") + entry2.getValue().getType() + ((sv.getToken().getIdType() != null) ? ", " + sv.getToken().getIdType() : "")) + "|"
                    );
                }

            });

            System.out.println("---------------------------");


        });

    }

    public static void parse() {

        try {
            if (program()) {

                System.out.println(formatedCode);

                printSymbolTables();

            } else {
                System.out.println(formatedCode);
                System.err.println("Error Invalid program format");
            }
        } catch (Exception e) {

            System.out.println(formatedCode);
            System.err.println(e.getMessage());

        }

    }

    public static boolean match(String t) throws Exception {

        if (lookahead.getValueString().equals(t)) {

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

        throw new Exception("Error at: " + lineCount + ":" + charCount + ": Expected " + expected + " but received " + received);

    }

    public static boolean program() throws Exception {

        boolean correctFormat = fdecls();

        if (correctFormat) {
            correctFormat = declarations();

            if (correctFormat) {
                correctFormat = statement_seq();

                if (correctFormat) {
                    match(TOKENS.DOT.getSymbol());
                    return true;
                } else {
                    error("id, if, while, print or return", lookahead.getValueString());
                }
            } else {
                error("int or double", lookahead.getValueString());
            }
        } else {
            error("def", lookahead.getValueString());
        }

        return true;

    }

    public static boolean fdecls() throws Exception {

        if (fdec()) {
            match(TOKENS.SEMICOLON.getSymbol());
            newLineInFormat();
            return fdecls();
        } else {
            return true;
        }

    }

    public static boolean fdec() throws Exception {

        if (lookahead.getValueString().equals("def")) {

            int id = -1;

            match("def");
            type();
            id = fname();
            newSymbolTable(id);

            match("(");
            params();
            match(")");

            changeIndent(1);
            newLineInFormat();

            declarations();
            statement_seq();

            changeIndent(-1);
            
            currentSymbolTable = MAIN_SYMBOL_TABLE_ID;
            match("fed");

            return true;

        }

        return false;

    }

    public static boolean params() throws Exception {

        String t = type();
        if (t != null) {
            var(t);
            params_rest();
            return true;
        } else {
            return true;
        }

    }

    public static boolean params_rest() throws Exception {

        if (lookahead.getValueString().equals(",")) {

            match(",");
            params();
            return true;
        } else {
            return true;
        }

    }

    public static int fname() throws Exception {

        return id(null);
    }

    public static boolean declarations() throws Exception {

        if (decl()) {
            match(TOKENS.SEMICOLON.getSymbol());

            newLineInFormat();
            return declarations();
        } else {
            return true;
        }

    }

    public static boolean decl() throws Exception {

        String t = type();

        return t != null && varlist(t);

    }

    public static String type() throws Exception {
        
        if (lookahead.getValueString().equals("int")) {
            match("int");
            return "INT";
        } else if (lookahead.getValueString().equals("double")){
            match("double");
            return "DOUBLE";
        }

        return null;

    }

    public static boolean varlist(String t) throws Exception {

        var(t);
        varlist_rest(t);

        return true;
    }

    public static boolean varlist_rest(String t) throws Exception {

        if (lookahead.getValueString().equals(",")) {
            match(",");
            varlist(t);
            return true;
        } else {
            return true;
        }
    }

    public static boolean statement_seq() throws Exception {

        return statement() && statement_seq_rest();
    }

    public static boolean statement_seq_rest() throws Exception {

        if (lookahead.getValueString().equals(";")) {
            match(";");
            newLineInFormat();
            statement_seq();
            return true;
        } else {
            newLineInFormat();
            return true;
        }
    }

    public static boolean statement() throws Exception {

        if (var(null)) {
            match("=");
            expr();
            return true;
        } else if (lookahead.getValueString().equals("if")) {
            match("if");
            bexpr();
            match("then");

            changeIndent(1);
            newLineInFormat();

            statement_seq();
            changeIndent(-1);
            if_rest();
            return true;
        } else if (lookahead.getValueString().equals("while")) {
            match("while");
            bexpr();
            match("do");

            changeIndent(1);
            newLineInFormat();

            statement_seq();

            changeIndent(-1);

            match("od");
            return true;
        } else if (lookahead.getValueString().equals("print")) {
            match("print");
            expr();
            return true;
        } else if (lookahead.getValueString().equals("return")) {
            match("return");
            expr();
            return true;
        }

        return true;
    }

    public static boolean if_rest() throws Exception {

        if (lookahead.getValueString().equals("fi")) {

            match("fi");

            return true;
        } else if (lookahead.getValueString().equals("else")) {
            match("else");

            changeIndent(1);
            newLineInFormat();

            statement_seq();

            changeIndent(-1);

            match("fi");
            return true;
        }

        error("fi or else", lookahead.getValueString());
        return false;
    }

    public static boolean expr() throws Exception {

        return term() && expr_rest();

    }

    public static boolean expr_rest() throws Exception {

        if (lookahead.getValueString().equals("+")) {

            match("+");
            return term() && expr_rest();

        } else if (lookahead.getValueString().equals("-")) {
            match("-");
            return term() && expr_rest();
        }

        return true;
    }

    public static boolean term() throws Exception {

        return factor() && term_rest();

    }

    public static boolean term_rest() throws Exception {

        if (lookahead.getValueString().equals("*")) {

            match("*");
            return factor() && term_rest();

        } else if (lookahead.getValueString().equals("/")) {

            match("/");
            return factor() && term_rest();

        } else if (lookahead.getValueString().equals("%")) {

            match("%");
            return factor() && term_rest();
        }

        return true;
    }

    public static boolean factor() throws Exception {

        if (id(null) != -1) {

            return factor_rest();
        }
        if (number() != null) {
            return true;
        } else if (lookahead.getValueString().equals("(")) {
            match("(");
            expr();
            match(")");
            return true;
        } else if (lookahead.getValueString().equals("-")) {
            match("-");
            Token<?> val = number();
            if (val != null) {
                Token<?> val2 = symbolTables.get(currentSymbolTable).get(val.getConstId()).getToken();
                val2.flipSign();
            }
            return true;
        }

        error("id, number or -", lookahead.getValueString());
        return false;
    }

    public static boolean factor_rest() throws Exception {
        
        if (lookahead.getValueString().equals("(")) {
            match("(");
            exprseq();
            match(")");
            return true;
        } else if (var_rest()) {
            return true;
        }

        return false;
    }

    public static boolean exprseq() throws Exception {

        return expr() && exprseq_rest();

    }

    public static boolean exprseq_rest() throws Exception {

        if (lookahead.getValueString().equals(",")) {
            match(",");
            return exprseq();
        }

        return true;
    }

    public static boolean bexpr() throws Exception {
        
        return bterm() && bexpr_rest();

    }

    public static boolean bexpr_rest() throws Exception {

        if (lookahead.getValueString().equals("or")) {
            match("or");
            return bterm() && bexpr_rest();
        }

        return true;
    }

    public static boolean bterm() throws Exception {

        return bfactor() && bterm_rest();
    }

    public static boolean bterm_rest() throws Exception {

        if (lookahead.getValueString().equals("and")) {
            match("and");
            return bfactor() && bterm_rest();
        }

        return true;
    }

    public static boolean bfactor() throws Exception {

        if (lookahead.getValueString().equals("(")) {
            match("(");
            bfactor_rest();
            match(")");
            return true;
        } else if (lookahead.getValueString().equals("not")) {
            match("not");
            bfactor();
            return true;
        }

        return false;
    }

    public static boolean bfactor_rest() throws Exception {

        if (bexpr()) {
            return true;
        } else if (expr()) {
            comp();
            expr();
            return true;
        }

        error("id, number, ( or not", lookahead.getValueString());
        return false;
    }

    public static boolean comp() throws Exception {


        switch (lookahead.getValueString()) {

            case "<":
                match("<");
                return true;
            case ">":
                match(">");
                return true;
            case "==":
                match("==");
                return true;
            case "<=":
                match("<=");
                return true;
            case ">=":
                match(">=");
                return true;
                case "<>":
                match("<>");
                return true;
            default:
                error("<, >, ==, <=, >= or <>", lookahead.getValueString());
                return false;
        }

    }

    public static boolean var(String type) throws Exception {

        if (lookahead.getToken() == TOKENS.ID) {
            if (lookahead.getIdType() == null && type != null) {
                symbolTables.get(currentSymbolTable).get(lookahead.getValue()).getToken().setIdType(type);
            }
            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                error("id", lookahead.getValueString());
            }

            return var_rest();
        }

        return false;
    }

    public static boolean var_rest() throws Exception{

        if (lookahead.getValueString().equals("[")) {
            match("[");
            expr();
            match("]");
        }

        return true;
    }

    public static int id(String type) throws Exception {

        if (lookahead.getToken() == TOKENS.ID) {
            if (lookahead.getIdType() == null && type != null) {
                symbolTables.get(currentSymbolTable).get(lookahead.getValue()).getToken().setIdType(type);
            }
            Object value = lookahead.getValue();
            int symbolIndex = (int) value;
            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                error("id", lookahead.getValueString());
            }
            return symbolIndex;
        }

        return -1;
    }

    public static Token<?> number() throws Exception {

        if (lookahead.getToken() == TOKENS.DOUBLE || lookahead.getToken() == TOKENS.INTEGER) {

            Token<?> num = lookahead;

            try {
                addToFormat(lookahead);
                lookahead = getNextToken();
            } catch(Exception e) {
                error("number", lookahead.getValueString());
            }
            return num;
        }

        return null;
    }
}