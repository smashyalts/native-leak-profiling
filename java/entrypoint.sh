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

(
    sleep 60
    PID=$(pgrep java)
    INPUT_FILE="jvm.log"
    OUTPUT_FILE="dumps/matches.log"
    KEYWORD=$(echo "$PARSED" | sed -n 's/.*-Dkeyword=\([^ ]*\).*/\1/p')
    ENABLED=$(echo "$PARSED" | sed -n 's/.*-Danalyse=\([^ ]*\).*/\1/p')
    
    if [ "${ENABLED}" = "true" ]; then
        if [ -z "$PID" ]; then
            printf "No Java process found\n"
        else
            printf "Java process PID: $PID. Starting thread dumps.\n"
            while true; do
                kill -3 ${PID}
                
                tail -f "$INPUT_FILE" | while read -r line; do
                    if [[ "$line" =~ ^"Full thread dump" ]]; then
                        buffer=""
                        while read -r line; do
                            buffer+="$line"$'\n'
                            if [[ "$line" =~ ^"VM Thread" ]]; then
                                if echo "$buffer" | grep -q "$KEYWORD"; then
                                    echo "Found matching trace at $(date)" >> "$OUTPUT_FILE"
                                    echo "$buffer" >> "$OUTPUT_FILE"
                                    echo "---" >> "$OUTPUT_FILE"
                                fi
                                break
                            fi
                        done
                    fi
                done
            done
        fi
    fi
) &

# shellcheck disable=SC2086
exec env ${PARSED}