#!/bin/bash

list_pid=$(ps w | grep "[j]ava -jar /tmp/karst-21" | grep -E -o "^ *[0-9]+")
for pid in $list_pid; do kill -9 $pid; done