var tdMirror= {
tdTakePhotos: function(takeType, successCallback, errorCallback) {
    console.log("tdTakePhotos");
    cordova.exec(
                 successCallback,
                 errorCallback,
                 "TDMirror",
                 "tdMirrorTakePhotos",
                 [takeType]
                 );
    
},
 
tdSelectPhotos: function(successCallback, errorCallback) {
    console.log("tdSelectPhotos");
    cordova.exec(
                 successCallback,
                 errorCallback,
                 "TDMirror",
                 "tdMirrorSelectPhotos",
                 []
                 );
    
},
               
tdScanPhotos: function(paramDic,successCallback, errorCallback) {
    console.log("tdScanPhotos");
    cordova.exec(
                 successCallback,
                 errorCallback,
                 "TDMirror",
                 "tdMirrorScanPhotos",
                 [paramDic]
                 );
    
}
    
}

module.exports = tdMirror;