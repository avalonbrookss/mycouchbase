package MyCouchbase;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.couchbase.client.java.*;
import com.couchbase.client.java.document.*;
import com.couchbase.client.java.document.json.*;
import com.couchbase.client.java.query.*;
import com.couchbase.client.java.query.consistency.*;
import com.couchbase.client.java.env.*;




public class MyCouchbase {
	public static void main(String[] args) {		
		// CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
// 			.connectTimeout(10000)
// 				.build();
		Cluster cluster = CouchbaseCluster.create("couchbase://127.0.0.1");
		cluster.authenticate("american", "password");
		Bucket bucket = cluster.openBucket("american");

		// Retrieve a valid address from db to search for in Query #1 for address
		N1qlQueryResult addrArgResponse = bucket.query(
			N1qlQuery.simple("SELECT addr.street " +
				"FROM american " +
					"UNNEST home_address AS addr " +
						"WHERE addr.address_id=251;")
		);
		String addressArg = "'" + addrArgResponse.allRows().get(0).value().get("street").toString() + "'";
		
		
		//--------------------------------------------------------------------------->>
		//TIMESTAMP QUERY SECTION. (begin)
		
		LocalDateTime time21 = LocalDateTime.now();
		String timestampQuery = "SELECT ARRAY s FOR s IN amer.customer_charges " +
							 "WHEN s.charge_timestamp BETWEEN '2018-10-04 %' AND '2018-10-05 %' END AS cust_charge " +
								 "FROM american AS amer " +
									 "WHERE ANY a in customer_charges " +
											 "SATISFIES a.charge_timestamp BETWEEN '2018-10-04 %' AND '2018-10-05 %' END " +
												 "ORDER BY a.charge_id ASC;";
		N1qlQueryResult timestampResult = bucket.query(
			N1qlQuery.simple(timestampQuery)
		);
		LocalDateTime time3 = LocalDateTime.now();
				
		// 4. Create Index for street to expedite address Query for #5.
		String indexQuery = "CREATE INDEX idx_time " +
			"ON `american`(ALL DISTINCT ARRAY b.charge_timestamp FOR b IN customer_charges END);";
		bucket.query(
			N1qlQuery.simple(indexQuery)
		);
		LocalDateTime timeIndex1 = LocalDateTime.now();
		N1qlQueryResult timestampResultIndex = bucket.query(
			N1qlQuery.simple(timestampQuery)
		);
		LocalDateTime timeIndex2 = LocalDateTime.now();
		
		long timeWithNewIndex = calculateTimeElapsed(timeIndex1, timeIndex2);
		long timeBeforeIndex = calculateTimeElapsed(time21, time3);
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Query searching for customer charges between 2018-10-01 TO 2018-10-05 \n" +
			"\nTime for query with no index: " + timeBeforeIndex + "ms \n" +
				"Time for query WITH index: " + timeWithNewIndex + "ms");
				
		bucket.query(
			N1qlQuery.simple("DROP INDEX `american`.`idx_time`;")
		);
		
		//TIMESTAMP QUERY SECTION  (end)
		//--------------------------------------------------------------------------->>
		
		
		//--------------------------------------------------------------------------->>
		//ADDRESS QUERY SECTION. (begin)
		LocalDateTime timeNoIndexAddress1 = LocalDateTime.now();
		bucket.query(
			N1qlQuery.simple("SELECT * " + 
				"FROM american AS cardholder " +
					"WHERE ANY cardAddress " +
						"IN cardholder.home_address " +
							"SATISFIES cardAddress.street=" + addressArg + " END;")
								);
		LocalDateTime timeNoIndexAddress2 = LocalDateTime.now();
		
		
		// 5. Address Query *with* INDEX
		String indexStreetQuery = "CREATE INDEX idx_street " +
			 "ON `american` (ALL DISTINCT ARRAY h.street FOR h IN home_address END);";
		bucket.query(
					N1qlQuery.simple(indexStreetQuery)
				);
		
		LocalDateTime timeWithIndexStreet1 = LocalDateTime.now();
		N1qlQueryResult addressResultIndexed = bucket.query(
			N1qlQuery.simple("SELECT last_name " + 
				"FROM american AS cardholder " +
				"WHERE ANY cardAddress " +
				"IN cardholder.home_address " +
				"SATISFIES cardAddress.street=" + addressArg + " END;")
		);
		LocalDateTime timeWithIndexStreet2 = LocalDateTime.now();

		System.out.println();
		System.out.println();
		System.out.println("Query searching for resident at: " + addressArg);
		System.out.println();
		System.out.println("Time for address search with NO index: " + 
			calculateTimeElapsed(timeNoIndexAddress1, timeNoIndexAddress2) + "ms");
		System.out.println("Time for address search WITH index: " + 
			 calculateTimeElapsed(timeWithIndexStreet1, timeWithIndexStreet2) + "ms");
		bucket.query(
			N1qlQuery.simple("DROP INDEX `american`.`idx_street`;")
		);
		
	
		//ADDRESS QUERY SECTION  (end)
		//--------------------------------------------------------------------------->>
		System.out.println();
		System.out.println();
		bucket.close();
		// cluster disconnect below was taking ~4s to complete
		// cluster.disconnect();
		
	}
	
	
	// private methods 
	private static void printRows(N1qlQueryResult result) {
		for(N1qlQueryRow row : result){
			System.out.println(row);
		}
	}
	
	private static long calculateTimeElapsed(LocalDateTime start, LocalDateTime finish) {
		return start.until(finish, ChronoUnit.MILLIS);
	}

}
