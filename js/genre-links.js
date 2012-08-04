$(document).ready(
    function() { 
	$('#genre-template').tmpl({ id : escape(genre) }).appendTo('#play-enqueue-links');  

	$("a.playgenre").click(
	    function () {
		$.post("/control", {"type": "genre", "name": this.id, "command": "play"});
		return false;
	    }); 
		      
	$("a.enqueuegenre").click(
	    function () {
		$.post("/control", {"type": "genre", "name": this.id, "command": "enqueue"});
		return false;
	    }); 

	$("a.enqueueloved").click(
	    function () {
		$.post("/control", {"type": "love", "name": this.id, "command": "enqueue-genre"});
		return false;
	    }); 
    }
);
    

