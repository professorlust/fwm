// this is our currently shown index.
var currentIndex = 0;

// these are our default class, and index.
var objClass = 0;
var objIndex;

alert('we got called');
var asdf = "/webservice1_0bs/getPrev/";

$(document).ready(function() {
	actualFixImage();
	$.ajaxSetup({ cache: true });
	showDefault();
	setInterval(function() {
		$.ajax({
			url: "/webservice1_0bs/getCurrent/",
			type: 'GET',
			contentType : "application/json",
			dataType: 'json',
			success: function(data) {
				// method checks to see if anything new has been pushed, and sets us to that if it has. 
				checkDefault(data);
			},
			fail: function(data) {
				console.log("error");
			}
		});
	}, 2000);
});

function showDefault(){
	$.ajax({
		url: "/webservice1_0bs/getCurrent",
		method: 'GET',
		success: function(data){
			objIndex = data['ObjectId'];
			objClass = data['ObjectClass'];
			currentIndex = data['CurrentIndex'];
			showElement(data);
			actualFixImage();
		}
	});
}

function checkDefault(obj){
	if(objIndex != obj['ObjectId'] && objClass != obj['ObjectClass']){
		showDefault();
	}
	objIndex = obj['ObjectId'];
	objClass = obj['ObjectClass'];
}
	
function showElement(obj){
	currentIndex = obj['CurrentIndex'];
	$('#desc').html(obj['Description']);
	$('#name').html(obj['Name']);
	document.getElementById('img').src = "/webservice1_0bs/multimediaImage/" + obj['ImageFileName'];
}

function getPrev(){
	$.ajax({
		url: "/webservice1_0bs/getPrev/" + currentIndex,
		method: 'GET',
		success: function(data){
			currentIndex = data['CurrentIndex'];
			showElement(data);
			actualFixImage();
		}
	});
}

function getNext(){
	$.ajax({
		url: "/webservice1_0bs/getNext/" + currentIndex,
		method: 'GET',
		success: function(data){
			currentIndex = data['CurrentIndex'];
			showElement(data);
			actualFixImage();
		}
	});
}



function fixImageContainerWidthHeight(){
	var height = ($(window).height() - $('.navbar').height()) * 6.5 / 10;
	$('#imagecontainer').attr('height', height);
	$('#imagecontainer').attr('max-height', height);
}

function actualFixImage(){
	fixImageContainerWidthHeight();
	var imgContainer = $('#imagecontainer');
	calculateWidthHeightImg(imgContainer.width(), imgContainer.attr('height'), $('#img'));
}