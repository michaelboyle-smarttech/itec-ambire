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

package org.eun.itec.ambire.student {
	
	import flash.display.*;
	import flash.external.*;
	
	public class AmbireCapture {
		public static function capture():Bitmap {
			var bm:Bitmap = null;
			try {
				var ec:ExtensionContext = ExtensionContext.createExtensionContext("AmbireCapture", null);
				if(ec != null) {
					var n:int = int(ec.call("getScreenSize"));
					var w:int = n & 0xFFFF;
					var h:int = (n >> 16) & 0xFFFF;
					if(w > 0 && h > 0) {
						var data:BitmapData = new BitmapData(w, h, true, 0x00000000);
						var success:Boolean = Boolean(ec.call("capture", data));
						if(success) {
							bm = new Bitmap(data);
						}
					}
					ec.dispose();
				}
			} catch(error:*) {
				// ignored
			}
			return bm;
		}
	}
}
