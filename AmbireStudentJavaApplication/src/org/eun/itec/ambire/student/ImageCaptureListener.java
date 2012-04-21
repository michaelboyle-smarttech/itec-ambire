package org.eun.itec.ambire.student;

import java.awt.Image;

public interface ImageCaptureListener {
	public void imageCaptured(Image img);
	public void captureCancelled();
}
