var sort_func = function(lhs, rhs) {
    if (lhs.tag.toLowerCase() < rhs.tag.toLowerCase())
	return -1;
    else
	return 1;
};

var update_genres = function() {
    $("#genres").tagCloud(genres, { sort: sort_func, 
				    click: function(tag, event) { window.location = "/genre/" + tag; }, 
				    maxFontSizeEm: 2 });
};

$(document).ready(function() { update_genres(); });
