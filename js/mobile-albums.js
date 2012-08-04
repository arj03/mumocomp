var inited = false;
var album = "";

var enqueue_album = function (name) {
    $.post("/audio/control", {"type": "album", "name": name, "command": "enqueue"});
    showStatusMessage("added album tracks to playlist");    
}; 

var play_album = function (name) {
    $.post("/audio/control", {"type": "album", "name": name, "command": "play"});
}; 

$(document).live("pageinit", function(event) {
   if (inited)
      return;

   inited = true;

   $(".go").live("taphold", function() { album = $(this).prop('name'); $.mobile.changePage($("#dialog")); });
   $("#enqueue-album-ok").bind('vclick', function() { enqueue_album(album); });
   $(".play").bind('vclick', function() { play_album($(this).prop('name')); });
}); 
