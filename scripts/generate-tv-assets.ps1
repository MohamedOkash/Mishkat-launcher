# Generate Mishkat TV Brand Assets for Samsung Tizen & LG webOS
Add-Type -AssemblyName System.Drawing

function Create-MishkatIcon([string]$path, [int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    # Background Night Blue
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 15, 23, 42))
    $g.FillRectangle($bgBrush, 0, 0, $size, $size)

    # Gold Circular Ambient
    $goldBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 245, 158, 11))
    $glowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 245, 158, 11))
    
    $glowSize = [int]($size * 0.75)
    $glowOffset = [int](($size - $glowSize) / 2)
    $g.FillEllipse($glowBrush, $glowOffset, $glowOffset, $glowSize, $glowSize)

    # Islamic Gold Crescent Icon
    $crescentSize = [int]($size * 0.45)
    $cx = [int](($size - $crescentSize) / 2)
    $cy = [int](($size - $crescentSize) / 2)

    # Base gold circle
    $g.FillEllipse($goldBrush, $cx, $cy, $crescentSize, $crescentSize)
    # Masking circle for crescent
    $maskOffset = [int]($crescentSize * 0.3)
    $g.FillEllipse($bgBrush, ($cx + $maskOffset), ($cy - [int]($maskOffset * 0.2)), $crescentSize, $crescentSize)

    # Star / Dot
    $starSize = [int]($crescentSize * 0.22)
    $g.FillEllipse($goldBrush, ($cx + [int]($crescentSize * 0.85)), ($cy + [int]($crescentSize * 0.2)), $starSize, $starSize)

    # Save
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Created icon: $path ($size x $size)"
}

function Create-MishkatSplash([string]$path) {
    $bmp = New-Object System.Drawing.Bitmap(1920, 1080)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 15, 23, 42))
    $g.FillRectangle($bgBrush, 0, 0, 1920, 1080)

    # Center Brand Emblem
    $goldBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 245, 158, 11))
    $glowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(35, 245, 158, 11))
    
    $g.FillEllipse($glowBrush, 810, 390, 300, 300)
    $g.FillEllipse($goldBrush, 870, 450, 180, 180)
    $g.FillEllipse($bgBrush, 920, 435, 180, 180)
    $g.FillEllipse($goldBrush, 1020, 480, 36, 36)

    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Created splash: $path (1920 x 1080)"
}

# Output to tv-web, tizen, and webos
$targets = @("tv-web", "tizen", "webos")
foreach ($t in $targets) {
    Create-MishkatIcon "$t/icon.png" 192
    Create-MishkatIcon "$t/largeIcon.png" 300
    Create-MishkatSplash "$t/splash.png"
}
