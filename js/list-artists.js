var sort_func = function(lhs, rhs) {
    if (lhs.name.toLowerCase() < rhs.name.toLowerCase())
	return -1;
    else
	return 1;
};

var play_artist = function (name) {
    $.post("/control", {"type": "artist", "name": name, "command": "play"});
    showStatusMessage("playing artist tracks");    
};

var enqueue_artist = function (name) {
    $.post("/control", {"type": "artist", "name": name, "command": "enqueue"});
    showStatusMessage("adding artist tracks to playlist");    
};

var update_artists = function() {
    var t = "";
    artists.sort(sort_func);
    artists = $.map(artists, function(e, i) { e.encoded_name = encodeURIComponent(e.name); e.escaped_name = escape(e.name); return e; });

    $('#artists').empty();
    $('#artists-template').tmpl(artists).appendTo('#artists');
};

$(document).ready(function() { update_artists(); });
