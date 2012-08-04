$(document).bind("pageinit", function(event) {
   $("#playback-prev").bind('vclick', function() { 
      $.post("/audio/control", {"type": "", "name": "", "command": "prev-track"});
      return false; 
   });

   $("#playback-next").bind('vclick', function() { 
      $.post("/audio/control", {"type": "", "name": "", "command": "next-track"});
      return false; 
   });

   $("#playback-toggle").bind('vclick', function() { 
      if (mobile_playback.playing)
          $.post("/audio/control", {"type": "", "name": "", "command": "pause"});
      else
          $.post("/audio/control", {"type": "", "name": "", "command": "play"});
      return false; 
   });

   $("#playback-stop")..bind('vclick', function() { 
      $.post("/audio/control", {"type": "", "name": "", "command": "stop"});
      return false; 
   });

   $("#playback-love")..bind('vclick', function() { 
      $.post("/audio/control", {"type": "", "name": "", "command": "love-track"});
      return false; 
   });
});
