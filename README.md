[![CI Check](https://github.com/Donnerbart/split-tests-java/actions/workflows/check.yml/badge.svg)](https://github.com/Donnerbart/split-tests-java/actions/workflows/check.yml)
[![Release](https://github.com/Donnerbart/split-tests-java/actions/workflows/release.yml/badge.svg)](https://github.com/Donnerbart/split-tests-java/actions/workflows/release.yml)

# Split Tests Java

Divides a test suite into groups with equal execution time, based on prior test timings.

This ensures optimal parallel execution. Since test file runtimes can vary significantly, splitting them evenly without
considering timing may result in inefficient grouping.

## Compatibility

This tool was written for Java and released as JAR archive.

## Usage

Download and extract the latest build from the releases page.

### Using the command line

Execute the test split with the required arguments.
The tool returns the set of test classes for the current split, joined by spaces.

```shell
java -jar split-tests-java.jar --split-index 0 --split-total 10 --glob '**/*Test.java'
```

### Using a JUnit report

For example, check out the project into `project` and the JUnit reports into `reports`. 

```
java -jar split-tests-java.jar --split-index 0 --split-total 10 --glob 'project/**/*Test.java' --junit 'reports/**/*.xml'
```

## Arguments

```plain
Usage: <main class> [options]
  Options:
    --debug, -d
      Enables debug logging.
      Default: false
    --exclude-glob, -e
      Glob pattern to exclude test files. Defaults to '**/*Abstract*'. Make 
      sure to single-quote the pattern to avoid shell expansion.
  * --glob, -g
      Glob pattern to find test files. Make sure to single-quote the pattern 
      to avoid shell expansion.
    --help, -h
      Prints the usage.
    --junit-glob, -j
      Glob pattern to find JUnit reports. Make sure to single-quote the 
      pattern to avoid shell expansion.
  * --split-index, -i
      This test split index.
      Default: 0
  * --split-total, -t
      Total number of test splits.
      Default: 0
    --working-directory, -w
      The working directory. Defaults to the current directory.
```

## Compilation

This tool is written in Java and uses Gradle as build tool.

- Install Java 21
- Checkout the repository
- `./gradlew shadowJar`

## Note

split-tests-java is inspired by [`split-test`](https://github.com/mtsmfm/split-test) for Ruby.

In comparison to split-test, split-tests-java works with default JUnit reports.
The output are also fully qualified class names instead of file names.
This makes it compatible with default Java tooling.
