#!/bin/bash
# Heartbeat lock management. Prevents overlapping cron runs.
# Usage: heartbeat-lock.sh check|acquire|release

LOCKFILE=/tmp/reg-heartbeat.lock

case "$1" in
    check)
        if [ -f "$LOCKFILE" ]; then
            PID=$(cat "$LOCKFILE" 2>/dev/null)
            AGE=$(( $(date +%s) - $(stat -c %Y "$LOCKFILE" 2>/dev/null || echo 0) ))
            # Stale if older than 20 minutes
            if [ "$AGE" -gt 1200 ]; then
                rm -f "$LOCKFILE"
                echo "STALE_LOCK_REMOVED"
                exit 0
            fi
            echo "LOCKED (PID $PID, age ${AGE}s)"
            exit 1
        fi
        echo "UNLOCKED"
        exit 0
        ;;
    acquire)
        echo $$ > "$LOCKFILE"
        echo "ACQUIRED"
        ;;
    release)
        rm -f "$LOCKFILE"
        echo "RELEASED"
        ;;
    *)
        echo "Usage: heartbeat-lock.sh check|acquire|release"
        exit 1
        ;;
esac
