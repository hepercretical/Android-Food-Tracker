param(
    [Parameter(Mandatory = $true)][string]$Url,
    [Parameter(Mandatory = $true)][long]$Length,
    [Parameter(Mandatory = $true)][Alias('Sha256')][string]$Checksum,
    [ValidateSet('SHA256', 'SHA1')][string]$Algorithm = 'SHA256',
    [Parameter(Mandatory = $true)][string]$Destination
)
$ErrorActionPreference = 'Stop'
$stage = Join-Path (Join-Path $env:LOCALAPPDATA 'Temp') ('foodtracker-download-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $stage | Out-Null
$chunkSize = [long][Math]::Ceiling($Length / 8.0)
$jobs = @()
for ($i = 0; $i -lt 8; $i++) {
    $start = $i * $chunkSize
    $end = [Math]::Min($Length - 1, $start + $chunkSize - 1)
    $part = Join-Path $stage "$i.part"
    $jobs += Start-Job -ArgumentList $Url, $start, $end, $part -ScriptBlock {
        param($url, $start, $end, $part)
        & curl.exe -fsSL --retry 2 --range "$start-$end" $url -o $part
        if ($LASTEXITCODE -ne 0) { throw "Download segment failed: $start-$end" }
        if ((Get-Item -LiteralPath $part).Length -ne ($end - $start + 1)) {
            throw 'Server returned an unexpected segment length.'
        }
    }
}
try {
    $jobs | Wait-Job | Out-Null
    $jobs | Receive-Job -ErrorAction Stop
    $output = [System.IO.File]::Create($Destination)
    try {
        for ($i = 0; $i -lt 8; $i++) {
            $inputStream = [System.IO.File]::OpenRead((Join-Path $stage "$i.part"))
            try { $inputStream.CopyTo($output) } finally { $inputStream.Dispose() }
        }
    } finally { $output.Dispose() }
    if ((Get-FileHash -LiteralPath $Destination -Algorithm $Algorithm).Hash.ToLowerInvariant() -ne $Checksum.ToLowerInvariant()) {
        throw 'Downloaded archive checksum mismatch.'
    }
    Write-Host "Verified download: $Destination"
} finally {
    $jobs | Stop-Job
    $jobs | Remove-Job
}
