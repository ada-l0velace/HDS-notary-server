#!/bin/bash
rm user*
for i in $(seq "$1"); do
  ssh-keygen -t rsa -f "user$i" -N ""
done
#ssh-keygen -t rsa -f user0
