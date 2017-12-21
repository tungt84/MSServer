/* -*-mode:java; c-basic-offset:2; -*- */
/* JCTermSwingFrame
 * Copyright (C) 2002,2007 ymnk, JCraft,Inc.
 *  
 * Written by: ymnk<ymnk@jcaft.com>
 *   
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jcterm;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * 
 * @author tungt Receiving the host, username, password and auto login in
 *         channel shell
 */
public class SJCTermSwingFrame extends JFrame implements Frame, ActionListener, Runnable {
	static String COPYRIGHT = "JCTerm 0.0.11\nCopyright (C) 2002,2012 ymnk<ymnk@jcraft.com>, JCraft,Inc.\n"
			+ "Official Homepage: http://www.jcraft.com/jcterm/\n" + "This software is licensed under GNU LGPL.";

	private static int counter = 1;

	static void resetCounter() {
		counter = 1;
	}

	private int mode = SHELL;

	private String user = "";
	private String host = "127.0.0.1";

	private JSchSession jschsession = null;
	private Proxy proxy = null;

	private int compression = 0;

	private JCTermSwing term = null;

	private Connection connection = null;

	private Channel channel = null;

	private boolean close_on_exit = true;

	private Frame frame = this;

	private String configName = "default";
	private String destination;
	private IPasswordProvider passwordProvider;

	public boolean getCloseOnExit() {
		return close_on_exit;
	}

	public void setCloseOnExit(boolean close_on_exit) {
		this.close_on_exit = close_on_exit;
	}

	public SJCTermSwingFrame() {
	}

	public SJCTermSwingFrame(String name, String destination, IPasswordProvider passwordProvider) {
		this(name, "default", destination, passwordProvider);
	}

	public SJCTermSwingFrame(String name, String configName, String destination, IPasswordProvider passwordProvider) {
		super(name);
		this.destination = destination;
		this.configName = configName;
		this.passwordProvider =  passwordProvider;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		enableEvents(AWTEvent.KEY_EVENT_MASK);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		JMenuBar mb = getJMenuBar();
		setJMenuBar(mb);

		term = new JCTermSwing();
		getContentPane().add("Center", term);
		pack();
		term.setVisible(true);

		ComponentAdapter l = new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Component c = e.getComponent();
				Container cp = ((JFrame) c).getContentPane();
				int cw = c.getWidth();
				int ch = c.getHeight();
				int cwm = c.getWidth() - cp.getWidth();
				int chm = c.getHeight() - cp.getHeight();
				cw -= cwm;
				ch -= chm;
				SJCTermSwingFrame.this.term.setSize(cw, ch);
			}
		};
		addComponentListener(l);

		applyConfig(configName);

		openSession();
	}

	private Thread thread = null;

	public void kick() {
		this.thread = new Thread(this);
		this.thread.start();
	}

	public void run() {
		String destination = null;
		while (thread != null) {
			try {
				int port = 22;
				try {
					String _host =  this.destination ;
					destination = _host;
					if (_host == null) {
						break;
					}
					String _user = _host.substring(0, _host.indexOf('@'));
					_host = _host.substring(_host.indexOf('@') + 1);
					if (_host == null || _host.length() == 0) {
						continue;
					}
					if (_host.indexOf(':') != -1) {
						try {
							port = Integer.parseInt(_host.substring(_host.indexOf(':') + 1));
						} catch (Exception eee) {
						}
						_host = _host.substring(0, _host.indexOf(':'));
					}
					user = _user;
					host = _host;
				} catch (Exception ee) {
					continue;
				}

				try {
					UserInfo ui = new MyUserInfo(passwordProvider);

					jschsession = JSchSession.getSession(user, null, host, port, ui, proxy);
					setCompression(compression);

					Configuration conf = JCTermSwing.getCR().load(configName);
					conf.addDestination(destination);
					JCTermSwing.getCR().save(conf);
				} catch (Exception e) {
					e.printStackTrace();
					// System.out.println(e);
					break;
				}

				Channel channel = null;
				OutputStream out = null;
				InputStream in = null;

				if (mode == SHELL) {
					channel = jschsession.getSession().openChannel("shell");
					out = channel.getOutputStream();
					in = channel.getInputStream();

					channel.connect();
				} else if (mode == SFTP) {

					out = new PipedOutputStream();
					in = new PipedInputStream();

					channel = jschsession.getSession().openChannel("sftp");

					channel.connect();

					(new Sftp((ChannelSftp) channel, (InputStream) (new PipedInputStream((PipedOutputStream) out)),
							new PipedOutputStream((PipedInputStream) in))).kick();
				}

				final OutputStream fout = out;
				final InputStream fin = in;
				final Channel fchannel = channel;

				connection = new Connection() {
					public InputStream getInputStream() {
						return fin;
					}

					public OutputStream getOutputStream() {
						return fout;
					}

					public void requestResize(Term term) {
						if (fchannel instanceof ChannelShell) {
							int c = term.getColumnCount();
							int r = term.getRowCount();
							((ChannelShell) fchannel).setPtySize(c, r, c * term.getCharWidth(),
									r * term.getCharHeight());
						}
					}

					public void close() {
						fchannel.disconnect();
					}
				};
				frame.setTitle("[" + (counter++) + "] " + user + "@" + host
						+ (port != 22 ? (":" + new Integer(port).toString()) : ""));
				term.requestFocus();
				term.start(connection);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		frame.setTitle("JCTerm");
		thread = null;

		dispose_connection();

		if (getCloseOnExit()) {
			frame.setVisible(false);
			frame.dispose();
		} else {
			term.clear();
			term.redraw(0, 0, term.getWidth(), term.getHeight());
		}
	}

	void dispose_connection() {
		synchronized (this) {
			if (channel != null) {
				channel.disconnect();
				channel = null;
			}
		}
	}

	public class MyUserInfo implements UserInfo, UIKeyboardInteractive {
		private IPasswordProvider passwordProvider;

		public MyUserInfo(IPasswordProvider passwordProvider) {
			this.passwordProvider = passwordProvider;
		}

		public boolean promptYesNo(String str) {
			Object[] options = { "yes", "no" };
			int foo = JOptionPane.showOptionDialog(SJCTermSwingFrame.this.term, str, "Warning",
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			return foo == 0;
		}

		String passphrase = null;
		JTextField pword = new JPasswordField(20);

		public String getPassword() {
			return passwordProvider.getPassword();
		}

		public String getPassphrase() {
			return passphrase;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public void showMessage(String message) {
			JOptionPane.showMessageDialog(null, message);
		}

		final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		private Container panel;

		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
				boolean[] echo) {
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());

			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;

			gbc.gridwidth = GridBagConstraints.RELATIVE;

			JTextField[] texts = new JTextField[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]), gbc);

				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if (echo[i]) {
					texts[i] = new JTextField(20);
				} else {
					texts[i] = new JPasswordField(20);
					texts[i].requestFocusInWindow();
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}
			for (int i = prompt.length - 1; i > 0; i--) {
				texts[i].requestFocusInWindow();
			}
			JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
				public void selectInitialValue() {
				}
			};
			JDialog dialog = pane.createDialog(SJCTermSwingFrame.this.term, destination + ": " + name);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			Object o = pane.getValue();
			if (o != null && ((Integer) o).intValue() == JOptionPane.OK_OPTION) {
				String[] response = new String[prompt.length];
				for (int i = 0; i < prompt.length; i++) {
					response[i] = texts[i].getText();
				}
				return response;
			} else {
				return null; // cancel
			}
		}
	}

	public void setCompression(int compression) {
		if (compression < 0 || 9 < compression)
			return;
		this.compression = compression;
		if (jschsession != null) {
			if (compression == 0) {
				jschsession.getSession().setConfig("compression.s2c", "none");
				jschsession.getSession().setConfig("compression.c2s", "none");
				jschsession.getSession().setConfig("compression_level", "0");
			} else {
				jschsession.getSession().setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
				jschsession.getSession().setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
				jschsession.getSession().setConfig("compression_level", new Integer(compression).toString());
			}
			try {
				jschsession.getSession().rekey();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	public void setFontSize(int size) {
		Configuration conf = JCTermSwing.getCR().load(configName);
		conf.font_size = size;
		JCTermSwing.getCR().save(conf);
		_setFontSize(size);
	}

	private void _setFontSize(int size) {
		int mwidth = frame.getWidth() - term.getTermWidth();
		int mheight = frame.getHeight() - term.getTermHeight();
		term.setFont("Monospaced-" + size);
		frame.setSize(mwidth + term.getTermWidth(), mheight + term.getTermHeight());
		term.clear();
		term.redraw(0, 0, term.getWidth(), term.getHeight());
	}

	public int getCompression() {
		return this.compression;
	}

	public void setLineSpace(int foo) {
		term.setLineSpace(foo);
	}

	public boolean getAntiAliasing() {
		return term.getAntiAliasing();
	}

	public void setAntiAliasing(boolean foo) {
		term.setAntiAliasing(foo);
	}

	public void setUserHost(String userhost) {
		try {
			String _user = userhost.substring(0, userhost.indexOf('@'));
			String _host = userhost.substring(userhost.indexOf('@') + 1);
			this.user = _user;
			this.host = _host;
		} catch (Exception e) {
		}
	}

	public void openSession() {
		kick();
	}

	public void setPortForwardingL(int port1, String host, int port2) {
		if (jschsession == null)
			return;
		try {
			jschsession.getSession().setPortForwardingL(port1, host, port2);
		} catch (JSchException e) {
		}
	}

	public void setPortForwardingR(int port1, String host, int port2) {
		if (jschsession == null)
			return;
		try {
			jschsession.getSession().setPortForwardingR(port1, host, port2);
		} catch (JSchException e) {
		}
	}

	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if (action.equals("About...")) {
			JOptionPane.showMessageDialog(this, COPYRIGHT);
			return;
		} else if (action.equals("Quit")) {
			quit();
		}
	}

	public JMenuBar getJMenuBar() {
		JMenuBar mb = new JMenuBar();
		JMenu m;
		JMenuItem mi;

		m = new JMenu("File");
		mi = new JMenuItem("Quit");
		mi.addActionListener(this);
		mi.setActionCommand("Quit");
		m.add(mi);
		mb.add(m);

		m = new JMenu("Help");
		mi = new JMenuItem("About...");
		mi.addActionListener(this);
		mi.setActionCommand("About...");
		m.add(mi);
		mb.add(m);

		return mb;
	}

	public void quit() {
		thread = null;
		if (connection != null) {
			connection.close();
			connection = null;
		}
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	public void setTerm(JCTermSwing term) {
		this.term = term;
	}

	public Term getTerm() {
		return term;
	}

	public void openFrame(int _mode, String configName) {
		SJCTermSwingFrame c = new SJCTermSwingFrame("JCTerm", configName, null);
		c.mode = _mode;
		c.setLocationRelativeTo(null);
		c.setVisible(true);
		c.setResizable(true);
	}

	void setFgBg(String fg_bg) {
		Configuration conf = JCTermSwing.getCR().load(configName);
		conf.addFgBg(fg_bg);
		JCTermSwing.getCR().save(conf);
		_setFgBg(fg_bg);
	}

	private void _setFgBg(String fg_bg) {
		String[] tmp = fg_bg.split(":");
		Color fg = JCTermSwing.toColor(tmp[0]);
		Color bg = JCTermSwing.toColor(tmp[1]);
		term.setForeGround(fg);
		term.setDefaultForeGround(fg);
		term.setBackGround(bg);
		term.setDefaultBackGround(bg);
		term.resetCursorGraphics();
		term.clear();
		term.redraw(0, 0, term.getWidth(), term.getHeight());
	}
	
	void setFrame(Frame frame) {
		this.frame = frame;
	}

	void applyConfig(String configName) {
		this.configName = configName;
		Configuration conf = JCTermSwing.getCR().load(configName);
		_setFontSize(conf.font_size);
		_setFgBg(conf.fg_bg[0]);
	}

	public static void main(String[] arg) {
		JCTermSwing.setCR(new ConfigurationRepositoryFS());
		final SJCTermSwingFrame frame = new SJCTermSwingFrame("JCTerm", "oraosb@192.168.181.63",
				new IPasswordProvider() {
					public String getPassword() {
						return "oraosb";
					}
				});
		frame.setCloseOnExit(false);
		frame.setVisible(true);
		frame.setResizable(true);
	}
}
