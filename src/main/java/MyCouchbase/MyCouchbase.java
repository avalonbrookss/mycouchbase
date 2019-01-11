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

		// 1. N1ql Query for specific address 
		spacerRows("Cardholder by their ADDRESS \n \t \t searching for street = " + addressArg);
		LocalDateTime time1 = LocalDateTime.now();
		N1qlQueryResult addressResult = bucket.query(
			N1qlQuery.simple("SELECT * " + 
				"FROM american AS cardholder " +
				"WHERE ANY cardAddress " +
				"IN cardholder.home_address " +
				"SATISFIES cardAddress.street=" + addressArg + " END;")
		);
		LocalDateTime time2 = LocalDateTime.now();
		printRows(addressResult);
		printTimeElapsed(time1, time2);
		
		
		// 2. N1ql Query for elements within Date range 
		spacerRows("TIMESTAMP within Date Range: \n \t \t  2018-10-04 TO 2018-10-05");
		LocalDateTime time21 = LocalDateTime.now();
		N1qlQueryResult timestampResult = bucket.query(
			N1qlQuery.simple("SELECT single_charge " +
							 "FROM american AS cardholder " +
								 "UNNEST cardholder.customer_charges AS single_charge " +
									 "WHERE single_charge.charge_timestamp BETWEEN '2018-10-04 %' AND '2018-10-05' " +
											 "ORDER BY single_charge.charge_id ASC;")
		);
		LocalDateTime time3 = LocalDateTime.now();
		printRows(timestampResult);
		printTimeElapsed(time21, time3);
		
		
		// 3. N1ql Query using parameterized function
		// spacerRows("Parameterized Search");
// 		LocalDateTime time31 = LocalDateTime.now();
// 		N1qlQueryResult parameterizedResult = bucket.query(
// 			N1qlQuery.parameterized("SELECT * FROM american WHERE email = $email;",
// 					JsonObject.create().put("email", "Mina22@hotmail.com"))
// 		);
// 		LocalDateTime time4 = LocalDateTime.now();
// 		printRows(parameterizedResult);
// 		printTimeElapsed(time31, time4);
		
	}
	
	
	// private methods 
	private static void printRows(N1qlQueryResult result) {
		for(N1qlQueryRow row : result){
			System.out.println(row);
		}
	}
	
	private static void printTimeElapsed(LocalDateTime start, LocalDateTime finish) {
		System.out.println("##");
		System.out.println(" \t [ Time elapsed for search: " + start.until(finish, ChronoUnit.MILLIS) + "ms ]");
		System.out.println("##");
		System.out.println("##");
		System.out.println();
	}
	
	private static void spacerRows(String customStr) {
		System.out.println("**");
		System.out.println("**");
		System.out.println("**");
		System.out.println("  \t ______Query for " + customStr + "______\t ");
		System.out.println("**");
	}

}
