$ErrorActionPreference = "Stop"
$url = "https://services.gradle.org/distributions/gradle-8.4-bin.zip"
$zip = "d:\Software\CallRecorder\gradle-8.4-bin.zip"
$dest = "d:\Software\CallRecorder\.gradle-dist"

Write-Host "Downloading Gradle 8.4 from $url..."
Invoke-WebRequest -Uri $url -OutFile $zip

Write-Host "Extracting Gradle to $dest..."
if (Test-Path $dest) {
    Remove-Item -Recurse -Force $dest
}
New-Item -ItemType Directory -Path $dest -Force | Out-Null
Expand-Archive -Path $zip -DestinationPath $dest

Write-Host "Cleaning up zip file..."
Remove-Item $zip

Write-Host "Gradle bootstrap complete!"
