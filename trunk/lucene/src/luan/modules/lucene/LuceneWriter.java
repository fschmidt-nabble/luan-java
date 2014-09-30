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
	public static final String FLD_TYPE = "type index";
	public static final String FLD_ID = "id index";

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

	void addDocument(LuanTable doc) throws IOException {
		index.writer.addDocument(LuceneDocument.toLucene(doc));
	}

	void updateDocument(Term term,LuanTable doc) throws IOException {
		index.writer.updateDocument(term,LuceneDocument.toLucene(doc));
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
			list.add( new Term( (String)key, (String)value ) );
		}
		index.writer.deleteDocuments(list.toArray(new Term[list.size()]));
	}

	String nextId() {
		return index.nextId();
	}

	public void save_document(LuanTable doc) throws IOException {
		if( doc.get(FLD_TYPE)==null )
			throw new RuntimeException("missing '"+FLD_TYPE+"'");
		String id = (String)doc.get(FLD_ID);
		if( id == null ) {
			id = nextId();
			doc.put(FLD_ID,id);
			addDocument(doc);
		} else {
			updateDocument(new Term(FLD_ID,id),doc);
		}
	}

	// luan

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(LuceneWriter.class.getMethod(method,parameterTypes),this) );
	}

	LuanTable table() {
		LuanTable tbl = Luan.newTable();
		try {
			add( tbl, "save_document", LuanTable.class );
			add( tbl, "delete_documents", LuanState.class, LuanTable.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}

}
