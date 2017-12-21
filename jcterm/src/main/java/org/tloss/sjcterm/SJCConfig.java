package org.tloss.sjcterm;

public class SJCConfig {
	private String destination;
	private String xhost = "127.0.0.1";
	private int xport = 0;
	private boolean xforwarding = false;

	private String proxy_http_host = null;
	private int proxy_http_port = 0;

	private String proxy_socks5_host = null;
	private int proxy_socks5_port = 0;

	private int mode = com.jcraft.jcterm.Frame.SHELL;

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getXhost() {
		return xhost;
	}

	public void setXhost(String xhost) {
		this.xhost = xhost;
	}

	public int getXport() {
		return xport;
	}

	public void setXport(int xport) {
		this.xport = xport;
	}

	public boolean isXforwarding() {
		return xforwarding;
	}

	public void setXforwarding(boolean xforwarding) {
		this.xforwarding = xforwarding;
	}

	public String getProxy_http_host() {
		return proxy_http_host;
	}

	public void setProxy_http_host(String proxy_http_host) {
		this.proxy_http_host = proxy_http_host;
	}

	public int getProxy_http_port() {
		return proxy_http_port;
	}

	public void setProxy_http_port(int proxy_http_port) {
		this.proxy_http_port = proxy_http_port;
	}

	public String getProxy_socks5_host() {
		return proxy_socks5_host;
	}

	public void setProxy_socks5_host(String proxy_socks5_host) {
		this.proxy_socks5_host = proxy_socks5_host;
	}

	public int getProxy_socks5_port() {
		return proxy_socks5_port;
	}

	public void setProxy_socks5_port(int proxy_socks5_port) {
		this.proxy_socks5_port = proxy_socks5_port;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

}
