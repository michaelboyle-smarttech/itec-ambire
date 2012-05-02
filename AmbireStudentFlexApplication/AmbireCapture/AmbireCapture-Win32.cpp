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
#ifndef WINVER
#define WINVER 0x502
#endif

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x502
#endif

#include <windows.h>
#include <FlashRuntimeExtensions.h>

FREObject __cdecl AmbireCaptureGetScreenSize(FREContext ctx, void * functionData, uint32_t argc, FREObject argv[]) {
	POINT pt = { 0 };
	HMONITOR hMonitor = MonitorFromPoint(pt, MONITOR_DEFAULTTOPRIMARY);
	MONITORINFO mi = { 0 };
	mi.cbSize = sizeof(mi);
	GetMonitorInfo(hMonitor, &mi);
	int W = mi.rcMonitor.right - mi.rcMonitor.left;
	int H = mi.rcMonitor.bottom - mi.rcMonitor.top;
	FREObject rv = { 0 };
	FRENewObjectFromInt32(W | (H << 16), &rv);
	return rv;
}

FREObject __cdecl AmbireCaptureCapture(FREContext ctx, void * functionData, uint32_t argc, FREObject argv[]) {
	POINT pt = { 0 };
	HMONITOR hMonitor = MonitorFromPoint(pt, MONITOR_DEFAULTTOPRIMARY);
	MONITORINFO mi = { 0 };
	mi.cbSize = sizeof(mi);
	GetMonitorInfo(hMonitor, &mi);
	int W = mi.rcMonitor.right - mi.rcMonitor.left;
	int H = mi.rcMonitor.bottom - mi.rcMonitor.top;
	FREBitmapData bd = { 0 };
	FREAcquireBitmapData(argv[0], &bd);
	int bpp = 32;
	int width = bd.width;
	int height = bd.height;
	BITMAPINFOHEADER bmi = { 0 };
	bmi.biSize = sizeof(bmi);
	bmi.biWidth = width;
	bmi.biHeight = height;
	bmi.biPlanes = 1;
	bmi.biBitCount = bpp;
	int stride = ((width * bpp + 31) / 32) * 4;
	bmi.biSizeImage = stride * height;
	void * ppvBits = 0;
	HDC hdcDesktop = GetDC(0);
	HDC hdcMem = CreateCompatibleDC(hdcDesktop);
	HBITMAP hbm = CreateDIBSection(hdcMem, (const BITMAPINFO *)&bmi, 0, &ppvBits, 0, 0);
	SelectObject(hdcMem, hbm);
	BitBlt(hdcMem, 0, 0, W, H, hdcDesktop, mi.rcMonitor.left, mi.rcMonitor.top, SRCCOPY);
	GdiFlush();
	ReleaseDC(0, hdcDesktop);
	DeleteDC(hdcMem);
	memmove(bd.bits32, ppvBits, bmi.biSizeImage);
	DeleteObject(hbm);
	FREReleaseBitmapData(argv[0]);
	FREObject rv = { 0 };
	return rv;
}


static FRENamedFunction g_functions[3] = {
	{ (const uint8_t *)"getScreenSize", 0, AmbireCaptureGetScreenSize },
	{ (const uint8_t *)"capture", 0, AmbireCaptureCapture },
	0
};

void __cdecl AmbireCaptureContextInitializer(void *extData, const uint8_t * ctxType, FREContext ctx, uint32_t * numFunctionsToSet, const FRENamedFunction** functionsToSet) {
	*numFunctionsToSet = 1;
	*functionsToSet = &g_functions[0];
}

void __cdecl AmbireCaptureContextFinalizer(FREContext ctx) {
}

void __cdecl AmbireCaptureInitializer(void **extDataToSet, FREContextInitializer *ctxInitializerToSet, FREContextFinalizer *contextFinalizerToSet) {
	*extDataToSet = 0;
	*ctxInitializerToSet = AmbireCaptureContextInitializer;
	*contextFinalizerToSet = AmbireCaptureContextFinalizer;
}

void __cdecl AmbireCaptureFinalizer(void * extData) {
}
