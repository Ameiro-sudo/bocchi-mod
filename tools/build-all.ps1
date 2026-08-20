<#
.SYNOPSIS
  一键构建全部变体: 版本 (1.21.1 / 1.21.5) x 加载器 (Fabric / NeoForge)

.DESCRIPTION
  构建产物收集到 成品/<mc>/<loader>/原版/ 目录 (与 README 目录约定一致)。
  可选参数:
    -Only1215  仅构建 1.21.5
    -Only1211  仅构建 1.21.1

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/build-all.ps1
  powershell -ExecutionPolicy Bypass -File tools/build-all.ps1 -Only1215
#>
param(
  [switch]$Only1215,
  [switch]$Only1211
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outRoot = Join-Path $root "成品"

function Invoke-Tree($mc, $label) {
  Write-Host "===== 构建 $label ($mc) =====" -ForegroundColor Cyan
  $tree = Join-Path (Join-Path $root "src") $mc
  $gradlew = Join-Path $tree "gradlew.bat"
  if (-not (Test-Path $gradlew)) { throw "未找到 $gradlew" }

  Push-Location $tree
  try {
    & $gradlew ":fabric:build" ":neoforge:build" --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "gradle 构建失败 (exit $LASTEXITCODE)" }

    $modVersion = (Select-String -Path (Join-Path $tree "gradle.properties") -Pattern '^mod_version=').Line.Split('=')[1].Trim()

    $targets = @(
      @{ Loader = "fabric";    Pattern = "bocchi-fabric-$label-$modVersion.jar";        Dir = "fabric" },
      @{ Loader = "neoforge";  Pattern = "bocchi-neoforge-$label-$modVersion-all.jar"; Dir = "neoforge" }
    )
    foreach ($t in $targets) {
      $src = Get-ChildItem (Join-Path $tree "$($t.Dir)\build\libs") -Filter $t.Pattern -ErrorAction SilentlyContinue | Select-Object -First 1
      if (-not $src) {
        Write-Host "WARN: 未找到产物 $($t.Pattern)" -ForegroundColor Yellow
        continue
      }
      $dstDir = Join-Path (Join-Path (Join-Path $outRoot $label) $t.Loader) "原版"
      New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
      Copy-Item $src.FullName (Join-Path $dstDir $src.Name) -Force
      Write-Host "  -> $($src.Name) 已复制到 成品/$label/$($t.Loader)/原版/" -ForegroundColor Green
    }
  } finally {
    Pop-Location
  }
}

$do1215 = -not $Only1211
$do1211 = -not $Only1215
if ($do1215) { Invoke-Tree "bocchi--MC-1.21.5-main" "1.21.5" }
if ($do1211) { Invoke-Tree "bocchi--MC-1.21.1-main" "1.21.1" }

Write-Host "===== 全部完成 =====" -ForegroundColor Cyan