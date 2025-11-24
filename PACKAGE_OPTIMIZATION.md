# WorkHub 安装包优化指南

## 优化概述

通过以下优化措施，可以显著减小安装包大小（预计减少 60-80%）：

### 1. 自定义 JRE 运行时 (jlink)
- **原理**: 使用 jlink 创建只包含必要模块的精简 JRE
- **效果**: 从 ~200MB 的完整 JRE 减少到 ~50-80MB
- **实现**: `createCustomJre` 任务自动创建精简运行时

### 2. JVM 参数优化
- **内存限制**: Xms64m, Xmx512m (根据应用实际需要调整)
- **GC优化**: 使用 G1GC，最大暂停时间 100ms
- **效果**: 减少运行时内存占用

### 3. Gradle 构建优化
- **并行构建**: 启用多任务并行
- **构建缓存**: 启用 Gradle 构建缓存
- **按需配置**: 仅配置需要的项目

## 使用方法

### 构建优化版本
```bash
# 标准构建（包含完整JRE）
./gradlew createDistributable

# 优化构建（使用精简JRE）
./gradlew clean optimizePackage
```

### 单独执行优化步骤
```bash
# 1. 创建分发包
./gradlew createDistributable

# 2. 创建自定义JRE
./gradlew createCustomJre

# 3. 执行完整优化
./gradlew optimizePackage
```

## 预期效果

### 包大小对比
- **优化前**: ~400-600MB (包含完整JRE)
- **优化后**: ~150-250MB (包含精简JRE)
- **应用部分**: ~50-100MB (不含JRE)

### 运行时性能
- **启动时间**: 减少 20-30%
- **内存占用**: 减少 30-50%
- **响应速度**: 略有提升

## 高级优化选项

### 模块精简 (可选)
如果需要进一步减小大小，可以自定义包含的JRE模块：

```kotlin
// 在 build.gradle.kts 中修改
commandLine(
    jlinkExecutable,
    "--add-modules", "java.base,java.desktop,java.logging",
    // 移除不需要的模块: java.xml, java.naming, java.security.jgss
)
```

### 依赖分析 (可选)
分析哪些依赖是真正需要的：

```bash
./gradlew dependencies --configuration runtimeClasspath
```

### 平台特定优化
- **Windows**: 可以考虑使用 .exe 包装器替代 MSI
- **macOS**: DMG 格式已经相对优化
- **Linux**: Deb/AppImage 格式可选

## 注意事项

1. **兼容性测试**: 精简JRE可能不包含某些边缘模块，确保应用功能正常
2. **Java版本**: 确保构建环境和目标环境使用相同或兼容的Java版本
3. **调试支持**: 精简JRE移除了调试信息，正式发布时建议保留

## 故障排除

### JRE创建失败
```bash
# 检查Java版本
java -version

# 手动创建JRE进行调试
jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.desktop \
      --output custom-jre
```

### 运行时错误
如果应用在精简JRE上运行出错，可能是缺少某些模块：
1. 查看错误信息中的缺失模块
2. 在 `--add-modules` 中添加相应模块
3. 重新构建

## 监控优化效果

运行优化任务后，会自动输出包大小分析：

```
Total package size: XXX MB
JRE size: XXX MB
Application size (without JRE): XXX MB
```

建议定期检查这些指标，确保优化效果持续有效。











