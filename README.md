# Interpreter

## Description
This project was an assigment for my compilers class, There are 3 parts to the project each representing the different stages in an interpreter.
* Lexical Analyzer
* Parser
* Interpreter

## File Layout

### Java Files
There is a java file per stage each containing all the previous stages. Each fill will run  the stages it has defined up to and print the results of the stage the file is named after
* LexAn.java -> Prints the Lexical Analysis
* Parser.java -> Prints the Parsed code
* Interpreter.java -> Will run the given code (only printing if there are print statements in the code)

Some steps also prints out some info about the state of the interpeter as it runs

### Tests
There are some tests that contain valid and invalid code that the interpreter can read that cover various scenarios. The tests can be found in the tests folder, while the outputs (with the given input) can be found in the output folder

### Docs
Contains the Language rules as well as the breakdown of Lexical Analyser
## To Run

To easly run the interpreter with the tests you can just run `run.sh`. If you want to run things manually you can run it as follows

`javac Interpreter.java`

`java Interpreter < input.txt`

## Examples

Loops
```
int x,i;
x=0;i=1;
while (i < 10) do
	x = x+i*i; i=i+1
od;
print(x);.

=============================
285

```
Loops with If then
```
int x,i;
x=0;i=1;
while(i<10) do if (i>2)
then x=2*x else x=x*x; if (x>2) then x=i+x else x = i fi fi;
x = x+i*i; i=i+1
od;
print(x);.

=============================
3461

```
Parse Error
```
int a,b,r;
a=21; b=1r5;
while (b<>0) do
	r = a % b;
	a=b;
	b=r;
od;
print(a).
=============================
int a, b, r;
a=21;
b=

ParseException 2:10: Expected . but received 1r
```
Recursion
```
def int gcd(int a, int b)
	if(a==b) then
		return (a) 
	fi;
	if(a>b) then
		return(gcd(a-b,b))
	else 
		return(gcd(a,b-a)) 
	fi;
fed;
print gcd(21,15).
=============================
3
```