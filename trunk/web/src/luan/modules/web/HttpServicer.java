package luan.modules.web;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
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
import luan.AbstractLuanTable;
import luan.LuanJavaFunction;
import luan.LuanExitException;
import luan.LuanProperty;
import luan.DeepCloner;
import luan.modules.PackageLuan;
import luan.modules.IoLuan;
import luan.modules.TableLuan;


public final class HttpServicer {

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

		LuanTable module = (LuanTable)PackageLuan.loaded(luan).get("luan:web/Http");
		if( module == null )
			throw luan.exception( "module 'web/Http' not defined" );
		HttpServicer lib = new HttpServicer(request,response);
		try {
			module.put( "request", lib.requestTable() );
			module.put( "response", lib.responseTable() );
			module.put( "session", lib.sessionTable() );
/*
			module.put( "write", new LuanJavaFunction(
				HttpServicer.class.getMethod( "text_write", LuanState.class, new Object[0].getClass() ), lib
			) );
*/
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		try {
			luan.call(fn,"<http>");
		} catch(LuanExitException e) {
//			System.out.println("caught LuanExitException");
		}
		return true;
	}



	private final HttpServletRequest request;
	private final HttpServletResponse response;
//	private PrintWriter writer = null;
//	private ServletOutputStream sos = null;

	private HttpServicer(HttpServletRequest request,HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	private LuanTable requestTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newPropertyTable();
		tbl.put("java",request);
		LuanTable parameters = new NameTable() {

			@Override Object get(String name) {
				return request.getParameter(name);
			}

			@Override Iterator<String> names() {
				return new EnumerationIterator<String>(request.getParameterNames());
			}

			@Override protected String type() {
				return "request.parameters-table";
			}
		};
		tbl.put( "parameters", parameters );
		add( tbl, "get_parameter_values", String.class );
		LuanTable headers = new NameTable() {

			@Override Object get(String name) {
				return request.getHeader(name);
			}

			@Override Iterator<String> names() {
				return new EnumerationIterator<String>(request.getHeaderNames());
			}

			@Override protected String type() {
				return "request.headers-table";
			}
		};
		tbl.put( "headers", headers );
		tbl.put( "method", new LuanProperty() { public Object get() {
			return request.getMethod();
		} } );
/*
		tbl.put( "servlet_path", new LuanProperty() { public Object get() {
			return request.getServletPath();
		} } );
*/
		tbl.put( "path", new LuanProperty() { public Object get() {
			return request.getRequestURI();
		} } );
		tbl.put( "server_name", new LuanProperty() { public Object get() {
			return request.getServerName();
		} } );
		tbl.put( "url", new LuanProperty() { public Object get() {
			return getURL(request);
		} } );
		tbl.put( "query_string", new LuanProperty() { public Object get() {
			return getQueryString(request);
		} } );
		tbl.put( "remote_address", new LuanProperty() { public Object get() {
			return request.getRemoteAddr();
		} } );
		LuanTable cookies = new AbstractLuanTable() {

			@Override public final Object get(Object key) {
				if( !(key instanceof String) )
					return null;
				String name = (String)key;
				return getCookieValue(request,name);
			}

			@Override public final Iterator<Map.Entry<Object,Object>> iterator() {
				return new Iterator<Map.Entry<Object,Object>>() {
					final Cookie[] cookies = request.getCookies();
					int i = 0;
	
					@Override public boolean hasNext() {
						return i < cookies.length;
					}
					@Override public Map.Entry<Object,Object> next() {
						Cookie cookie = cookies[i++];
						String name = cookie.getName();
						Object val = unescape(cookie.getValue());
						return new AbstractMap.SimpleEntry<Object,Object>(name,val);
					}
					@Override public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override protected String type() {
				return "request.cookies-table";
			}
		};
		tbl.put( "cookies", cookies );
		return tbl;
	}

	private LuanTable responseTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newPropertyTable();
		tbl.put("java",response);
		add( tbl, "send_redirect", String.class );
		add( tbl, "send_error", Integer.TYPE, String.class );
		LuanTable headers = new NameTable() {

			@Override Object get(String name) {
				return response.getHeader(name);
			}

			@Override Iterator<String> names() {
				return response.getHeaderNames().iterator();
			}

			@Override public void put(Object key,Object val) {
				if( !(key instanceof String) )
					throw new IllegalArgumentException("key must be string for headers table");
				String name = (String)key;
				if( val instanceof String ) {
					response.setHeader(name,(String)val);
					return;
				}
				Integer i = Luan.asInteger(val);
				if( i != null ) {
					response.setIntHeader(name,i);
					return;
				}
				throw new IllegalArgumentException("value must be string or integer for headers table");
			}

			@Override protected String type() {
				return "response.headers-table";
			}
		};
		tbl.put( "headers", headers );
		tbl.put( "content_type", new LuanProperty() {
			@Override public Object get() {
				return response.getContentType();
			}
			@Override public boolean set(Object value) {
				response.setContentType(string(value));  return true;
			}
		} );
		tbl.put( "character_encoding", new LuanProperty() {
			@Override public Object get() {
				return response.getCharacterEncoding();
			}
			@Override public boolean set(Object value) {
				response.setCharacterEncoding(string(value));  return true;
			}
		} );
		add( tbl, "text_writer" );
		add( tbl, "set_cookie", String.class, String.class, Boolean.TYPE, String.class );
		add( tbl, "remove_cookie", String.class, String.class );
		return tbl;
	}

	private LuanTable sessionTable() throws NoSuchMethodException {
		LuanTable tbl = Luan.newTable();
		LuanTable attributes = new NameTable() {

			@Override Object get(String name) {
				return request.getSession().getAttribute(name);
			}

			@Override Iterator<String> names() {
				return new EnumerationIterator<String>(request.getSession().getAttributeNames());
			}

			@Override public void put(Object key,Object val) {
				if( !(key instanceof String) )
					throw new IllegalArgumentException("key must be string for session attributes table");
				String name = (String)key;
				request.getSession().setAttribute(name,val);
			}

			@Override protected String type() {
				return "session.attributes-table";
			}
		};
		tbl.put( "attributes", attributes );
		return tbl;
	}

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(HttpServicer.class.getMethod(method,parameterTypes),this) );
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

	public LuanTable get_parameter_values(String name) {
		Object[] a = request.getParameterValues(name);
		return a==null ? null : TableLuan.pack(a);
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

	public static String getURL(HttpServletRequest request) {
		return getURL(request,0);
	}

	public static String getURL(HttpServletRequest request,int maxValueLen) {
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



	// util classes

	static final class EnumerationIterator<E> implements Iterator<E> {
		private final Enumeration<E> en;

		EnumerationIterator(Enumeration<E> en) {
			this.en = en;
		}

		@Override public boolean hasNext() {
			return en.hasMoreElements();
		}

		@Override public E next() {
			return en.nextElement();
		}

		@Override public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static abstract class NameTable extends AbstractLuanTable {
		abstract Object get(String name);
		abstract Iterator<String> names();

		@Override public final Object get(Object key) {
			if( !(key instanceof String) )
				return null;
			String name = (String)key;
			return get(name);
		}

		@Override public final Iterator<Map.Entry<Object,Object>> iterator() {
			return new Iterator<Map.Entry<Object,Object>>() {
				Iterator<String> names = names();

				@Override public boolean hasNext() {
					return names.hasNext();
				}
				@Override public Map.Entry<Object,Object> next() {
					String name = names.next();
					Object val = get(name);
					return new AbstractMap.SimpleEntry<Object,Object>(name,val);
				}
				@Override public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	};

	private static String string(Object value) {
		if( !(value instanceof String) )
			throw new IllegalArgumentException("value must be string");
		return (String)value;
	}
}
