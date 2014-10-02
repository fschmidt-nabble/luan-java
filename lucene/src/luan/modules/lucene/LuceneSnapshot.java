package luan.modules.lucene;

import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.index.IndexCommit;


public final class LuceneSnapshot {
	private final LuceneIndex index;
	private final IndexCommit ic;

	LuceneSnapshot(LuceneIndex index) throws IOException {
		this.index = index;
		this.ic = index.snapshotDeletionPolicy.snapshot();
	}

	// call in finally block
	public void close() throws IOException {
		index.snapshotDeletionPolicy.release(ic);
	}

	public Collection<String> getFileNames() throws IOException {
		return ic.getFileNames();
	}

}
