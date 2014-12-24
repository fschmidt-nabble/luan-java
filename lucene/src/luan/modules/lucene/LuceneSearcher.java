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
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;
import luan.LuanRuntimeException;
import luan.LuanMethod;


public final class LuceneSearcher {
	private final LuceneIndex index;
	private final IndexSearcher searcher;

	LuceneSearcher(LuceneIndex index,IndexReader reader) {
		this.index = index;
		this.searcher = new IndexSearcher(reader);
	}

	// call in finally block
	void close() throws IOException {
		searcher.getIndexReader().decRef();
	}

	LuanTable doc(LuanState luan,int docID) throws LuanException, IOException {
		return index.toTable(luan,searcher.doc(docID));
	}

	TopDocs search(Query query,int n) throws IOException {
		return searcher.search(query,n);
	}

	TopFieldDocs search(Query query,int n,Sort sort) throws IOException {
		return searcher.search(query,n,sort);
	}

	// luan

	private static final LuanFunction nothingFn = new LuanFunction() {
		@Override public Object call(LuanState luan,Object[] args) {
			return LuanFunction.NOTHING;
		}
	};

	private static abstract class MyCollector extends Collector {
		int docBase;

		@Override public void setScorer(Scorer scorer) {}
		@Override public void setNextReader(AtomicReaderContext context) {
			this.docBase = context.docBase;
		}
		@Override public boolean acceptsDocsOutOfOrder() {
			return true;
		}
	}

	@LuanMethod public Object[] search( final LuanState luan, Object queryObj, Object nObj, Sort sort ) throws LuanException, IOException, QueryNodeException {
		Query query;
		if( queryObj instanceof Query ) {
			query = (Query)queryObj;
		} else if( queryObj instanceof String ) {
			String s = (String)queryObj;
			query = index.parse(s);
		} else
			throw luan.exception("bad argument #1 (string or Query expected, got "+Luan.type(queryObj)+")");
		if( nObj instanceof LuanFunction ) {
			final LuanFunction fn = (LuanFunction)nObj;
			Collector col = new MyCollector() {
				@Override public void collect(int doc) {
					try {
						LuanTable docTbl = doc(luan,docBase+doc);
						luan.call(fn,new Object[]{docTbl});
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					} catch(IOException e) {
						throw new LuanRuntimeException(luan.exception(e));
					}
				}
			};
			try {
				searcher.search(query,col);
			} catch(LuanRuntimeException e) {
				throw (LuanException)e.getCause();
			}
			return LuanFunction.NOTHING;
		}
		Integer nI = Luan.asInteger(nObj);
		if( nI == null )
			throw luan.exception("bad argument #2 (integer or function expected, got "+Luan.type(nObj)+")");
		int n = nI;
		if( n==0 ) {
			TotalHitCountCollector thcc = new TotalHitCountCollector();
			searcher.search(query,thcc);
			return new Object[]{ nothingFn, 0, thcc.getTotalHits() };
		}
		TopDocs td = sort==null ? searcher.search(query,n) : searcher.search(query,n,sort);
		final ScoreDoc[] scoreDocs = td.scoreDocs;
		LuanFunction results = new LuanFunction() {
			int i = 0;

			@Override public Object call(LuanState luan,Object[] args) throws LuanException {
				if( i >= scoreDocs.length )
					return LuanFunction.NOTHING;
				try {
					LuanTable doc = doc(luan,scoreDocs[i++].doc);
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
			add( tbl, "search", LuanState.class, Object.class, Object.class, Sort.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}

}
