package luan.modules.lucene;

import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.index.IndexCommit;


public final class LuceneSnapshot {
	private final LuceneIndex index;
	private final IndexCommit ic;

	LuceneSnapshot(LuceneIndex index) {
		this.index = index;
		try {
			this.ic = index.snapshotDeletionPolicy.snapshot();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	// call in finally block
	public void close() {
		try {
			index.snapshotDeletionPolicy.release(ic);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Collection<String> getFileNames() {
		try {
			return ic.getFileNames();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

}
