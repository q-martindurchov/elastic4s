## Clients

The entry point to executing requests in elastic4s is the concrete class  `ElasticClient`.
This class is used to execute requests, such as `SearchRequest`, against an Elasticsearch cluster and returns a response type such as `SearchResponse`.

`ElasticClient` takes care of transforming the requests and responses, and handling success and failure, but the actual HTTP functions are delegated to an instance of `HttpClient.`
This typeclass wraps an underlying http client, such as akka-http or sttp so that it can be used by the `ElasticClient`.

The most simple example is the `JavaClient` class, provided by the `elastic4s-client-esjava` module. This implementation wraps the http library provided by the offical Java elasticsearch library.

So, to connect to an ElasticSearch cluster, pass an instance of `JavaClient` to an `ElasticClient`.
`JavaClient` is configured using `ElasticProperties` in which you can specify protocol, host, and port in a single string

```scala
val client = ElasticClient(JavaClient(ElasticProperties("http://host1:9200")))
```

For multiple nodes you can pass a comma-separated list of endpoints in a single string:

```scala
val nodes = "http://host1:9200,host2:9200,host3:9200"
val client = ElasticClient(JavaClient(ElasticProperties(nodes)))
```

### Credentials

Credentials can be passed by defining `CommonRequestOptions` implicit in the visibility scope when calling
`client.execute()` method:

```scala
implicit val options: CommonRequestOptions = CommonRequestOptions.defaults.copy(
  credentials = Some(BasicHttpCredentials("user", "pass"))
)

client.execute(search("myindex"))
```

Currently, two methods of authentication are supported:

* `Authentication.UsernamePassword` to pass a username and password
* `Authentication.ApiKey` to pass an API key



### Using different clients

Other [alternative clients](https://search.maven.org/search?q=g:com.sksamuel.elastic4s%20elastic4s-client) are provided as part of elastic4s - such as akka-http and sttp.

To use these, add the appropriate module to your build, and then pass an instance of that `HttpClient` to `ElasticClient`.

#### Akka HTTP
For Akka HTTP, we use `AkkaHttpClient`:

```scala
val client = ElasticClient(AkkaHttpClient(AkkaHttpClientSettings(List("host1:9200"))))
```

It's possible to create the `AkkaHttpClientSettings` from Typesafe configuration using `AkkaHttpClientSettings.defaults` or by passing in a `Config` instance using `AkkaHttpClientSettings(config)`.

The default configuration:

```
com.sksamuel.elastic4s.akka {
  hosts: []
  https: false
  verify-ssl-certificate : true
  queue-size: 1000
  blacklist {
    min-duration = 1m
    max-duration = 30m
  }
  max-retry-timeout = 30s
  akka.http {
    // akka-http settings specific for elastic4s
    // can be overwritten in this section
  }
}
```

Using `AkkaHttpClientSettings.defaults` you can still override any of these settings by defining the right keys in your own `application.conf`

The Akka HTTP client supports basic authentication by specifying a username and password:

```
com.sksamuel.elastic4s.akka {
  username = user
  password = pass
}
```

#### STTP
For sttp, we use `SttpRequestHttpClient`:

```scala
val elasticNodeEndpoint = ElasticNodeEndpoint("http", "host1", 9200, None)
val client = ElasticClient(SttpRequestHttpClient(elasticNodeEndpoint))
```
