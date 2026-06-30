public class FizzBuzz {
    public static void main(String[] args) {
        String[] fizzBuzzWords = new String[30];
        int count = 0;
        System.out.println("initial length: " + fizzBuzzWords.length);
        for(int i=1;i<=30;i++){
            String output = "";
            if(i%3==0) output += "Fizz";
            if(i%5==0) output += "Buzz";
            if(!output.isEmpty()) count++;
            if(output.isEmpty()) output = Integer.toString(i);
            fizzBuzzWords[i - 1] = output;
            System.out.println(output);
        }
        System.out.println("final length: " + fizzBuzzWords.length);
        System.out.println(count);
    }
}