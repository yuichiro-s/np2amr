#!/usr/bin/env bash

# Report the distribution of sizes of AMRs
sed -e "s/.*(.*/(/g" -e "s/#.*//g" -e "s/.*:.*/(/g" $1 | uniq -c | grep "(" | sort | uniq -c
