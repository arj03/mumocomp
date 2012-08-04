var enqueue_tracks = function (name) {
    $.post("/control", {"type": "track", "name": name, "command": "enqueue"});
    showStatusMessage("added track to playlist");
};

var update_tracks = function() {
    tracks = $.map(tracks, function(t, j) { 
		t.tracks = $.map(t.tracks, function(e, i) { e.escaped_id = escape(e.id); return e; });
		return t;
	      });

    $('#tracks').empty();
    $('#tracks-template').tmpl(tracks).appendTo('#tracks');

    $(".tracks tr:odd").addClass("odd");
    $(".tracks tr.header").removeClass("odd");
};

$(document).ready(function() { update_tracks(); });
