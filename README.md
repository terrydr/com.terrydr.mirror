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