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
		
		//Retrieve a valid timestamp from one of the customers' charges
		N1qlQueryResult timestampArgQueryResult = bucket.query(
			N1qlQuery.simple("SELECT each_charge.charge_timestamp " +
								"FROM american " +
									"UNNEST customer_charges AS each_charge " +
										"LIMIT 1;")
		);
		String timestampStr = "'" + timestampArgQueryResult.allRows().get(0).value().get("charge_timestamp").toString() + "'";
		
		
		//--------------------------------------------------------------------------->>
		//TIMESTAMP QUERY SECTION. (begin)
		
		String timestampQuery = "SELECT ARRAY s FOR s IN amer.customer_charges " +
							 "WHEN s.charge_timestamp BETWEEN date_add_str(" + timestampStr + ", -1, 'day') AND " +
								 "date_add_str(" + timestampStr + ", 1, 'day') END AS cust_charge " +
								 "FROM american AS amer " +
								 "WHERE ANY a in customer_charges " +
								 "SATISFIES a.charge_timestamp BETWEEN date_add_str(" + timestampStr + ", -1, 'day') AND " +
								 "date_add_str(" + timestampStr + ", 1, 'day') END " + 
									 "AND type = 'customer';";
		
		LocalDateTime time21 = LocalDateTime.now();
		N1qlQueryResult timestampResult = bucket.query(
			N1qlQuery.simple(timestampQuery)
		);
		int sizeOfResultNoIndex = timestampResult.allRows().size();
		LocalDateTime time3 = LocalDateTime.now();
				
		// 4. Create Index for street to expedite address Query for #5.
		String indexQuery = "CREATE INDEX idx_time " +
			"ON `american`(ALL DISTINCT ARRAY b.charge_timestamp FOR b IN customer_charges END) " +
				"WHERE type = 'customer';";
		bucket.query(
			N1qlQuery.simple(indexQuery)
		);
		LocalDateTime timeIndex1 = LocalDateTime.now();
		N1qlQueryResult timestampResultIndex = bucket.query(
			N1qlQuery.simple(timestampQuery)
		);
		int resultSizeWithNewIndex = timestampResultIndex.allRows().size();
		LocalDateTime timeIndex2 = LocalDateTime.now();
		
		long timeWithNewIndex = calculateTimeElapsed(timeIndex1, timeIndex2);
		long timeBeforeIndex = calculateTimeElapsed(time21, time3);
		
		N1qlQueryResult explainTimeStamp = bucket.query(
			N1qlQuery.simple("EXPLAIN " + timestampQuery)
		);
		
		
		bucket.query(
			N1qlQuery.simple("DROP INDEX `american`.`idx_time`;")
		);
		// Create covering index - show improved performance **
		String coveringIndexQuery = "CREATE INDEX idx_time_and_amount " +
			"ON `american`(ALL ARRAY b.charge_timestamp FOR b IN customer_charges END, customer_charges) " +
				"WHERE type = 'customer';";
		bucket.query(
			N1qlQuery.simple(coveringIndexQuery)
		);
		
		// Re-run timestamp query
		LocalDateTime timeCoveringIndex = LocalDateTime.now();
		N1qlQueryResult timestampResultCoveringIndex = bucket.query(
			N1qlQuery.simple(timestampQuery)
		);
		int resultSizeWithCoveringIndex = timestampResultCoveringIndex.allRows().size();
		LocalDateTime timeCoveringIndex2 = LocalDateTime.now(); 
		long timeWithCoveringIndex = calculateTimeElapsed(timeCoveringIndex, timeCoveringIndex2);
		N1qlQueryResult explainTimeStampCoveringIndex = bucket.query(
			N1qlQuery.simple("EXPLAIN " + timestampQuery)
		);
		bucket.query(
			N1qlQuery.simple("DROP INDEX `american`.`idx_time_and_amount`;")
		);
		
		
		System.out.println("\n\n\n\n\nQuery searching for customer charges within 24 hours of " + timestampStr + " \n" +
			"\nQuery with no index: \n" + 
				"execution time: " + timeBeforeIndex + "ms \n" +
				"result size: " + sizeOfResultNoIndex + "\n" +
			"\nQuery WITH index: \n" + 
				"execution time: " + timeWithNewIndex + "ms \n" + 
				"result size: " + resultSizeWithNewIndex);
		System.out.println("\nThe following is the EXPLAIN for the indexed timestamp query:\n\n");
		printRows(explainTimeStamp);
		System.out.println("\n\n\n");
		System.out.println("Timestamp query with covering index;\n" +
			"execution time: " + timeWithCoveringIndex + "ms\n" +
				"result size: " + resultSizeWithCoveringIndex + "\n\n");
		System.out.println("\nThe following is the EXPLAIN for the Covering indexed timestamp query:\n\n");
		printRows(explainTimeStampCoveringIndex);
		System.out.println("\n\n\n");
		
		
		
		
			
		//TIMESTAMP QUERY SECTION  (end)
		//--------------------------------------------------------------------------->>
		
		
		//--------------------------------------------------------------------------->>
		String addressQuery = "SELECT * " + 
				"FROM american AS cardholder " +
					"WHERE ANY cardAddress " +
						"IN cardholder.home_address " +
							"SATISFIES cardAddress.street=" + addressArg + " END " + 
								"AND `type` = 'customer';";
		
		//ADDRESS QUERY SECTION. (begin)
		LocalDateTime timeNoIndexAddress1 = LocalDateTime.now();
		N1qlQueryResult addressResultOriginal = bucket.query(
			N1qlQuery.simple(addressQuery)
								);
		LocalDateTime timeNoIndexAddress2 = LocalDateTime.now();
		int originalResultSize = addressResultOriginal.allRows().size();
		
		// 5. Address Query *with* INDEX
		String indexStreetQuery = "CREATE INDEX idx_street " +
			 "ON `american` (ALL DISTINCT ARRAY h.street FOR h IN home_address END);";
		bucket.query(
					N1qlQuery.simple(indexStreetQuery)
				);
		N1qlQueryResult explainAddressCoveringIndex = bucket.query(
			N1qlQuery.simple("EXPLAIN " + addressQuery)
		);
		LocalDateTime timeWithIndexStreet1 = LocalDateTime.now();
		N1qlQueryResult addressResultIndexed = bucket.query(
			N1qlQuery.simple(addressQuery)
		);
		LocalDateTime timeWithIndexStreet2 = LocalDateTime.now();
		int indexedResultSize = addressResultIndexed.allRows().size();
		
		
		System.out.println("\n\n\nQuery searching for resident at: " + addressArg);
		System.out.println("\nAddress query with NO index: \n" + 
			"execution time: " + calculateTimeElapsed(timeNoIndexAddress1, timeNoIndexAddress2) + "ms \n" +
				"result size: " + originalResultSize + "\n");
		System.out.println("Address query WITH index: \n" + 
			 "execution time: " + calculateTimeElapsed(timeWithIndexStreet1, timeWithIndexStreet2) + "ms \n" + 
				 "result size: " + indexedResultSize);
		System.out.println("\n\nEXPLAIN for ADDRESS INDEXED: \n");
		printRows(explainAddressCoveringIndex);
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
