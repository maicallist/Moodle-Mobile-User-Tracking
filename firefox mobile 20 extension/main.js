//---------------save new open tab--------------------------------------
    var tabs = require("sdk/tabs");
    var windows = require("sdk/windows").browserWindows;
    var profilePath = require('sdk/system').pathFor('ProfD');
    var system = require("sdk/system");
    
    //tabs.open("http://www.unisa.edu.au");
    
    tabs.on('ready', function(tab) {
        var mydate = new Date();
        var strdate = mydate.getFullYear() + " " + (mydate.getMonth()+1) + " " + mydate.getDate() + " " + mydate.getHours() + ":" + mydate.getMinutes() + ":" + mydate.getSeconds(); 
        var oourl = tabs.activeTab.url + " " + strdate + " "+ system.id + " " + system.platform + " " + system.name + "\r\n";
        var fullPathToFile = "/mnt/sdcard/USQ_IMAGE//USQRecorders.txt";
        writeFile(oourl, fullPathToFile);
    });
    
    //---------------save data to file--------------------------------------
    function writeFile(str, path){

        //var fullPathToFile = "\\USQRecords.txt"; //system.id + "_" + system.platform + "_" + system.name
        const {Cc,Ci,Cm,Cr,Cu} = require("chrome");
        Cu.import("resource://gre/modules/FileUtils.jsm");
        
        var file = Cc["@mozilla.org/file/local;1"].createInstance(Ci.nsILocalFile);
        
        try{
            file.initWithPath(path);
            //if (file.exists() == false)file.create(Ci.nsIFile.NORMAL_FILE_TYPE, 420);
        
            var outputStream = Cc["@mozilla.org/network/file-output-stream;1"].createInstance(Ci.nsIFileOutputStream);
            var converter    = Cc["@mozilla.org/intl/converter-output-stream;1"].createInstance(Ci.nsIConverterOutputStream);
            
            outputStream.init(file, 0x04 | 0x08 | 0x10, 420, 0);
            converter.init(outputStream, "UTF-8", 0, 0);
            
            converter.writeString(str);     // save activeTab.url to file
            converter.close();
        }catch(e){
            console.log("write file failed")
        }
    }