function xmlHttpRequest() {
    return (function (x,y,i) {
		if (x) 
		    return new x();
		for (i=0; i<y.length; y++) 
		    try { 
			return new ActiveXObject(y[i]);
		    } catch (e) {}
	    })(
	window.XMLHttpRequest, 
	['Msxml2.XMLHTTP','Microsoft.XMLHTTP']
    );
}

function onData(text) {
    // do it here
    if (text)
	console.log(text);
}

function stream(url, onDataCallback) {
    var xmlHttp = xmlHttpRequest();
    xmlHttp.open("GET", url, true);
    var len = 0;
    xmlHttp.onreadystatechange = function() {
	if (xmlHttp.status == 200 && xmlHttp.readyState >=3) {
	    var text = xmlHttp.responseText;
	    text = text.substr(len, text.length-len);
	    len = xmlHttp.responseText.length;
	    onDataCallback(text);
	}
	if (xmlHttp.readyState == 4) {
            xmlHttp.onreadystatechange = fnNull;
	    xmlHttp.abort();
	}
    }
    xmlHttp.send(null);
}

function fnNull() { };

