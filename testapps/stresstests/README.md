
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
	public void run() throws Exception{}

	public abstract S execute(T prerequisites) throws Exception;
}
```

### Profiling (Instrumented Tests)
We should be collecting data regarding the device performance during the test run.

* CPU Usage
* Memory Usage
* Network Information (Data Received vs Data Sent)


#### Output file
The stress tests generate an output file with the following format
| Time | CPu Usage (%) | Memory Usage (KB) | Data Received (bytes) | Data Sent (bytes) | Number of Threads | Time Limit | Total Device memory | Device Name |
|------|---------------|-------------------|-----------------------|-------------------|-------------------|------------|---------------------|-------------|
|      |               |                   |                       |                   |                   |            |                     |             |

The output file is stored in the test device at `/sdcard/automation/`

The name of the output file is defined here:
```java
public abstract class AbstractMsalUiStressTest {
	public abstract String getOutputFileName();
}
```
### Analysis and Reporting
The output file can be analyzed with Power BI.

Example analysis.
![analysis](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/git/stress-test/testapps/stresstests/analysis.png?raw=true)

### References
https://www.geeksforgeeks.org/difference-between-performance-and-stress-testing/
https://docs.microsoft.com/en-us/power-bi/?WT.mc_id=sitertzn_learntab_docs-card-powerbi