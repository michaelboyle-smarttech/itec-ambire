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
package org.eun.itec.ambire.student;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.Manager;
import javax.media.Player;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FormatControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class WebcamCaptureDialog extends JDialog {
	private static final int MARGIN_LEFT = 5;
	private static final int MARGIN_TOP = 5;
	private static final int MARGIN_BOTTOM = 5;
	private static final int MARGIN_RIGHT = 5;
	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_HEIGHT = 40;
	public static void beginWebcamCaptureDialog(AmbireStudentApplication owner, ImageCaptureListener listener) {
		@SuppressWarnings("rawtypes")
		Vector v = CaptureDeviceManager.getDeviceList(null);
		CaptureDeviceInfo device = null;
		for(int i = 0; i < v.size(); ++i) {
			CaptureDeviceInfo d = (CaptureDeviceInfo)v.elementAt(i);
			Format[] fmts = d.getFormats();
			for(int j = 0; j < fmts.length; ++j) {
				if(fmts[j] instanceof VideoFormat) {
					device = d;
					break;
				}
			}
		}
		if(device == null) {
			owner.playErrorSound();
			return;
		}
		Player player = null;
		FrameGrabbingControl control = null;
		Component component = null;
		VideoFormat format = null;
		try {
			player = Manager.createRealizedPlayer(device.getLocator());
		} catch(Throwable e) {
			e.printStackTrace();
			owner.playErrorSound();
			return;
		}
		try {
			FormatControl formatcontrol = (FormatControl)player.getControl("javax.media.control.FormatControl");
			format = (VideoFormat)formatcontrol.getFormat();
			Format[] fmts = formatcontrol.getSupportedFormats();
  			for(int i = 0; i < fmts.length; ++i) {
				if(fmts[i] instanceof VideoFormat) {
					VideoFormat q = (VideoFormat)fmts[i];
					if(format != null) {
						Dimension x = format.getSize();
						Dimension y = q.getSize();
						int px = x.width * x.height;
						int py = y.width * y.height;
						if(((x.width > AmbireStudentApplication.MAX_IMAGE_WIDTH_PIXELS || x.height > AmbireStudentApplication.MAX_IMAGE_HEIGHT_PIXELS) && (py < px)) || (y.width <= AmbireStudentApplication.MAX_IMAGE_WIDTH_PIXELS && y.height <= AmbireStudentApplication.MAX_IMAGE_HEIGHT_PIXELS && py > px)) {
							format = q;
						}
					} else {
						format = q;
					}
				}
			}
			formatcontrol.setFormat(format);
			player.start();
			control = (FrameGrabbingControl)player.getControl("javax.media.control.FrameGrabbingControl");
			component = player.getVisualComponent();
		} catch(Throwable e) {
			e.printStackTrace();
			player.stop();
			player.close();
			owner.playErrorSound();
			return;
		}
		if(control == null || component == null || format == null) {
			player.close();
			owner.playErrorSound();
			return;
		}
		WebcamCaptureDialog dialog = new WebcamCaptureDialog(owner, player, component, control, format, listener);
		dialog.setVisible(true);
	}
	private AmbireStudentApplication m_owner;
	private ImageCaptureListener m_listener;
	private Player m_player;
	private Component m_component;
	private FrameGrabbingControl m_control;
	WebcamCaptureDialog(AmbireStudentApplication owner, Player player, Component component, FrameGrabbingControl control, VideoFormat format, ImageCaptureListener listener) {
		super(owner.getWindow(), Dialog.ModalityType.APPLICATION_MODAL);
		m_owner = owner;
		m_player = player;
		m_component = component;
		m_control = control;
		m_listener = listener;
		Dimension size = format.getSize();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel panel = new JPanel(new GridBagLayout());
		m_component.setPreferredSize(size);
		panel.add(m_component, new GridBagConstraints(0, 0, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		JButton okButton = new JButton(new AbstractAction("Take Picture") {
			public void actionPerformed(ActionEvent e) {
				okButtonPressed();
			}
		});
		okButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
		if(m_listener == null) {
			okButton.setEnabled(false);
		}
		panel.add(okButton, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
			public void actionPerformed(ActionEvent e) {
				cancelButtonPressed();
			}
		});
		cancelButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
		panel.add(cancelButton, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(panel);
		setSize(new Dimension(Math.max(size.width, 2 * BUTTON_WIDTH) + 2 * MARGIN_LEFT + 2 * MARGIN_RIGHT , size.height + 2 * MARGIN_TOP + 2 * MARGIN_BOTTOM + BUTTON_HEIGHT));
	}
	private void okButtonPressed() {
		Image img = null;
		try {
			Buffer buf = m_control.grabFrame();
			Format fmt = buf.getFormat();
			if(fmt instanceof VideoFormat) {
				BufferToImage converter = new BufferToImage((VideoFormat)fmt);
				img = converter.createImage(buf);
			}
		} catch(Throwable e) {
			e.printStackTrace();
			img = null;
		}
		if(img != null) {
			m_owner.playShutterSound();
			m_listener.imageCaptured(img);
			close();		
		} else {
			m_owner.playErrorSound();
		}
	}
	private void cancelButtonPressed() {
		if(m_listener != null) {
			m_listener.captureCancelled();
		}
		close();
	}
	private void close() {
		m_player.stop();
		m_player.close();
		setVisible(false);
	}
}
