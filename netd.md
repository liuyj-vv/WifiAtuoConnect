转： [!https://www.kancloud.cn/alex_wsc/android-wifi-nfc-gps/414018]

Netd是Android系统中专门负责网络管理和控制的后台daemon程序，其功能主要分三大块：

1. 设置防火墙（Firewall）、网络地址转换（NAT）、带宽控制、无线网卡软接入点（Soft Access Point）控制，网络设备绑定（Tether）等。
2. Android系统中DNS信息的缓存和管理。
3. 网络服务搜索（Net Service Discovery，简称NSD）功能，包括服务注册（Service Registration）、服务搜索（Service Browse）和服务名解析（Service Resolve）等。

>Netd的工作流程和Vold类似[1]，其工作可分成两部分：
>>1. Netd接收并处理来自Framework层中NetworkManagementService或NsdService的命令。这些命令最终由Netd中对应的Command对象去处理。
>>2. Net接收并解析来自Kernel的UEvent消息，然后再转发给Framework层中对应Service去处理。
 
 ------
 
>由上述内容可知，Netd位于Framework层和Kernel层之间，它是Android系统中网络相关消息和命令转发及处理的中枢模块。
Netd的代码量不大，难度较低，但其所涉及的相关背景知识却比较多。本章对Netd的分析将从以下几个方面入手：
>>* 首先介绍Netd的大体工作流程以及DNS、MDns相关的背景知识。关于Netd的工作流程分析，读者也可参考[1]中的内容。
>>* 本章将集中介绍Netd中涉及到的Android系统中网络管理和控制的相关工具。它们是iptables、tc和ip。
>>* 然后将介绍Netd中CommandListener的命令处理。这些命令的正常工作依赖于上面介绍的iptables等工具。
>>* 最后，我们将介绍Java Framework中的NetworkManagementService服务。
