var update_mobile_playlist = function() {
    var index = mobile_playlist["index"];
    $("li").removeClass("ui-btn-active");
    $("li:nth-child(" + (index + 1) + ")").addClass("ui-btn-active");
};

$(document).ready(function() { update_mobile_playlist(); });
