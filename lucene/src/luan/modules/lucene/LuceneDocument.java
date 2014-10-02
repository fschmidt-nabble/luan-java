package luan.modules.lucene;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.util.BytesRef;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanException;


public class LuceneDocument {
	// I assume there will be more flags later
	public static final String INDEX = "index";

	private LuceneDocument(String a) {}  // never

	static Document toLucene(LuanState luan,LuanTable table,Map<String,String> nameMap) throws LuanException {
		Document doc = new Document();
		for( Map.Entry<Object,Object> entry : table ) {
			Object key = entry.getKey();
			if( !(key instanceof String) )
				throw luan.exception("key must be string");
			String name = (String)key;
			Object value = entry.getValue();
			String newName = nameMap.get(name);
			if( newName != null )
				name = newName;
			Set<String> flags = new HashSet<String>();
			String[] a = name.split(" +");
			for( int i=1; i<a.length; i++ ) {
				flags.add(a[i]);
			}
			if( value instanceof String ) {
				String s = (String)value;
				if( flags.remove(INDEX) ) {
					doc.add(new StringField(name, s, Field.Store.YES));
				} else {
					doc.add(new StoredField(name, s));
				}
			} else if( value instanceof Integer ) {
				int i = (Integer)value;
				if( flags.remove(INDEX) ) {
					doc.add(new IntField(name, i, Field.Store.YES));
				} else {
					doc.add(new StoredField(name, i));
				}
			} else if( value instanceof Long ) {
				long i = (Long)value;
				if( flags.remove(INDEX) ) {
					doc.add(new LongField(name, i, Field.Store.YES));
				} else {
					doc.add(new StoredField(name, i));
				}
			} else if( value instanceof Double ) {
				double i = (Double)value;
				if( flags.remove(INDEX) ) {
					doc.add(new DoubleField(name, i, Field.Store.YES));
				} else {
					doc.add(new StoredField(name, i));
				}
			} else if( value instanceof byte[] ) {
				byte[] b = (byte[])value;
				doc.add(new StoredField(name, b));
			} else
				throw luan.exception("invalid value type "+value.getClass()+"' for '"+name+"'");
			if( !flags.isEmpty() )
				throw luan.exception("invalid flags "+flags+" in '"+name+"'");
		}
		return doc;
	}

	static LuanTable toTable(LuanState luan,Document doc,Map<String,String> nameMap) throws LuanException {
		if( doc==null )
			return null;
		LuanTable table = Luan.newTable();
		for( IndexableField ifld : doc ) {
			String name = ifld.name();
			String newName = nameMap.get(name);
			if( newName != null )
				name = newName;
			BytesRef br = ifld.binaryValue();
			if( br != null ) {
				table.put(name,br.bytes);
				continue;
			}
			Number n = ifld.numericValue();
			if( n != null ) {
				table.put(name,n);
				continue;
			}
			String s = ifld.stringValue();
			if( s != null ) {
				table.put(name,s);
				continue;
			}
			throw luan.exception("invalid field type for "+ifld);
		}
		return table;
	}
}
