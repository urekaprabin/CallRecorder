$ErrorActionPreference = "Stop"
$y = "y`n" * 20
$y | & "C:\Program Files (x86)\Android\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
Write-Host "Licenses accepted successfully!"
Start-Sleep -Seconds 2
