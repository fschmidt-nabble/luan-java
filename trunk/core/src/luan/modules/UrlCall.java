package luan.modules;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.IOException;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;


public final class UrlCall {
	public final URLConnection connection;

	public UrlCall(String url) throws IOException {
		this(new URL(url));
	}

	public UrlCall(URL url) throws IOException {
		connection = url.openConnection();
	}

	public void acceptJson() {
		connection.setRequestProperty("accept","application/json");
	}

	public String get() throws IOException {
		Reader in = new InputStreamReader(connection.getInputStream());
		String rtn = Utils.readAll(in);
		in.close();
		return rtn;
	}

	public String post(String content,String contentType) throws IOException {
		HttpURLConnection connection = (HttpURLConnection)this.connection;

		connection.setRequestProperty("Content-type",contentType);
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");

		byte[] post = content.getBytes();
		connection.setRequestProperty("Content-Length",Integer.toString(post.length));
		OutputStream out = connection.getOutputStream();
		out.write(post);
		out.flush();

		Reader in;
		try {
			in = new InputStreamReader(connection.getInputStream());
		} catch(IOException e) {
			InputStream is = connection.getErrorStream();
			if( is == null )
				throw e;
			in = new InputStreamReader(is);
			String msg = Utils.readAll(in);
			in.close();
			throw new UrlCallException(msg,e);
		}
		String rtn = Utils.readAll(in);
		in.close();
		out.close();
		return rtn;
	}

	public String post(String content) throws IOException {
		return post(content,"application/x-www-form-urlencoded");
	}

	public String postJson(String content) throws IOException {
		return post(content,"application/json");
	}

	public static final class UrlCallException extends IOException {
		UrlCallException(String msg,IOException e) {
			super(msg,e);
		}
	}
}
