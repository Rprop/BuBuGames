# 正式包用 R8 压缩。保留行号，崩溃栈还能对上源码。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 扫码库通过反射找界面和相机回调。
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
