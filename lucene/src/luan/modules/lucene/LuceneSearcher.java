package luan.modules.lucene;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.ScoreDoc;
import luan.LuanTable;


public final class LuceneSearcher {
	private final IndexSearcher searcher;

	LuceneSearcher(IndexReader reader) {
		this.searcher = new IndexSearcher(reader);
	}

	// call in finally block
	public void close() {
		try {
			searcher.getIndexReader().decRef();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Document rawDoc(int docID) {
		try {
			return searcher.doc(docID);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public LuanTable doc(int docID) {
		return LuceneDocument.toTable(rawDoc(docID));
	}

	public TopDocs search(Query query,int n) {
		try {
			return searcher.search(query,n);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public TopFieldDocs search(Query query,int n,Sort sort) {
		try {
			return searcher.search(query,n,sort);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterable<LuanTable> docs(TopDocs td) {
		final ScoreDoc[] scoreDocs = td.scoreDocs;
		return new Iterable<LuanTable>() {
			public Iterator<LuanTable> iterator() {
				return new Iterator<LuanTable>() {
					private int i = 0;

					public boolean hasNext() {
						return i < scoreDocs.length;
					}

					public LuanTable next() {
						if( !hasNext() )
							throw new NoSuchElementException();
						return doc(scoreDocs[i++].doc);
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}
