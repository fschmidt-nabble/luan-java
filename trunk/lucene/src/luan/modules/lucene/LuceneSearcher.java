package luan.modules.lucene;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class LuceneSearcher {
	private final IndexSearcher searcher;

	LuceneSearcher(IndexReader reader) {
		this.searcher = new IndexSearcher(reader);
	}

	// call in finally block
	void close() {
		try {
			searcher.getIndexReader().decRef();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public LuanTable doc(int docID) throws IOException {
		return LuceneDocument.toTable(searcher.doc(docID));
	}

	TopDocs search(Query query,int n) throws IOException {
		return searcher.search(query,n);
	}

	TopFieldDocs search(Query query,int n,Sort sort) throws IOException {
		return searcher.search(query,n,sort);
	}

	// luan

	private Query termQuery(Map<Object,Object> map) {
		if( map.size() != 1 )
			return null;
		Map.Entry<Object,Object> entry = map.entrySet().iterator().next();
		Object key = entry.getKey();
		Object value = entry.getValue();
		if( key instanceof String && value instanceof String ) {
			return new TermQuery(new Term( (String)key, (String)value ));
		}
		return null;
	}

	private Query booleanQuery(Map<Object,Object> map) {
		BooleanQuery query = new BooleanQuery();
		for( Map.Entry<Object,Object> entry : map.entrySet() ) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if( !(key instanceof String && value instanceof LuanTable) )
				return null;
			Query subQuery = query( (LuanTable)value );
			if( subQuery == null )
				return null;
			BooleanClause.Occur occur;
			try {
				occur = BooleanClause.Occur.valueOf( ((String)key).toUpperCase() );
			} catch(IllegalArgumentException e) {
				return null;
			}
			query.add(subQuery,occur);
		}
		return query;
	}

	private Query query(LuanTable queryTbl) {
		Map<Object,Object> map = queryTbl.asMap();
		if( map.isEmpty() )
			return null;
		Query query;
		query = termQuery(map);  if(query!=null) return query;
		query = booleanQuery(map);  if(query!=null) return query;
		return null;
	}

	private SortField sortField(LuanState luan,List<Object> list,String pos) throws LuanException {
		int size = list.size();
		if( size < 2 || size > 3 )
			throw luan.exception("invalid sort field"+pos);
		Object obj0 = list.get(0);
		Object obj1 = list.get(1);
		if( !(obj0 instanceof String && obj1 instanceof String) )
			throw luan.exception("invalid sort field"+pos);
		String field = (String)obj0;
		SortField.Type type;
		try {
			type = SortField.Type.valueOf( ((String)obj1).toUpperCase() );
		} catch(IllegalArgumentException e) {
			throw luan.exception("invalid sort field type"+pos);
		}
		if( size == 2 )
			return new SortField(field,type);
		Object obj2 = list.get(2);
		if( !(obj2 instanceof Boolean) )
			throw luan.exception("invalid sort field"+pos+", 'reverse' must be boolean");
		boolean reverse = (Boolean)obj2;
		return new SortField(field,type,reverse);
	}

	private Sort sort(LuanState luan,LuanTable sortTbl) throws LuanException {
		if( !sortTbl.isList() )
			throw luan.exception("invalid sort, must be list");
		List<Object> list = sortTbl.asList();
		if( list.isEmpty() )
			throw luan.exception("sort cannot be empty");
		if( list.get(0) instanceof String )
			return new Sort(sortField(luan,list,""));
		SortField[] flds = new SortField[list.size()];
		for( int i=0; i<flds.length; i++ ) {
			Object obj = list.get(i);
			if( !(obj instanceof LuanTable) )
				throw luan.exception("invalid sort parameter at position "+(i+1));
			LuanTable fldTbl = (LuanTable)obj;
			if( !fldTbl.isList() )
				throw luan.exception("invalid sort field at position "+(i+1)+", must be list");
			flds[i] = sortField(luan,fldTbl.asList()," at position "+(i+1));
		}
		return new Sort(flds);
	}

	public Object[] search( LuanState luan, LuanTable queryTbl, int n, LuanTable sortTbl ) throws LuanException, IOException {
		Query query = query(queryTbl);
		if( query == null )
			throw luan.exception("invalid query");
		TopDocs td = sortTbl==null ? searcher.search(query,n) : searcher.search(query,n,sort(luan,sortTbl));
		final ScoreDoc[] scoreDocs = td.scoreDocs;
		LuanFunction results = new LuanFunction() {
			int i = 0;

			@Override public Object call(LuanState luan,Object[] args) throws LuanException {
				if( i >= scoreDocs.length )
					return LuanFunction.NOTHING;
				try {
					LuanTable doc = doc(scoreDocs[i++].doc);
					return doc;
				} catch(IOException e) {
					throw luan.exception(e);
				}
			}
		};
		return new Object[]{ results, scoreDocs.length, td.totalHits };
	}

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(LuceneSearcher.class.getMethod(method,parameterTypes),this) );
	}

	LuanTable table() {
		LuanTable tbl = Luan.newTable();
		try {
			add( tbl, "search", LuanState.class, LuanTable.class, Integer.TYPE, LuanTable.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}

}
