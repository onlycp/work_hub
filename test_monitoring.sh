#!/bin/bash

echo "=== 系统监控测试脚本 ==="

echo -e "\n1. 测试CPU信息:"
top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1}'

echo -e "\n2. 测试负载平均值:"
uptime | awk -F'load average:' '{print $2}' | cut -d',' -f1 | tr -d ' '

echo -e "\n3. 测试内存信息:"
free -m | awk 'NR==2{printf "%.2f %.0f %.0f", $3*100/$2, $3, $2}'

echo -e "\n4. 测试磁盘信息:"
df -h / | awk 'NR==2{printf "%.2f %s %s", $5, $3, $2}' | tr -d '%'

echo -e "\n5. 测试网络信息:"
cat /proc/net/dev | awk 'NR>2 {sum_rx += $2; sum_tx += $10} END {printf "%.0f %.0f", sum_rx/1024/1024, sum_tx/1024/1024}'

echo -e "\n6. 测试系统信息:"
echo "$(uptime -p 2>/dev/null || echo '未知') $(ps aux 2>/dev/null | wc -l 2>/dev/null || echo '0')"

echo -e "\n=== 测试完成 ==="
