var last_update_playing = false;
var last_update_index = -1;
var last_pause_state = -1;
var last_love_state = -1;
var last_art = "";

var update_playback = function() {

    $("#playback").html(playback.status);

    var pause_state = playback.status.indexOf("Paused:") != -1;

    if (playback.status != "Not playing :(" && (!last_update_playing
       || pause_state != last_pause_state || playback.love != last_love_state 
       || playback.art != last_art))
    {
        playback.pause = pause_state;

	$('#playback-control').empty();
        $('#playback-template').tmpl(playback).appendTo('#playback-control');

	if (playback.art)
	    $("#playback-art").html("<img src=\"" + playback.art + "\">");
	else
	    $("#playback-art").html("");

	$("a.pause_playback").click(
	    function () {
		$.post("/control", {"type": "", "name": "", "command": "pause"});
		return false;
	    }); 
    
	$("a.stop_playback").click(
	    function () {
		$.post("/control", {"type": "", "name": "", "command": "stop"});
		showStatusMessage("stopping playback");    
		return false;
	    }); 

	$("a.prev_playback").click(
	    function () {
		$.post("/control", {"type": "", "name": "", "command": "prev-track"});
		return false;
	    }); 

	$("a.next_playback").click(
	    function () {
		$.post("/control", {"type": "", "name": "", "command": "next-track"});
		return false;
	    }); 

	$("a.love_track").click(
	    function () {
		$.post("/control", {"type": "", "name": "", "command": "love-track"});
		showStatusMessage("loving track");    
		return false;
	    }); 
    }

    if ($("#playlist") && ($(".tracks tr.playing").length == 0 ||
			   last_update_index != playback.index)) {
	$(".tracks tr").removeClass("playing");
	$(".tracks tr:nth-child(" + (playback.index + 1) + ")").addClass("playing");
    }

    if (playback.status == "Not playing :(") {
	$(".tracks tr").removeClass("playing");
	$("#playback-control").html("");
    }

    last_update_index = playback.index;
    last_update_playing = playback.status != "Not playing :(";
    last_love_state = playback.love;
    last_pause_state = pause_state;
    last_art = playback.art;
};

$(document).ready(function() { update_playback(); });
