package luan.modules.lucene;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class LuceneWriter {
	private final LuceneIndex index;

	LuceneWriter(LuceneIndex index) {
		index.writeLock.lock();
		this.index = index;
	}

	// call in finally block
	void close() {
		index.writeLock.unlock();
	}

	void commit() throws IOException {
		index.writer.commit();
	}

	void addDocument(LuanState luan,LuanTable doc) throws LuanException, IOException {
		index.writer.addDocument(index.toLucene(luan,doc));
	}

	void updateDocument(LuanState luan,Term term,LuanTable doc) throws LuanException, IOException {
		index.writer.updateDocument(term,index.toLucene(luan,doc));
	}

	public void delete_documents(LuanState luan,LuanTable tblTerms) throws LuanException, IOException {
		List<Term> list = new ArrayList<Term>();
		for( Map.Entry<Object,Object> entry : tblTerms ) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if( !(key instanceof String) )
				throw luan.exception("key must be a string but got "+key.getClass().getSimpleName());
			if( !(value instanceof String) )
				throw luan.exception("value must be a string but got "+value.getClass().getSimpleName());
			list.add( index.newTerm( (String)key, (String)value ) );
		}
		index.writer.deleteDocuments(list.toArray(new Term[list.size()]));
	}

	String nextId(LuanState luan) throws LuanException, IOException {
		return index.nextId(luan);
	}

	public void save_document(LuanState luan,LuanTable doc) throws LuanException, IOException {
		if( doc.get("type")==null )
			throw luan.exception("missing 'type' field");
		String id = (String)doc.get("id");
		if( id == null ) {
			id = nextId(luan);
			doc.put("id",id);
			addDocument(luan,doc);
		} else {
			updateDocument(luan,index.newTerm("id",id),doc);
		}
	}

	// luan

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(LuceneWriter.class.getMethod(method,parameterTypes),this) );
	}

	LuanTable table() {
		LuanTable tbl = Luan.newTable();
		try {
			add( tbl, "save_document", LuanState.class, LuanTable.class );
			add( tbl, "delete_documents", LuanState.class, LuanTable.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}

}
