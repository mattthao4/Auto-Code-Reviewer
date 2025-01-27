package com.reviewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;

@Tag("skip")
public abstract class SetupTest {
    
    // This will hold all the test files that will be run.
    protected ArrayList<TestGroup> testGroups = new ArrayList<>();  // Submission groups
    protected String[] testFiles;   // Java files to compile
    protected String assignmentName; 
    protected String parentFolderName;

    public SetupTest(String parentFolderName, String assignmentName, String[] testFiles) throws IOException {
        this.testFiles = testFiles;
        this.assignmentName = assignmentName;
        this.parentFolderName = parentFolderName;
        this.testGroups = new ArrayList<>();
        
        // Load all the tests into a list for this test file to access
        Path testPath, newTestPath, groupPath;
        String defaultPath = Paths.get("").toAbsolutePath().getParent().toString().replace("\\","/");
        testPath = Paths.get(defaultPath+"/"+this.parentFolderName+"/Submissions/"); 
        newTestPath = Paths.get(defaultPath+"/"+this.parentFolderName+"/src/test/java/com/"+this.assignmentName+"/Tests/"); 
        // Cleaning up previous tests
        if(Files.exists(newTestPath)){
            Files.walk(newTestPath)
                .sorted((path1, path2) -> path2.compareTo(path1)) // Sort to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path); // Delete each file or directory
                    } catch (IOException e) {
                    }
                });
        }
        Files.createDirectory(newTestPath);
        
        // Check all the test directories in the main folder and 
        // verify whether the source java file is there in the test directory
        TestGroup testGroup;
        String fileName;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testPath)) {
            for (Path file : stream) {
                if(Files.isDirectory(file)){
                    fileName = file.getFileName().toString();
                    testGroup = new TestGroup(fileName.replace(" ","_"));
                    
                    // Add individual folders for the groups
                    groupPath = Paths.get(defaultPath+"/"+this.parentFolderName+"/src/test/java/com/"+this.assignmentName+"/Tests/"+testGroup.getName());

                    if(Files.exists(groupPath)){
                        Files.walk(groupPath)
                            .sorted((path1, path2) -> path2.compareTo(path1)) // Sort to delete files before directories
                            .forEach(path -> {
                                try {
                                    Files.delete(path); // Delete each file or directory
                                } catch (IOException e) {
                                }
                            });
                    }
                    Files.createDirectory(groupPath);

                    for(String testFile : this.testFiles){
                        addTest(testGroup,fileName,testFile);
                    }
                    this.testGroups.add(testGroup);
                }
            }
        } catch (IOException | DirectoryIteratorException e) {
        }
    }

    /**
     * addTest
     * 
     * Adds SetupTest.testFiles to the Tests directory
     * Verifies the existence of test files
     * 
     * @param fileName - name of the file
     * @throws IOException - throws if there is an error with obtaining the file
     */
    
    private void addTest(TestGroup testGroup, String fileName, String problem) throws IOException{
        // Get the paths of our source and target
        Path targetPath, sourcePath;
        String defaultPath = Paths.get("").toAbsolutePath().getParent().toString().replace("\\","/");
        // Get the expected path of the source file
        sourcePath = Paths.get(defaultPath+"/"+this.parentFolderName+"/Submissions/"+fileName+"/"+problem+".java");

        // Get the path of the file to transfer
        targetPath = Paths.get(defaultPath+"/"+this.parentFolderName+"/src/test/java/com/"+this.assignmentName+"/Tests/"+testGroup.getName()+"/"+problem+".java");

        // Verify if the source file exists
        if(Files.exists(sourcePath)){
            // Add the source file to the list of tests
            testGroup.addFile(problem);
            // Clone the source file into the path of the file to transfer
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Modify package argument to the package of this test file
            addPackage(targetPath, "com."+this.assignmentName+".Tests."+testGroup.getName());
        }
    }
    
    // Add @TestFactory to this
    public abstract Stream<DynamicNode> graderTest();
    
    /**
     * caseCheck
     * 
     * This method creates a thread for each test case, testing it on its own files and classes
     * 
     * @param testGroup
     * @param fileName
     * @param methodName
     * @param params
     * @param paramTypes
     * @return
     */
    protected Future<Object> caseCheck(TestGroup testGroup, String fileName, String methodName, Object[] params, Class<?>... paramTypes){
        // Try invoking reflection to pull the methods from the test cases
        // Using Future to execute the test cases as threads and check for runtime complications
        String className = "com." + assignmentName + ".Tests." + testGroup.getName()+"."+fileName;
        ExecutorService executor = Executors.newSingleThreadExecutor();  // Single-thread executor for timeouts    
        Future<Object> codeChecker = executor.submit(() -> {
            try {
                Class<?> testClass = Class.forName(className);
                Object testInstance = testClass.getDeclaredConstructor().newInstance();
                Method method = testClass.getMethod(methodName,paramTypes);
                return method.invoke(testInstance,params);
            } catch (InvocationTargetException e) {
                // If an InvocationTargetException is thrown, rethrow its cause
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof Exception) {
                        throw (Exception) cause;  // Cast to Exception
                    } else {
                        throw new RuntimeException(cause);  // Wrap in RuntimeException if not an Exception
                    }
                } else {
                    throw e;  // No cause, rethrow the original InvocationTargetException
                }
            } catch (Exception e) {
                // Rethrow any other exceptions as-is
                throw e;
            }
        });
        return codeChecker;
    }

    /**
     * caseCheck
     * 
     * This method creates a thread for each test case, testing it on its own files, classes, and constructors
     * 
     * @param testGroup
     * @param fileName
     * @param methodName
     * @param params
     * @param paramTypes
     * @return
     */

    protected Future<Object> caseCheck(TestGroup testGroup, String fileName, String methodName, Object[] constructorParams, Class<?>[] constructorParamsTypes, Object[] methodParams, Class<?>... methodParamTypes) {
        // Try invoking reflection to pull the methods from the test cases
        // Using Future to execute the test cases as threads and check for runtime complications
        String className = "com." + assignmentName + ".Tests." + testGroup.getName() + "." + fileName;
        ExecutorService executor = Executors.newSingleThreadExecutor();  // Single-thread executor for timeouts

        Future<Object> codeChecker = executor.submit(() -> {
            try {
                Class<?> testClass = Class.forName(className);
                Object testInstance;
                if (constructorParams != null && constructorParams != null) {
                    Constructor<?> constructor = testClass.getDeclaredConstructor(constructorParamsTypes);  // Pass constructor parameter types
                    testInstance = constructor.newInstance(constructorParams);  // Pass constructor arguments
                } else {
                    testInstance = testClass.getDeclaredConstructor().newInstance();  // No-arg constructor
                }
                Method method = testClass.getMethod(methodName, methodParamTypes);  // Specify method parameter types
                return method.invoke(testInstance, (Object[]) methodParams);  // Pass method arguments
            } catch (InvocationTargetException e) {
                // If an InvocationTargetException is thrown, rethrow its cause
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof Exception) {
                        throw (Exception) cause;  // Cast to Exception
                    } else {
                        throw new RuntimeException(cause);  // Wrap in RuntimeException if not an Exception
                    }
                } else {
                    throw e;  // No cause, rethrow the original InvocationTargetException
                }
            } catch (Exception e) {
                // Rethrow any other exceptions as-is
                throw e;
            }
        });
        return codeChecker;
    }

    /**
     * caseCheck
     * 
     * This method creates a thread for each test case, testing it on its own files, classes, and constructors
     * 
     * @param testGroup
     * @param fileName
     * @param methodName
     * @param params
     * @param paramTypes
     * @return
     */

     protected Future<Object> caseCheck(TestGroup testGroup, String fileName, boolean checkList, String methodName, Object[] constructorParams, Class<?>[] constructorParamsTypes, Object[] methodParams, Class<?>... methodParamTypes) {
        String className = "com." + assignmentName + ".Tests." + testGroup.getName() + "." + fileName;
        ExecutorService executor = Executors.newSingleThreadExecutor();  // Single-thread executor for timeouts
    
        if (checkList) {
            return executor.submit(() -> {
                try {
                    // Reflectively load class, constructor, and method
                    Class<?> testClass = Class.forName(className);
                    Object testInstance;
        
                    if (constructorParams != null && constructorParamsTypes != null) {
                        Constructor<?> constructor = testClass.getDeclaredConstructor(constructorParamsTypes);
                        testInstance = constructor.newInstance(constructorParams);
                    } else {
                        testInstance = testClass.getDeclaredConstructor().newInstance();
                    }
        
                    Method method = testClass.getMethod(methodName, methodParamTypes);
                    Object result = method.invoke(testInstance, methodParams);
        
                    // Check if the result is a List and convert it to an Object array
                    if (result instanceof List<?>) {
                        return ((List<?>) result).toArray();  // Convert List to Object array
                    } else if (result instanceof Object[]) {
                        return (Object[]) result;  // Return as Object array directly if already an array
                    } else if (result instanceof int[]) {
                        return Arrays.stream((int[]) result).boxed().toArray(Integer[]::new);  // Convert int[] to Integer[]
                    } else if (result instanceof boolean[] bs) {
                        Boolean[] boxedArray = new Boolean[bs.length];
                        for (int i = 0; i < bs.length; i++) {
                            boxedArray[i] = bs[i];
                        }
                        return boxedArray;  // Convert boolean[] to Boolean[]
                    } else if (result instanceof char[] cs) {
                        Character[] boxedArray = new Character[cs.length];
                        for (int i = 0; i < cs.length; i++) {
                            boxedArray[i] = cs[i];
                        }
                        return boxedArray;  // Convert char[] to Character[]
                    } else if (result instanceof byte[] bs) {
                        Byte[] boxedArray = new Byte[bs.length];
                        for (int i = 0; i < bs.length; i++) {
                            boxedArray[i] = bs[i];
                        }
                        return boxedArray;  // Convert byte[] to Byte[]
                    } else if (result instanceof long[] ls) {
                        return Arrays.stream(ls).boxed().toArray(Long[]::new);  // Convert long[] to Long[]
                    } else if (result instanceof float[] fs) {
                        Float[] boxedArray = new Float[fs.length];
                        for (int i = 0; i < fs.length; i++) {
                            boxedArray[i] = fs[i];
                        }
                        return boxedArray;  // Convert float[] to Float[]
                    } else if (result instanceof double[] ds) {
                        Double[] boxedArray = new Double[ds.length];
                        for (int i = 0; i < ds.length; i++) {
                            boxedArray[i] = ds[i];
                        }
                        return boxedArray;  // Convert double[] to Double[]
                    } else {
                        return result;  // Return single Object directly if neither array nor list
                    }
        
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;  // Rethrow cause if it's an exception
                    } else {
                        throw new RuntimeException(cause);  // Wrap non-exception causes in RuntimeException
                    }
                } catch (Exception e) {
                    throw e;  // Rethrow other exceptions
                }
            });
        } else {
            return caseCheck(testGroup, fileName, methodName, constructorParams, constructorParamsTypes, methodParams, methodParamTypes);
        }
    }
    
    /**
     * handleTestResults
     * 
     * A handler for the test results that checks outputs and lists
     * 
     * @param compiledSuccess
     * @param expectedResult
     * @param actual
     * @param testCase
     * @param dynamicTests
     */
    protected void handleTestResults(Object[] expectedResult, Future<?> code, String testCase, String testGroup, String methodName, int timeoutTime, List<DynamicNode> dynamicTests, boolean checkOutput, boolean checkList) {
        Exception exceptionCause = null;
        final Object[] actual = new Object[1];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        final String[] capturedOutput = {""};
    
        PrintStream originalOut = System.out;
    
        if (checkOutput) {
            System.setOut(printStream);
        }
    
        try {
            actual[0] = code.get(timeoutTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            code.cancel(true);
            exceptionCause = e;
        } catch (ExecutionException e) {
            code.cancel(true);
            exceptionCause = e.getCause() != null ? (Exception) e.getCause() : e;
        } catch (Exception e) {
            code.cancel(true);
            exceptionCause = e;
        }
    
        if (checkOutput) {
            capturedOutput[0] = outputStream.toString().replaceAll("\\r?\\n", " ");
            System.setOut(originalOut);
        }
    
        if (exceptionCause != null) {
            handleExceptions(exceptionCause, testGroup, methodName, dynamicTests, testCase);
        } else {
            DynamicTest expectedResultTest, actualResultTest;
    
            if (checkOutput) {
                expectedResultTest = DynamicTest.dynamicTest(
                    "Expected Output: " + Arrays.toString(expectedResult),
                    () -> assertEquals(Arrays.toString(expectedResult), Arrays.toString(expectedResult))
                );
                actualResultTest = DynamicTest.dynamicTest(
                    "Actual: " + capturedOutput[0],
                    () -> assertEquals(Arrays.toString(expectedResult), capturedOutput[0])
                );
            } else {
                if (checkList && actual[0] instanceof Object[]) {
                    expectedResultTest = DynamicTest.dynamicTest(
                        "Expected: " + Arrays.toString(expectedResult),
                        () -> assertEquals(true, true)
                    );
                    actualResultTest = DynamicTest.dynamicTest(
                        "Actual: " + Arrays.deepToString((Object[]) actual[0]),
                        () -> assertEquals(Arrays.toString(expectedResult), Arrays.deepToString((Object[]) actual[0]))
                    );
                } else if (checkList && actual[0] instanceof List) {
                    Object[] actualArray = ((List<?>) actual[0]).toArray();
                    expectedResultTest = DynamicTest.dynamicTest(
                        "Expected: " + Arrays.toString(expectedResult),
                        () -> assertEquals(true, true)
                    );
                    actualResultTest = DynamicTest.dynamicTest(
                        "Actual: " + Arrays.toString(actualArray),
                        () -> assertEquals(Arrays.toString(expectedResult), Arrays.toString(actualArray))
                    );
                } else {
                    expectedResultTest = DynamicTest.dynamicTest(
                        "Expected: " + Arrays.toString(expectedResult),
                        () -> assertEquals(true, true)
                    );
    
                    actualResultTest = DynamicTest.dynamicTest(
                        "Actual: " + (actual[0] instanceof Object[] ? Arrays.toString((Object[]) actual[0]) : String.valueOf(actual[0])),
                        () -> assertEquals(Arrays.toString(expectedResult), String.valueOf(actual[0]))
                    );
                }
            }
    
            DynamicContainer testCaseContainer = DynamicContainer.dynamicContainer(
                "Input: " + testCase,
                Stream.of(expectedResultTest, actualResultTest)
            );
    
            dynamicTests.add(testCaseContainer);
        }
    }
    
    
    
    


    /**
     * handleTestResults
     * 
     * A handler for the test results that checks outputs
     * 
     * @param compiledSuccess
     * @param expectedResult
     * @param actual
     * @param testCase
     * @param dynamicTests
     */

    protected void handleTestResults(Object expectedResult, Future<?> code, String testCase, String testGroup, String methodName, int timeoutTime, List<DynamicNode> dynamicTests, boolean checkOutput){
        final Object[] actual = new Object[1];
        Exception exceptionCause = null;
        // Create a ByteArrayOutputStream to capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        String capturedOutput = new String();

        // Save the original System.out to restore later
        PrintStream originalOut = System.out;
        
        // Try running the code and check for possible runtime errors
        // If the code takes too long to compute, fail the test case
        if(checkOutput){
            // Redirect System.out to the ByteArrayOutputStream
            System.setOut(printStream);
        }
        try {
            actual[0] = code.get(timeoutTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            code.cancel(true);
            exceptionCause = e;
        } catch (ExecutionException e) {
            code.cancel(true);
            Throwable cause = e.getCause();
            if(cause != null) {
                exceptionCause = (Exception) cause;
            } else {
                exceptionCause = e;
            }
        } catch (Exception e) {
            code.cancel(true);
            exceptionCause = e;
        }

        if(checkOutput){
            // Convert the captured output to a String
            capturedOutput = outputStream.toString().replaceAll("\\r?\\n", " ");
    
            // Reset System.out to its original state
            System.setOut(originalOut);
        }


        // Successful compilation leads to checking actual results
        if(exceptionCause !=null){
            handleExceptions(exceptionCause, testGroup, methodName, dynamicTests,testCase);
        } else {
            DynamicTest expectedResultTest, actualResultTest;
            if(checkOutput){
                final String[] actualRes = {"null"};
                expectedResultTest = DynamicTest.dynamicTest(
                    "Expected: " + expectedResult,
                    () -> assertEquals(expectedResult, expectedResult) // This always passes, for display
                );
    
                // Since using objects, need to have a null checker
                if(capturedOutput != null) {
                    actualRes[0] = capturedOutput;
                }

                actualResultTest = DynamicTest.dynamicTest(
                    "Actual: " + capturedOutput,
                    () -> 
                    assertEquals(expectedResult,actualRes[0])   
                );
            } else {
                expectedResultTest = DynamicTest.dynamicTest(
                    "Expected: " + expectedResult,
                    () -> assertEquals(expectedResult, expectedResult) // This always passes, for display
                );
                String actualRes;
    
                // Since using objects, need to have a null checker
                if(actual[0] != null) {
                    actualRes = actual[0].toString();
                } else {
                    actualRes = "null";
                }
    
                actualResultTest = DynamicTest.dynamicTest(
                    "Actual: " + actualRes,
                    () -> 
                    assertEquals(expectedResult, actual[0])
                );
            }
        
            DynamicContainer testCaseContainer = DynamicContainer.dynamicContainer(
                "Input: " + testCase,
                Stream.of(
                    expectedResultTest,
                    actualResultTest
                )
            );

            
            dynamicTests.add(testCaseContainer);
        }
    }


    /**
     * handleTestResults
     * 
     * A handler for the test results
     * 
     * @param compiledSuccess
     * @param expectedResult
     * @param actual
     * @param testCase
     * @param dynamicTests
     */

     protected void handleTestResults(Object expectedResult, Future<?> code, String testCase, String testGroup, String methodName, int timeoutTime, List<DynamicNode> dynamicTests){
        final Object[] actual = new Object[1];
        Exception exceptionCause = null;
        
        // Try running the code and check for possible runtime errors
        // If the code takes too long to compute, fail the test case

        try {
            actual[0] = code.get(timeoutTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            code.cancel(true);
            exceptionCause = e;
        } catch (ExecutionException e) {
            code.cancel(true);
            Throwable cause = e.getCause();
            if(cause != null) {
                exceptionCause = (Exception) cause;
            } else {
                exceptionCause = e;
            }
        } catch (Exception e) {
            code.cancel(true);
            exceptionCause = e;
        }

        // Successful compilation leads to checking actual results
        if(exceptionCause !=null){
            handleExceptions(exceptionCause, testGroup, methodName, dynamicTests,testCase);
        } else {
            DynamicTest expectedResultTest = DynamicTest.dynamicTest(
                "Expected: " + expectedResult,
                () -> assertEquals(expectedResult, expectedResult) // This always passes, for display
            );
            String actualRes = new String();

            // Since using objects, need to have a null checker
            if(actual[0] != null) {
                actualRes = actual[0].toString();
            } else {
                actualRes = "null";
            }

            DynamicTest actualResultTest = DynamicTest.dynamicTest(
                "Actual: " + actualRes,
                () -> 
                assertEquals(expectedResult, actual[0])
            );
        
            DynamicContainer testCaseContainer = DynamicContainer.dynamicContainer(
                "Input: " + testCase,
                Stream.of(
                    expectedResultTest,
                    actualResultTest
                )
            );

            
            dynamicTests.add(testCaseContainer);
        }
    }

    /**
     * handleTestResults
     * 
     * A handler for the test results with multiple possible answers
     * 
     * @param compiledSuccess
     * @param expectedResult
     * @param actual
     * @param testCase
     * @param dynamicTests
     */

    protected void handleTestResults(Object[] expectedResult, Future<?> code, String testCase, String testGroup, String methodName, int timeoutTime, List<DynamicNode> dynamicTests){
        final Object[] actual = new Object[1];
        final boolean[] res = {false};
        Exception exceptionCause = null;
        
        // Try running the code and check for possible runtime errors
        // If the code takes too long to compute, fail the test case

        try {
            actual[0] = code.get(timeoutTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            code.cancel(true);
            exceptionCause = e;
        } catch (ExecutionException e) {
            code.cancel(true);
            Throwable cause = e.getCause();
            if(cause != null) {
                exceptionCause = (Exception) cause;
            } else {
                exceptionCause = e;
            }
        } catch (Exception e) {
            code.cancel(true);
            exceptionCause = e;
        }

        // Successful compilation leads to checking actual results
        if(exceptionCause !=null){
            handleExceptions(exceptionCause, testGroup, methodName, dynamicTests,testCase);
        } else {
            DynamicTest expectedResultTest = DynamicTest.dynamicTest(
                "Expected: " + Arrays.toString(expectedResult),
                () -> assertEquals(true, true) // This always passes, for display
            );
            String actualRes = new String();

            // Since using objects, need to have a null checker
            if(actual[0] != null) {
                actualRes = actual[0].toString();
                for(int i = 0; i < expectedResult.length;i++){
                    if(expectedResult[i].equals(actual[0])){
                        res[0] = true;
                        break;
                    }
                }
            } else {
                actualRes = "null";
            }

            DynamicTest actualResultTest = DynamicTest.dynamicTest(
                "Actual: " + actualRes,
                () -> 
                assertEquals(res[0], true)
            );
        
            DynamicContainer testCaseContainer = DynamicContainer.dynamicContainer(
                "Input: " + testCase,
                Stream.of(
                    expectedResultTest,
                    actualResultTest
                )
            );

            
            dynamicTests.add(testCaseContainer);
        }
    }

    /**
     * handleExceptions
     * 
     * A custom exception method responsible for detailing the errors that may occur.
     * 
     * @param e - exception error
     * @param testGroupName - the group's name
     * @param methodName - the method
     * @param dynamicTests - the tests to be compiled 
     * @param input - the inputs of the test case
     */
    protected void handleExceptions(Exception e, String testGroupName, String methodName, List<DynamicNode> dynamicTests, String input) {
        String errorMessage;
        String lineNumberInfo = "";
    
        if (e instanceof ClassNotFoundException) {
            errorMessage = "class not found";
        } else if (e instanceof IllegalAccessException) {
            errorMessage = "access modifier not public";
        } else if (e instanceof IllegalArgumentException) {
            errorMessage = "argument mismatch (check parameters or types)";
        } else if (e instanceof TimeoutException) {
            errorMessage = "code timeout";
        } else if (e instanceof InstantiationException) {
            errorMessage = "no class constructor or instantiating a non-class";
        } else if (e instanceof NoSuchMethodException) {
            errorMessage = "method not found";
        } else if (e instanceof SecurityException) {
            errorMessage = "security violation";
        } else if (e instanceof InvocationTargetException || e instanceof java.lang.reflect.InvocationTargetException) {
            Throwable targetException = e.getCause();
            if (targetException != null) {
                errorMessage = targetException.getClass().getSimpleName() + " - " + targetException.getMessage();
            } else {
                errorMessage = "exception thrown by this method";
            }
        } else {
            errorMessage = e.getMessage();
        }
    
        // Get the first element of the stack trace to capture the line number where the exception occurred
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            StackTraceElement element = stackTrace[0];
            lineNumberInfo = " at " + element.getClassName() + "." + element.getMethodName() +
                            "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
        }
    
        DynamicTest failedResult = DynamicTest.dynamicTest(
            "Failed to compile: " + errorMessage + lineNumberInfo, // Include line number in the test name
            () -> 
            fail()
        );
    
        DynamicContainer testCaseContainer = DynamicContainer.dynamicContainer(
            "Input: " + input,
            Stream.of(failedResult)
        );
    
        dynamicTests.add(testCaseContainer);
    }

    /**
     * addPackage
     * 
     * Updates the packages of the imported test java files to match this file's package
     * 
     * @param filePath - Path of the file to update
     * @param packageName - same package as this file
     * @param fileName - Name of file to update
     * @param problem - the problem of the assignment
     */
    private static void addPackage(Path filePath, String packageName){
        
        try {
            // Read all lines from the file into a List
            List<String> fileContent = Files.readAllLines(filePath);
            final boolean[] stopChecks = {false,false};

            fileContent = fileContent.stream()
                    .map(line -> {
                        String trimmedLine = line.trim();
                        // Filter out any package lines in the code
                        if (!stopChecks[0]&&trimmedLine.toLowerCase().startsWith("package ")) {  
                            return "";  
                        }
                        if (!stopChecks[0]&&trimmedLine.contains("public class")){
                            stopChecks[0] = true;
                            return line;
                        }
                        return line;  
                    })
                    .collect(Collectors.toList());

            // Add the package of this test to the code that is being tested
            fileContent.add(0,"package "+packageName+";");

            // Write the updated content back to the file (overwrite the file)
            Files.write(filePath, fileContent);


        } catch (IOException e) {
        }
    }
    
}