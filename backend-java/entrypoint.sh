#!/bin/sh
set -eu

case "${1:-}" in
  eod)
    : "${API_HOST:?API_HOST must be configured for the EOD job}"
    : "${EOD_TOKEN:?EOD_TOKEN must be configured for the EOD job}"
    exec curl --fail --silent --show-error --request POST \
      --header "X-EOD-Token: ${EOD_TOKEN}" \
      "https://${API_HOST}/internal/eod"
    ;;
  "")
    exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
    ;;
  *)
    echo "Usage: entrypoint.sh [eod]" >&2
    exit 64
    ;;
esac
