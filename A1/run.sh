#!/bin/sh

javac LexAn.java

for i in {1..4}; do
    java LexAn < test$i.txt > test$i.html
done
