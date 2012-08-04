function showStatusMessage(text)
{
    if($("#statusMessage").length < 1)
    {
        //If the message div doesn't exist, create it
        $("body").append("<div id='statusMessage' style='text-align:center;vertical-align:middle;position:fixed;top:0px;left:300px;border:1px solid black;background-color:#98AFC7;margin:0px;padding:7px;display:none'>" + text + "</div>");
    }
    else
    {
        //Else, update the text
        $("#statusMessage").html(text);
    }

    $("#statusMessage").slideDown();
    setTimeout('$("#statusMessage").slideUp()', 3000);
}
