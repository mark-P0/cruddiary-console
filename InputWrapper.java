import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

class InputWrapper {
    Scanner SCANNER_NATIVE = new Scanner(System.in);

    <DynamicType> DynamicType prompt(String message, Class<?> class_) {
        return prompt(this.SCANNER_NATIVE, message, class_);
    }

    @SuppressWarnings("unchecked")
    static <DynamicType> DynamicType prompt(Scanner scanner, String message, Class<?> class_) {
        DynamicType returnVar = null;
        Callable<DynamicType> lambdaScannerNext;

        boolean shouldPrompt = true;
        while (shouldPrompt) {
            try {
                // The suppressed warnings are for the following lines of returnVar.
                if (class_ == String.class){
                    lambdaScannerNext = () -> (DynamicType) (String) scanner.nextLine();
                }
                // else if ((class_ == Integer.class) || (class_ == int.class)){
                else if (List.of(Integer.class, int.class).contains(class_)){
                    lambdaScannerNext = () -> (DynamicType) (Integer) scanner.nextInt();
                }
                else if (class_ == Double.class){
                    lambdaScannerNext = () -> (DynamicType) (Double) scanner.nextDouble();
                }
                else if (class_ == Float.class){
                    lambdaScannerNext = () -> (DynamicType) (Float) scanner.nextFloat();
                }
                else {
                    throw new Exception(String.format(
                        "'%s' is not supported! Please review.", class_
                    ));
                }

                System.out.print(message);
                returnVar = lambdaScannerNext.call();

                if (class_ != String.class) {
                    scanner.nextLine();
                }

                shouldPrompt = false;
            }
            catch (InputMismatchException error) {
                // System.out.println(String.format(
                //     "Something wrong happened! Please try again.\n" +
                //     "[%s]\n", error
                // ));
                System.out.printf(
                    "Something wrong happened! Please try again.\n" +
                    "[%s]\n\n", error
                );

                scanner.next();
            }
            catch (Exception error) {
                System.out.println(error);
                System.exit(1);
            }
        }

        return returnVar;
    }
}