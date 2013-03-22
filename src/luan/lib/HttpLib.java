package luan.lib;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Enumeration;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanElement;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanExitException;


public final class HttpLib {

	public static final String NAME = "Http";
	public static final String FN_NAME = "Http.server";

	public static void load(LuanState luan) throws LuanException {
		PackageLib.require(luan,NAME);
		Object fn = luan.get(HttpLib.FN_NAME);
		if( !(fn instanceof LuanFunction) )
			throw luan.JAVA.exception( "function '"+HttpLib.FN_NAME+"' not defined" );
	}

	public static void service(LuanState luan,HttpServletRequest request,HttpServletResponse response)
		throws LuanException, IOException
	{
		LuanFunction fn = (LuanFunction)luan.get(FN_NAME);
		ServletOutputStream sout = response.getOutputStream();
		luan.out = new PrintStream(sout);

		LuanTable module = (LuanTable)luan.loaded().get(NAME);

		try {
			new HttpLib(request,response,module);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		luan.JAVA.call(fn,FN_NAME);
	}

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	private HttpLib(HttpServletRequest request,HttpServletResponse response,LuanTable module) throws NoSuchMethodException {
		this.request = request;
		this.response = response;

		LuanTable req = new LuanTable();
		module.put("request",req);
		LuanTable resp = new LuanTable();
		module.put("response",resp);

		req.put( "get_attribute", new LuanJavaFunction(HttpServletRequest.class.getMethod("getAttribute",String.class),request) );
		req.put( "set_attribute", new LuanJavaFunction(HttpServletRequest.class.getMethod("setAttribute",String.class,Object.class),request) );
		req.put( "get_parameter", new LuanJavaFunction(HttpServletRequest.class.getMethod("getParameter",String.class),request) );
		req.put( "get_parameter_values", new LuanJavaFunction(HttpServletRequest.class.getMethod("getParameterValues",String.class),request) );
		req.put( "get_header", new LuanJavaFunction(HttpServletRequest.class.getMethod("getHeader",String.class),request) );
		add( req, "get_cookie_value", String.class );
		req.put( "method", new LuanJavaFunction(HttpServletRequest.class.getMethod("getMethod"),request) );
		req.put( "servlet_path", new LuanJavaFunction(HttpServletRequest.class.getMethod("getServletPath"),request) );
		req.put( "server_name", new LuanJavaFunction(HttpServletRequest.class.getMethod("getServerName"),request) );
		add( req, "current_url" );
		req.put( "remote_address", new LuanJavaFunction(HttpServletRequest.class.getMethod("getRemoteAddr"),request) );

		add( resp, "send_redirect", String.class );
		add( resp, "send_error", Integer.TYPE, String.class );
		resp.put( "contains_header", new LuanJavaFunction(HttpServletResponse.class.getMethod("containsHeader",String.class),response) );
		resp.put( "set_header", new LuanJavaFunction(HttpServletResponse.class.getMethod("setHeader",String.class,String.class),response) );
		add( resp, "set_cookie", String.class, String.class, Boolean.TYPE, String.class );
		add( resp, "remove_cookie", String.class, String.class );
	}

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(HttpLib.class.getMethod(method,parameterTypes),this) );
	}

	public String get_cookie_value(String name) {
		return getCookieValue(request, name);
	}

	public String current_url() {
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