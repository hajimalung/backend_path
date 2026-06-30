public class DataTypes {
    public static void main(String[] args) {
        // --- Primitive types (8 total, stored on the stack) ---
        int     age     = 25;
        long    pop     = 8_000_000_000L;  // L suffix for long literals
        double  price   = 19.99;
        float   temp    = 36.6f;           // f suffix for float
        boolean active  = true;
        char    grade   = 'A';             // single quotes for char
        byte    b       = 127;
        short   s       = 32000;
        System.out.println(b);
        System.out.println(s);
        System.out.println(grade);
        System.out.println(active);
        System.out.println(price);
        System.out.println(pop);
        System.out.println(temp);
        System.out.println(age);
        System.out.println("---------------------------");

// --- Reference types (objects, stored on heap) ---
        String  name    = "Alice";         // double quotes for String
        int[]   nums    = {1, 2, 3};
        System.out.println(name);
        System.out.println(nums);
        System.out.println("---------------------------");

// Autoboxing: Java wraps primitives into objects automatically
        Integer boxed   = 42;   // int -> Integer (object wrapper)
        System.out.println(boxed);
        int     prim    = boxed; // Integer -> int (unboxing)
        System.out.println(prim);
        System.out.println("---------------------------");

        String ar = "Hello";
        String br = "Hello";
        System.out.println("ar == br :"+ar == br);
    }
}