(function($){
  var failed_tries = 0;

  $.comet = function(updatefunc, id) {
    var error = false;
    $.ajax({
      type: "GET",
      cache: false,
      url: "/activity/" + id,
      success: function(data) {
	  var json_data = $.evalJSON(data);
	  if (data && !json_data.msg)
	      updatefunc(data);

	  failed_tries = 0;
	  $.comet(updatefunc, id);
      },
      error: function() {
	  failed_tries += 1;
	  if (failed_tries < 4) {
	      setTimeout(function() {
			     $.comet(updatefunc, id);
                         }, 2*failed_tries*1000);
	  }
      }
    });
  };
})(jQuery);
