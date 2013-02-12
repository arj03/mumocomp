$(document).bind("pageinit", function(event) {
   $("#playback-reverse").bind('vclick', function() { 
      $.post("/movie/control", {"type": "", "name": "", "command": "reverse"});
      return false; 
   });

   $("#playback-forward").bind('vclick', function() { 
      $.post("/movie/control", {"type": "", "name": "", "command": "forward"});
      return false; 
   });

   $("#playback-toggle").bind('vclick', function() { 
      $.post("/movie/control", {"type": "", "name": "", "command": "pause"});
      return false; 
   });

   $("#playback-stop").bind('vclick', function() { 
      $.post("/movie/control", {"type": "", "name": "", "command": "stop"});
      return false; 
   });

   $("#playback-osd").bind('vclick', function() { 
      $.post("/movie/control", {"type": "", "name": "", "command": "osd"});
      return false; 
   });
});
