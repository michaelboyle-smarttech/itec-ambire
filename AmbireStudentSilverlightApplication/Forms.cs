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
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading;
using System.Windows.Threading;

namespace System.Net
{
    public abstract class FormData : IDisposable
    {
        private string m_name;
        protected FormData(string name)
        {
            m_name = name;
        }
        public string Name
        {
            get
            {
                return m_name;
            }
        }
        public abstract string ContentType { get; }
        public abstract string ContentDisposition { get; }
        public abstract long ContentLength { get; }
        public abstract Stream Stream { get; }
        public virtual void Dispose()
        {
            m_name = null;
        }
    }

    public class FormSubmissionEventArgs : EventArgs
    {
        private HttpWebRequest m_request;
        private HttpWebResponse m_response;
        private Exception m_exception;
        private Form m_form;
        public FormSubmissionEventArgs(HttpWebRequest request, HttpWebResponse response, Exception e, Form f)
        {
            m_request = request;
            m_response = response;
            m_exception = e;
            m_form = f;
        }
        public HttpWebRequest Request
        {
            get
            {
                return m_request;
            }
        }
        public HttpWebResponse Response
        {
            get
            {
                return m_response;
            }
        }
        public Exception Exception
        {
            get
            {
                return m_exception;
            }
        }
        public Form Form
        {
            get
            {
                return m_form;
            }
        }
        public bool Success
        {
            get
            {
                return m_response != null && m_exception == null && m_response.StatusCode >= HttpStatusCode.OK && m_response.StatusCode < HttpStatusCode.MultipleChoices;
            }
        }
    }
    public class Form : IEnumerable<FormData>, IDisposable
    {
        private Dictionary<string, FormData> m_data;
        private string m_boundary;
        private byte[] m_one;
        private byte[] m_two;
        private byte[] m_three;
        private byte[] m_four;
        private byte[] m_five;

        public Form()
        {
            m_data = new Dictionary<string, FormData>();
            StringBuilder buf = new StringBuilder();
            Random r = new Random();
            int N = 20 + r.Next(22);
            for (int i = 0; i < N; ++i)
            {
                int q = r.Next(26 + 26 + 10);
                char c;
                if (q < 26)
                {
                    c = (char)('A' + (q % 26));
                }
                else if (q < 26 + 26)
                {
                    c = (char)('a' + ((q - 26) % 26));
                }
                else
                {
                    c = (char)('0' + ((q - 26 - 26) % 10));
                }
                buf.Append(c);
            }
            m_boundary = buf.ToString();
            m_one = Encoding.UTF8.GetBytes(string.Format("\r\n--{0}\r\nContent-Type: ", m_boundary));
            m_two = Encoding.UTF8.GetBytes("\r\nContent-Disposition: ");
            m_three = Encoding.UTF8.GetBytes("\r\nContent-Length: ");
            m_four = Encoding.UTF8.GetBytes("\r\n\r\n");
            m_five = Encoding.UTF8.GetBytes(string.Format("\r\n--{0}--\r\n", m_boundary));
        }
        public Dictionary<string, FormData> Data
        {
            get
            {
                return m_data;
            }
        }
        public object this[string name]
        {
            get
            {
                return m_data[name];
            }
            set
            {
                if (value == null)
                {
                    m_data.Remove(name);
                }
                else if (value is FormData)
                {
                    FormData d = value as FormData;
                    if (!m_data.Comparer.Equals(d.Name, name))
                    {
                        throw new ArgumentException(string.Format("Name of indexed property [\"{0}\"] does not equal Name property \"{1}\" of FormData value supplied.", name, d.Name));
                    }
                    m_data[name] = d;
                }
                else
                {
                    m_data[name] = new StringData(name, value.ToString());
                }
            }
        }
        public void Add(FormData d)
        {
            m_data.Add(d.Name, d);
        }
        public bool Remove(string name)
        {
            return m_data.Remove(name);
        }
        public int Count
        {
            get
            {
                return m_data.Count;
            }
        }
        public ICollection<string> Keys
        {
            get
            {
                return m_data.Keys;
            }
        }
        public ICollection<FormData> Values
        {
            get
            {
                return m_data.Values;
            }
        }
        public IEnumerator<FormData> GetEnumerator()
        {
            return m_data.Values.GetEnumerator();
        }
        IEnumerator IEnumerable.GetEnumerator()
        {
            return m_data.Values.GetEnumerator();
        }
        public void Clear()
        {
            m_data.Clear();
        }
        public bool Contains(string name)
        {
            return m_data.ContainsKey(name);
        }
        public long ContentLength
        {
            get
            {
                int B = m_boundary.Length;
                int M = m_data.Count;
                long N = 0;
                foreach (FormData d in m_data.Values)
                {
                    N += m_one.Length;
                    N += d.ContentType.Length;
                    N += m_two.Length; 
                    N += d.ContentDisposition.Length;
                    N += m_three.Length;
                    long n = d.ContentLength;
                    N += n.ToString().Length;
                    N += m_four.Length;
                    N += n;
                }
                N += m_five.Length;
                return N;
            }
        }
        public string ContentType
        {
            get
            {
                return "multipart/form-data; boundary=" + m_boundary + "";
            }
        }
        public long CopyTo(Stream stream, Stream logger = null)
        {
            byte[] b;
            long written = 0;
            byte[] buffer = new byte[32 * 1024];
            foreach(FormData d in m_data.Values)
            {
                b = m_one;
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length);
                written += b.Length;
                b = Encoding.UTF8.GetBytes(d.ContentType);
                Debug.Assert(b.Length == d.ContentType.Length);
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length); 
                written += b.Length;
                b = m_two;
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length);
                written += b.Length;
                b = Encoding.UTF8.GetBytes(d.ContentDisposition);
                Debug.Assert(b.Length == d.ContentDisposition.Length);
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length);
                written += b.Length;
                b = m_three;
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length);
                written += b.Length;
                long expected = d.ContentLength;
                b = Encoding.UTF8.GetBytes(expected.ToString());
                Debug.Assert(b.Length == expected.ToString().Length);
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length);
                written += b.Length;
                b = m_four;
                stream.Write(b, 0, b.Length);
                if (logger != null) logger.Write(b, 0, b.Length);
                written += b.Length;
                Stream s = d.Stream;
                long total = 0;
                for(int i = 0; i < expected;)
                {
                    int M = (int)Math.Min(expected - i, buffer.Length);
                    int m = s.Read(buffer, 0, M);
                    if(m <= 0)
                    {
                        break;
                    }
                    stream.Write(buffer, 0, m);
                    if (logger != null) logger.Write(buffer, 0, m);
                    i += m;
                    total += m;
                }
                written += total;
                Debug.Assert(total == expected);
            }
            b = m_five;
            stream.Write(b, 0, b.Length);
            if (logger != null) logger.Write(b, 0, b.Length);
            written += b.Length;
            return written;
        }
        public void Dispose()
        {
            if (m_data != null)
            {
                foreach (FormData d in m_data.Values)
                {
                    d.Dispose();
                }
                m_data.Clear();
                m_data = null;
            }
            m_boundary = null;
        }
        private static void PrintException(Exception e)
        {
        }
        public class Submission : IAsyncResult, IDisposable
        {
            internal IAsyncResult result;
            internal ManualResetEvent waitHandle;
            internal Form form;
            internal HttpWebRequest request;
            internal HttpWebResponse response;
            internal Action<FormSubmissionEventArgs> completedHandler;
            internal Dispatcher dispatcher;
            internal long expected;
            internal Stream logger;
            public Submission(Form f, HttpWebRequest r, Action<FormSubmissionEventArgs> h, Dispatcher d, Stream l)
            {
                form = f;
                request = r;
                completedHandler = h;
                dispatcher = d;
                logger = l;
                waitHandle = new ManualResetEvent(false);
            }
            internal void Failed(Exception e, HttpWebResponse r = null)
            {
                result = null;
                response = r;
                waitHandle.Set();
                FormSubmissionEventArgs args = new FormSubmissionEventArgs(request, response, e, form);
                while (e != null)
                {
                    Debug.WriteLine(e.Message);
                    Debug.WriteLine(e.StackTrace);
                    e = e.InnerException;
                }
                dispatcher.BeginInvoke(() =>
                {
                    completedHandler.Invoke(args);
                    Dispose();
                });
            }
            internal void Succeeded(HttpWebResponse r)
            {
                result = null;
                response = r;
                waitHandle.Set();
                FormSubmissionEventArgs args = new FormSubmissionEventArgs(request, response, null, form);
                dispatcher.BeginInvoke(() =>
                {
                    completedHandler.Invoke(args);
                    Dispose();
                });
            }
            public void Dispose()
            {
                Dispose(false);
            }
            public void Dispose(bool disposing)
            {
                if (form != null)
                {
                    form.Dispose();
                    form = null;
                }
                if (logger != null)
                {
                    logger.Close();
                    logger.Dispose();
                    logger = null;
                }
                if (response != null)
                {
                    response.Dispose();
                }
                if (disposing && waitHandle != null)
                {
                    waitHandle.Close();
                    waitHandle.Dispose();
                    waitHandle = null;
                }
            }
            public object AsyncState
            {
                get
                {
                    return this;
                }
            }

            public Threading.WaitHandle AsyncWaitHandle
            {
                get
                {
                    return waitHandle;
                }
            }

            public bool CompletedSynchronously
            {
                get
                {
                    return false;
                }
            }

            public bool IsCompleted
            {
                get
                {
                    return waitHandle.WaitOne(0);
                }
            }

            public void Abort()
            {
                if (result != null)
                {
                    request.Abort();
                    result = null;
                }
            }
        }
        public Submission Submit(HttpWebRequest request, Action<FormSubmissionEventArgs> completedHandler, Dispatcher dispatcher, Stream logger = null)
        {
            Submission state = new Submission(this, request, completedHandler, dispatcher, logger);
            state.expected = this.ContentLength;
            state.request.Method = "POST";
            state.request.ContentType = ContentType;
            state.request.ContentLength = state.expected;
            try
            {
                state.result = state.request.BeginGetRequestStream(submission_GotRequestStream, state);
            }
            catch(Exception e)
            {
                state.Failed(e);
            }
            return state;
        }
        private void submission_GotRequestStream(IAsyncResult ar)
        {
            Submission state = ar.AsyncState as Submission;
            try
            {
                Stream s = state.request.EndGetRequestStream(ar);
                long actual = CopyTo(s, state.logger);
                Debug.Assert(actual == state.expected);
                s.Close();
                state.result = state.request.BeginGetResponse(submission_GotResponse, state);
            }
            catch (Exception e)
            {
                PrintException(e);
                state.dispatcher.BeginInvoke(state.completedHandler, new FormSubmissionEventArgs(state.request, null, e, this));
            }
            finally
            {
                Dispose();
            }
        }
        private void submission_GotResponse(IAsyncResult ar)
        {
            Submission state = ar.AsyncState as Submission;
            try
            {
                HttpWebResponse response = state.request.EndGetResponse(ar) as HttpWebResponse;
                if (response.StatusCode != HttpStatusCode.OK)
                {
                    state.Failed(null, response);
                }
                else
                {
                    state.Succeeded(response);
                }
            }
            catch (Exception e)
            {
                state.Failed(e);
            }
        }
    }
    public class StringData : FormData
    {
        private string m_value;
        private byte[] m_bytes;
        private Stream m_stream;
        public StringData(string name, string stringValue = "")
            : base(name)
        {
            this.Value = stringValue;
        }
        public String Value
        {
            get
            {
                return m_value;
            }
            set
            {
                m_value = value;
                m_bytes = Encoding.UTF8.GetBytes(m_value);
                m_stream = new MemoryStream(m_bytes);
            }
        }
        public override String ContentType
        {
            get
            {
                return "text/plain; charset=UTF-8";
            }
        }
        public override String ContentDisposition
        {
            get
            {
                return string.Format("form-data; name=\"{0}\"", this.Name);
            }
        }
        public override long ContentLength
        {
            get
            {
                return m_bytes.Length;
            }
        }
        public override Stream Stream
        {
            get
            {
                return m_stream;
            }
        }
        public override string ToString()
        {
            return m_value;
        }
        public override void Dispose()
        {
            base.Dispose();
            m_value = null;
            m_bytes = null;
            m_stream.Dispose();
            m_stream = null;
        }
    }
    public class FileData : FormData
    {
        private string m_fileName;
        private string m_contentType;
        private long m_contentLength;
        private Stream m_stream;
        public FileData(string name, string filePath, string contentType = "application/octet-stream")
            : base(name)
        {
            m_fileName = Path.GetFileName(filePath);
            m_contentType = contentType;
            m_stream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.Read);
            m_contentLength = (int)Math.Min(int.MaxValue, m_stream.Length);
        }
        public FileData(string name, Stream stream, long contentLength = -1, string contentType = "application/octet-stream", string fileName = null)
            : base(name)
        {
            m_fileName = fileName;
            m_contentType = contentType;
            if (contentLength >= 0)
            {
                m_contentLength = contentLength;
            }
            else
            {
                m_contentLength = stream.Length;
            }
            m_stream = stream;
        }
        public override string ContentType
        {
            get
            {
                return m_contentType;
            }
        }
        public override string ContentDisposition
        {
            get
            {
                if(!string.IsNullOrWhiteSpace(m_fileName))
                {
                    return string.Format("form-data; name=\"{0}\"; filename=\"{1}\"", this.Name, m_fileName);
                }
                else
                {
                return string.Format("form-data; name=\"{0}\"", this.Name);
                }
            }
        }
        public override long ContentLength
        {
            get
            {
                return m_contentLength;
            }
        }
        public override Stream Stream
        {
            get
            {
                return m_stream;
            }
        }
        public string FileName
        {
            get
            {
                return m_fileName;
            }
            set
            {
                m_fileName = value;
            }
        }
        public override string ToString()
        {
            return m_fileName;
        }
        public override void Dispose()
        {
            base.Dispose();
            m_fileName = null;
            m_contentType = null;
            m_contentLength = 0;
            if (m_stream != null)
            {
                m_stream.Dispose();
                m_stream = null;
            }
        }
    }
}
