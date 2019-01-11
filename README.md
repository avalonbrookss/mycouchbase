# mycouchbase

This sample project was used in conjunction with a local Couchbase server (6.0.0)

Pre-requisites:
1. Local Couchbase Server installed with:
 a) bucket named 'american'
 b) user: 'american' password: 'password' (admin priveleges) 
2. FakeIt installed.

To prepare to execute the main class, use the 'finance.yaml' file in this repository to generate mock data, run the following command to produce a directory 'output' with sample JSON files to populate the 'american' bucket.

1. 'fakeit directory --count 3000 --verbose output finance.yaml'

Next, to load these json files into the local couchbase bucket run the following command:

2. 'cbimport json -c 127.0.0.1 -b american -u american -p password -d file://output/ -f sample -g key::%user_id%::#MONO_INCR#'

Next, add primary index to the 'american' bucket via browser's Query console:

3. 'CREATE PRIMARY INDEX ON american USING GSI;'

4. cd into 'mycouchbase' directory and execute 'gradle run' command - results will output to console.
