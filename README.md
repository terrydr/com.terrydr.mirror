cordova-plugin-mirrorcamera
-------------------------------
####泰瑞镜下拍摄相机 cordova插件

支持平台：Android

安装：cordova plugin add https://github.com/terrydr/com.terrydr.mirror.git

卸载：cordova plugin rm cordova-plugin-mirrorcamera

示例:

        tdmirror.tdTakePhotos('before', function(result) {
                
        }, function(error) {
                console.log(error);
        });
		
使用说明：

1.必须引用插件资源依赖库：src/android/TerrydrResour

2.将依赖库拷贝到：项目工程路径/platforms/android/

3.修改项目工程路径/platforms/android/project.properties文件

      添加引用依赖代码 ：android.library.reference.1=TerrydrResource
		
