#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source .env
set +a
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./mvnw spring-boot:run
