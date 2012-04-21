<%@ page %>
<% if(session.getAttribute("sid") == null) {
	response.sendRedirect("/");
	return;
}
%>
<!DOCTYPE html>
<html>
<head>
<title>Ambire</title>
<style type="text/css">
body {
	background-color: Black;
	margin: 0 0 0 0;
	padding: 0 0 0 0;
}
span#leftScrollButton {
	position: fixed;
	left: 0px;
	width: 80px;
	height: 80px;
	top: 50%;
	margin-top: -40px;
	cursor: pointer;
	background-image: url(img/left.png)
}
span#leftScrollButton:hover {
	background-image: url(img/left-hot.png)
}
span#rightScrollButton {
	position: absolute;
	right: 0px;
	width: 80px;
	height: 80px;
	top: 50%;
	margin-top: -40px;
	cursor: pointer;
	background-image: url(img/right.png);
}
span#rightScrollButton:hover {
	background-image: url(img/right-hot.png);
}
div#bottomButtons {
	position: absolute;
	right: 10px;
	bottom: 10px;
	width: 120px;
	height: 60px;
}
span#pauseButton {
	display: inline-block;
	width: 40px;
	height: 40px;
	cursor: pointer;
	background-image: url(img/pause.png);
	background-size: contain;
}
span#pauseButton:hover {
	background-image: url(img/pause-hot.png)
}
span#playButton {
	display: inline-block;
	width: 40px;
	height: 40px;
	cursor: pointer;
	background-image: url(img/play.png);
	background-size: contain;
}
span#playButton:hover {
	background-image: url(img/play-hot.png);
}
div.slide {
	display: block;
	position: absolute;
	left: 80px;
	right: 80px;
	top: 40px;
	bottom: 120px;
	overflow: hidden;
	text-align: center;
	vertical-align: bottom;
	font-family: Tahoma;
	font-size: 55px;
	color: White;
	background-repeat: no-repeat;
	background-position: center;
	background-image: url(img/ambire.jpg);
	background-size: contain;
	text-shadow: 5px 5px  #333;
}
div#frontSlide {
	opacity: 0;
	z-index: 1;
}
div#backSlide {
	opacity: 1;
	z-index: -1;
}
span#pinDisplay {
	font-family: Tahoma;
	font-size: 55px;
	color: White;
	display: inline-block;
	position: absolute;
	left: 10px;
	bottom: 10px;
	height: 80px;
	vertical-align: bottom;
}
a.logoutLink {
	margin-left: 80px;
	color: Silver;
	text-decoration: none;
	border: none;
}
a.logoutLink:hover {
	color: CornflowerBlue;
	text-decoration: underline;
}
a img {
	border: none;
}
</style>
<script type="text/javascript" src="js/jquery-1.7.2.min.js"></script>
<script type="text/javascript">
"use strict";
var POLLING_INTERVAL_MILLIS = 18000;
var ANIMATION_INTERVAL_MILLIS = 6000;
var KEEP_ALIVE_INTERVAL_MILLIS = POLLING_INTERVAL_MILLIS * 20;
var g_uploads = new Array();
var g_index = -1;
var g_polling = false;
var g_repoll = false;
var g_firstRun = true;
var g_animating = false;
var g_play = true;
var g_autoAdvance = false;
var g_current = null;
var g_keepalive = new Date();

function firstRun() {
	window.setInterval(beginPoll, POLLING_INTERVAL_MILLIS);
	window.setInterval(autoAdvance, ANIMATION_INTERVAL_MILLIS);
}

function showSlide(href, caption) {
	if(!g_animating && (href !== g_current)) {
		g_animating = true;
		$('#frontSlide').css('opacity', '0');
		$('#frontSlide').css('background-image', 'url(' + href + ')');
		$('#frontSlide').text(caption);
		$('#frontSlide').animate({ opacity: 1 }, 800);
		$('#backSlide').animate({ opacity: 0 }, {
			duration: 800,
			complete: function() {
				$('#backSlide').css('background-image', 'url(' + href + ')');
				$('#backSlide').text(caption);
				$('#backSlide').css('opacity', '1');
				$('#frontSlide').css('opacity', '0');
				g_animating = false;
				g_current = href;
			}
		});
	}
}

function showEmptySlide() {
	g_index = -1;
	showSlide('img/ambire.jpg', 'Ambire');
}

function showCurrentSlide() {
	showSlide(g_uploads[g_index].href, g_uploads[g_index].name);
}

function nextSlide() {
	g_autoAdvance = false;
	if(g_uploads.length == 0) {
		showEmptySlide();
	} else {
		g_index = (g_index + 1) % g_uploads.length;
		showCurrentSlide();
	}
}

function prevSlide() {
	g_autoAdvance = false;
	if(g_uploads.length == 0) {
		showEmptySlide();
	} else {
		g_index = (g_index + g_uploads.length - 1) % g_uploads.length;
		showCurrentSlide();
	}
}

function autoAdvance() {
	if(!g_play) {
		g_autoAdvance = true;
	} else {
		nextSlide();
	}
}

function sync(settings) {
    var ops = new Array();
    for(var i = 0; i < settings.prev.length;) {
        var o = settings.prev[i];
        var found = false;
        for(var j = 0; j < settings.next.length; ++j) {
            if(settings.equals(o, settings.next[j])) {
                found = true;
                break;
            }
        }
        if(!found) {
            settings.prev.splice(i,1);
            if(settings.removed) {
	            settings.removed(o,i);
            }
        } else {
            ++i;
        }
    }
    for(var i = 0; i < settings.next.length; ++i) {
        var o = settings.next[i];
        var found = false;
        var j;
        for(j = 0; j < settings.prev.length; ++j) {
            if(settings.equals(o, settings.prev[j])) {
                found = true;
                break;
            }
        }
        if(!found) {
        	j = settings.prev.length;
            settings.prev.push(o);
            if(settings.added) {
	            settings.added(o,j);
            }
        } else if(settings.unchanged) {
        	settings.unchanged(o,j);
        }
    }
}

function endPoll(data) {
    sync({
        prev: g_uploads,
        next: data,
        equals: function(a,b) {
            return (a.uploadId === b.uploadId) && (a.href === b.href);
        },
        added: function(o,i) {
        	if(i <= g_index) {
        		g_index++;
        	}
        },
        removed: function(o,i) {
        	if(i <= g_index) {
        		g_index--;
        	}
        }
    });
	if(g_uploads.length > 0) {
		if(g_index < 0 || g_index >= g_uploads.length) {
			g_index = 0;
		}
		showCurrentSlide();
	} else {
		showEmptySlide();
	}
    if(g_firstRun) {
        g_firstRun = false;
        firstRun();
    }
    var repoll = g_repoll;
    g_repoll = false;
    g_polling = false;
    if(repoll) {
        beginPoll();
    }
}

function beginPoll() {
    if(g_polling) {
        g_repoll = true;
        return;
    } else {
        g_polling = true;
    }
    var u = "select";
    var n = new Date();
    if((n.valueOf() - g_keepalive.valueOf()) >= KEEP_ALIVE_INTERVAL_MILLIS) {
    	g_keepalive = n;
    	u += "?keepalive=1";
    }
    $.ajax({
        url: u,
        success: function(data) {
            endPoll(data);
        },
        error: function(err) {
            console.log(err);
        }
    });
}

function run() {
	showEmptySlide();
	beginPoll();	
}

function pause() {
	g_play = false;
}

function play() {
	g_play = true;
	if(g_autoAdvance) {
		autoAdvance();
	}
}
</script>
</head>
<body>
<div id="frontSlide" class="slide"></div>
<div id="backSlide" class="slide"></div>
<span id="leftScrollButton" onclick="prevSlide()"></span>
<span id="rightScrollButton" onclick="nextSlide()"></span>
<div id="bottomButtons"><span id="pauseButton" onclick="pause()"></span><span id="playButton" onclick="play()"></span></div>
<span id="pinDisplay">PIN: <%= session.getAttribute("pin") %><a class="logoutLink" href="logout"><img src="img/logout.png" width="80" height="80" /> Logout</a></span>
<script type="text/javascript">
$(run);
</script>
</body>
</html>
