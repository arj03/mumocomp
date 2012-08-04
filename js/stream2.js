function onData(req, text) {
    if (text == "close\n") {
	req.abort();
	console.log("finished, ABORTING req");
    }
    else
	console.log(text);
}

function fnNull() { };

// do cancel do http_request.abort()
// this also allows on to call multiple times

function makeRequest(url) {
    var http_request = new XMLHttpRequest();

    if (http_request.overrideMimeType)
	http_request.overrideMimeType('text/xml');

    if (!http_request) {
	alert('Cannot create XMLHTTP instance');
	return false;
    }

    var len = 0;

    http_request.onreadystatechange = function() {
	if (http_request.status == 200 && http_request.readyState >=3) {
	    var text = http_request.responseText;
	    text = text.substr(len, text.length-len);
	    len = http_request.responseText.length;
	    onData(http_request, text);
	}
	if (http_request.readyState == 4) {
            http_request.onreadystatechange = fnNull;
	}
    };

    http_request.open('GET', url, true);
    http_request.send(null);

    return http_request;
}
