#!/bin/sh
# Exports key=value pairs in vars files as environment variables
# Check conents of vars file prior to execution

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )" 
while read line; do 
	export $line; 
done < "$DIR/vars"

