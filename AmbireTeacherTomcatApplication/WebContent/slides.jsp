<!-- Copyright (C) 2012, SMART Technologies.
     All rights reserved.
  
     Redistribution and use in source and binary forms, with or without modification, are permitted
     provided that the following conditions are met:
   
      * Redistributions of source code must retain the above copyright notice, this list of
        conditions and the following disclaimer.
   
      * Redistributions in binary form must reproduce the above copyright notice, this list of
        conditions and the following disclaimer in the documentation and/or other materials
        provided with the distribution.
   
      * Neither the name of SMART Technologies nor the names of its contributors may be used to
         endorse or promote products derived from this software without specific prior written
         permission.
   
     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
     IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
     FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
     CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
     SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
     THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
     OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     POSSIBILITY OF SUCH DAMAGE.
   
     Author: Michael Boyle
-->
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
	background-color: white;
	color: #555;
	font-family: Calibri, Tahoma, sans-serif;
	font-size: 40px;
	margin: 0 0 0 0;
	padding: 0 0 0 0;
}
span#leftScrollButton {
	display: inline-block;
	width: 40px;
	height: 40px;
	cursor: pointer;
	background-size: contain;
	background-image: url(img/left.png)
}
span#leftScrollButton:hover {
	background-image: url(img/left-hot.png)
}
span#rightScrollButton {
	display: inline-block;
	width: 40px;
	height: 40px;
	cursor: pointer;
	background-size: contain;
	background-image: url(img/right.png);
}
span#rightScrollButton:hover {
	background-image: url(img/right-hot.png);
}
div#bottomButtons {
	position: absolute;
	right: 10px;
	bottom: 10px;
	width: 340px;
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
span#refreshButton {
	display: inline-block;
	width: 40px;
	height: 40px;
	cursor: pointer;
	background-image: url(img/refresh.png);
	background-size: contain;
}
span#refreshButton:hover {
	background-image: url(img/refresh-hot.png);
}
span#logoutButton {
	display: inline-block;
	width: 40px;
	height: 40px;
	cursor: pointer;
	margin-left: 40px;
	background-image: url(img/logout.png);
	background-size: contain;
}
span#logoutButton:hover {
	background-image: url(img/logout-hot.png);
}
div.slide {
	padding-top: 10px;
	display: block;
	position: absolute;
	left: 10px;
	right: 10px;
	top: 40px;
	bottom: 120px;
	overflow: hidden;
	background-repeat: no-repeat;
	background-position: center;
	background-image: url(img/ambire.jpg);
	background-size: contain;
	text-align: center;
}
span.slideCaption {
	color: #555;
	background-color: #ffe;
	padding-left: 5px;
	padding-right: 5px;
	border: 1px solid #555;
	visibility: hidden;
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
	display: inline-block;
	position: absolute;
	left: 10px;
	bottom: 10px;
	height: 80px;
	vertical-align: bottom;
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
	window.setInterval(refresh, POLLING_INTERVAL_MILLIS);
	window.setInterval(autoAdvance, ANIMATION_INTERVAL_MILLIS);
}

function showSlide(href, caption) {
	if(!g_animating && (href !== g_current)) {
		g_animating = true;
		$('#frontSlide').css('opacity', '0');
		$('#frontSlide').css('background-image', 'url(' + href + ')');
		if(caption) {
			$('#frontSlide > .slideCaption').text(caption);
			$('#frontSlide > .slideCaption').css('visibilty', 'visible');
		} else {
			$('#frontSlide > .slideCaption').css('visibilty', 'hidden');
		}
		$('#frontSlide').animate({ opacity: 1 }, 800);
		$('#backSlide').animate({ opacity: 0 }, {
			duration: 800,
			complete: function() {
				$('#backSlide').css('background-image', 'url(' + href + ')');
				$('#backSlide > .slideCaption').text(caption);
				if(caption) {
					$('#backSlide > .slideCaption').text(caption);
					$('#backSlide > .slideCaption').css('visibilty', 'visible');
				} else {
					$('#backSlide > .slideCaption').css('visibilty', 'hidden');
				}
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
	showSlide('img/ambire.jpg', null);
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

function update(data) {
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
        refresh();
    }
}

function refresh() {
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
            update(data);
        },
        error: function(err) {
            console.log(err);
        }
    });
}

function run() {
	showEmptySlide();
	refresh();	
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

function logout() {
	window.location = "logout";
}
</script>
</head>
<body>
<div id="frontSlide" class="slide"><span class="slideCaption">&nbsp;</span></div>
<div id="backSlide" class="slide"><span class="slideCaption">&nbsp;</span></div>
<div id="bottomButtons"><span id="leftScrollButton" onclick="prevSlide()"></span><span id="pauseButton" onclick="pause()"></span><span id="refreshButton" onclick="refresh()"></span><span id="playButton" onclick="play()"></span><span id="rightScrollButton" onclick="nextSlide()"></span><span id="logoutButton" onclick="logout()"></span></div>
<span id="pinDisplay">PIN: <%= session.getAttribute("pin") %></span>
<script type="text/javascript">
$(run);
</script>
</body>
</html>
