package luan.modules.web;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Enumeration;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import luan.Luan;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanElement;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanExitException;
import luan.DeepCloner;
import luan.modules.PackageLuan;
import luan.modules.IoLuan;


public final class HttpLuan {

	public static final LuanFunction LOADER = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			LuanTable module = Luan.newTable();
			try {
				addStatic( module, "new_luan_handler", LuanState.class );
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			return module;
		}
	};

	private static void addStatic(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(HttpLuan.class.getMethod(method,parameterTypes),null) );
	}

	public static LuanHandler new_luan_handler(LuanState luan) {
		return new LuanHandler(luan);
	}

	public static boolean service(LuanState luan,HttpServletRequest request,HttpServletResponse response,String modName)
		throws LuanException
	{
		LuanFunction fn;
		synchronized(luan) {
			Object mod = PackageLuan.load(luan,modName);
			if( mod==null )
				return false;
			if( !(mod instanceof LuanTable) )
				throw luan.exception( "module '"+modName+"' must return a table" );
			LuanTable tbl = (LuanTable)mod;
			if( Luan.toBoolean( tbl.get("per_session") ) ) {
				HttpSession session = request.getSession();
				LuanState sessionLuan  = (LuanState)session.getValue("luan");
				if( sessionLuan!=null ) {
					luan = sessionLuan;
				} else {
					DeepCloner cloner = new DeepCloner();
					luan = cloner.deepClone(luan);
					session.putValue("luan",luan);
				}
				tbl = (LuanTable)PackageLuan.require(luan,modName);
				fn = (LuanFunction)tbl.get("service");
			} else {
				fn = (LuanFunction)tbl.get("service");
				if( fn == null )
					throw luan.exception( "function 'service' is not defined" );
				DeepCloner cloner = new DeepCloner();
				luan = cloner.deepClone(luan);
				fn = cloner.get(fn);
			}
		}

		LuanTable module = (LuanTable)PackageLuan.loaded(luan).get("web/Http");
		if( module == null )
			throw luan.exception( "module 'web/Http' not defined" );
		HttpLuan lib = new HttpLuan(request,response);
		try {
			module.put( "request", lib.requestTable() );
			module.put( "response", lib.responseTable() );
			module.put( "cookie", lib.cookieTable() );
			module.put( "session", lib.sessionTable() );
/*
			module.put( "write", new LuanJavaFunction(
				HttpLuan.class.getMethod( "text_write", LuanState.class, new Object[0].getClass() ), lib
			) );
*/
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		luan.call(fn,"<http>");
		return true;
	}



	private final HttpServletRequest request;
	private final HttpServletResponse response;
//	private PrintWriter writer = null;
//	private ServletOutputStream sos = null;

	private HttpLuan(HttpServletRequest request,HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	private LuanTable requestTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newTable();
		tbl.put("java",request);
		add( tbl, "get_parameter", String.class );
		tbl.put( "get_header", new LuanJavaFunction(
			HttpServletRequest.class.getMethod("getHeader",String.class), request
		) );
		tbl.put( "get_method", new LuanJavaFunction(
			HttpServletRequest.class.getMethod("getMethod"), request
		) );
		tbl.put( "get_servlet_path", new LuanJavaFunction(
			HttpServletRequest.class.getMethod("getServletPath"), request
		) );
		tbl.put( "get_server_name", new LuanJavaFunction(
			HttpServletRequest.class.getMethod("getServerName"), request
		) );
		add( tbl, "get_current_url" );
		tbl.put( "get_remote_address", new LuanJavaFunction(
			HttpServletRequest.class.getMethod("getRemoteAddr"), request
		) );
		return tbl;
	}

	private LuanTable responseTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newTable();
		tbl.put("java",response);
		add( tbl, "send_redirect", String.class );
		add( tbl, "send_error", Integer.TYPE, String.class );
		tbl.put( "contains_header", new LuanJavaFunction(
			HttpServletResponse.class.getMethod("containsHeader",String.class), response
		) );
		tbl.put( "set_header", new LuanJavaFunction(
			HttpServletResponse.class.getMethod("setHeader",String.class,String.class), response
		) );
		tbl.put( "set_content_type", new LuanJavaFunction(
			HttpServletResponse.class.getMethod("setContentType",String.class), response
		) );
		tbl.put( "set_character_encoding", new LuanJavaFunction(
			HttpServletResponse.class.getMethod("setCharacterEncoding",String.class), response
		) );
		add( tbl, "text_writer" );
		return tbl;
	}

	private LuanTable cookieTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newTable();
		tbl.put( "get", new LuanJavaFunction(
			HttpLuan.class.getMethod("get_cookie",String.class), this
		) );
		tbl.put( "set", new LuanJavaFunction(
			HttpLuan.class.getMethod("set_cookie", String.class, String.class, Boolean.TYPE, String.class), this
		) );
		tbl.put( "remove", new LuanJavaFunction(
			HttpLuan.class.getMethod("remove_cookie", String.class, String.class), this
		) );
		return tbl;
	}

	private LuanTable sessionTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newTable();
		tbl.put( "get_attribute", new LuanJavaFunction(
			HttpLuan.class.getMethod("get_session_attribute",String.class), this
		) );
		tbl.put( "set_attribute", new LuanJavaFunction(
			HttpLuan.class.getMethod("set_session_attribute",String.class, Object.class), this
		) );
		return tbl;
	}

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(HttpLuan.class.getMethod(method,parameterTypes),this) );
	}
/*
	public void text_write(LuanState luan,Object... args) throws LuanException, IOException {
		if( writer == null )
			writer = response.getWriter();
		for( Object obj : args ) {
			writer.print( luan.toString(obj) );
		}
	}
*/
	public LuanTable text_writer() throws IOException {
		return IoLuan.textWriter(response.getWriter());
	}

	public Object get_parameter(String name) {
		String[] a = request.getParameterValues(name);
		return a==null ? null : a.length==1 ? a[0] : a;
	}

	public String get_cookie(String name) {
		return getCookieValue(request, name);
	}

	public String get_current_url() {
		return getCurrentURL(request);
	}

	public void send_redirect(String redirectUrl)
		throws IOException
	{
		response.sendRedirect(redirectUrl);
		throw new LuanExitException();
	}

	public void send_error(int code,String text)
		throws IOException
	{
		response.sendError(code, text);
		throw new LuanExitException();
	}

	public void set_cookie(String name,String value,boolean isPersistent, String domain) {
		setCookie(request,response,name,value,isPersistent,domain);
	}

	public void remove_cookie(String name, String domain) {
		removeCookie(request,response,name,domain);
	}

	public Object get_session_attribute(String name) {
		return request.getSession().getAttribute(name);
	}

	public void set_session_attribute(String name,Object value) {
		request.getSession().setAttribute(name,value);
	}


	// static utils

	public static String getQueryString(HttpServletRequest request) {
		return getQueryString(request,0);
	}

	public static String getQueryString(HttpServletRequest request,int maxValueLen) {
		String method = request.getMethod();
		if( method.equals("GET") )
			return request.getQueryString();
		if( !method.equals("POST") && !method.equals("HEAD") )
			throw new RuntimeException(method);
		Enumeration en = request.getParameterNames();
		StringBuilder queryBuf = new StringBuilder();
		if( !en.hasMoreElements() )
			return null;
		do {
			String param = (String)en.nextElement();
			String value = request.getParameter(param);
			if( maxValueLen > 0 ) {
				int len = value.length();
				if( len > maxValueLen )
					value = value.substring(0,maxValueLen) + "..." + (len-maxValueLen);
			}
			queryBuf.append(param);
			queryBuf.append('=');
			queryBuf.append(value);
			queryBuf.append('&');
		} while( en.hasMoreElements() );
		queryBuf.deleteCharAt(queryBuf.length() - 1);
		return queryBuf.toString();
	}

	public static String getCurrentURL(HttpServletRequest request) {
		return getCurrentURL(request,0);
	}

	public static String getCurrentURL(HttpServletRequest request,int maxValueLen) {
//		StringBuffer buf = HttpUtils.getRequestURL(request);
		StringBuffer buf = request.getRequestURL();
		String qStr = getQueryString(request,maxValueLen);
		if(qStr != null && qStr.length() > 0) {
			buf.append('?');
			buf.append(qStr);
		}
		return buf.toString();
	}

	private static String escape(String value) {
		return value.replaceAll(";", "%3B");
	}

	private static String unescape(String value) {
		return value.replaceAll("%3B", ";");
	}

	private static Cookie getCookie(HttpServletRequest request,String name) {
		Cookie[] cookies = request.getCookies();
		if( cookies == null )
			return null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name))
				return cookie;
		}
		return null;
	}

	public static String getCookieValue(HttpServletRequest request,String name) {
		Cookie cookie = getCookie(request,name);
		return cookie==null ? null : unescape(cookie.getValue());
	}

	public static void setCookie(HttpServletRequest request,HttpServletResponse response,String name,String value,boolean isPersistent, String domain) {
		Cookie cookie = getCookie(request,name);
		if( cookie==null || !cookie.getValue().equals(value) ) {
			cookie = new Cookie(name, escape(value));
			cookie.setPath("/");
			if (domain != null && domain.length() > 0)
				cookie.setDomain(domain);
			if( isPersistent )
				cookie.setMaxAge(10000000);
			response.addCookie(cookie);
		}
	}

	public static void removeCookie(HttpServletRequest request,
									HttpServletResponse response,
									String name,
									String domain

	) {
		Cookie cookie = getCookie(request, name);
		if(cookie != null) {
			Cookie delCookie = new Cookie(name, "delete");
			delCookie.setPath("/");
			delCookie.setMaxAge(0);
			if (domain != null && domain.length() > 0)
				delCookie.setDomain(domain);
			response.addCookie(delCookie);
		}
	}

}
