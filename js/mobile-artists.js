$(document).ready(function() { 
   $("#reload").bind('vclick', function() { 
       $.post("/audio/control", {"type": "", "name": "", "command": "reload"});
       return false; 
   });
});
