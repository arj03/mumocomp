var play_track = function (name) {
    $.post("/audio/control", {"type": "", "name": name, "command": "play-track"});
};
