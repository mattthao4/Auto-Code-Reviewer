package com.reviewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;


public class GraderTest extends SetupTest{

    // This will hold all the test files that will be run.

    public GraderTest() throws IOException {
        // first parameter is the assignment name/folder name
        // second parameter is the list of java files/assignment files to compile
        super("testunit1","reviewer", new String[] {"FizzBuzzer"});
    }

    @TestFactory
    @Override
    public Stream<DynamicNode> graderTest() {
        // Create a List to hold all DynamicNodes
        List<DynamicNode> dynamicNodes = new ArrayList<>();
        
        // Iterate over each test group
        for (TestGroup testGroup : testGroups) {
            // Add all dynamic nodes for the current test group to the list
            dynamicNodes.add(DynamicContainer.dynamicContainer(testGroup.getName(), Stream.of(
                // Add the results from isSumTest
                fizzTest(testGroup.getName(),testFiles[0])
            ).flatMap(stream -> stream))); 
        }
        
        // Return the complete Stream of DynamicNodes
        return dynamicNodes.stream();
    }
    

    /**
     * fizzTest
     * 
     * Create a list of fizzBuzz test cases for all the Tests
     * 
     * @param groupNameFilter - the group that is being tested on
     * @param fileName - the java file that is being used for the tests
     * @return Stream<DynamicNode> - a list of tests
     */

     public Stream<DynamicNode> fizzTest(String groupNameFilter, String fileName) {
        String methodName = "convert";
        final int timeoutTime = 100; // in milliseconds
    
        // Returns the tests for all the groups
        return testGroups.stream()
        .filter(testGroup -> testGroup.getName().equals(groupNameFilter))   // Filter by group name
        .map(testGroup -> {

            // Create a list of tests for each group
            List<DynamicNode> dynamicTests = new ArrayList<>();
            ArrayList<Integer> testCases = new ArrayList<>();

            int[] cases = {
                -1,
                2,
                3,
                5,
                6,
                9,
                15
            };
    
            String[] expectedResults = {
               "N/A",
               "2",
               "Fizz",
               "Buzz",
               "Fizz",
               "Fizz",
               "FizzBuzz" 
            };

            for(int i = 0; i < cases.length; i++){ 
                testCases.add(cases[i]);
            }
            
            for (int i = 0; i < testCases.size(); i++) {
                
                final int input = i;
                final String expectedResult = expectedResults[input]; // Store expected result
                final int testCase = testCases.get(input);
                Object[] params = new Object[]{testCase};
                Class<?>[] paramTypes = new Class<?>[]{int.class};


                Future<Object> codeChecker = caseCheck(testGroup,fileName,methodName,params,paramTypes);
                // The results of the test cases
                handleTestResults(expectedResult,codeChecker,Integer.toString(testCase),testGroup.getName(),methodName,timeoutTime,dynamicTests);
            }
            
            // Return the tests under the test name
            return DynamicContainer.dynamicContainer(methodName+"()", dynamicTests.stream());
        });
    }
}