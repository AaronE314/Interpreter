import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class LexAn {

    private static int nextID = 0;

    public static final String HTML_START = "<!DOCTYPE html>\n"
    + "<html lang=\"en\">\n"
    + "<head>\n"
    + "\t<meta charset=\"UTF-8\">\n"
    + "\t<title>Assignment1</title>\n"
    + "\t<style>\n"
    + "\tbody {\n"
    + "\t\tbackground-color: #062E32;\n"
    + "\t}\n"
    + "\t</style>"
    + "</head>\n"
    + "<body>\n"
    + "<p>";

    public static final String HTML_END = "</p>\n</body>\n</html>";

    public static HashMap<Integer, SymbolValue> symbolTable = new HashMap<>();
    public static HashMap<String, Integer> symbolNameToID = new HashMap<>();

    public static final String KEYWORDS[] = { "while", "do", "od", "def", "fed", "if", "then", "else", "fi", "print", "return"};
    public static final HashSet<String> TYPES = new HashSet<>(
        Arrays.asList(new String[] {"int", "double"}));

    public static boolean readNextChar = true;
    public static char prevChr = 0;

    public static enum TOKEN_TYPE {
        KEYWORD, IDENTIFIER;
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
        private String color;
        private String symbol;

        private TOKENS(String token, String color, String symbol) {
            this.token = token;
            this.color = color;
            this.symbol = symbol;
        }

        private TOKENS(String token, String color) {
            this.token = token;
            this.color = color;
            this.symbol = "";
        }

        private String getSymbol() {
            return symbol;
        }

        private String getColor() {
            return color;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    public static class Token<T> {

        private T value;
        private TOKENS token;

        public Token(T value, TOKENS token) {
            this.value = value;
            this.token = token;
        }

        public T getValue() {
            return value;
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
        private String name;
        private TOKEN_TYPE type;
        
        public SymbolValue(int id, String name, TOKEN_TYPE type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public int getId() { return this.id;}
        public String getName() { return this.name;}
        public TOKEN_TYPE getType() { return this.type;}

        @Override
        public String toString() {
            return "(" + id + ", " + name + ", " + type + ")";
        }
    }

    public static void main(String[] args) {
        
        Scanner sin = new Scanner(System.in);
        
        sin.useDelimiter("");

        ArrayList<Token<?>> tokens = new ArrayList<>();

        installKeywords();

        while (sin.hasNext()) {
            tokens.add(getNextToken(sin));
        }

        System.out.println(prettyDraw(tokens));

        sin.close();

    }

    public static String colouredTag(String colour, String contents) {
        return "<font color=" + colour + ">" + contents + "</font>";
    }

    public static String prettyDraw(ArrayList<Token<?>> tokens) {

        String html = HTML_START;

        String comment = "<!--";
        String colouredCode = "";

        for (Token<?> token : tokens) {

            if (token.getToken() == TOKENS.FORMATING) {

                if (token.getValueString().equals(" ")) {
                    colouredCode += " ";
                } else if (token.getValueString().equals("\t")) {
                    colouredCode += "&emsp;";
                } else {
                    colouredCode += "<br>";
                }

            } else if (token.getToken() == TOKENS.ID){
                comment += token.toString();
                colouredCode += colouredTag(token.getToken().getColor(), symbolTable.get(token.getValue()).name);
            } else {
                comment += token.toString();
                colouredCode += colouredTag(token.getToken().getColor(), token.getValueString());
            }

            colouredCode += "\n";
            
        }

        comment += "-->";

        html += comment + "\n" + colouredCode + "\n" + HTML_END;

        return html;

    }

    public static void installKeywords() {

        for (String keyword: KEYWORDS) {
            installSymbol(keyword, TOKEN_TYPE.KEYWORD);
        }
    }

    public static int installSymbol(String name, TOKEN_TYPE type) {
        
        int id = -1;

        if (symbolNameToID.containsKey(name)) {
            id = symbolNameToID.get(name);
        } else {
            id = nextID++;

            symbolNameToID.put(name, id);
            symbolTable.put(id, new SymbolValue(id, name, type));
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

    public static Token<?> getNextToken(Scanner sin) {

        char chr = (readNextChar) ? sin.next().charAt(0) : prevChr;
        readNextChar = true;

        if (chr >= '0' && chr <= '9') {

            return getNum(chr, sin);

        } else if (chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z') {

            return getSymbol(chr, sin);
        } else {

            switch(chr) {

                case '<':
                case '>':
                case '=':
                    String operator = "" + chr;
                    chr = sin.next().charAt(0);

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
                case '\r':
                case '\n':
                case '\t':
                case ' ':
                    return new Token<String>("" + chr, TOKENS.FORMATING);
                default:
                    return new Token<Void>(null, TOKENS.ERROR);

            }

        }

    }

    public static Token<?> getNum(char chr, Scanner sin) {

        int intValue = chr - '0';
        double doubleValue = 0.0;
        int exponent = 0;
        double pow = 10;

        boolean typeDouble = false;

        chr = sin.next().charAt(0);

        while (sin.hasNext() && chr >= '0' && chr <= '9') {

            intValue = (int) pow * intValue + chr - '0';

            chr = sin.next().charAt(0);

        }

        if (chr == '.') {

            typeDouble = true;

            pow = 0.1;

            if (sin.hasNext()) {
                
                chr = sin.next().charAt(0);
                while (chr >= '0' && chr <= '9') {

                    doubleValue += pow * (chr - '0');
                    
                    if (!sin.hasNext()) {
                        break;
                    }
                    chr = sin.next().charAt(0);
                    pow /= 10;
                    
                }

            }

        }
        
        if (chr == 'e') {

            pow = 10;

            chr = sin.next().charAt(0);

            int sign = 1;
            if (chr == '-') {
                sign = -1;
                typeDouble = true;
                chr = sin.next().charAt(0);
            }


            while (sin.hasNext() && chr >= '0' && chr <= '9') {

                exponent = (int) pow * exponent + chr - '0';
    
                chr = sin.next().charAt(0);
    
            }

            exponent *= sign;

        }

        boolean error = ((chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z'));

        readNextChar = error;
        prevChr = chr;

        if (!typeDouble) {

            int finalValue = intValue * (int) Math.pow(10, exponent);

            return (!error) ? new Token<Integer>(finalValue, TOKENS.INTEGER)
                            : new Token<String>(String.valueOf(finalValue) + chr, TOKENS.ERROR);
        } else {

            double finalValue = (intValue + doubleValue) * Math.pow(10, exponent);

            return (!error) ? new Token<Double>(finalValue, TOKENS.DOUBLE)
                            : new Token<String>(String.valueOf(finalValue) + chr, TOKENS.ERROR);

        }

    }

    public static Token<?> getSymbol(char chr, Scanner sin) {

        String name = "";

        do {

            name += chr;

            chr = sin.next().charAt(0);

        } while (chr >= '0' && chr <= '9' || chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z');

        readNextChar = false;
        prevChr = chr;

        if (TYPES.contains(name)) {

            return new Token<String>(name, TOKENS.TYPE);
        } else if (is_keyword(name)) {

            return new Token<String>(name, TOKENS.KEYWORD);
        } else {

            return new Token<Integer>(installSymbol(name, TOKEN_TYPE.IDENTIFIER), TOKENS.ID);
        }

    }

}