# naive-jmeter-java-request-demo
Use Apache Jmeter to run a custom Java request.

## Usage

### Windows

```powershell
javac -cp ".;/path/to/your/ApacheJMeter_core.jar;/path/to/your/ApacheJMeter_java.jar" JavaTest/.java

jar -cvf JavaTest.jar JavaTest.class
```

### Linux

Replace the semicolon with colon in javac command.

### Move  the generated jar file to %JMETER_PATH%/lib/ext or %JMETER_PATH%/lib