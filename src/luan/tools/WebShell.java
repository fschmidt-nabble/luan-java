package luan.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import luan.Luan;
import luan.LuanFunction;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanException;
import luan.lib.BasicLib;
import luan.lib.HtmlLib;


public class WebShell extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(WebShell.class);

	protected LuanState newLuanState() throws LuanException {
		return LuanState.newStandard();
	}

	protected Object[] eval(LuanState luan,String cmd) throws LuanException {
		return Luan.array(luan.eval(cmd,"WebShell",true));
	}

	@Override protected void service(HttpServletRequest request,HttpServletResponse response)
		throws ServletException, IOException
	{
		PrintWriter out = response.getWriter();
		HttpSession session = request.getSession();

		ByteArrayOutputStream history = (ByteArrayOutputStream)session.getValue("luan_history");
		if( history==null ) {
			history = new ByteArrayOutputStream();
			session.putValue("luan_history",history);
		}

		if( request.getParameter("clear") != null ) {
			history.reset();
		} else {
			String cmd = request.getParameter("cmd");
			if( cmd != null ) {
				Writer writer = new OutputStreamWriter(history);
				writer.write( "% " + HtmlLib.encode(cmd) + "\r\n" );
				writer.flush();
				try {
					LuanState luan  = (LuanState)session.getValue("luan");
					if( luan==null ) {
						luan = newLuanState();
						session.putValue("luan",luan);
					}
					luan.out = new PrintStream(history);
					LuanTable env = luan.global();
					env.put("request",request);
					env.put("response",response);
					Object[] result = eval(luan,cmd);
					if( result.length > 0 ) {
						for( int i=0; i<result.length; i++ ) {
							if( i > 0 )
								writer.write("  ");
							writer.write(HtmlLib.encode(luan.JAVA.toString(result[i])));
						}
						writer.write("\r\n");
					}
				} catch(LuanException e) {
					logger.info("",e);
					writer.write( HtmlLib.encode(e.toString()) );
					writer.write("\r\n");
				}
				writer.flush();
			}
		}

		out.println( "<html>" );
		out.println( "<title>Luan Shell</title>" );
		out.println( "<body>" );
		out.println( "<p>This is a command shell.  Enter commands below." );
		out.println( "<pre>" + history + "</pre>" );
		out.println( "<form name='theForm' method='post'>" );
		out.println( "% <input name='cmd' size=60>" );
		out.println( "<input type=submit value=run>" );
		out.println( "<input type=submit name=clear value=clear>" );
		out.println( "</form>" );
		
		out.println( "<script>document.theForm.cmd.focus();</script>" );
		
		out.println( "<p>" );
		out.println( "</body>" );
		out.println( "</html>" );
	}
}
