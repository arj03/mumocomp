var update_mobile_playback = function() {
    $("#img").attr("src", mobile_playback.art);
    $("#playback-status").html(mobile_playback.status);
    $("#playback-info").html(mobile_playback.track);
    if (mobile_playback.paused || !mobile_playback.playing)
	$("#playback-toggle .ui-btn-text").text("play");
    else
	$("#playback-toggle .ui-btn-text").text("pause");
};

$(document).ready(function() { update_mobile_playback(); });
