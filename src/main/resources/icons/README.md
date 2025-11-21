# 应用图标说明

基于 `icon.png` (500x500) 生成的各种平台图标文件。

## 生成的文件

### macOS 图标
- `icon.icns` - macOS 应用图标（包含多个尺寸）
- `icon.iconset/` - macOS iconset 目录，包含以下尺寸：
  - 16x16, 32x32, 64x64, 128x128, 256x256, 512x512, 1024x1024

### Windows 图标
- `icon.ico` - Windows 应用图标（包含 16x16, 32x32, 48x48, 256x256）

### 托盘图标
- `tray_icon.png` (16x16) - 默认托盘图标
- `tray_icon_16x16.png` (16x16)
- `tray_icon_20x20.png` (20x20)
- `tray_icon_24x24.png` (24x24)
- `tray_icon_32.png` (32x32)
- `tray_icon_32x32.png` (32x32)
- `tray_icon_48x48.png` (48x48)
- `tray_icon_64x64.png` (64x64)

### 任务栏图标
- `taskbar_icon_16.png` (16x16)
- `taskbar_icon_24.png` (24x24)
- `taskbar_icon_32.png` (32x32)
- `taskbar_icon_48.png` (48x48)
- `taskbar_icon_64.png` (64x64)

## 使用建议

### macOS
- 应用图标：使用 `icon.icns`
- 托盘图标：推荐 `tray_icon_32x32.png` 或 `tray_icon_24x24.png`
- 任务栏：推荐 `taskbar_icon_32.png` 或 `taskbar_icon_48.png`

### Windows
- 应用图标：使用 `icon.ico`
- 托盘图标：推荐 `tray_icon_16x16.png` 或 `tray_icon_24x24.png`
- 任务栏：推荐 `taskbar_icon_32.png`

### 通用
- 窗口最小化图标：使用 16x16 或 24x24 尺寸
- 高DPI显示器：优先选择较大尺寸的图标

## 重新生成

如需重新生成所有图标，运行：

```bash
python3 generate_icons.py
```

脚本会自动从 `../icon.png` 生成所有需要的图标格式。
