package luan.tools;

import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import luan.LuanState;
import luan.LuanException;
import luan.lib.HtmlLib;


public class WebRun extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(WebRun.class);

	protected LuanState newLuanState() throws LuanException {
		return LuanState.newStandard();
	}

	protected void service(HttpServletRequest request,HttpServletResponse response)
		throws ServletException, IOException
	{
		ServletOutputStream sout = response.getOutputStream();
		PrintStream out = new PrintStream(sout);
		String contentType = request.getParameter("content_type");
		if( contentType != null )
			response.setContentType("text/plain");
		String code = request.getParameter("code");
		try {
			LuanState luan = newLuanState();
			luan.out = out;
			luan.global.put("request",request);
			luan.global.put("response",response);
			luan.eval(code,"WebRun");
		} catch(LuanException e) {
			logger.error(null,e);
			response.reset();
			response.setHeader("Content-Type","text/html");
			out.println( "<html>" );
			out.println( "<body>" );
			out.println( "<pre>" );
			out.println( e );
			out.println();
			out.println( addLineNumbers(HtmlLib.encode(code)) );
			out.println( "</pre>" );
			out.println( "</body>" );
			out.println( "</html>" );
		}
	}

	public static String addLineNumbers(String s) {
		StringBuilder buf = new StringBuilder();
		int line = 1;
		int i = 0;
		while(true) {
			buf.append( fmt(line++,3) );
			buf.append("  ");
			int i2 = s.indexOf('\n',i);
			if( i2 == -1 ) {
				buf.append( s.substring(i) );
				break;
			}
			buf.append( s.substring(i,i2+1) );
			i = i2 + 1;
		}
		return buf.toString();
	}

	private static String fmt(int i,int w) {
		StringBuilder buf = new StringBuilder();
		buf.append(i);
		while( buf.length() < w ) {
			buf.insert(0,' ');
		}
		return buf.toString();
	}

}
