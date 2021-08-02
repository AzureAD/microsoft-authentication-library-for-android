
  
## Stress Testing for Microsoft Authentication Library for Android (MSAL)  
  
### Introduction  
Stress testing is a type of software testing that verifies the stability and reliability of the system. This test particularly determines the system's robustness and  
error handling under extremely heavy load conditions.  
  
It is basically a subset of performance testing that aims to check the stability of the application under sudden increased load.  
  
### Components  
- User app to run stress tests  
- Android Instrumented test run  
  
### Scenarios  
- Acquire token silent (broker-less)  
  
This is not an exhaustive list of scenarios. For this case, we are covering non-interactive cases, and background executing tasks.  
  
### How to measure  
* There will be two variables involved.  
   1. n - the number of threads  
   2. t - the time limit for the stress tests  
  
In order to build a stress testing tool, there are two stages involved.  
1. Preparation - basically fetching of prerequisites required for running the stress tests.  
2. Execution - with the prerequisites, have an synchronous function that will be stress tested. The input for this function should only be the prerequisites in the previous step. This function is to be executed in multiple threads defined by `n`  
  
```java  
public abstract class AbstractStressTest<T, S> {  
   public abstract T prepare() throws Exception;  
   public abstract S execute(T prerequisites) throws Exception;  
}  
```  
  
### Test app template  
![screenshot](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/git/stress-test/testapps/stresstests/screenshot.png?raw=true)  
  
### Instrumented Tests  
Making use of the existing `msalautomationapp` the instrumented tests are located in the folder:  
`msal/msalautomationapp/src/androidTest/java/com/microsoft/identity/client/msal/automationapp/testpass/stress`  
  
The base stress test file:  
```java  
public abstract class AbstractMsalUiStressTest<T, S> extends AbstractMsalUiTest {  
   /**  
    * This runs the stress test defined in the {@link AbstractMsalUiStressTest#execute} method  
    **/  
   public void run() throws Exception;  
  
   public abstract S execute(T prerequisites) throws Exception;  
}  
```  

#### Extending Instrumented Tests
In order to extend functionality of the instrumented tests:
1. Extend the `AbstractMsalUiStressTest` class. 
2. Implement the `prepare()` method - this can be used to fetch some data  required for the stress tests.
3. Implement the `execute()` method - this is the function that will be put under stress.
4. Implement the `getNumberOfThreads()` method - provide an integer that defines the number of threads that will be executed.
5. Implement the `getTimeLimit()` method to provide the number of minutes that the stress tests will be run.
6. Implement the `getOutputFileName()` method - a file to store the output data. The file will exist in the test device on `sdcard/automation` 
7. Implement the `isTestPassed()` method - return true or false on whether the result from the stress test run passed.

  
### Profiling (Instrumented Tests)  
We should be collecting data regarding the device performance during the test run.  
  
* CPU Usage  
* Memory Usage  
* Network Information (Data Received vs Data Sent)  
  
  
#### Output file  
The stress tests generate an output file with the following format  
| Time | CPu Usage (%) | Memory Usage (KB) | Data Received (bytes) | Data Sent (bytes) | Number of Threads | Time Limit | Total Device memory | Device Name | Tests Passed | Tests Failed |  
|------|---------------|-------------------|-----------------------|-------------------|-------------------|------------|---------------------|-------------|-------------|-------------|  
|      |               |                   |                       |                   |                   |            |                     |             |             |             |
  
The output file is stored in the test device at `/sdcard/automation/`  
  
The name of the output file is defined here:  
```java  
public abstract class AbstractMsalUiStressTest {  
   public abstract String getOutputFileName();  
}  
```  
### Analysis and Reporting  
The output file can be analyzed with Power BI. 

1. Pull the file from the device to your computer to a folder e.g. `/home/moses/Downloads` after the tests are run. 
```sh
adb pull sdcard/automation/StressTestsAcquireTokenSilent.csv /home/moses/Downloads
```
2. Upload and replace the csv file in the [OneDrive folder](https://microsoft.sharepoint-df.com/:f:/t/AndroidBrokerRelease9/EuchSWQ9AVRBls9IBDfmpH4By6_pk8l1e0d3SqfDd9xOrg?e=TvSBNm)
3. Refresh the data set [StressTestsAcquireTokenSilent] on [Power Bi](https://msit.powerbi.com/groups/be33b3e7-c501-4225-9413-3b88046f3eb3/list)
4. Open the updated [Power BI report.](https://msit.powerbi.com/groups/be33b3e7-c501-4225-9413-3b88046f3eb3/reports/7cfd0c57-d579-46d3-b9a9-87f9e2190264/ReportSection)
  
### References  
https://www.geeksforgeeks.org/difference-between-performance-and-stress-testing/  
https://docs.microsoft.com/en-us/power-bi/?WT.mc_id=sitertzn_learntab_docs-card-powerbi