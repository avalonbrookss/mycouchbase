package MyCouchbase;
import com.couchbase.client.java.*;
import com.couchbase.client.java.document.*;
import com.couchbase.client.java.document.json.*;
import com.couchbase.client.java.query.*;
import com.couchbase.client.java.query.consistency.*;




public class MyCouchbase {
	public static void main(String[] args) {
		Cluster cluster = CouchbaseCluster.create("couchbase://127.0.0.1");
		Bucket bucket = cluster.openBucket("person", "password");
		
		JsonObject person = JsonObject.create();
		person.put("id", "77");
		person.put("firstname", "Jane");
		person.put("age", "100");
// 		JsonArray socialMedia = JsonArray.create();
// 		socialMedia.add(JsonObject.create().put("title", "Twitter").put("link", "https://www.google.com"));
// 		person.put("socialmedia", socialMedia);
		JsonDocument document = JsonDocument.create("77", person);
		
		bucket.upsert(document);
		
		System.out.println(bucket.get("77").content());
		
	}
    public boolean someLibraryMethod() {
        return true;
    }
}
