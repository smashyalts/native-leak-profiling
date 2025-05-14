#!/bin/bash

#
# Copyright (c) 2021 Matthew Penner
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

# Default the TZ environment variable to UTC.
TZ=${TZ:-UTC}
export TZ

# Set environment variable that holds the Internal Docker IP
INTERNAL_IP=$(ip route get 1 | awk '{print $(NF-2);exit}')
export INTERNAL_IP

# Switch to the container's working directory
cd /home/container || exit 1

# Print Java version
printf "\033[1m\033[33mcontainer@pterodactyl~ \033[0mjava -version\n"
java -version

# Convert all of the "{{VARIABLE}}" parts of the command into the expected shell
# variable format of "${VARIABLE}" before evaluating the string and automatically
# replacing the values.
PARSED=$(echo "${STARTUP}" | sed -e 's/{{/${/g' -e 's/}}/}/g' | eval echo "$(cat -)")
TRACE_ENABLED=$(echo "$PARSED" | sed -n 's/.*-Danalyse=\([^ ]*\).*/\1/p')

# Display the command we're running in the output, and then execute it with the env
# from the container itself.
printf "\033[1m\033[33mcontainer@pterodactyl~ \033[0m%s\n" "$PARSED"

# failsafe in case dumps folder does not exist
mkdir -p dumps

# haha we hate nohup
(
    while true; do
        # loop through heapdump files
        for heapfile in dumps/*.heap; do
            if [ -f "$heapfile" ]; then
                basefilename="${heapfile%.heap}"
                
                timestamp=$(date +"%d.%m.%y-%H:%M")
                
                gif_output="dumps/output/${basefilename}-${timestamp}.gif"
                
                mkdir -p "$(dirname "$gif_output")"
                
                jeprof --show_bytes --gif \
                    /opt/java/openjdk/bin/java \
                    "$heapfile" > "$gif_output"
                
                # Remove processed heap file
                rm "$heapfile"
            fi
        done
        
        # Wait one minute before checking again
        sleep 60
    done
) &

if [ "$TRACE_ENABLED" = "true" ]; then
    # Extract the keyword from the PARSED variable
    KEYWORD=$(echo "$PARSED" | sed -n 's/.*-Dkeyword=\([^ ]*\).*/\1/p')

    (
        mkdir -p dumps/traces

        while true; do
            sleep 120

            PID=$(pgrep java)
            kill -3 ${PID}

            # give it a moment to dump
            sleep 5 

            JVM_LOG="jvm.log"

            if [ -f "$JVM_LOG" ]; then
                timestamp=$(date +"%d.%m.%y-%H:%M")
                TRACE_OUTPUT="dumps/traces/trace-${timestamp}.log"

                if grep -qE "$KEYWORD" "$JVM_LOG"; then
                    cat "$JVM_LOG" > "$TRACE_OUTPUT"

                    echo "Detected keyword ($KEYWORD):" >> "$TRACE_OUTPUT"
                    grep -E "$KEYWORD" "$JVM_LOG" >> "$TRACE_OUTPUT"
                fi

                sleep 5

                > "$JVM_LOG"
            fi
        done
    ) &
fi

# shellcheck disable=SC2086
exec env ${PARSED}