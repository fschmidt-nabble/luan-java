package luan.modules;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanException;


public final class HtmlLuan {

	public static String encode(LuanState luan,String s) throws LuanException {
		Utils.checkNotNull(luan,s);
		char[] a = s.toCharArray();
		StringBuilder buf = new StringBuilder();
		for( int i=0; i<a.length; i++ ) {
			char c = a[i];
			switch(c) {
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '"':
				buf.append("&quot;");
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

/*
//	public static final String TEXTAREA = "textarea";
	public static final String SCRIPT = "script";
	public static final String STYLE = "style";

	public static Set<String> containerTags = new HashSet<String>(Arrays.asList(SCRIPT,STYLE));
*/
	public static LuanTable parse(LuanState luan,String text,LuanTable containerTagsTbl)
		throws LuanException
	{
		Utils.checkNotNull(luan,text);
		Utils.checkNotNull(luan,containerTagsTbl);
		Set<String> containerTags = new HashSet<String>();
		for( Object v : containerTagsTbl.asList() ) {
			containerTags.add((String)v);
		}
		List<Object> html = new ArrayList<Object>();
		int len = text.length();
		int i = 0;
outer:
		while( i < len ) {
			int i2 = text.indexOf('<',i);
			while( i2 != -1 && i2+1 < len ) {
				char c = text.charAt(i2+1);
				if( Character.isLetter(c) || c=='/' || c=='!' )
					break;
				i2 = text.indexOf('<',i2+1);
			}
			if( i2 == -1 ) {
				html.add( text.substring(i) );
				break;
			}
			if( i < i2 )
				html.add( text.substring(i,i2) );
			if( text.startsWith("<!--",i2) ) {
				i = text.indexOf("-->",i2+4);
				if( i == -1 ) {
					html.add( text.substring(i2) );
					break;
				}
				html.add( comment( text.substring(i2+4,i) ) );
				i += 3;
			} else if( text.startsWith("<![CDATA[",i2) ) {
				i = text.indexOf("]]>",i2+9);
				if( i == -1 ) {
					html.add( text.substring(i2) );
					break;
				}
				html.add( cdata( text.substring(i2+9,i) ) );
				i += 3;
			} else {
				i = text.indexOf('>',i2);
				if( i == -1 ) {
					html.add( text.substring(i2) );
					break;
				}
				String tagText = text.substring(i2+1,i);
				try {
					LuanTable tag = parseTag(tagText);
					String tagName = (String)tag.get("name");
					if( containerTags.contains(tagName) ) {
						i2 = i;
						String endTagName = '/' + tagName;
						while(true) {
							i2 = text.indexOf('<',i2+1);
							if( i2 == -1 )
								break;
							int i3 = text.indexOf('>',i2);
							if( i3 == -1 )
								break;
							int j = i2+1;
							while( j<i3 && !Character.isWhitespace(text.charAt(j)) )  j++;
							String s = text.substring(i2+1,j);
							if( s.equalsIgnoreCase(endTagName) ) {
								String text2 = text.substring(i+1,i2);
								LuanTable textContainer = textContainer(tag,text2);
								html.add( textContainer );
								i = i3 + 1;
								continue outer;
							}
						}
//						logger.warn("unclosed "+tagName);
					}
					i += 1;
					html.add( tag );
				} catch(BadTag e) {
//					logger.debug("bad tag",e);
					i += 1;
//					if( !removeBadTags ) {
						html.add( "&lt;" );
						html.add( encode(luan,tagText) );
						html.add( "&gt;" );
//					}
				}
			}
		}
		return Luan.newTable(html);
	}

	static LuanTable comment(String text) {
		LuanTable tbl = Luan.newTable();
		tbl.put("type","comment");
		tbl.put("text",text);
		return tbl;
	}

	static LuanTable cdata(String text) {
		LuanTable tbl = Luan.newTable();
		tbl.put("type","cdata");
		tbl.put("text",text);
		return tbl;
	}

	static LuanTable textContainer(LuanTable tag,String text) {
		LuanTable tbl = Luan.newTable();
		tbl.put("type","container");
		tbl.put("tag",tag);
		tbl.put("text",text);
		return tbl;
	}



	static final class BadTag extends RuntimeException {
		private BadTag(String msg) {
			super(msg);
		}
	}

	static LuanTable parseTag(String text) {
		LuanTable tbl = Luan.newTable();
		tbl.put("type","tag");
		if( text.endsWith("/") ) {
			text = text.substring(0,text.length()-1);
			tbl.put("is_empty",true);
		} else {
			tbl.put("is_empty",false);
		}
		int len = text.length();
		int i = 0;
		int i2 = i;
		if( i2<len && text.charAt(i2)=='/' )
			i2++;
		while( i2<len ) {
			char c = text.charAt(i2);
			if( Character.isWhitespace(c) )
				break;
			if( !( Character.isLetterOrDigit(c) || c=='_' || c=='.' || c=='-' || c==':' ) )
				throw new BadTag("invalid tag name for <"+text+">");
			i2++;
		}
		String name = text.substring(i,i2).toLowerCase();
		tbl.put("name",name);
		LuanTable attributes = Luan.newTable();
		tbl.put("attributes",attributes);
		i = i2;
		while( i<len && Character.isWhitespace(text.charAt(i)) )  i++;
		while( i<len ) {
			i2 = toEndName(text,i,len);
			String attrName = unquote(text.substring(i,i2).toLowerCase());
			if( attributes.get(attrName) != null )
				throw new BadTag("duplicate attribute: "+attrName);
			i = i2;
			while( i<len && Character.isWhitespace(text.charAt(i)) )  i++;
			if( i<len && text.charAt(i) == '=' ) {
				i++;
				i2 = i;
				while( i<len && Character.isWhitespace(text.charAt(i)) )  i++;
				i2 = toEndValue(text,i,len);
				String attrValue = text.substring(i,i2);
				if( attrValue.indexOf('<') != -1 || attrValue.indexOf('>') != -1 )
					throw new BadTag("invalid attribute value: "+attrValue);
				attrValue = unquote(attrValue);
				attributes.put(attrName,attrValue);
				i = i2;
				while( i<len && Character.isWhitespace(text.charAt(i)) )  i++;
			} else {
				attributes.put(attrName,true);
			}
		}
		return tbl;
	}

	private static int toEndName(String text,int i,int len) {
		if( i==len )
			return i;
		char c = text.charAt(i);
		switch(c) {
		case '"':
		case '\'':
			i = text.indexOf(c,i+1);
			return i==-1 ? len : i+1;
		default:
			if( Character.isWhitespace(c) ) {
				throw new RuntimeException("text="+text+" i="+i);
			}
			do {
				i++;
			} while( i<len && (c=text.charAt(i))!='=' && !Character.isWhitespace(c) );
			return i;
		}
	}

	private static int toEndValue(String text,int i,int len) {
		if( i==len )
			return i;
		char c = text.charAt(i);
		switch(c) {
		case '"':
		case '\'':
			i = text.indexOf(c,i+1);
			return i==-1 ? len : i+1;
		default:
			if( Character.isWhitespace(c) ) {
				throw new RuntimeException("text="+text+" i="+i);
			}
			do {
				i++;
			} while( i<len && !Character.isWhitespace(text.charAt(i)) );
			return i;
		}
	}

	public static String unquote(String s) {
		if( s==null || s.length()<=1 )
			return s;
		char c = s.charAt(0);
		return (c=='"' || c=='\'') && s.charAt(s.length()-1)==c
			? s.substring(1,s.length()-1) : s;
	}




	public static String to_string(LuanState luan,LuanTable tbl) throws LuanException {
		List<Object> html = tbl.asList();
		StringBuilder buf = new StringBuilder();
		for( Object o : html ) {
			if( o instanceof String ) {
				buf.append( o );
			} else if( o instanceof LuanTable ) {
				LuanTable t = (LuanTable)o;
				String type = (String)t.get("type");
				if( type==null )
					throw luan.exception( "no type in element of table for 'Html.to_string'" );
				if( type.equals("comment") ) {
					buf.append( "<!--" ).append( t.get("text") ).append( "-->" );
				} else if( type.equals("cdata") ) {
					buf.append( "<![CDATA[" ).append( t.get("text") ).append( "]]" );
				} else if( type.equals("tag") ) {
					buf.append( tagToString(t) );
				} else if( type.equals("container") ) {
					LuanTable tag  = (LuanTable)t.get("tag");
					buf.append( tagToString(tag) );
					buf.append( t.get("text") );
					buf.append( "</" ).append( tag.get("name") ).append( ">" );
				} else {
					throw luan.exception( "invalid element type for 'Html.to_string'" );
				}
			} else 
				throw luan.exception( "invalid value ("+Luan.type(o)+") in table for 'Html.to_string'" );
		}
		return buf.toString();
	}

	private static String tagToString(LuanTable tbl) {
		StringBuilder buf = new StringBuilder();
		buf.append('<');
		buf.append(tbl.get("name"));
		LuanTable attributes = (LuanTable)tbl.get("attributes");
		for( Map.Entry<Object,Object> attr : attributes ) {
			buf.append( ' ' );
			buf.append( attr.getKey() );
			Object val = attr.getValue();
			if( !val.equals(Boolean.TRUE) ) {
				buf.append( '=' );
				buf.append( quote((String)val) );
			}
		}
		if( tbl.get("is_empty").equals(Boolean.TRUE) )
			buf.append('/');
		buf.append('>');
		return buf.toString();
	}

	public static String quote(String s) {
		StringBuilder buf = new StringBuilder();
		buf.append('"');
		int i = 0;
		while(true) {
			int i2 = s.indexOf('"',i);
			if( i2 == -1 ) {
				buf.append(s.substring(i));
				break;
			} else {
				buf.append(s.substring(i,i2));
				buf.append("&quot;");
				i = i2 + 1;
			}
		}
		buf.append('"');
		return buf.toString();
	}

}
