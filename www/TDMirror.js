var tdMirror= {
tdTakePhotos: function(takeType, successCallback, errorCallback) {
    console.log("tdTakePhotos");
    cordova.exec(
                 successCallback,
                 errorCallback,
                 "TDMirror",
                 "tdEyeTakePhotos",
                 [takeType]
                 );
    
},
 
tdSelectPhotos: function(successCallback, errorCallback) {
    console.log("tdSelectPhotos");
    cordova.exec(
                 successCallback,
                 errorCallback,
                 "TDMirror",
                 "tdEyeSelectPhotos",
                 []
                 );
    
},
               
tdScanPhotos: function(paramDic,successCallback, errorCallback) {
    console.log("tdScanPhotos");
    cordova.exec(
                 successCallback,
                 errorCallback,
                 "TDMirror",
                 "tdEyeScanPhotos",
                 [paramDic]
                 );
    
}
    
}

module.exports = tdMirror;