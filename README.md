# Auto Code Reviewer

An automated code review system built in Java that helps evaluate student submissions against predefined test cases. This project provides a structured way to test and grade programming assignments.

## Project Overview

This project is a Java-based automated testing framework that:
- Processes student submissions from a structured directory
- Runs predefined test cases against each submission
- Provides detailed test results and grading information
- Supports multiple test cases and different types of assignments

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Maven
- VS Code with the Extension Pack for Java installed

## Project Structure

```
testunit1/
├── src/
│   └── test/           # Contains test cases and grading logic
├── Submissions/        # Directory for student submissions
│   └── Group_X/       # Each group's submissions in separate folders
├── pom.xml            # Maven project configuration
└── HowToUse.txt       # Detailed usage instructions
```

## Setup Instructions

1. Clone the repository
2. Open the project in VS Code
3. Ensure you have the Extension Pack for Java installed
4. Open the project folder using File > Open Folder

## How to Use

1. **Prepare Submissions**
   - Create a folder in the `Submissions` directory with the group name
   - Place the student's Java file inside the group folder
   - The path should follow the pattern: `Submissions/Group_X/Assignment.java`

2. **Run Tests**
   - Navigate to `src/test/java/com/foo/GraderTest.java`
   - Run the GraderTest file (you may need to compile it twice)
   - If you encounter compilation issues:
     - Try cleaning the Java Language Server Workspace
     - Check the pom.xml configuration
     - Ensure all directories match the expected structure

3. **View Results**
   - Open the Testing tab in VS Code's sidebar
   - Expand the Java Test dropdown
   - View individual student submissions and their test results
   - Each submission will show which test cases passed or failed

## Troubleshooting

If you encounter issues:

1. **Compilation Errors**
   - Clean the Java Language Server Workspace
   - Verify directory structure matches requirements
   - Check pom.xml configuration

2. **Directory Structure Issues**
   - Ensure the Tests directory only contains .java files
   - Verify submission paths match the structure in GraderTest.java
   - Check that group folders are properly named

3. **Maven Issues**
   - If pom.xml needs updating, use the provided version
   - Run `mvn clean install` to rebuild the project
