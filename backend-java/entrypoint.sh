#!/bin/sh
set -eu

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
