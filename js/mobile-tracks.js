var enqueue_track = function (name) {
    $.post("/audio/control", {"type": "track", "name": name, "command": "enqueue"});
// FIXME
//    showStatusMessage("added track to playlist");
};
