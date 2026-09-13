using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;

namespace GameBox.Windows;

public sealed record PairingProfile(string Host, int Port, string Secret);

/// <summary>Persists the LAN endpoint while protecting pairing material with Windows DPAPI for the current user.</summary>
public sealed class PairingProfileStore
{
    private const uint CryptProtectUiForbidden = 0x1;
    private readonly string path;

    public PairingProfileStore(string path)
    {
        this.path = Path.GetFullPath(path ?? throw new ArgumentNullException(nameof(path)));
    }

    public async Task SaveAsync(PairingProfile profile, CancellationToken cancellationToken = default)
    {
        Validate(profile);
        var payload = new PersistedPairingProfile(
            profile.Host.Trim(),
            profile.Port,
            Convert.ToBase64String(Protect(Encoding.UTF8.GetBytes(profile.Secret))));
        var directory = Path.GetDirectoryName(path) ?? throw new InvalidDataException("Pairing profile directory is unavailable.");
        Directory.CreateDirectory(directory);
        var partial = path + ".partial";
        try
        {
            await File.WriteAllTextAsync(
                partial, JsonSerializer.Serialize(payload), Encoding.UTF8, cancellationToken);
            File.Move(partial, path, true);
        }
        finally
        {
            if (File.Exists(partial)) File.Delete(partial);
        }
    }

    public async Task<PairingProfile?> LoadAsync(CancellationToken cancellationToken = default)
    {
        if (!File.Exists(path)) return null;
        var info = new FileInfo(path);
        if (info.Length is <= 0 or > 16 * 1024)
            throw new InvalidDataException("Saved pairing profile is invalid.");
        PersistedPairingProfile? payload;
        try
        {
            payload = JsonSerializer.Deserialize<PersistedPairingProfile>(
                await File.ReadAllTextAsync(path, cancellationToken));
        }
        catch (JsonException exception)
        {
            throw new InvalidDataException("Saved pairing profile is invalid.", exception);
        }
        if (payload is null) throw new InvalidDataException("Saved pairing profile is invalid.");
        byte[] protectedSecret;
        try { protectedSecret = Convert.FromBase64String(payload.ProtectedSecret); }
        catch (FormatException exception) { throw new InvalidDataException("Saved pairing secret is invalid.", exception); }
        string secret;
        try { secret = Encoding.UTF8.GetString(Unprotect(protectedSecret)); }
        catch (Exception exception) when (exception is System.ComponentModel.Win32Exception or InvalidDataException)
        {
            throw new InvalidDataException("Saved pairing secret cannot be decrypted for this Windows user.", exception);
        }
        var profile = new PairingProfile(payload.Host, payload.Port, secret);
        Validate(profile);
        return profile;
    }

    public void Delete()
    {
        if (File.Exists(path)) File.Delete(path);
        if (File.Exists(path + ".partial")) File.Delete(path + ".partial");
    }

    private static void Validate(PairingProfile profile)
    {
        ArgumentNullException.ThrowIfNull(profile);
        var host = profile.Host?.Trim() ?? "";
        if (host.Length is < 1 or > 253 || host.Contains('/') || host.Contains('\\') ||
            host.Contains('?') || host.Contains('#') || host.Any(char.IsWhiteSpace))
            throw new InvalidDataException("Paired device host is invalid.");
        if (profile.Port is < 10240 or > 65535)
            throw new InvalidDataException("Paired device port is invalid.");
        if (profile.Secret.Length != 64 ||
            profile.Secret.Any(static c => !(char.IsAsciiHexDigit(c))))
            throw new InvalidDataException("Pairing secret is invalid.");
    }

    private static byte[] Protect(byte[] input) => Transform(input, protect: true);
    private static byte[] Unprotect(byte[] input) => Transform(input, protect: false);

    private static byte[] Transform(byte[] input, bool protect)
    {
        if (!OperatingSystem.IsWindows())
            throw new PlatformNotSupportedException("Pairing protection requires Windows.");
        if (input.Length is <= 0 or > 4096) throw new InvalidDataException("Pairing secret payload is invalid.");
        var inputPointer = Marshal.AllocHGlobal(input.Length);
        try
        {
            Marshal.Copy(input, 0, inputPointer, input.Length);
            var inputBlob = new DataBlob { Size = input.Length, Data = inputPointer };
            DataBlob outputBlob;
            var success = protect
                ? CryptProtectData(ref inputBlob, null, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero,
                    CryptProtectUiForbidden, out outputBlob)
                : CryptUnprotectData(ref inputBlob, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero,
                    CryptProtectUiForbidden, out outputBlob);
            if (!success) throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());
            try
            {
                if (outputBlob.Size <= 0 || outputBlob.Size > 16 * 1024)
                    throw new InvalidDataException("Protected pairing payload is invalid.");
                var result = new byte[outputBlob.Size];
                Marshal.Copy(outputBlob.Data, result, 0, result.Length);
                return result;
            }
            finally { LocalFree(outputBlob.Data); }
        }
        finally
        {
            Marshal.Copy(new byte[input.Length], 0, inputPointer, input.Length);
            Marshal.FreeHGlobal(inputPointer);
        }
    }

    private sealed record PersistedPairingProfile(string Host, int Port, string ProtectedSecret);

    [StructLayout(LayoutKind.Sequential)]
    private struct DataBlob { public int Size; public IntPtr Data; }

    [DllImport("crypt32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CryptProtectData(
        ref DataBlob dataIn, string? description, IntPtr optionalEntropy, IntPtr reserved,
        IntPtr prompt, uint flags, out DataBlob dataOut);

    [DllImport("crypt32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CryptUnprotectData(
        ref DataBlob dataIn, IntPtr description, IntPtr optionalEntropy, IntPtr reserved,
        IntPtr prompt, uint flags, out DataBlob dataOut);

    [DllImport("kernel32.dll")]
    private static extern IntPtr LocalFree(IntPtr memory);
}
