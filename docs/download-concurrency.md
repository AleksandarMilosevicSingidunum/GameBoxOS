# Download concurrency

GameBox limits authorized remote content transfers to two concurrent workers process-wide. WorkManager still keeps each game unique, so repeated install requests cannot create duplicate transfers for the same title. Additional eligible jobs remain managed by WorkManager and enter the transfer gate as permits become available.

The gate is cancellation-aware: pausing or cancelling a WorkManager job while it waits does not consume a permit. Network policy, retry, checksum verification, staging, and atomic promotion continue to apply independently to every job.

This closes the automated implementation portion of Blueprint requirement FILE-04. Process-death and live multi-network transfer validation remain separate release evidence.
