$(document).ready(
    function() { 
	$('a[href^="/movie/reload"]').bind('vclick', function () {
		$.post("/movie/control", {"type": "", "name": "", "command": "reload"});
		showStatusMessage("reloading folders");
		return false;
	    }); 

	$(".file").bind('vclick', function (e) {
		$.post("/movie/control", {"type": "", "name": $(this).attr('href').substr(7), "command": "play-movie"});
		showStatusMessage("starting playback");
		return false;
	    });

	var path = "";
	$(".go").live("taphold", function() { path = $(this).prop('name'); $.mobile.changePage($("#dialog")); });
	$("#update-internet-info-ok").click(function() { 
	    $.post("/movie/control", {"type": "", "name": path, "command": "update-internet-info"});
	    showStatusMessage("updating movie information");
	});
    });
