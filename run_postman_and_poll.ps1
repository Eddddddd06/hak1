param(
    [Parameter(Mandatory=$true)]
    [string]$ExportedEnvPath
)

# run_postman_and_poll.ps1
# Lee un environment exportado por Newman/Postman (JSON), extrae baseUrl y lastRequestId
# y hace polling a varios endpoints candidatos para comprobar el estado del reporte asíncrono.

function Get-EnvValue {
    param($envJson, $key)
    if ($null -eq $envJson) { return $null }
    if ($envJson.values) {
        $v = $envJson.values | Where-Object { $_.key -eq $key }
        if ($v) { return $v.value }
    }
    # Try lower-case keys or top-level
    return $null
}

if (-not (Test-Path $ExportedEnvPath)) {
    Write-Error "El archivo de environment no existe: $ExportedEnvPath"
    exit 1
}

try {
    $envJson = Get-Content -Raw -Path $ExportedEnvPath | ConvertFrom-Json
} catch {
    Write-Error "No se pudo parsear $ExportedEnvPath como JSON: $_"
    exit 1
}

$baseUrl = Get-EnvValue -envJson $envJson -key 'baseUrl'
$reqId = Get-EnvValue -envJson $envJson -key 'lastRequestId'
if (-not $reqId) { $reqId = Get-EnvValue -envJson $envJson -key 'lastPremiumRequestId' }

if (-not $baseUrl) {
    $baseUrl = Read-Host 'No se encontró baseUrl en el environment. Introduce la base URL (ej. http://localhost:8080)'
}

if (-not $reqId) {
    $reqId = Read-Host 'No se encontró lastRequestId en el environment. Introduce el requestId a chequear'
}

if (-not $reqId) {
    Write-Error 'No se proporcionó requestId. Abortando.'
    exit 1
}

# Normalizar baseUrl (sin slash final)
if ($baseUrl.EndsWith('/')) { $baseUrl = $baseUrl.TrimEnd('/') }

$candidatePaths = @(
    "/reports/$reqId",
    "/reports/$reqId/status",
    "/reports/status/$reqId",
    "/sales/summary/$reqId",
    "/sales/summary/status/$reqId",
    "/sales/summary/status/$reqId",
    "/sales/summary/request/$reqId",
    "/reports/$reqId/state"
)

$maxAttempts = 60
$delaySeconds = 5

Write-Host "Iniciando polling para requestId='$reqId' en baseUrl='$baseUrl'"

for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    Write-Host "Intento $attempt de $maxAttempts..."
    foreach ($path in $candidatePaths) {
        $url = "$baseUrl$path"
        Write-Host "  Probing: $url"
        try {
            $resp = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 30 -ErrorAction Stop
            # Interpret response
            if ($null -ne $resp) {
                # If response is a simple string
                if ($resp -is [string]) {
                    $statusStr = $resp
                } elseif ($resp.status) {
                    $statusStr = $resp.status
                } elseif ($resp.state) {
                    $statusStr = $resp.state
                } elseif ($resp.statusCode) {
                    $statusStr = $resp.statusCode
                } else {
                    # Try to find any property that looks like status
                    $props = $resp | Get-Member -MemberType NoteProperty | Select-Object -ExpandProperty Name -ErrorAction SilentlyContinue
                    $found = $null
                    foreach ($p in $props) {
                        if ($p -match 'status|state|result') { $found = $p; break }
                    }
                    if ($found) { $statusStr = $resp.$found } else { $statusStr = $null }
                }

                if ($statusStr) {
                    $statusUp = $statusStr.ToString().ToUpper()
                    Write-Host "    Estado: $statusStr"
                    if ($statusUp -match 'COMPLET|DONE|READY|SUCCESS') {
                        Write-Host "Reporte COMPLETADO. URL usada: $url"
                        exit 0
                    }
                    if ($statusUp -match 'FAILED|ERROR|FAILED') {
                        Write-Error "El reporte terminó en estado FAILED/ERROR. URL: $url. Estado devuelto: $statusStr"
                        exit 2
                    }
                    # Si el estado es PROCESSING o PENDING, continuar polling
                } else {
                    # No status field; if HTTP 200 and non-empty body, print it and decide
                    Write-Host "    Respuesta recibida (sin campo status detectado):"
                    Write-Host ($resp | ConvertTo-Json -Depth 3)
                    # Heurística: si el JSON tiene 'completedAt' o 'result' considerar completado
                    if ($resp.completedAt -or $resp.result) {
                        Write-Host "Se detectó posible completion en la respuesta. URL: $url"
                        exit 0
                    }
                }
            }
        } catch {
            # Ignorar errores 404/500 y continuar con siguiente path
            Write-Host "    No disponible o error al consultar $url: $($_.Exception.Message)"
        }
    }

    Write-Host "Esperando $delaySeconds segundos antes del siguiente intento..."
    Start-Sleep -Seconds $delaySeconds
}

Write-Error "Timeout: no se obtuvo status COMPLETED después de $maxAttempts intentos."
exit 3

