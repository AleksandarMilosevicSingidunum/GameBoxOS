namespace GameBox.Windows.Core;

/// <summary>Bounded recovery for transient LAN failures; authentication and payload errors never retry.</summary>
public static class CompanionRetryPolicy
{
    public static async Task<T> ExecuteAsync<T>(
        Func<CancellationToken, Task<T>> operation,
        CancellationToken cancellationToken = default,
        int maxAttempts = 3,
        TimeSpan? initialDelay = null,
        Func<TimeSpan, CancellationToken, Task>? delay = null)
    {
        ArgumentNullException.ThrowIfNull(operation);
        if (maxAttempts is < 1 or > 5) throw new ArgumentOutOfRangeException(nameof(maxAttempts));
        var backoff = initialDelay ?? TimeSpan.FromMilliseconds(250);
        if (backoff < TimeSpan.Zero || backoff > TimeSpan.FromSeconds(10))
            throw new ArgumentOutOfRangeException(nameof(initialDelay));
        delay ??= static (duration, token) => Task.Delay(duration, token);

        Exception? lastFailure = null;
        for (var attempt = 1; attempt <= maxAttempts; attempt++)
        {
            cancellationToken.ThrowIfCancellationRequested();
            try { return await operation(cancellationToken).ConfigureAwait(false); }
            catch (Exception exception) when (
                attempt < maxAttempts &&
                IsTransient(exception, cancellationToken))
            {
                lastFailure = exception;
                await delay(backoff, cancellationToken).ConfigureAwait(false);
                backoff = TimeSpan.FromMilliseconds(
                    Math.Min(backoff.TotalMilliseconds * 2, 5_000));
            }
        }
        throw lastFailure ?? new InvalidOperationException("Companion retry operation did not execute.");
    }

    public static bool IsTransient(Exception exception, CancellationToken callerToken) =>
        exception is HttpRequestException or IOException ||
        (exception is TaskCanceledException && !callerToken.IsCancellationRequested);
}
