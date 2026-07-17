param(
  [Parameter(Mandatory=$true)][string]$Path,
  [string]$Language = "auto",
  [string]$Endpoint = "http://127.0.0.1:5002/transcribe/file"
)

if (!(Test-Path $Path)) { throw "File not found: $Path" }

$boundary = [System.Guid]::NewGuid().ToString("N")
$lf = "`r`n"
$uri = [Uri]$Endpoint

$fileName = [System.IO.Path]::GetFileName($Path)
$fileBytes = [System.IO.File]::ReadAllBytes($Path)

$ms = New-Object System.IO.MemoryStream
$sw = New-Object System.IO.StreamWriter($ms, (New-Object System.Text.UTF8Encoding($false)))

# form field: language
# whisper-service 的 /transcribe/file 直接把 language 传给 faster-whisper，不支持 "auto"
if ($Language -and $Language.Trim().ToLower() -ne "auto") {
  $sw.Write("--$boundary$lf")
  $sw.Write("Content-Disposition: form-data; name=`"language`"$lf$lf")
  $sw.Write("$Language$lf")
}

# form field: file
$sw.Write("--$boundary$lf")
$sw.Write("Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$lf")
$sw.Write("Content-Type: application/octet-stream$lf$lf")
$sw.Flush()
$ms.Write($fileBytes, 0, $fileBytes.Length)
$sw.Write("$lf--$boundary--$lf")
$sw.Flush()

$ms.Position = 0

$headers = @{ "Content-Type" = "multipart/form-data; boundary=$boundary" }

try {
  $resp = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -Body $ms.ToArray() -TimeoutSec 300
  $resp | ConvertTo-Json -Depth 10
} catch {
  if ($_.Exception.Response) {
    $sr = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
    $body = $sr.ReadToEnd()
    $sr.Close()
    throw "HTTP $($_.Exception.Response.StatusCode.value__) $($_.Exception.Message)\n$body"
  }
  throw
} finally {
  $sw.Dispose()
  $ms.Dispose()
}
