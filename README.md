# SQL to DBSP compiler

This repository holds the source code for a compiler translating SQL
view definitions into DBSP circuits.  DBSP is a framework for
implementing incremental, streaming, (and non-streaming) queries.
DBSP is implemented in Rust in the repository
<https://github.com/vmware/database-stream-processor>

The SQL compiler is based on the Apache Calcite compiler
infrastructure <https://calcite.apache.org/>

## Dependencies

The code has been tested on Windows 10/11 and Linux.

You need Java 8 or later to build the compiler, and the maven (mvn)
Java build program.  Maven will take care of installing all required
Java dependencies.

The code generated by the compiler is Rust.  To run it you need a
working installation of Rust (we recommend using rustup to install:
<https://www.rust-lang.org/tools/install>) and the source code of the
DBSP library (<https://github.com/vmware/database-stream-processor>),
which is expected to be in a parallel directory.

The testing programs use sqllogictest -- see the [section on testing](#testing)

Some tests use MySQL or Postgres.  To run these tests you need to
create a database named `slt` and a user account in the database.  In
the `run-tests.sh` script you should replace the `-u user` with the
user name you have created, and `-p password` with the user's
password.

## Running

To run the tests:

```
$ cd SQL-compiler
$ ./run-tests.sh
```

Beware that the full sql logic tests can run for a few weeks, there
are more than 7 million of them!  Most of the time is spent compiling Rust,
hopefully we'll be able to speed that up at some point.

## Incremental view maintenance

The DBSP runtime is optimized for performing incremental view
maintenance.  In consequence, DBSP programs in SQL are expressed as
VIEWS, or *standing queries*.  A view is a function of one or more
tables and other views.

For example, the following query defines a view:

```SQL
CREATE VIEW V AS SELECT * FROM T WHERE T.age > 18
```

In order to interpret this query the compiler needs to have been given
a definition of table (or view) T.  The table T should be defined
using a SQL Data Definition Language (DDL) statement, e.g.:

```SQL
CREATE TABLE T
(
    name    VARCHAR,
    age     INT,
    present BOOLEAN
)
```

The compiler must be given the table definition first, and then the
view definition.  The compiler generates a Rust library which
implements the query as a function: given the input data, it produces
the output data.

The compiler can (optionally) generate a library which will
incrementally maintain the view `V` when presented with changes to
table `T`:

```
                                           table changes
                                                V
tables -----> SQL-to-DBSP compiler ------> DBSP circuit
views                                           V
                                           view changes
```

## Command-line compiler

A compiler from SQL to DBSP is produced by the build system usign `mvn
-DskipTests package`.  A one-line Linux shell script, called
`sql-to-dbsp`, residing in the directory `SQL-compiler` directory,
invokes the compiler.  Here is an example:

```
$ ./sql-to-dbsp
Usage: sql-to-dbsp [options] Input file to compile
  Options:
    -h, --help, -
      Show this message and exit
    -d
      Options: [BIG_QUERY, ORACLE, MYSQL, MYSQL_ANSI, SQL_SERVER, JAVA]
      Default: ORACLE
    -f
      Name of function to generate
      Default: circuit
    -i
      Generate an incremental circuit
      Default: false
    -o
      Output file; stdout if null
$ ./sql-to-dbsp x.sql -o ../temp/src/lib.rs
```

The last command-line compiles a script called `x.sql` and writes the
result in a file `lib.rs`.  Let's assume we are compiling the
following input file:

```
$ cat x.sql
-- example input file
CREATE TABLE T(COL0 INTEGER, COL1 INTEGER);
CREATE VIEW V AS SELECT T.COL1 FROM T;
```

The input file can contain comments, and only two kinds of SQL
statements, separated by semicolons: `CREATE TABLE` and `CREATE VIEW`.
In the generated DBSP circuit every `CREATE TABLE` is translated to an
input, and every `CREATE VIEW` is translated to an output.  The result
produced will look like this:

```
$ cat ../temp/src/lib.rs
// Automatically-generated file
[...boring stuff removed...]

pub fn circuit(workers: usize) -> (DBSPHandle, Catalog) {
    let mut catalog = Catalog::new();
    let (circuit, handles) = Runtime::init_circuit(workers, |circuit| {
        let map34: _ = move |t: &Tuple2<Option<i32>, Option<i32>>, | -> Tuple1<Option<i32>> {
            Tuple1::new(t.1)
        };
        // CREATE TABLE `T` (`COL0` INTEGER, `COL1` INTEGER)
        let (T, handle0) = circuit.add_input_zset::<Tuple2<Option<i32>, Option<i32>>, Weight>();
        let stream38: Stream<_, OrdZSet<Tuple1<Option<i32>>, Weight>> = T.map(map34);
        // CREATE VIEW `V` AS
        // SELECT `T`.`COL1`
        // FROM `T`
        let handle1 = stream38.output();
        (handle0,handle1,)
    }).unwrap();
    catalog.register_input_zset_handle("T", handles.0);
    catalog.register_output_batch_handle("V", handles.1);
    (circuit, catalog)
}
```

You can compile the generated Rust code:

```
$ cd ../temp
$ cargo build
```

The generated file contains a Rust function called `circuit` (you can
change its name using the compiler option `-f`).  Calling `circuit`
will return an executable DBSP circuit handle, and a DBSP catalog.
These APIs can be used to execute the circuit.  See the DBSP
documentation for more information.

TODO: add here an example invoking the circuit.

## Compiler architecture

Compilation proceeds in several stages:

- SQL statements are parsed using the calcite SQL parser
  generating an IR representation using the Calcite `SqlNode` data types
- the SQL IR tree is validated, optimized, and converted to the Calcite `RelNode` representation
- The result of this stage is a `CalciteProgram` data structure, which packages together all the
  views that are being compiled (multiple views can be maintained simultaneously)
- `CalciteToDBSPCompiler` converts a `CalciteProgram` data structure into a `DBSPCircuit`
  data structure.
- The `CircuitOptimizer` class can optimize the generated circuit, optionally converting
  it into an incremental circuit, which is expected to compute only on changes.
- The `circuit` can be serialized as Rust using the `ToRustString` visitor.

## Testing

### Unit tests

Unit tests are written using JUnit and test pointwise parts of the compiler.
They can be executed usign `mvn test`.

### SQL logic tests

One of the means of testing the compiler is using sqllogictests:
<https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki>.

We assume that the sqllogictest source tree is installed in ../sqllogictest
with respect to the root directory of the compiler project
(we only need the .test files).  One way the source tree can be obtained
is from the git mirror: <https://github.com/gregrahn/sqllogictest.git>

We have implemented a general-purpose parser and testing framework for
running SqlLogicTest programs, in the `org.dbsp.sqllogictest` package.
The framework parses SqlLogicTest files and creates an internal
representation of these files.  The files are executed by "test executors".

We have multiple executors:

#### The `NoExecutor` test executor

This executor does not really run any tests.  But it can still be used
by the test loading mechanism to check that we correctly parse all
SQL logic test files.

#### The `DBSPExecutor`

The model of SQLLogicTest has to be adapted for testing using DBSP.
Since DBSP is not a database, but a streaming system, some SQL
statements are ignored (e.g., `CREATE INDEX`) and some other
cannot be supported (e.g., `CREATE UNIQUE INDEX`).

* The DDL statements to create tables are compiled into definitions of
  circuit inputs.
* The DML INSERT statements are converted into input-generating functions.
  In the absence of a database we cannot really execute statements that
  are supposed to fail, so we ignore such statements.
* Some DML statements like DELETE based on a WHERE clause cannot be compiled at all
* The queries are converted into DDL VIEW create statements, which are
  compiled into circuits.
* The validation rules in SQLLogicTest are compiled into Rust functions that compare
  the outputs of queries.

So a SqlLogicTest script is turned into multiple DBSP tests, each of
which creates a circuit, feeds it one input, reads the output, and
validates it, executing exactly one transaction.  When testing
incremental circuits the test code feeds multiple inputs and
only checks the final output.

#### The `JDBC` executor

This executor parallels the standard ODBC executor written in C by
sending the statements and queries to a database to be executed.  Any
database that supports JDBC and can handle the correct syntax of the
queries can be used.

To use this executor you have to install a suitable database and its
JDBC connector; we have tested with MySQL and Postgres.
For example, you can install MySQL:

- Downloadable from <https://dev.mysql.com/downloads/mysql>.

- Connecting to MySQL also requires a JDBC driver for your platform.
The maven pom.xml file already includes the mysql driver, you should
add the jar for your favorite DB.

- If you want to run these tests you need to create a database named
`slt` (from Sql Logic Test), and an appropriate user account.  Details
about the account and password are supplied as constructor parameters.

#### The hybrid `DBSP_JDBC_Executor`

This executor is a combination of the DBSP executor and the JDBC
executor, using a real database to store data in tables, but using
DBSP as a query engine.  It should be able to execute all SqlLogicTest
queries that are supported by the underlying database.

#### SqlLogicTest Test results

The 'inc' column shows tests for incremental circuits, the other
columns show tests for non-incremental (standard) implementations.

The matrix represents the current test results; the numbers indicate
the passing/failing tests in each category.  The failing test cases
are detailed below.

| Test group           | DBSP        | JDBC_DBSP | JDBC_DBSP inc |
|----------------------|------------:|----------:|--------------:|
| random/select        | 1,120,329/0 |           | 1,120,329/0   |
| random/groupby       |   118,757/0 |           |   118,757/0   |
| random/expr          | 1,317,682/0 |           | 1,198,926/0   |
| random/aggregates    | 1,172,825/2 |           | 1,172/825/2   |
| select1              |     1,000/0 |           |     1,000/0   |
| select2              |     1,000/0 |           |     1,000/0   |
| select3              |     3,320/0 |           |     3,320/0   |
| select4              |     2,832/0 |           |     2,832/0   |
| select5              |       732/0 |           |       732/0   |
| index/delete         |         N/A |  40,525/0 |    40,525/0   |
| index/in             |         N/A | 130,065/0 |   130,065/0   |
| index/commute        |         N/A | 507,514/0 |   507,514/0   |
| index/between        |         N/A | 121,811/0 |   121,811/0   |
| index/orderby_nosort |         N/A | 490,986/0 |   490,986/0   |
| index/view           |         N/A |  53,490/0 |    53,490/0   |
| index/random         |         N/A | 188,449/0 |   188,449/0   |
| index/orderby        |         N/A | 310,630/0 |   310,630/0   |
| evidence             |         N/A |    153/25 |               |

We have 2 failing tests; these tests depend on unspecified features of
the SQL semantics (numeric overflow), so we can argue that they are
broken by design.

The "index" tests cannot be executed with the `DBSPExecutor` since it
does not support the "unique index" SQL statement.

The results in this table were produced using the JDBC executor with
Postgres as a backing database.
