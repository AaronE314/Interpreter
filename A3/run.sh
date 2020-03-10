#!/bin/sh

javac Parser.java

for i in {1..6}; do
    echo "============================="
    echo test$i.txt
    echo "============================="
    java Parser < test$i.txt
    echo
done
