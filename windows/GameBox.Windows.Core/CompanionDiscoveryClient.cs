using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;

namespace GameBox.Windows.Core;

public sealed record DiscoveredGameBox(string Host, int Port, string DeviceName);

public static class CompanionDiscoveryProtocol
{
    public const int DiscoveryPort = 49_499;
    private const string RequestPrefix = "GAMEBOX_DISCOVER_V1:";
    private const string ResponsePrefix = "GAMEBOX_HERE_V1:";

    public static string CreateNonce() =>
        Convert.ToHexString(RandomNumberGenerator.GetBytes(16)).ToLowerInvariant();

    public static byte[] CreateRequest(string nonce)
    {
        RequireNonce(nonce);
        return Encoding.ASCII.GetBytes(RequestPrefix + nonce);
    }

    public static DiscoveredGameBox? ParseResponse(
        byte[] bytes,
        string expectedNonce,
        IPAddress senderAddress)
    {
        ArgumentNullException.ThrowIfNull(bytes);
        ArgumentNullException.ThrowIfNull(senderAddress);
        RequireNonce(expectedNonce);
        if (bytes.Length is < 1 or > 512) return null;
        var parts = Encoding.ASCII.GetString(bytes).Split(':');
        if (parts.Length != 4 || parts[0] != ResponsePrefix.TrimEnd(':') ||
            !string.Equals(parts[1], expectedNonce, StringComparison.Ordinal)) return null;
        if (!int.TryParse(parts[2], out var port) || port is < 10_240 or > 65_535) return null;
        string name;
        try { name = Encoding.UTF8.GetString(Convert.FromBase64String(parts[3])).Trim(); }
        catch (FormatException) { return null; }
        if (string.IsNullOrWhiteSpace(name) || Encoding.UTF8.GetByteCount(name) > 192) return null;
        return new DiscoveredGameBox(senderAddress.ToString(), port, name);
    }

    private static void RequireNonce(string nonce)
    {
        if (nonce.Length != 32 || nonce.Any(static c => !(char.IsAsciiHexDigit(c) && !char.IsUpper(c))))
            throw new ArgumentException("Discovery nonce is invalid.", nameof(nonce));
    }
}

public sealed class CompanionDiscoveryClient
{
    private readonly TimeSpan timeout;

    public CompanionDiscoveryClient(TimeSpan? timeout = null)
    {
        this.timeout = timeout ?? TimeSpan.FromSeconds(2);
        if (this.timeout <= TimeSpan.Zero || this.timeout > TimeSpan.FromSeconds(10))
            throw new ArgumentOutOfRangeException(nameof(timeout));
    }

    public async Task<IReadOnlyList<DiscoveredGameBox>> DiscoverAsync(
        CancellationToken cancellationToken = default)
    {
        var nonce = CompanionDiscoveryProtocol.CreateNonce();
        var request = CompanionDiscoveryProtocol.CreateRequest(nonce);
        using var udp = new UdpClient(AddressFamily.InterNetwork);
        udp.EnableBroadcast = true;
        udp.Client.Bind(new IPEndPoint(IPAddress.Any, 0));
        foreach (var endpoint in BroadcastEndpoints())
        {
            cancellationToken.ThrowIfCancellationRequested();
            try { await udp.SendAsync(request, request.Length, endpoint).ConfigureAwait(false); }
            catch (SocketException) { /* Another active interface may still discover the device. */ }
        }

        using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        deadline.CancelAfter(timeout);
        var devices = new Dictionary<string, DiscoveredGameBox>(StringComparer.OrdinalIgnoreCase);
        while (true)
        {
            try
            {
                var packet = await udp.ReceiveAsync(deadline.Token).ConfigureAwait(false);
                var device = CompanionDiscoveryProtocol.ParseResponse(
                    packet.Buffer, nonce, packet.RemoteEndPoint.Address);
                if (device is not null)
                    devices[device.Host + ":" + device.Port] = device;
            }
            catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
            {
                return devices.Values
                    .OrderBy(static item => item.DeviceName, StringComparer.CurrentCultureIgnoreCase)
                    .ThenBy(static item => item.Host, StringComparer.OrdinalIgnoreCase)
                    .ToList();
            }
        }
    }

    private static IReadOnlyList<IPEndPoint> BroadcastEndpoints()
    {
        var addresses = new HashSet<string>(StringComparer.Ordinal);
        var endpoints = new List<IPEndPoint>();
        void Add(IPAddress address)
        {
            if (addresses.Add(address.ToString()))
                endpoints.Add(new IPEndPoint(address, CompanionDiscoveryProtocol.DiscoveryPort));
        }
        Add(IPAddress.Broadcast);
        foreach (var network in NetworkInterface.GetAllNetworkInterfaces()
                     .Where(static item => item.OperationalStatus == OperationalStatus.Up))
        {
            foreach (var unicast in network.GetIPProperties().UnicastAddresses)
            {
                if (unicast.Address.AddressFamily != AddressFamily.InterNetwork ||
                    unicast.IPv4Mask is null) continue;
                var address = unicast.Address.GetAddressBytes();
                var mask = unicast.IPv4Mask.GetAddressBytes();
                var broadcast = new byte[4];
                for (var index = 0; index < 4; index++)
                    broadcast[index] = (byte)(address[index] | ~mask[index]);
                Add(new IPAddress(broadcast));
            }
        }
        return endpoints;
    }
}
