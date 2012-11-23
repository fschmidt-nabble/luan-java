package luan.interp;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.errors.ErrorUtils;
import luan.Lua;
import luan.LuaNumber;
import luan.LuaState;


public class LuaParser extends BaseParser<Object> {

	public Rule Target() {
		return Sequence(
			Spaces(),
			FirstOf(
				Sequence( ExpList(), EOI ),
				Sequence( Chunk(), EOI )
			)
		);
	}

	Rule Chunk() {
		return Sequence(
			push(new ArrayList<Stmt>()),
			Optional( Stmt() ),
			ZeroOrMore(
				StmtSep(),
				Optional( Stmt() )
			),
			push( newChunk() )
		);
	}

	boolean addStmt() {
		Stmt stmt = (Stmt)pop();
		@SuppressWarnings("unchecked")
		List<Stmt> stmts = (List<Stmt>)peek();
		stmts.add(stmt);
		return true;
	}

	Stmt newChunk() {
		@SuppressWarnings("unchecked")
		List<Stmt> stmts = (List<Stmt>)peek();
		switch( stmts.size() ) {
		case 0:
			return Stmt.EMPTY;
		case 1:
			return stmts.get(0);
		default:
			return new Block(stmts.toArray(new Stmt[0]));
		}
	}

	Rule StmtSep() {
		return Sequence( OneOrMore(AnyOf(";\r\n")), Spaces() );
	}

	Rule Stmt() {
		return Sequence(
			FirstOf(
				SetStmt(),
				ExpressionsStmt()
			),
			addStmt()
		);
	}

	Rule ExpressionsStmt() {
		return Sequence(
			ExpList(),
			push( new ExpressionsStmt((Expressions)pop()) )
		);
	}

	Rule SetStmt() {
		return Sequence(
			VarList(),
			'=', Spaces(),
			ExpList(),
			push( newSetStmt() )
		);
	}

	SetStmt newSetStmt() {
		Expressions values = (Expressions)pop();
		@SuppressWarnings("unchecked")
		List<SetStmt.Var> vars = (List<SetStmt.Var>)pop();
		return new SetStmt( vars.toArray(new SetStmt.Var[0]), values );
	}

	Rule VarList() {
		return Sequence(
			push(new ArrayList<SetStmt.Var>()),
			Var(),
			addToVarList(),
			ZeroOrMore(
				',', Spaces(), Var(),
				addToVarList()
			)
		);
	}

	boolean addToVarList() {
		Object obj = pop();
		if( obj==null )
			return false;
		Expr key = expr(obj);
		Expr table = expr(pop());
		@SuppressWarnings("unchecked")
		List<SetStmt.Var> vars = (List<SetStmt.Var>)peek();
		vars.add( new SetStmt.Var(table,key) );
		return true;
	}

	Rule Expr() {
		return OrExpr();
	}

	Rule OrExpr() {
		return Sequence(
			AndExpr(),
			ZeroOrMore( "or", Spaces(), AndExpr(), push( new OrExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule AndExpr() {
		return Sequence(
			RelExpr(),
			ZeroOrMore( "and", Spaces(), RelExpr(), push( new AndExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule RelExpr() {
		return Sequence(
			ConcatExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( "==", Spaces(), ConcatExpr(), push( new EqExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( "~=", Spaces(), ConcatExpr(), push( new NotExpr(new EqExpr(expr(pop(1)),expr(pop()))) ) ),
					Sequence( "<=", Spaces(), ConcatExpr(), push( new LeExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( ">=", Spaces(), ConcatExpr(), push( new LeExpr(expr(pop()),expr(pop())) ) ),
					Sequence( "<", Spaces(), ConcatExpr(), push( new LtExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( ">", Spaces(), ConcatExpr(), push( new LtExpr(expr(pop()),expr(pop())) ) )
				)
			)
		);
	}

	Rule ConcatExpr() {
		return Sequence(
			SumExpr(),
			Optional( "..", Spaces(), ConcatExpr(), push( new ConcatExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule SumExpr() {
		return Sequence(
			TermExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '+', Spaces(), TermExpr(), push( new AddExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( '-', Spaces(), TermExpr(), push( new SubExpr(expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	Rule TermExpr() {
		return Sequence(
			UnaryExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '*', Spaces(), UnaryExpr(), push( new MulExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( '/', Spaces(), UnaryExpr(), push( new DivExpr(expr(pop(1)),expr(pop())) ) ),
					Sequence( '%', Spaces(), UnaryExpr(), push( new ModExpr(expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	Rule UnaryExpr() {
		return FirstOf(
			Sequence( '#', Spaces(), PowExpr(), push( new LenExpr(expr(pop())) ) ),
			Sequence( '-', Spaces(), PowExpr(), push( new UnmExpr(expr(pop())) ) ),
			Sequence( "not", Spaces(), PowExpr(), push( new NotExpr(expr(pop())) ) ),
			PowExpr()
		);
	}

	Rule PowExpr() {
		return Sequence(
			SingleExpr(),
			Optional( '^', Spaces(), PowExpr(), push( new PowExpr(expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule SingleExpr() {
		return FirstOf(
			TableExpr(),
			VarExp(),
			LiteralExpr()
		);
	}

	Rule TableExpr() {
		return Sequence(
			'{', Spaces(),
			push( new ArrayList<TableExpr.Field>() ),
			push( 1.0 ),  // counter
			Optional(
				Field(),
				ZeroOrMore(
					FieldSep(),
					Field()
				),
				Optional( FieldSep() )
			),
			'}', Spaces(),
			push( newTableExpr() )
		);
	}

	TableExpr newTableExpr() {
		pop();  // counter
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)pop();
		return new TableExpr(list.toArray(new TableExpr.Field[0]));
	}

	Rule FieldSep() {
		return Sequence( AnyOf(",;"), Spaces() );
	}

	Rule Field() {
		return FirstOf(
			Sequence(
				FirstOf( SubExpr(), Name() ),
				'=', Spaces(), Expr(),
				addField()
			),
			Sequence(
				Expr(),
				addIndexedField()
			)
		);
	}

	boolean addField() {
		TableExpr.Field field = new TableExpr.Field( expr(pop(1)), expr(pop()) );
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)peek(1);
		list.add(field);
		return true;
	}

	boolean addIndexedField() {
		Expr val = expr(pop());
		double i = (Double)pop();
		TableExpr.Field field = new TableExpr.Field( new ConstExpr(new LuaNumber(i)), val );
		push( i + 1 );
		@SuppressWarnings("unchecked")
		List<TableExpr.Field> list = (List<TableExpr.Field>)peek(1);
		list.add(field);
		return true;
	}

	static Expr expr(Object obj) {
		if( obj instanceof Expressions )
			return new ExpressionsExpr((Expressions)obj);
		return (Expr)obj;
	}

	Rule VarExp() {
		return Sequence(
			Var(),
			makeVarExp()
		);
	}

	Rule Var() {
		return Sequence(
			FirstOf(
				Sequence(
					'(', Spaces(), Expr(), ')', Spaces(),
					push(expr(pop())),
					push(null)  // marker
				),
				Sequence(
					push(EnvExpr.INSTANCE),
					Name()
				)
			),
			ZeroOrMore(
				makeVarExp(),
				FirstOf(
					SubExpr(),
					Sequence( '.', Spaces(), Name() ),
					Sequence(
						Args(),
						push(null)  // marker
					)
				)
			)
		);
	}

	boolean makeVarExp() {
		Object obj = pop();
		if( obj==null )
			return true;
		return push( new GetExpr( expr(pop()), expr(obj) ) );
	}

	// function should be on top of the stack
	Rule Args() {
		return Sequence(
			FirstOf(
				Sequence(
					'(', Spaces(), Expressions(), ')', Spaces()
				),
				Sequence(
					TableExpr(),
					push( new ExpList.SingleExpList(expr(pop())) )
				),
				Sequence(
					StringLiteral(), Spaces(),
					push( new ExpList.SingleExpList(new ConstExpr(pop())) )
				)
			),
			push( new FnCall( expr(pop(1)), (Expressions)pop() ) )
		);
	}

	Rule Expressions() {
		return FirstOf(
			ExpList(),
			push( ExpList.emptyExpList )
		);
	}

	Rule ExpList() {
		return Sequence(
			push(new ExpList.Builder()),
			Expr(),
			addToExpList(),
			ZeroOrMore(
				',', Spaces(), Expr(),
				addToExpList()
			),
			push( ((ExpList.Builder)pop()).build() )
		);
	}

	boolean addToExpList() {
		Object obj = pop();
		ExpList.Builder bld = (ExpList.Builder)peek();
		if( obj instanceof Expressions ) {
			bld.add( (Expressions)obj );
		} else {
			bld.add( (Expr)obj );
		}
		return true;
	}

	Rule SubExpr() {
		return Sequence( '[', Spaces(), Expr(), ']', Spaces() );
	}

	Rule Name() {
		return Sequence(
			Sequence(
				NameStart(),
				ZeroOrMore(
					FirstOf( NameStart(), Digit() )
				)
			),
			pushNameExpr(),
			Spaces()
		);
	}

	Rule NameStart() {
		return FirstOf(
			CharRange('a', 'z'),
			CharRange('A', 'Z'),
			'_'
		);
	}

	boolean pushNameExpr() {
		String name = match();
		if( keywords.contains(name) )
			return false;
		return push( new ConstExpr(name) );
	}

	private static final Set<String> keywords = new HashSet<String>(Arrays.asList(
		"and",
		"break",
		"do",
		"else",
		"elseif",
		"end",
		"false",
		"for",
		"function",
		"goto",
		"if",
		"in",
		"local",
		"nil",
		"not",
		"or",
		"repeat",
		"return",
		"then",
		"true",
		"until",
		"while"
	));

	Rule LiteralExpr() {
		return Sequence(
			Literal(), Spaces(),
			push(new ConstExpr(pop()))
		);
	}

	Rule Literal() {
		return FirstOf(
			NilLiteral(),
			BooleanLiteral(),
			NumberLiteral(),
			StringLiteral()
		);
	}

	Rule NilLiteral() {
		return Sequence( "nil", push(null) );
	}

	Rule BooleanLiteral() {
		return FirstOf(
			Sequence( "true", push(true) ),
			Sequence( "false", push(false) )
		);
	}

	Rule NumberLiteral() {
		return Sequence(
			Number(),
			push(new LuaNumber((Double)pop()))
		);
	}

	Rule Number() {
		return FirstOf(
			Sequence(
				IgnoreCase("0x"),
				OneOrMore(HexDigit()),
				push((double)Long.parseLong(match(),16))
			),
			Sequence(
				DecNumber(),
				push(Double.parseDouble(match()))
			)
		);
	}

	Rule DecNumber() {
		return FirstOf(
			Sequence(
				Int(),
				Optional( '.', Optional(Int()) ),
				Exponent()
			),
			Sequence( '.', Int(), Exponent() )
		);
	}

	Rule Exponent() {
		return Optional(
			IgnoreCase('e'),
			Optional(AnyOf("+-")),
			Int()
		);
	}

	Rule Int() {
		return OneOrMore(Digit());
	}

	Rule HexDigit() {
		return FirstOf(
			Digit(),
			AnyOf("abcdefABCDEF")
		);
	}

	Rule Digit() {
		return CharRange('0', '9');
	}

	Rule StringLiteral() {
		return FirstOf(
			QuotedString('"'),
			QuotedString('\'')
		);
	}

	Rule QuotedString(char quote) {
		return Sequence(
			quote,
			push(new StringBuffer()),
			ZeroOrMore(
				FirstOf(
					Sequence(
						NoneOf("\\\n"+quote),
						append(matchedChar())
					),
					EscSeq()
				)
			),
			quote,
			push(((StringBuffer)pop()).toString())
		);
	}

	Rule EscSeq() {
		return Sequence(
			'\\',
			FirstOf(
				Sequence( 'a', append('\u0007') ),
				Sequence( 'b', append('\b') ),
				Sequence( 'f', append('\f') ),
				Sequence( 'n', append('\n') ),
				Sequence( 'r', append('\r') ),
				Sequence( 't', append('\t') ),
				Sequence( 'v', append('\u000b') ),
				Sequence( '\\', append('\\') ),
				Sequence( '"', append('"') ),
				Sequence( '\'', append('\'') ),
				Sequence(
					'x',
					Sequence( HexDigit(), HexDigit() ),
					append( (char)Integer.parseInt(match(),16) )
				),
				Sequence(
					Sequence(
						Digit(),
						Optional(
							Digit(),
							Optional(
								Digit()
							)
						)
					),
					append( (char)Integer.parseInt(match()) )
				)
			)
		);
	}

	boolean append(char ch) {
		StringBuffer sb = (StringBuffer)peek();
		sb.append(ch);
		return true;
	}

	Rule Spaces() {
		return ZeroOrMore(AnyOf(" \t"));
	}

	// for debugging
	boolean print(String s) {
		System.out.println(s);
		return true;
	}

	// for testing
	public static void main(String[] args) throws Exception {
		LuaParser parser = Parboiled.createParser(LuaParser.class);
		while( true ) {
			String input = new Scanner(System.in).nextLine();
			ParsingResult<?> result = new ReportingParseRunner(parser.Target()).run(input);
			if( result.hasErrors() ) {
				System.out.println("Parse Errors:\n" + ErrorUtils.printParseErrors(result));
			} else {
				Expr expr = expr(result.resultValue);
				LuaState lua = new LuaState();
				Object val = expr.eval(lua);
				System.out.println("Result: "+Lua.toString(val));
			}
		}
	}
}
