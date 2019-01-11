# mycouchbase

This sample project was used in conjunction with a local Couchbase server (6.0.0), containing a bucket named 'american'
and a user: 'american' with password: 'password'.

Using the 'finance.yaml' file in the mycouchbase repo, run the following command to produce a directory 'output' with 
with sample json files to populate the 'american' bucket.

1. 'fakeit directory --count 3000 --verbose output finance.yaml'

Next, to load these json files into the local couchbase bucket run the following command:

2. 'cbimport json -c 127.0.0.1 -b american -u american -p password -d file://output/ -f sample -g key::%user_id%::#MONO_INCR#'

Next, add primary index to american bucket via Query:

3. 'CREATE PRIMARY INDEX ON american USING GSI;'
