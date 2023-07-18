package webservice

import java.sql.DriverManager

import groovy.sql.Sql
import groovy.xml.MarkupBuilder
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod

class Main extends AbstractVerticle{
	
	static void main(String[] args) {
		def app = new Main()
		def connection = app.createDatabaseConnection()
//		def connection = DriverManager.getConnection("jdbc:h2:~/test;MODE=Mysql;DB_CLOSE_DELAY=-1", "sa",""); 
		app.createDatabaseStructure(connection)
		app.addDemoRecords(connection)
		connection.close()

//		println(app.generateXML())
		
		Vertx vertx = Vertx.vertx()
		vertx.deployVerticle(new Main())
		
	}
	
	public void start(Future fut) { // Verticle을 성공적으로 초기화하고 시작했는지를 Vert.x에 알리기 위해 전달된 Future 객체를 사용
		vertx
			.createHttpServer()
			.requestHandler() { request ->
				if(request.path() == "/blogs/" && request.method() == HttpMethod.GET) {
					request
					.response()
					.putHeader("content-type", "application/xml")
					.end(generateXML().toString())
				}else {
					request
					.response()
					.setStatusCode(404)
					.end("Error 404")
				}
			}
			.listen(8080) { result -> 
				if(result.succeeded()) {
					fut.complete()
				}else {
					fut.fail(result.cause())
				}
			}
	}
	
	def createDatabaseConnection() {
		def connection = DriverManager.getConnection("jdbc:h2:~/test;MODE=Mysql;DB_CLOSE_DELAY=-1", "sa",""); 
		return connection
	}
	
	// 테이블 생성
	def createDatabaseStructure(connection) {
		
		def statement = connection.createStatement()
		def sqlUsers = """		
		CREATE TABLE users(
		id INT AUTO_INCREMENT NOT NULL,
		name VARCHAR(255),
		PRIMARY KEY (id)
		)
	"""
		// 서버 실행 할때마다 오류나서 있는테이블 삭제도..ㅠ
		statement.executeUpdate("DROP TABLE users")
		statement.executeUpdate("DROP TABLE blog")
		statement.executeUpdate(sqlUsers)
		
		def sqlBlog = """  		
		CREATE TABLE blog(
		id INT AUTO_INCREMENT NOT NULL,
		title VARCHAR(255) NOT NULL,
		users INT NOT NULL,
		post CLOB,
		PRIMARY KEY (id),
		FOREIGN KEY(users) REFERENCES users(id))
	"""
		statement.executeUpdate(sqlBlog)
		statement.close()
	}
	 
	// 레코드 생성
	def addDemoRecords(connection) {
		def sql = new Sql(connection)
		def createdUsers = sql.executeInsert("INSERT INTO users(name) VALUES (?)", ["Admin"])
		def userId = createdUsers[0][0]
		sql.execute("""
INSERT INTO blog(title, users, post) VALUES (?, ?, ?)""",
["Test post", userId, "This is a test post"])
		sql.close()
	}
	
	// xml 생성
	def generateXML() {
		def xmlContent = new StringWriter()
		def xmlWriter = new MarkupBuilder(xmlContent)
		def connection = createDatabaseConnection()
		def sql = new Sql(connection) // 여기 커넥션 안해서 계속 오류났음ㅠ
		def sqlQuery= """
			SELECT B.id, B.title, B.post, U.name AS user_name
			FROM blog B
			INNER JOIN users U ON B.users = U.id"""
			sql.eachRow(sqlQuery) { record -> 
				xmlWriter.posts {
				post(id : record.id){
					title(record.title)
					users(record.user_name)
					def p = record.post
				post(p.getSubString(1, p.length().intValue()))
				}
			}

		}


		
//		
//		def writer = new StringWriter()
//		def html = new MarkupBuilder(writer)
//		html.html {
//			head {
//				title 'Simple document'
//			}
//			body(id: 'main') {
//				h1 'Building HTML the Groovy Way'
//				p {
//				   mkp.yield 'Mixing text with '
//				   strong 'bold'
//				   mkp.yield ' elements.'
//				}
//				a href: 'more.html', 'Read more...'
//			}
//		}

		
//println(xmlContent)
//		return println(writer.toString())
		sql.close()
		return xmlContent.toString()

	}
}

