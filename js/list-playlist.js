var remove_track = function (name) {
    $.post("/control", {"type": "", "name": name, "command": "remove"});
    showStatusMessage("removed track");    
};

var play_track = function (name) {
    $.post("/control", {"type": "", "name": name, "command": "play-track"});
};

var update_playlist = function() {
    playlist = $.map(playlist, function(e, i) { e.id = i; e.hid = i + 1; return e; });

    $('#playlist').empty();
    $('#playlist-template').tmpl(playlist).appendTo('#playlist');

    $("table.tracks a").addClass("black-link");

    $(".tracks tr:odd").addClass("odd");
};

$(document).ready(function() { update_playlist(); });
