// do images and stuff. 
// takes in the width & height of a container, and the image as a $(jquery) thing. 
function calculateWidthHeightImg(width, height, image){
	that = image;
	var img = document.getElementById(image.attr('id'));
	that = img;
	var imgWidth = img.naturalWidth;
	var imgHeight = img.naturalHeight;
	var rel = width/height;
	var imgRel = imgWidth/imgHeight;
	if(imgRel > rel){
		// set to max width.
		if(imgWidth > width){
			image.attr('width', width);
			image.attr('height', width / imgWidth * imgHeight);
		}
		else
		{
			// do nothing our image should already fit. 
		}
	}
	else{
		// set to max height. 
		if(imgHeight > height){
			image.attr('height', height);
			image.attr('width', height / imgHeight * imgWidth);
		}else
		{
			// do nothing our image should already fit. 
		}
	}
}
