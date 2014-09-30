package luan.modules.lucene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import luan.modules.Utils;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class LuceneIndex {
	private static final String FLD_TYPE = LuceneWriter.FLD_TYPE;
	private static final String FLD_NEXT_ID = "nextId";

	final Lock writeLock = new ReentrantLock();
	private final File indexDir;
	final SnapshotDeletionPolicy snapshotDeletionPolicy;
	final IndexWriter writer;
	private DirectoryReader reader;
	private LuceneSearcher searcher;

	public LuceneIndex(String indexDirStr) {
		try {
			File indexDir = new File(indexDirStr);
			this.indexDir = indexDir;
			Directory dir = FSDirectory.open(indexDir);
			Version version = Version.LUCENE_4_9;
			Analyzer analyzer = new StandardAnalyzer(version);
			IndexWriterConfig conf = new IndexWriterConfig(version,analyzer);
			snapshotDeletionPolicy = new SnapshotDeletionPolicy(conf.getIndexDeletionPolicy());
			conf.setIndexDeletionPolicy(snapshotDeletionPolicy);
			writer = new IndexWriter(dir,conf);
			writer.commit();  // commit index creation
			reader = DirectoryReader.open(dir);
			searcher = new LuceneSearcher(reader);
			initId();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public LuceneWriter openWriter() {
		return new LuceneWriter(this);
	}

	public synchronized LuceneSearcher openSearcher() {
		try {
			DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
			if( newReader != null ) {
				reader.decRef();
				reader = newReader;
				searcher = new LuceneSearcher(reader);
			}
			reader.incRef();
			return searcher;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public LuceneSnapshot openSnapshot() {
		return new LuceneSnapshot(this);
	}


	private long id = 0;
	private long idLim = 0;
	private final int idBatch = 10;

	private void initId() {
		TopDocs td = searcher.search(new TermQuery(new Term(FLD_TYPE,"next_id")),1);
		switch(td.totalHits) {
		case 0:
			break;  // do nothing
		case 1:
			LuanTable doc = searcher.doc(td.scoreDocs[0].doc);
			idLim = (Long)doc.get(FLD_NEXT_ID);
			id = idLim;
			break;
		default:
			throw new RuntimeException();
		}
	}

	synchronized String nextId() {
		try {
			String rtn = Long.toString(++id);
			if( id > idLim ) {
				idLim += idBatch;
				LuanTable doc = Luan.newTable();
				doc.put( FLD_TYPE, "next_id" );
				doc.put( FLD_NEXT_ID, idLim );
				writer.updateDocument(new Term(FLD_TYPE,"next_id"),LuceneDocument.toLucene(doc));
			}
			return rtn;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public LuanTable getDocument(String id) {
		return getDocument(new Term(LuceneWriter.FLD_ID,id));
	}

	public LuanTable getDocument(Term term) {
		LuceneSearcher searcher = openSearcher();
		try {
			TopDocs td = searcher.search(new TermQuery(term),1);
			switch(td.totalHits) {
			case 0:
				return null;
			case 1:
				return searcher.doc(td.scoreDocs[0].doc);
			default:
				throw new RuntimeException();
			}
		} finally {
			searcher.close();
		}
	}


	public void backup(String zipFile) {
		if( !zipFile.endsWith(".zip") )
			throw new RuntimeException("file "+zipFile+" doesn't end with '.zip'");
		LuceneSnapshot snapshot = openSnapshot();
		try {
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
			for( String fileName : snapshot.getFileNames() ) {
				out.putNextEntry(new ZipEntry(fileName));
				FileInputStream in = new FileInputStream(new File(indexDir,fileName));
				Utils.copyAll(in,out);
				in.close();
				out.closeEntry();
			}
			out.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			snapshot.close();
		}
	}



	// luan

	public String to_string() {
		return writer.getDirectory().toString();
	}

	public void Writer(LuanState luan,LuanFunction fn) throws LuanException, IOException {
		LuceneWriter writer = openWriter();
		try {
			luan.call( fn, new Object[]{writer.table()} );
			writer.commit();
		} finally {
			writer.close();
		}
	}

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(LuceneIndex.class.getMethod(method,parameterTypes),this) );
	}

	public LuanTable table() {
		LuanTable tbl = Luan.newTable();
		try {
			add( tbl, "to_string" );
			add( tbl, "backup", String.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}

}
