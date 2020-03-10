

public class test {


    public static void main(String[] args) {
        

        try {
            recurse(0);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void recurse(int i) throws Exception {

        System.out.println(i);

        recurse(i + 1);

    }
}