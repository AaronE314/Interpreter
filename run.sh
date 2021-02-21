#!/bin/sh

javac Interpreter.java

for i in {1..9}; do
    echo test$i.txt > out/out$i.txt
    echo "=============================" >> out/out$i.txt
    cat tests/test$i.txt >> out/out$i.txt
    echo >> out/out$i.txt
    echo "=============================" >> out/out$i.txt
    
    java Interpreter < tests/test$i.txt "$@" &>> out/out$i.txt
done
