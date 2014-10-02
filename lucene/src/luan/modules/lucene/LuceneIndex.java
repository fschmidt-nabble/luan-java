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
import org.apache.lucene.document.Document;
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
	private static final String FLD_NEXT_ID = "nextId";

	final Lock writeLock = new ReentrantLock();
	private final File indexDir;
	final SnapshotDeletionPolicy snapshotDeletionPolicy;
	final IndexWriter writer;
	private DirectoryReader reader;
	private LuceneSearcher searcher;
	final FieldTable fields = new FieldTable();

	public LuceneIndex(LuanState luan,String indexDirStr) throws LuanException, IOException {
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
		searcher = new LuceneSearcher(this,reader);
		initId(luan);
	}

	Document toLucene(LuanState luan,LuanTable table) throws LuanException {
		return LuceneDocument.toLucene(luan,table,fields.map);
	}

	LuanTable toTable(LuanState luan,Document doc) throws LuanException {
		return LuceneDocument.toTable(luan,doc,fields.reverseMap);
	}

	String fixFieldName(String fld) {
		String s = fields.map.get(fld);
		return s!=null ? s : fld;
	}

	Term newTerm(String fld,String text) {
		return new Term(fixFieldName(fld),text);
	}

	public LuceneWriter openWriter() {
		return new LuceneWriter(this);
	}

	synchronized LuceneSearcher openSearcher() throws IOException {
		DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
		if( newReader != null ) {
			reader.decRef();
			reader = newReader;
			searcher = new LuceneSearcher(this,reader);
		}
		reader.incRef();
		return searcher;
	}

	LuceneSnapshot openSnapshot() throws IOException {
		return new LuceneSnapshot(this);
	}


	private long id = 0;
	private long idLim = 0;
	private final int idBatch = 10;

	private void initId(LuanState luan) throws LuanException, IOException {
		TopDocs td = searcher.search(new TermQuery(newTerm("type","next_id")),1);
		switch(td.totalHits) {
		case 0:
			break;  // do nothing
		case 1:
			LuanTable doc = searcher.doc(luan,td.scoreDocs[0].doc);
			idLim = (Long)doc.get(FLD_NEXT_ID);
			id = idLim;
			break;
		default:
			throw new RuntimeException();
		}
	}

	synchronized String nextId(LuanState luan) throws LuanException, IOException {
		String rtn = Long.toString(++id);
		if( id > idLim ) {
			idLim += idBatch;
			LuanTable doc = Luan.newTable();
			doc.put( "type", "next_id" );
			doc.put( FLD_NEXT_ID, idLim );
			writer.updateDocument(newTerm("type","next_id"),toLucene(luan,doc));
		}
		return rtn;
	}


	public void backup(LuanState luan,String zipFile) throws LuanException, IOException {
		if( !zipFile.endsWith(".zip") )
			throw luan.exception("file "+zipFile+" doesn't end with '.zip'");
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

	public Object Searcher(LuanState luan,LuanFunction fn) throws LuanException, IOException {
		LuceneSearcher searcher = openSearcher();
		try {
			return luan.call( fn, new Object[]{searcher.table()} );
		} finally {
			searcher.close();
		}
	}

	private void add(LuanTable t,String method,Class<?>... parameterTypes) throws NoSuchMethodException {
		t.put( method, new LuanJavaFunction(LuceneIndex.class.getMethod(method,parameterTypes),this) );
	}

	public LuanTable table() {
		LuanTable tbl = Luan.newTable();
		try {
			tbl.put("fields",fields);
			add( tbl, "to_string" );
			add( tbl, "backup", LuanState.class, String.class );
			add( tbl, "Writer", LuanState.class, LuanFunction.class );
			add( tbl, "Searcher", LuanState.class, LuanFunction.class );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}

}
