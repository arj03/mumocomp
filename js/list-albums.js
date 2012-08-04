var sort_func_albums = function(lhs, rhs) {
    if (lhs.year > rhs.year)
	return -1;
    else
	return 1;
};

var update_info = function (name) {
    $.post("/control", {"type": "album", "name": name, "command": "update-album"});
    showStatusMessage("updating album info");
}; 

var play_album = function (name) {
    $.post("/control", {"type": "album", "name": name, "command": "play"});
    showStatusMessage("playing album tracks");    
}; 

var enqueue_albums = function (name) {
    $.post("/control", {"type": "album", "name": name, "command": "enqueue"});
    showStatusMessage("adding album tracks to playlist");    
}; 

var enqueue_loved_album = function (name) {
    $.post("/control", {"type": "love", "name": name, "command": "enqueue-album"});
    showStatusMessage("adding loved tracks from album to playlist");    
};

var update_albums = function() {
    albums.sort(sort_func_albums);
    albums = $.map(albums, function(e, i) { e.encoded_name = escape(e.name); return e; });
    $('#albums').empty();
    $('#albums-template').tmpl(albums).appendTo('#albums');
};

$(document).ready(function() { update_albums(); });
