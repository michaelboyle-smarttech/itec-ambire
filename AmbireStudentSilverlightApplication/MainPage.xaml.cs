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
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.Windows.Threading;
using System.Threading;
using FJ = FluxJpeg.Core;
using FluxJpeg.Core.Encoder;
using System.Windows.Resources;
using System.Threading.Tasks;


namespace AmbireStudentSilverlightApplication
{
    [System.Security.SecuritySafeCritical]
    public partial class MainPage : UserControl
    {

        private enum Mode
        {
            Disconnected = 0,
            Screenshot,
            Webcam,
            Upload
        }

        private enum Reason
        {
            MissingInfo,
            AllGood,
            VerifyingInfo,
            Rejected,
            NoConnection
        }

        private Mode m_mode = Mode.Disconnected;
        private Mode m_previousMode = Mode.Screenshot;
        private CaptureSource m_source;
        private WriteableBitmap m_currentFrame;
        private WriteableBitmap m_garbageFrame;
        private object m_monitor = new object();
        private string m_verified = null;
        private string m_verifying = null;
        private DateTime m_verification = DateTime.MinValue;
        private DateTime m_screenshot = DateTime.MinValue;
        private static readonly long REVERIFY_INTERVAL_MILLIS = 5 * 60 * 1000;
        private static readonly long SCREENSHOT_INTERVAL_MILLIS = 2 * 60 * 1000;
        private string m_baseUrl = "http://ambire.itec.smartlabs.mobi:8081/";
        private string m_uniqueIdentifier = Guid.NewGuid().ToString();
        public MainPage()
        {
            InitializeComponent();
            this.Loaded += new RoutedEventHandler(MainPage_Loaded);
        }

        void MainPage_Loaded(object sender, RoutedEventArgs e)
        {
            if (Application.Current.IsRunningOutOfBrowser)
            {
                inBrowserPanel.Visibility = System.Windows.Visibility.Collapsed;
                outOfBrowserPanel.Visibility = System.Windows.Visibility.Visible;
                StreamResourceInfo res = Application.GetResourceStream(new Uri("/AmbireStudentSilverlightApplication;component/ambire.jpg", UriKind.Relative));
                BitmapImage g = new BitmapImage();
                g.SetSource(res.Stream);
                m_garbageFrame = new WriteableBitmap(g);
                DispatcherTimer taskTimer = new DispatcherTimer();
                taskTimer.Interval = new TimeSpan(0, 0, 1);
                taskTimer.Tick += new EventHandler(taskTimer_Tick);
                taskTimer.Start();
                SetReason(Reason.MissingInfo);
                SetMode(Mode.Disconnected, true);
            }
        }

        void taskTimer_Tick(object sender, EventArgs e)
        {
            DateTime now = DateTime.Now;
            Monitor.Enter(m_monitor);
            bool shouldVerifyNow = (string.IsNullOrWhiteSpace(m_verified) && !string.IsNullOrWhiteSpace(m_verifying)) || (string.IsNullOrWhiteSpace(m_verifying) && !string.IsNullOrWhiteSpace(m_verified) && (now - m_verification).TotalMilliseconds >= REVERIFY_INTERVAL_MILLIS);
            Monitor.Exit(m_monitor);
            if (shouldVerifyNow)
            {
                BeginVerify();
            }
            if (m_mode == Mode.Screenshot && (now - m_screenshot).TotalMilliseconds >= SCREENSHOT_INTERVAL_MILLIS)
            {
                TakeScreenshot();
            }
        }

        void TakeScreenshot()
        {
            m_screenshot = DateTime.Now;
            SetCurrentFrame(ScreenCapture.Capture(), "screenshot.jpg");
        }

        void source_CaptureImageCompleted(object sender, CaptureImageCompletedEventArgs e)
        {
            Dispatcher.BeginInvoke(() =>
            {
                SetCurrentFrame(e.Result, "webcam.jpg");
                m_source.Stop();
                m_source = null;
                FinishWebcam();
            });
        }

        void PlaySound(string sound)
        {
            StreamResourceInfo res = Application.GetResourceStream(new Uri("/AmbireStudentSilverlightApplication;component/" + sound + ".mp3", UriKind.Relative));
            mediaPlayer.SetSource(res.Stream);
            mediaPlayer.Play();
        }

        private static void PrintException(Exception e)
        {
            while(e != null)
            {
                Debug.WriteLine(e.Message);
                Debug.WriteLine(e.StackTrace);
                e = e.InnerException;
            }
        }

        private void PlayErrorSound()
        {
            Dispatcher.BeginInvoke(() =>
            {
                PlaySound("error");
            });
        }

        static WriteableBitmap ScaleBitmap(WriteableBitmap bmp, int width, int height)
        {
            int W = bmp.PixelWidth;
            int H = bmp.PixelHeight;
            if (W <= width && H <= height)
            {
                return bmp;
            }
            int w = width;
            int h = height;
            if (((double)W) / ((double)H) >= (((double)width) / (double)height))
            {
                h = (int)Math.Min(H * w / (double)W, height);
            }
            else
            {
                w = (int)Math.Min(W * h / (double)H, width);
            }
            return bmp.Resize(w, h, WriteableBitmapExtensions.Interpolation.Bilinear);
        }
        void UploadFrame(WriteableBitmap bmp, string kind, string fileName, bool replace)
        {
            PlaySound("shutter");
            int W = bmp.PixelWidth;
            int H = bmp.PixelHeight;
            byte[][,] buf = FJ.Image.CreateRaster(W, H, 3);
            int[] pix = bmp.Pixels;
            for (int y = 0; y < H; ++y)
            {
                for (int x = 0, i = y * W; x < W; ++x, ++i)
                {
                    int n = pix[i];
                    buf[0][x,y] = (byte)((n >> 16) & 255);
                    buf[1][x,y] = (byte)((n >> 8) & 255);
                    buf[2][x,y] = (byte)(n & 255);
                }
            }
            MemoryStream stream = new MemoryStream();
            FJ.Image img = new FJ.Image(new FJ.ColorModel { colorspace = FJ.ColorSpace.RGB }, buf);
            JpegEncoder enc = new JpegEncoder(img, 80, stream);
            enc.Encode();
            stream.Seek(0, SeekOrigin.Begin);
            Form form = new Form();
            form["name"] = nameTextField.Text;
            form["pin"] = m_verified;
            form["kind"] = kind;
            form["width"] = W.ToString();
            form["height"] = H.ToString();
            if (replace)
            {
                form["replace"] = "replace";
            }
            form.Add(new FileData("file", stream, stream.Length, "image/jpeg", System.IO.Path.GetFileNameWithoutExtension(fileName) + ".jpg"));
            form.Submit(WebRequest.CreateHttp(m_baseUrl + "upload"), (FormSubmissionEventArgs e) =>
            {
                if (!e.Success)
                {
                    PlaySound("error");
                    SetMode(Mode.Disconnected);
                    if (e.Response != null && e.Response.StatusCode == HttpStatusCode.NotFound)
                    {
                        SetReason(Reason.Rejected);
                    }
                    else
                    {
                        SetReason(Reason.NoConnection);
                    }
                }
            }, Dispatcher);
        }

        void SetCurrentFrame(WriteableBitmap bmp, string fileName)
        {
            m_currentFrame = bmp;
            if (bmp == null)
            {
                bmp = m_garbageFrame;
            }
            else if(bmp != m_garbageFrame && fileName != null)
            {
                bmp = ScaleBitmap(bmp, 500, 400);
                string kind = m_uniqueIdentifier;
                bool replace = false;
                if (m_mode == Mode.Screenshot)
                {
                    kind += ".screenshot";
                    replace = true;
                }
                else if (m_mode == Mode.Webcam)
                {
                    kind += ".webcam";
                }
                else if (m_mode == Mode.Upload)
                {
                    kind += ".upload";
                }
                UploadFrame(bmp, kind, fileName, replace);
            }
            if (bmp == null)
            {
                currentFrame.Fill = new SolidColorBrush(Colors.White);
            }
            else
            {
                ImageBrush br = new ImageBrush();
                br.ImageSource = bmp;
                br.Stretch = Stretch.Uniform;
                currentFrame.Fill = br;
            }
        }

        void SetMode(Mode m, bool force = false)
        {
            if (m_mode == m && !force)
            {
                return;
            }
            if(m_mode != Mode.Disconnected)
            {
                m_previousMode = m_mode;
            }
            m_mode = m;
            if (m != Mode.Disconnected)
            {
                screenshotButton.IsEnabled = true;
                uploadButton.IsEnabled = true;
                webcamButton.IsEnabled = (CaptureDeviceConfiguration.AllowedDeviceAccess || CaptureDeviceConfiguration.RequestDeviceAccess()) && CaptureDeviceConfiguration.GetAvailableVideoCaptureDevices().Count > 0;
            }
            else
            {
                screenshotButton.IsEnabled = false;
                uploadButton.IsEnabled = false;
                webcamButton.IsEnabled = false;
                SetCurrentFrame(m_garbageFrame, null);
                reasonIcon.Source = Resources["GoIcon"] as BitmapImage;
            }
            if (m == Mode.Screenshot)
            {
                TakeScreenshot();
            }
            if (m == Mode.Webcam)
            {
                webcamControlPanel.Visibility = System.Windows.Visibility.Visible;
                webcamButton.Visibility = System.Windows.Visibility.Collapsed;
                m_source = new CaptureSource();
                m_source.VideoCaptureDevice = CaptureDeviceConfiguration.GetDefaultVideoCaptureDevice();
                VideoBrush brush = new VideoBrush();
                brush.Stretch = Stretch.Uniform;
                brush.SetSource(m_source);
                currentFrame.Fill = brush;
                m_source.CaptureImageCompleted += new EventHandler<CaptureImageCompletedEventArgs>(source_CaptureImageCompleted);
                if (CaptureDeviceConfiguration.AllowedDeviceAccess || CaptureDeviceConfiguration.RequestDeviceAccess())
                {
                    m_source.Start();
                }
            }
            else
            {
                webcamControlPanel.Visibility = System.Windows.Visibility.Collapsed;
                webcamButton.Visibility = System.Windows.Visibility.Visible;
            }
        }

        private void screenshotButton_Click(object sender, RoutedEventArgs e)
        {
            SetMode(Mode.Screenshot, true);
        }

        private void webcamButton_Click(object sender, RoutedEventArgs e)
        {
            SetMode(Mode.Webcam, true);
        }

        private void uploadButton_Click(object sender, RoutedEventArgs e)
        {
            OpenFileDialog ofd = new OpenFileDialog();
            ofd.Filter = "Image Files (*.jpg, *.png, *.bmp)|*.jpg;*.png;*.bmp";
            ofd.InitialDirectory = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
            ofd.Multiselect = true;
            if(ofd.ShowDialog() == true)
            {
                SetMode(Mode.Upload, true);
                foreach (FileInfo fi in ofd.Files)
                {
                    UploadFile(fi);
                }
            }
        }

        void UploadFile(FileInfo fi)
        {
            BitmapImage bi = new BitmapImage();
            bi.ImageOpened += new EventHandler<RoutedEventArgs>((object sender, RoutedEventArgs e) =>
            {
                SetCurrentFrame(new WriteableBitmap(bi), fi.FullName);
            });
            bi.ImageFailed += new EventHandler<ExceptionRoutedEventArgs>((object sender, ExceptionRoutedEventArgs e) =>
            {
                SetMode(m_previousMode);
            });
            bi.SetSource(fi.OpenRead());
        }

        void FinishWebcam()
        {
            webcamControlPanel.Visibility = System.Windows.Visibility.Collapsed;
            webcamButton.Visibility = System.Windows.Visibility.Visible;
        }

        private void takePictureButton_Click(object sender, RoutedEventArgs e)
        {
            m_source.CaptureImageAsync();
        }

        private void cancelWebcamButton_Click(object sender, RoutedEventArgs e)
        {
            SetCurrentFrame(m_currentFrame, null);
            m_source.Stop();
            m_source = null;
            m_mode = m_previousMode;
            FinishWebcam();
            
        }

        private void nameTextField_TextChanged(object sender, TextChangedEventArgs e)
        {
            CheckSettings();
        }

        private void pinTextField_TextChanged(object sender, TextChangedEventArgs e)
        {
            CheckSettings();
        }


        void CheckSettings()
        {
            String name = nameTextField.Text.Trim();
            String pin = pinTextField.Text.Trim();
            if (string.IsNullOrWhiteSpace(name) || string.IsNullOrWhiteSpace(pin))
            {
                SetReason(Reason.MissingInfo);
            }
            else
            {
                SetNeedsVerify(pin);
            }
        }

        void SetNeedsVerify(String pin)
        {
            Monitor.Enter(m_monitor);
            if (pin != m_verified)
            {
                m_verifying = pin;
                m_verified = null;
                m_verification = DateTime.MinValue;
            }
            Monitor.Exit(m_monitor);
        }

        Task BeginVerify()
        {
            Task t = new Task(() =>
            {
                Verify();
            });
            t.Start();
            return t;
        }

        void Verify()
        {
            Monitor.Enter(m_monitor);
            string pin = m_verifying;
            m_verifying = null;
            m_verified = null;
            Monitor.Exit(m_monitor);
            if (!string.IsNullOrWhiteSpace(pin))
            {
                Dispatcher.BeginInvoke(() =>
                {
                    SetReason(Reason.VerifyingInfo);
                });
                WebClient verifyRequest = new WebClient();
                string url = m_baseUrl + "verify?p=" + Uri.EscapeUriString(pin);
                verifyRequest.DownloadStringCompleted += new DownloadStringCompletedEventHandler(verifyRequest_DownloadStringCompleted);
                verifyRequest.DownloadStringAsync(new Uri(url), pin);
            }
        }

        void verifyRequest_DownloadStringCompleted(object sender, DownloadStringCompletedEventArgs e)
        {
            string pin = e.UserState as string;
            Reason r = Reason.NoConnection;
            bool accepted = false;
            if (e.Error == null && bool.TryParse(e.Result, out accepted))
            {
                if (accepted)
                {
                    r = Reason.AllGood;
                }
                else
                {
                    r = Reason.Rejected;
                }
            }
            Monitor.Enter(m_monitor);
            m_verification = DateTime.Now;
            m_verified = pin;
            Monitor.Exit(m_monitor);
            Dispatcher.BeginInvoke(() =>
            {
                SetReason(r);
                if(accepted)
                {
                    if(m_mode == Mode.Disconnected)
                    {
                        if(m_previousMode == Mode.Disconnected)
                        {
                            m_previousMode = Mode.Screenshot;
                        }
                        SetMode(m_previousMode, true);
                    }
                }
                else
                {
                    SetMode(Mode.Disconnected);
                }
            });
        }

        void SetReason(Reason r)
        {
            switch (r)
            {
                default:
                    reasonIcon.Visibility = System.Windows.Visibility.Collapsed;
                    reasonTextField.Visibility = System.Windows.Visibility.Collapsed;
                    break;
                case Reason.MissingInfo:
                    reasonIcon.Source = Resources["GoIcon"] as BitmapImage;
                    reasonIcon.Visibility = System.Windows.Visibility.Visible;
                    reasonTextField.Text = "Type your name and PIN to connect to the Ambire cloud.";
                    reasonTextField.Visibility = System.Windows.Visibility.Visible;
                    break;
                case Reason.VerifyingInfo:
                    reasonIcon.Source = Resources["ActivityIcon"] as BitmapImage;
                    reasonIcon.Visibility = System.Windows.Visibility.Visible;
                    reasonTextField.Text = "Connecting to Ambire cloud...";
                    reasonTextField.Visibility = System.Windows.Visibility.Visible;
                    break;
                case Reason.Rejected:
                    reasonIcon.Source = Resources["StopIcon"] as BitmapImage;
                    reasonIcon.Visibility = System.Windows.Visibility.Visible;
                    reasonTextField.Text = "PIN is incorrect.";
                    reasonTextField.Visibility = System.Windows.Visibility.Visible;
                    break;
                case Reason.NoConnection:
                    reasonIcon.Source = Resources["WarningIcon"] as BitmapImage;
                    reasonIcon.Visibility = System.Windows.Visibility.Visible;
                    reasonTextField.Text = "Can\'t connect to the Ambire cloud.";
                    reasonTextField.Visibility = System.Windows.Visibility.Visible;
                    break;
            }
        }
    }
}
