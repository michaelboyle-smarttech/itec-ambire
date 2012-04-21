package org.eun.itec.ambire.student;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.jnlp.BasicService;
import javax.jnlp.FileContents;
import javax.jnlp.ServiceManager;
import javax.jnlp.FileOpenService;
import javax.jnlp.UnavailableServiceException;
import javax.media.Player;
import javax.media.Manager;
import javax.media.Time;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

@SuppressWarnings("serial")
public class AmbireStudentApplication implements Runnable {
	static final long TIMER_DELAY_MILLISECONDS = 3 * 1000;
	static final long TIMER_PERIOD_MILLISECONDS = 1 * 1000;
	static final long AUTO_SCREENSHOT_AFTER_MILLISECONDS = 22 * 1000;
	static final long REVERIFY_PIN_AFTER_MILLISECONDS = 6 * 60 * 1000;
	static final long RECHECK_WEBCAM_AFTER_MILLISECONDS = 11 * 1000;
	static final int MARGIN_LEFT = 20;
	static final int MARGIN_TOP = 20;
	static final int MARGIN_BOTTOM = 20;
	static final int MARGIN_RIGHT = 20;
	static final int PADDING_X = 10;
	static final int PADDING_Y = 10;
	public static final int MAX_IMAGE_WIDTH_PIXELS = 500;
	public static final int MAX_IMAGE_HEIGHT_PIXELS = 400;
	public static final int HATCH_WIDTH_PIXELS = 8;
	public static final int HATCH_HEIGHT_PIXELS = 8;
	static final int INFO_PANEL_HEIGHT = 100;
	static final int BUTTON_HEIGHT = 40;
	static final int BUTTON_WIDTH = (MAX_IMAGE_WIDTH_PIXELS - 4 * PADDING_X) / 3;
	private BufferedImage m_genericImage;
	private JFrame m_frame;
	private JLabel m_currentImageLabel;
	private Timer m_timer;
	private JTextField m_nameTextField;
	private JTextField m_pinTextField;
	private JLabel m_reasonLabel;
	private JButton m_screenshotButton;
	private JButton m_webcamButton;
	private JButton m_uploadButton;
	private Player m_shutterSound;
	private Player m_errorSound;
	private Date m_lastScreenshotDate;
	private String m_pin;
	private boolean m_modality;
	private static final int SCREENSHOT_CAPTURE_TYPE = 0;
	private static final int WEBCAM_CAPTURE_TYPE = 1;
	private static final int UPLOAD_CAPTURE_TYPE = 2;
	private int m_captureType = SCREENSHOT_CAPTURE_TYPE;
	private String m_baseUrl = "http://localhost:8081/";
	private Date m_lastConnectionCheckDate;
	private static long CHECK_CONNECTION_PERIOD_MILLISECONDS = 13000;
	private String m_checking;
	private String m_recheck;
	private String m_uniqueIdentifier;
	public static void main(String[] args) {
		try {
			SwingUtilities.invokeLater(new AmbireStudentApplication());
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	public void run() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable e) {
			e.printStackTrace();
		}
		UUID uuid = UUID.randomUUID();
		final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#";
		StringBuilder b = new StringBuilder();
		long l = uuid.getMostSignificantBits();
		for(int i = 0; i < 64; i += 6) {
			int k = (int)(l & 0x3F);
			l = l >> 6;
			b.append(ALPHABET.charAt(k));
		}
		l = uuid.getLeastSignificantBits();
		for(int i = 0; i < 64; i += 6) {
			int k = (int)(l & 0x3F);
			l = l >> 6;
			b.append(ALPHABET.charAt(k));
		}
		m_uniqueIdentifier = b.toString();
		try {
			BasicService basic = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
			m_baseUrl = basic.getCodeBase().toExternalForm();
		} catch (UnavailableServiceException e) {
			m_baseUrl = "http://localhost:8081/";
		}
		createUserInterface();
		setupTimer();
	}
	public static BufferedImage scale(Image i, int W, int H) {
		int width = i.getWidth(null);
		int height = i.getHeight(null);
		if(width < 1 || height < 1) {
			return null;
		} else if(width > W || height > H) { 
			double factor = Math.min(((double)W)/((double)width), ((double)H)/((double)height));
			width = Math.min(W, Math.max(1,(int)(width * factor)));
			height = Math.min(H, Math.max(1,(int)(height * factor)));
			i = i.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
		}
		if(i instanceof BufferedImage) {
			return (BufferedImage)i;
		}
		BufferedImage buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = buf.createGraphics();
		g.setBackground(Color.white);
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		g.setBackground(Color.gray);
		g.setColor(Color.gray);
		for(int y = 0; (2 * HATCH_HEIGHT_PIXELS * y) < height; ++y) {
			for(int x = 0; (2 * HATCH_WIDTH_PIXELS * x) < width; ++x) {
				g.fillRect((2 * x + (y & 1)) * HATCH_WIDTH_PIXELS , 2 * HATCH_HEIGHT_PIXELS * y, HATCH_WIDTH_PIXELS, HATCH_HEIGHT_PIXELS);
			}
		}
		g.drawImage(i, 0, 0, null);
		g.dispose();
		return buf;
	}
	private static BufferedImage scale(Image i) {
		return scale(i, MAX_IMAGE_WIDTH_PIXELS, MAX_IMAGE_HEIGHT_PIXELS);
	}
	private URL getTemporaryCachedResource(String resourceName) {
		try {
			int lastSlash = resourceName.lastIndexOf('/');
			int lastPeriod = resourceName.lastIndexOf('.');
			String prefix = resourceName.substring(lastSlash + 1, lastPeriod);
			String suffix = resourceName.substring(lastPeriod);
			InputStream src = getClass().getResourceAsStream(resourceName);
			File tmp = null;
			try {
				tmp = File.createTempFile(prefix, suffix);
				FileOutputStream dst = new FileOutputStream(tmp);
				try {
					byte[] buffer = new byte[32 * 1024];
					while(true) {
						int n = src.read(buffer, 0, buffer.length);
						if(n > 0) {
							dst.write(buffer, 0, n);
						} else {
							break;
						}
					}
				} finally {
					dst.close();
				}
			} finally {
				src.close();
			}
			return tmp.toURI().toURL();
		} catch(Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	private void createUserInterface() {
		m_frame = new JFrame("Ambire");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.getContentPane().setSize(MAX_IMAGE_WIDTH_PIXELS + MARGIN_LEFT + MARGIN_RIGHT, MAX_IMAGE_HEIGHT_PIXELS + MARGIN_TOP + MARGIN_BOTTOM + 4 * PADDING_Y + BUTTON_HEIGHT + INFO_PANEL_HEIGHT);
		try {
			m_genericImage = scale(ImageIO.read(getClass().getResource("res/generic-image-icon.png")));
		} catch(Throwable e) {
			e.printStackTrace();
			m_genericImage = null;
		}
		m_currentImageLabel = new JLabel(new ImageIcon(m_genericImage));
		m_currentImageLabel.setPreferredSize(new Dimension(MAX_IMAGE_WIDTH_PIXELS,MAX_IMAGE_HEIGHT_PIXELS));
		m_nameTextField = new JTextField();
		m_pinTextField = new JTextField();
		m_reasonLabel = new JLabel();
		m_reasonLabel.setFont(m_reasonLabel.getFont().deriveFont(Font.ITALIC));
		try {
			m_shutterSound = Manager.createRealizedPlayer(getTemporaryCachedResource("res/shutter.mp3"));
			m_shutterSound.prefetch();
		} catch(Throwable e) {
			e.printStackTrace();
			m_shutterSound = null;
		}
		try {
			m_errorSound = Manager.createRealizedPlayer(getTemporaryCachedResource("res/error.mp3"));
			m_errorSound.prefetch();
		} catch(Throwable e) {
			e.printStackTrace();
			m_errorSound = null;
		}
		try {
			Player intro = Manager.createRealizedPlayer(getTemporaryCachedResource("res/intro.mp3"));
			intro.prefetch();
			intro.start();
		} catch(Throwable e) {
			e.printStackTrace();
		}
		JPanel outerPanel = new JPanel(new GridBagLayout());
		outerPanel.add(m_currentImageLabel, new GridBagConstraints(0, 0, 3, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(MARGIN_TOP, MARGIN_LEFT, PADDING_Y, MARGIN_RIGHT), 0, 0));
		JPanel infoPanel = new JPanel(new GridBagLayout());
		infoPanel.setPreferredSize(new Dimension(MAX_IMAGE_WIDTH_PIXELS, INFO_PANEL_HEIGHT));
		infoPanel.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, PADDING_Y / 2, PADDING_X / 2), 0, 0));
		infoPanel.add(m_nameTextField, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, PADDING_X / 2, PADDING_Y / 2, 0), 0, 0));
		infoPanel.add(new JLabel("PIN:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(PADDING_Y / 2, 0, 0, PADDING_X / 2), 0, 0));
		infoPanel.add(m_pinTextField, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(PADDING_Y / 2, PADDING_X / 2, 0, 0), 0, 0));
		infoPanel.add(m_reasonLabel, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(PADDING_Y / 2, PADDING_X / 2, 0, 0), 0, 0));
		
		outerPanel.add(infoPanel, new GridBagConstraints(0, 1, 3, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(PADDING_Y, MARGIN_LEFT, PADDING_Y, MARGIN_RIGHT), 0, 0));
		m_screenshotButton = new JButton(new AbstractAction("Screenshot") {
			public void actionPerformed(ActionEvent e) {
				takeScreenshot();
			}
		});
		m_screenshotButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
		m_screenshotButton.setEnabled(false);
		outerPanel.add(m_screenshotButton, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(PADDING_Y, MARGIN_LEFT, MARGIN_BOTTOM, PADDING_X), 0, 0));
		m_webcamButton = new JButton(new AbstractAction("Webcam") {
			public void actionPerformed(ActionEvent e) {
				takePictureWithWebcam();
			}
		});
		m_webcamButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
		m_webcamButton.setEnabled(false);
		outerPanel.add(m_webcamButton, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(PADDING_Y, PADDING_X, MARGIN_BOTTOM, PADDING_X), 0, 0));
		m_uploadButton = new JButton(new AbstractAction("Upload") {
			public void actionPerformed(ActionEvent e) {
				uploadButtonPressed();
			}
		});
		m_uploadButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
		m_uploadButton.setEnabled(false);
		outerPanel.add(m_uploadButton, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(PADDING_Y, PADDING_X, MARGIN_BOTTOM, MARGIN_RIGHT), 0, 0));
		m_frame.getContentPane().add(outerPanel);
		m_frame.pack();
		m_frame.setVisible(true);
	}
	public Window getWindow() {
		return m_frame;
	}
	private void setupTimer() {
		m_timer = new Timer();
		m_timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						timerTicked();
					}
				});
			}
		}, TIMER_DELAY_MILLISECONDS, TIMER_PERIOD_MILLISECONDS);
	}
	private boolean shouldTakeAutoScreenshotNow() {
		return (m_captureType == SCREENSHOT_CAPTURE_TYPE) && ((m_lastScreenshotDate == null) || (((new Date()).getTime() - m_lastScreenshotDate.getTime()) > AUTO_SCREENSHOT_AFTER_MILLISECONDS));
	}
	private boolean shouldCheckConnectionNow() {
		String p = m_pinTextField.getText();
		if(m_pin != null && !m_pin.contentEquals(p)) {
			m_pin = null;
		}
		long expiry = (m_lastConnectionCheckDate != null) ? ((new Date()).getTime() - m_lastConnectionCheckDate.getTime()) : 0;
		boolean rv = ((m_pin == null && !p.isEmpty()) || (m_pin != null && expiry > CHECK_CONNECTION_PERIOD_MILLISECONDS)); 
		return rv;
	}
	private void checkConnection() {
		String p = m_pinTextField.getText();
		if(m_checking == null) {
			m_checking = p;
			checkConnectionAsync();
		} else if(!m_checking.contentEquals(p) && (m_recheck == null || !m_recheck.contentEquals(p))) { 
			m_recheck = p;
		}
	}
	private void setButtonsEnabled(boolean e) {
		m_screenshotButton.setEnabled(e);
		m_uploadButton.setEnabled(e);
		m_webcamButton.setEnabled(e);
	}
	private void disableButtons() {
		setButtonsEnabled(false);
	}
	private void enableButtons() {
		setButtonsEnabled(true);
	}
	private void checkConnectionAsync() {
		new Thread(new Runnable() {
			public void run() {
				boolean enable = false;
				boolean connected = false;
				while(m_checking != null) {
					String p = m_checking;
					try {
						HttpGet get = new HttpGet(m_baseUrl + "verify?p=" + URLEncoder.encode(p,"UTF-8"));
						HttpClient client = new DefaultHttpClient();
						HttpResponse response = client.execute(get);
						int status = response.getStatusLine().getStatusCode();
						connected = true;
						if(status >= 200 && status <= 299) {
							try {
								String body = EntityUtils.toString(response.getEntity()).trim();
								enable = Boolean.parseBoolean(body);
							} catch(Throwable e) {
								enable = false;
							}
							if(enable) {
								m_lastConnectionCheckDate = new Date();
								m_pin = p;
								connected = true;
							}
						} else if(status == 404) {
							connected = false;
						}
					} catch(Throwable e) {
						e.printStackTrace();
						connected = false;
					}
					m_checking = m_recheck;
					m_recheck = null;
				}
				final String reason = enable ? "" : (connected ? "Invalid PIN" : "Can\'t connect to Ambire cloud");
				if(enable) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							m_reasonLabel.setText(reason);
							enableButtons();
						}
					});
				} else {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							m_reasonLabel.setText(reason);
							disableButtons();
						}
					});
				}
			}
		}).start();
	}
	private void timerTicked() {
		if(m_modality) {
			return;
		} else if(shouldCheckConnectionNow()) {
			checkConnection();
		} else if(m_pin != null && shouldTakeAutoScreenshotNow()) {
			takeScreenshot();
		}
	}
	private BufferedImage chooseBufferedImageAWT() {
		try {
			FileDialog dialog = new FileDialog(m_frame, "Open BufferedImage File...");
			dialog.setMode(FileDialog.LOAD);
			dialog.setVisible(true);
			String dir = dialog.getDirectory();
			String fname = dialog.getFile();
			if(dir == null || fname == null) {
				return null;
			}
			File file = new File(dir, fname);
			return ImageIO.read(file);
		} catch(Throwable e) {
			e.printStackTrace();
		}
		playErrorSound();
		return null;
	}
	private BufferedImage chooseBufferedImageJNLP() {
		try {
			FileOpenService service = null;
			try {
				service = (FileOpenService)ServiceManager.lookup("javax.jnlp.FileOpenService");
			} catch(UnavailableServiceException ue) {
				return chooseBufferedImageAWT();
			}
			FileContents fifo = service.openFileDialog(null, new String[] { "jpg", "jpeg", "gif", "png", "bmp", "tif", "tiff" });
			if(fifo == null) {
				return null;
			}
			return ImageIO.read(fifo.getInputStream());
		} catch(Throwable e) {
			e.printStackTrace();
		}
		playErrorSound();
		return null;
	}
	private void uploadButtonPressed() {
		BufferedImage img = chooseBufferedImageJNLP();
		if(img != null) {
			m_captureType = UPLOAD_CAPTURE_TYPE;
			useThisImage(img, "upload", false);
		}
	}
	private void takeScreenshot() {
		m_lastScreenshotDate = new Date();
		BufferedImage i = null;
		try {
			Robot robot = new Robot();
			Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			i = robot.createScreenCapture(rect);
		} catch(Throwable e) {
			e.printStackTrace();
			i = null;
		}
		if(i == null) {
			playErrorSound();
			return;
		}
		playShutterSound();
		m_captureType = SCREENSHOT_CAPTURE_TYPE;
		useThisImage(i, "screenshot", true);
	}
	private static String pathescape(String s) {
		StringBuffer b = new StringBuffer();
		int n = s.length();
		for(int i = 0; i < n; ++i) {
			char c = s.charAt(i);
			if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_') || (c == '-') || (c == '.')) {
				b.append(c);
			}
		}
		if(b.length() < 20) {
			b.insert(0, String.format("p%x", System.currentTimeMillis()));
		} else if(b.length() > 30) {
			b.delete(30, b.length() - 30);
		}
		char x = b.charAt(0);
		if((x < 'A' || x > 'Z') && (x < 'a' || x > 'z')) {
			b.insert(0, (char)('a' + (System.currentTimeMillis() % 26)));
		}
		return b.toString();
	}
	private void useThisImage(Image i, String kind, boolean replace) {
		final BufferedImage scaled = scale(i);
		final String name = m_nameTextField.getText();
		final String kindParam = m_uniqueIdentifier + "-" + kind;
		final String replaceParam = replace ? "replace" : null;
		m_currentImageLabel.setIcon(new ImageIcon(scaled));
		new Thread(new Runnable() {
			private boolean upload() {
				byte[] bytes = null;
				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ImageIO.write(scaled, "JPG", ImageIO.createImageOutputStream(bos));
					bytes = bos.toByteArray();
					HttpPost post = new HttpPost(m_baseUrl + "upload");
					MultipartEntity entity = new MultipartEntity();
					entity.addPart("name", new StringBody(name));
					entity.addPart("pin", new StringBody(m_pin));
					entity.addPart("kind", new StringBody(kindParam));
					String width = Integer.toString(scaled.getWidth());
					String height = Integer.toString(scaled.getHeight());
					entity.addPart("width", new StringBody(width));
					entity.addPart("height", new StringBody(height));
					if(replaceParam != null) {
						entity.addPart("replace", new StringBody(replaceParam));
					}
					entity.addPart("file", new ByteArrayBody(bytes, "image/jpeg", pathescape(name + "_" + m_pin + ".jpg")));
					post.setEntity(entity);
					HttpClient client = new DefaultHttpClient();
					HttpResponse response = null;
					response = client.execute(post);
					int status = response.getStatusLine().getStatusCode();
					if(status >= 200 && status <= 299) {
						return true;
					}
					System.err.println("HTTP/1.1 " + status + " " + response.getStatusLine().getReasonPhrase());
					Header[] headers = response.getAllHeaders();
					StringBuffer sb = new StringBuffer();
					for(int i = 0; i < headers.length; ++i) {
						sb.append(headers[i].getName());
						sb.append(": ");
						sb.append(headers[i].getValue());
						sb.append("\r\n");
					}
					System.err.println(sb.toString());
					System.err.println(EntityUtils.toString(response.getEntity()));
				} catch(Throwable e) {
					e.printStackTrace();
				}
				return false;
			}
			public void run() {
				if(!upload()) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							playErrorSound();
						}
					});
				}
			}
		}).start();
	}
	public void playShutterSound() {
		if(m_shutterSound == null) {
			return;
		}
		m_shutterSound.setMediaTime(new Time(0.0));
		m_shutterSound.start();
	}
	public void playErrorSound() {
		if(m_errorSound == null) {
			return;
		}
		m_errorSound.setMediaTime(new Time(0.0));
		m_errorSound.start();
	}
	private void takePictureWithWebcam() {
		m_modality = true;
		WebcamCaptureDialog.beginWebcamCaptureDialog(this, new ImageCaptureListener() {
			public void imageCaptured(Image img) {
				m_modality = false;
				m_captureType = WEBCAM_CAPTURE_TYPE;
				useThisImage(img, "webcam", false);
			}
			public void captureCancelled() {
				m_modality = false;
			}
		});
	}
}
