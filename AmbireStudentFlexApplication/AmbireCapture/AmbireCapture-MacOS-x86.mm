/*   Copyright (C) 2012, SMART Technologies.
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
*/
#import <CoreFoundation/CoreFoundation.h>
#import <ApplicationServices/ApplicationServices.h>
#import <Adobe AIR/Adobe AIR.h>
#include <algorithm>

extern "C" {

__attribute__((visibility("default")))
FREObject AmbireCaptureGetScreenSize(FREContext ctx, void * functionData, uint32_t argc, FREObject * argv) {
	CGDirectDisplayID display = CGMainDisplayID();
	int W = (int)CGDisplayPixelsWide(display);
	int H = (int)CGDisplayPixelsHigh(display);
	int N = W | (H << 16);
	FREObject rv = 0;
	FRENewObjectFromInt32(N, &rv);
	return rv;
}

__attribute__((visibility("default")))
FREObject AmbireCaptureCapture(FREContext ctx, void * functionData, uint32_t argc, FREObject * argv) {
	bool success = false;
	FREBitmapData bd = { 0 };
	FREResult result = FREAcquireBitmapData(argv[0], &bd);
	if(result == FRE_OK) {
		int w = (int)bd.width;
		int h = (int)bd.height;
		CGDirectDisplayID display = CGMainDisplayID();
		CGImageRef image = CGDisplayCreateImage(display);
		if(image) {
			int W = (int)CGImageGetWidth(image);
			int H = (int)CGImageGetHeight(image);
			int bpp = (int)CGImageGetBitsPerPixel(image);
			int stride = (int)CGImageGetBytesPerRow(image);
			CGDataProviderRef provider = CGImageGetDataProvider(image);
			if(provider) {
				CFDataRef data = CGDataProviderCopyData(provider);
				if(data) {
					const UInt8 * ptr = CFDataGetBytePtr(data);
					if(ptr) {
						if(bpp == 32) {
							memmove(bd.bits32, ptr, std::min<int>(stride * H, bd.lineStride32 * 4 * h));
						} else if(bpp == 24) {
							for(int y = 0; y < std::min<int>(h, H); ++y) {
								const UInt8 * pin = ptr + y * stride;
								uint32_t * pout = bd.bits32 + y * w;
								for(int x = 0; x < std::min<int>(w, W); ++x, ++pout, pin += 3) {
									*pout = 0xFF000000 | (pin[0] << 16) | (pin[1] << 8) | pin[2];
								}
							}
						}
						FREInvalidateBitmapDataRect(argv[0], 0, 0, w, h);
						success = true;
					}
					CFRelease(data);
				}
				// DO NOT RELEASE: FOLLOW THE 'GET' RULE: CGDataProviderRelease(provider);
			}
			CGImageRelease(image);
		}
		FREReleaseBitmapData(argv[0]);
	}
	FREObject rv = 0;
	FRENewObjectFromBool(success ? 1 : 0, &rv);
	return rv;
}


static FRENamedFunction g_functions[3] = {
	{ (const uint8_t *)"getScreenSize", 0, AmbireCaptureGetScreenSize },
	{ (const uint8_t *)"capture", 0, AmbireCaptureCapture },
	0
};

__attribute__((visibility("default")))
void AmbireCaptureContextInitializer(void *extData, const uint8_t * ctxType, FREContext ctx, uint32_t * numFunctionsToSet, const FRENamedFunction** functionsToSet) {
	*numFunctionsToSet = 2;
	*functionsToSet = &g_functions[0];
}

__attribute__((visibility("default")))
void AmbireCaptureContextFinalizer(FREContext ctx) {
}

__attribute__((visibility("default")))
void AmbireCaptureInitializer(void **extDataToSet, FREContextInitializer *ctxInitializerToSet, FREContextFinalizer *contextFinalizerToSet) {
	*extDataToSet = 0;
	*ctxInitializerToSet = AmbireCaptureContextInitializer;
	*contextFinalizerToSet = AmbireCaptureContextFinalizer;
}

__attribute__((visibility("default")))
void AmbireCaptureFinalizer(void * extData) {
}

}