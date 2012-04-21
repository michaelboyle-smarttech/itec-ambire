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
using System;
using System.Windows.Media.Imaging;
using System.Runtime.InteropServices;
using System.Diagnostics;

namespace AmbireStudentSilverlightApplication
{
    public class ScreenCapture
    {
        [StructLayout(LayoutKind.Sequential)]
        private struct BITMAPINFOHEADER
        {
            public uint biSize;
            public int biWidth;
            public int biHeight;
            public ushort biPlanes;
            public ushort biBitCount;
            public uint biCompression;
            public uint biSizeImage;
            public int biXPelsPerMeter;
            public int biYPelsPerMeter;
            public uint biClrUsed;
            public uint biClrImportant;
            public void Init()
            {
                biSize = (uint)Marshal.SizeOf(typeof(BITMAPINFOHEADER));
            }
            public void Init(int width, int height, int bpp)
            {
                Init();
                biWidth = width;
                biHeight = height;
                biPlanes = 1;
                biBitCount = (ushort)bpp;
                int stride = ((width * bpp + 31) / 32) * 4;
                biSizeImage = (uint)(stride * height);
                biCompression = 0;
                biXPelsPerMeter = 0;
                biYPelsPerMeter = 0;
                biClrUsed = 0;
                biClrImportant = 0;
            }
        }
        [StructLayout(LayoutKind.Sequential)]
        private struct BITMAPINFO
        {
            public BITMAPINFOHEADER bmiHeader;
            uint unused;
            public void Init()
            {
                bmiHeader = new BITMAPINFOHEADER();
                bmiHeader.Init();
            }
            public void Init(int width, int height, int bpp)
            {
                bmiHeader = new BITMAPINFOHEADER();
                bmiHeader.Init(width, height, bpp);
            }
        }
        [StructLayout(LayoutKind.Sequential)]
        private struct POINT
        {
            public int x;
            public int y;
        }
        private enum MonitorOptions : uint
        {
            MONITOR_DEFAULTTONULL = 0x00000000,
            MONITOR_DEFAULTTOPRIMARY = 0x00000001,
            MONITOR_DEFAULTTONEAREST = 0x00000002
        }
        [StructLayout(LayoutKind.Sequential)]
        public struct RECT
        {
            public int left;
            public int top;
            public int right;
            public int bottom;
        }
        [StructLayout(LayoutKind.Sequential)]
        private struct MONITORINFO
        {
            public uint cbSize;
            public RECT rcMonitor;
            public RECT rcWork;
            public uint dwFlags;
        }

        [DllImport("gdi32.dll")]
        private static extern IntPtr CreateDIBSection(IntPtr hdc, ref BITMAPINFOHEADER bmi, uint iUsage, out IntPtr ppvBits, IntPtr hSection, uint dwOffset);

        [DllImport("gdi32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool DeleteObject(IntPtr hgdiobj);

        [DllImport("user32.dll")]
        private static extern IntPtr MonitorFromPoint(POINT pt, MonitorOptions options);

        [DllImport("user32.dll")]
        private static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

        [DllImport("user32.dll")]
        private static extern IntPtr GetDC(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern int ReleaseDC(IntPtr hWnd, IntPtr hdc);

        [DllImport("gdi32.dll")]
        private static extern IntPtr CreateCompatibleDC(IntPtr hdc);

        [DllImport("gdi32.dll")]
        private static extern IntPtr SelectObject(IntPtr hdc, IntPtr hgdiobj);

        [DllImport("gdi32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool BitBlt(IntPtr hdcDest, int nXDest, int nYDest, int nWidth, int nHeight, IntPtr hdcSrc, int nXSrc, int nYSrc, uint dwRop);

        [DllImport("gdi32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool GdiFlush();

        [DllImport("gdi32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool DeleteDC(IntPtr hdc);

        public static WriteableBitmap Capture()
        {
            try
            {
                IntPtr hMonitor = MonitorFromPoint(new POINT(), MonitorOptions.MONITOR_DEFAULTTOPRIMARY);
                Debug.Assert(hMonitor != IntPtr.Zero);
                MONITORINFO mi = new MONITORINFO();
                mi.cbSize = (uint)Marshal.SizeOf(mi);
                bool bln = GetMonitorInfo(hMonitor, ref mi);
                Debug.Assert(bln);
                int W = mi.rcMonitor.right - mi.rcMonitor.left;
                int H = mi.rcMonitor.bottom - mi.rcMonitor.top;
                BITMAPINFOHEADER bmi = new BITMAPINFOHEADER();
                bmi.Init(W, H, 32);
                IntPtr hdcDesktop = GetDC(IntPtr.Zero);
                Debug.Assert(hdcDesktop != IntPtr.Zero);
                try
                {
                    IntPtr hdcMem = CreateCompatibleDC(hdcDesktop);
                    Debug.Assert(hdcMem != IntPtr.Zero);
                    try 
                    {
                        IntPtr ppvBits = IntPtr.Zero;
                        IntPtr hbm = CreateDIBSection(hdcMem, ref bmi, 0, out ppvBits, IntPtr.Zero, 0);
                        Debug.Assert(hbm != IntPtr.Zero);
                        try
                        {
                            Debug.Assert(ppvBits != IntPtr.Zero);
                            IntPtr hOldObj = SelectObject(hdcMem, hbm);
                            bln = BitBlt(hdcMem, 0, 0, W, H, hdcDesktop, mi.rcMonitor.left, mi.rcMonitor.top, 0x00CC0020 /* SRCCOPY */);
                            Debug.Assert(bln);
                            GdiFlush();
                            ReleaseDC(IntPtr.Zero, hdcDesktop);
                            hdcDesktop = IntPtr.Zero;
                            WriteableBitmap wb = new WriteableBitmap(W, H);
                            int[] buf = wb.Pixels;
                            for (int ofs = 0; ofs < H; ++ofs)
                            {
                                Marshal.Copy(new IntPtr(ppvBits.ToInt64() + 4 * W * ofs), buf, (H - 1 - ofs) * W, W);
                            }
                            DeleteDC(hdcMem);
                            hdcMem = IntPtr.Zero;
                            DeleteObject(hbm);
                            hbm = IntPtr.Zero;
                            return wb;
                        }
                        finally
                        {
                            if(hbm != IntPtr.Zero)
                            {
                                DeleteObject(hbm);
                            }
                        }
                    }
                    finally
                    {
                        if(hdcMem != IntPtr.Zero)
                        {
                            DeleteDC(hdcMem);
                        }
                    }
                }
                finally
                {
                    if(hdcDesktop != IntPtr.Zero)
                    {
                        ReleaseDC(IntPtr.Zero, hdcDesktop);
                    }
                }
            }
            catch(Exception)
            {
                return null;
            }
        }
    }
}
