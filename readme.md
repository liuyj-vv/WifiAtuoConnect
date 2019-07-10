# 知识
[Netd是Android系统中专门负责网络管理和控制的后台daemon程序]

# service和intentService
# 创建service
# wifi广播分析

# 自启动问题
假如APP没有Activity，只有Service，或者安装完毕后无法手动运行，也不能通过其他的APP来启动此APP的Service或者Activity，如何安装后，可以开机启动呢？ PS：在可以修改Android系统代码的基础上，又有哪些方法呢？

APP接收不到BOOT_COMPLETED广播可能的原因，有以下几种：

1. BOOT_COMPLETED对应的action和uses-permission没有一起添加
2. 应用安装到了sd卡内，安装在sd卡内的应用是收不到BOOT_COMPLETED广播的
3. 系统开启了Fast Boot模式，这种模式下系统启动并不会发送BOOT_COMPLETED广播
4. 应用程序安装后重来没有启动过，这种情况下应用程序接收不到任何广播，包括BOOT_COMPLETED、ACTION_PACKAGE_ADDED、CONNECTIVITY_ACTION等等。

> Android3.1之后，系统为了加强了安全性控制，应用程序安装后或是(设置)应用管理中被强制关闭后处于stopped状态，在这种状态下接收不到任何广播，除非广播带有FLAG_INCLUDE_STOPPED_PACKAGES标志，而默认所有系统广播都是FLAG_EXCLUDE_STOPPED_PACKAGES的，所以就没法通过系统广播自启动了。
所以Android3.1之后:
>>应用程序无法在安装后自己启动
没有ui的程序必须通过其他应用激活才能启动，如它的Activity、Service、Content Provider被其他应用调用。
不过，存在一种例外，就是应用程序被adb push you.apk /system/app/下是会自动启动的，不处于stopped状态。

也就是说，解决方式就是将APK推送到/system/app目录下,或者打包系统时，将APK放置到/system/app中打包

