#!/usr/bin/env python3
"""
生成 macOS 和 Windows 应用图标
基于 src/main/resources/icon.png 生成所有需要的图标格式
只做简单的放大缩小处理，保持原始图标的完整性
"""

import os
import sys
from PIL import Image
import subprocess

def resize_image(input_path, output_path, size):
    """简单地调整图像大小到指定尺寸"""
    # 读取原始图像
    source_img = Image.open(input_path).convert('RGBA')

    # 调整大小
    resized_img = source_img.resize(size, Image.Resampling.LANCZOS)

    # 保存
    resized_img.save(output_path, 'PNG', optimize=True)
    print(f"✓ 生成: {output_path} ({size[0]}x{size[1]})")

def generate_macos_iconset(source_path, iconset_dir):
    """生成 macOS iconset 目录（简单缩放）"""
    # macOS 需要的图标尺寸
    sizes = [
        (16, 16, "icon_16x16.png"),
        (32, 32, "icon_16x16@2x.png"),
        (32, 32, "icon_32x32.png"),
        (64, 64, "icon_32x32@2x.png"),
        (128, 128, "icon_128x128.png"),
        (256, 256, "icon_128x128@2x.png"),
        (256, 256, "icon_256x256.png"),
        (512, 512, "icon_256x256@2x.png"),
        (512, 512, "icon_512x512.png"),
        (1024, 1024, "icon_512x512@2x.png"),
    ]

    # 创建 iconset 目录
    os.makedirs(iconset_dir, exist_ok=True)

    # 生成所有尺寸的图标
    for width, height, filename in sizes:
        output_path = os.path.join(iconset_dir, filename)
        resize_image(source_path, output_path, (width, height))

    print(f"\n✓ macOS iconset 已生成: {iconset_dir}")

def generate_macos_icns(iconset_dir, icns_path):
    """使用 iconutil 生成 .icns 文件"""
    try:
        # 使用 macOS 的 iconutil 命令生成 .icns
        subprocess.run(
            ['iconutil', '-c', 'icns', iconset_dir, '-o', icns_path],
            check=True,
            capture_output=True
        )
        print(f"✓ macOS .icns 文件已生成: {icns_path}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"✗ 生成 .icns 失败: {e}")
        print(f"  错误输出: {e.stderr.decode() if e.stderr else 'N/A'}")
        return False
    except FileNotFoundError:
        print("✗ 未找到 iconutil 命令（仅在 macOS 上可用）")
        print(f"  请手动运行: iconutil -c icns {iconset_dir} -o {icns_path}")
        return False

def generate_windows_ico(source_path, ico_path):
    """生成 Windows .ico 文件（包含多个尺寸）"""
    # Windows 需要的图标尺寸（按从大到小排序，确保第一个是最大的）
    sizes = [256, 48, 32, 16]

    # 创建图像列表
    images = []

    for size in sizes:
        # 创建临时图像（不保存到文件）
        img = Image.open(source_path).convert('RGBA')

        # 调整大小，使用高质量重采样
        img = img.resize((size, size), Image.Resampling.LANCZOS)

        images.append(img)

    # 保存为 .ico 文件（PIL 会自动处理多尺寸）
    # 使用第一个图像（最大的）作为主图像，其他作为附加图像
    images[0].save(
        ico_path,
        format='ICO',
        sizes=[(img.width, img.height) for img in images],
        append_images=images[1:] if len(images) > 1 else []
    )

    print(f"✓ Windows .ico 文件已生成: {ico_path} (包含 {len(images)} 个尺寸)")

def generate_tray_icons(source_path, output_dir):
    """生成多个尺寸的托盘图标"""
    # 常见托盘图标尺寸
    tray_sizes = [
        (16, 16, "tray_icon_16x16.png"),
        (20, 20, "tray_icon_20x20.png"),
        (24, 24, "tray_icon_24x24.png"),
        (32, 32, "tray_icon_32x32.png"),
        (48, 48, "tray_icon_48x48.png"),
        (64, 64, "tray_icon_64x64.png"),
    ]

    # 同时生成传统托盘图标文件
    legacy_sizes = [
        (16, 16, "tray_icon.png"),  # 默认16x16
        (32, 32, "tray_icon_32.png"),  # 32x32版本
    ]

    all_sizes = tray_sizes + legacy_sizes

    for width, height, filename in all_sizes:
        output_path = os.path.join(output_dir, filename)
        resize_image(source_path, output_path, (width, height))

    print(f"✓ 托盘图标已生成: {len(all_sizes)} 个尺寸")

def main():
    # 获取项目根目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = script_dir
    
    # 源图标路径
    source_icon = os.path.join(project_root, "src/main/resources/icon.png")
    
    if not os.path.exists(source_icon):
        print(f"✗ 错误: 找不到源图标文件: {source_icon}")
        sys.exit(1)
    
    print(f"📦 开始生成图标...")
    print(f"   源文件: {source_icon}\n")
    
    # 创建 icons 目录
    icons_dir = os.path.join(project_root, "src/main/resources/icons")
    os.makedirs(icons_dir, exist_ok=True)
    
    # 1. 生成 macOS iconset
    iconset_dir = os.path.join(icons_dir, "icon.iconset")
    print("1️⃣ 生成 macOS iconset...")
    generate_macos_iconset(source_icon, iconset_dir)

    # 2. 生成 macOS .icns 文件
    icns_path = os.path.join(icons_dir, "icon.icns")
    print("\n2️⃣ 生成 macOS .icns 文件...")
    generate_macos_icns(iconset_dir, icns_path)

    # 3. 生成 Windows .ico 文件
    ico_path = os.path.join(icons_dir, "icon.ico")
    print("\n3️⃣ 生成 Windows .ico 文件...")
    generate_windows_ico(source_icon, ico_path)

    # 4. 生成托盘图标（多种尺寸）
    print("\n4️⃣ 生成托盘图标...")
    generate_tray_icons(source_icon, project_root + "/src/main/resources")

    # 5. 生成任务栏和窗口图标
    print("\n5️⃣ 生成任务栏和窗口图标...")
    taskbar_sizes = [
        (16, 16, "taskbar_icon_16.png"),
        (24, 24, "taskbar_icon_24.png"),
        (32, 32, "taskbar_icon_32.png"),
        (48, 48, "taskbar_icon_48.png"),
        (64, 64, "taskbar_icon_64.png"),
    ]

    for width, height, filename in taskbar_sizes:
        output_path = os.path.join(project_root, f"src/main/resources/{filename}")
        resize_image(source_icon, output_path, (width, height))
    
    print("\n✅ 图标生成完成！")
    print(f"\n生成的文件:")

    # macOS 文件
    print(f"  📱 macOS:")
    print(f"    - iconset: {iconset_dir}/")
    print(f"    - .icns:   {icns_path}")

    # Windows 文件
    print(f"  🪟 Windows:")
    print(f"    - .ico:    {ico_path}")

    # 托盘图标
    print(f"  🔔 托盘图标:")
    tray_icons = [
        "tray_icon.png", "tray_icon_16x16.png", "tray_icon_20x20.png",
        "tray_icon_24x24.png", "tray_icon_32.png", "tray_icon_32x32.png",
        "tray_icon_48x48.png", "tray_icon_64x64.png"
    ]
    for icon in tray_icons:
        icon_path = os.path.join(project_root, f"src/main/resources/{icon}")
        if os.path.exists(icon_path):
            print(f"    - {icon}")

    # 任务栏图标
    print(f"  📋 任务栏图标:")
    taskbar_icons = [
        "taskbar_icon_16.png", "taskbar_icon_24.png", "taskbar_icon_32.png",
        "taskbar_icon_48.png", "taskbar_icon_64.png"
    ]
    for icon in taskbar_icons:
        icon_path = os.path.join(project_root, f"src/main/resources/{icon}")
        if os.path.exists(icon_path):
            print(f"    - {icon}")

    print(f"\n💡 使用提示:")
    print(f"  - macOS: 使用 icon.icns 作为应用图标")
    print(f"  - Windows: 使用 icon.ico 作为应用图标")
    print(f"  - 托盘: 根据需要选择合适的尺寸")
    print(f"  - 任务栏: 32x32 或 48x48 适合大多数情况")

if __name__ == "__main__":
    main()

