#!/bin/bash

# 模拟networksetup -listallnetworkservices的输出，包含禁用服务
echo "An asterisk (*) denotes that a network service is disabled."
echo "Ethernet"
echo "*Disabled Service"
echo "Wi-Fi"
echo "*Another Disabled"
echo "VPN Connection"

# 我们的过滤逻辑：过滤掉以*开头或包含*的行
echo ""
echo "过滤后的结果："
echo "An asterisk (*) denotes that a network service is disabled.
Ethernet
*Disabled Service
Wi-Fi
*Another Disabled
VPN Connection" | grep -v '^\*' | grep -v '\*'
