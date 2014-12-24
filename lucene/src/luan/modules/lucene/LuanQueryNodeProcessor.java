package luan.modules.lucene;

import java.util.List;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessor;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;


public class LuanQueryNodeProcessor implements QueryNodeProcessor {
	private final LuceneIndex index;
	private final QueryNodeProcessor qnp;

	public LuanQueryNodeProcessor(LuceneIndex index,QueryNodeProcessor qnp) {
		this.index = index;
		this.qnp = qnp;
	}

	public QueryConfigHandler getQueryConfigHandler() {
		return qnp.getQueryConfigHandler();
	}

	public void setQueryConfigHandler(QueryConfigHandler queryConfigHandler) {
		qnp.setQueryConfigHandler(queryConfigHandler);
	}

	public QueryNode process(QueryNode queryTree) throws QueryNodeException {
		fix(queryTree);
//		System.out.println(queryTree);
		return qnp.process(queryTree);
	}

	private void fix(QueryNode queryTree) {
		if( queryTree instanceof FieldQueryNode ) {
			FieldQueryNode fqn = (FieldQueryNode)queryTree;
			CharSequence fldSeq = fqn.getField();
			if( fldSeq == null )
				throw new RuntimeException("missing field for value: "+fqn.getText());
			String fld = fldSeq.toString();
			fld = index.map_field_name(fld);
//			System.out.println("field = "+fld);
			fqn.setField(fld);
		}
		List<QueryNode> list = queryTree.getChildren();
		if( list != null ) {
			for( QueryNode qn : list ) {
				fix(qn);
			}
		}
	}
}
