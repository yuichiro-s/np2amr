#!/usr/bin/env bash

# Count number of samples
cat $1 | grep "^# ::snt" | wc -l
