# CS245 Assignment 3 - Friends in a Scandal

## How to compile
Use javac from JDK.
```bash
javac *.java
```

## How to run
Use java from JDK.
```bash
# java A3 <mails directory>, eg.
java A3 maildir
#  or java A3 <mails directory> <connectors output file>, eg.
java A3 maildir /tmp/connectors.txt
```

## Implementation Details
- Use thread poll to process reading email files task.
- Use an iterative version DFS method to find connectors and teams. 
  (the recursive version will cause stack overflow when graph is too large).