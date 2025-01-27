public class FizzBuzzer {

    public String convert(int number) {

        if (number <= 0 || number > 1000)

            return "N/A";
            
        if (number % 15 == 0)

            return "FizzBuzz";

        if (number % 3 == 0)

            return "Fizz";

        if (number % 5 == 0)

            return "Buzz";

        return Integer.toString(number);

    }

    public int doMath(int a, int b) {
        return a * b - 2;
    }

}