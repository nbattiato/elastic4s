package com.sksamuel.elastic4s.search.aggs

import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.http.search.IpRangeBucket
import com.sksamuel.elastic4s.testkit.DiscoveryLocalNodeProvider
import org.scalatest.{FreeSpec, Matchers}

import scala.util.Try

class IpRangeAggregationHttpTest extends FreeSpec with DiscoveryLocalNodeProvider with Matchers with ElasticDsl {

  Try {
    http.execute {
      deleteIndex("iprangeagg")
    }.await
  }

  http.execute {
    createIndex("iprangeagg") mappings {
      mapping("doc") fields ipField("ip")
    }
  }.await

  // based on the examples from IpRange aggregation docs
  http.execute(
    bulk(
      indexInto("iprangeagg/doc").fields("ip" -> "10.0.0.1"),
      indexInto("iprangeagg/doc").fields("ip" -> "10.0.0.2"),
      indexInto("iprangeagg/doc").fields("ip" -> "10.0.0.5"),
      indexInto("iprangeagg/doc").fields("ip" -> "10.0.0.100"),
      indexInto("iprangeagg/doc").fields("ip" -> "10.0.0.128")
    ).refreshImmediately
  ).await

  "ip range agg" - {
    "should return expected buckets" in {
      val resp = http.execute {
        search("iprangeagg").matchAllQuery().aggs {
          ipRangeAggregation("ip_ranges")
            .field("ip")
            .unboundedTo(to = "10.0.0.5")
            .unboundedFrom(from = "10.0.0.5")
        }
      }.await.right.get.result

      resp.totalHits shouldBe 5

      val agg = resp.aggs.ipRange("ip_ranges")
      agg.buckets.map(_.copy(data = Map.empty)) shouldBe Seq(
        IpRangeBucket(None, 2, None, Some("10.0.0.5"), Map.empty),
        IpRangeBucket(None, 3, Some("10.0.0.5"), None, Map.empty)
      )
    }

    "should return expected buckets with mask ranges" in {
      val resp = http.execute {
        search("iprangeagg").matchAllQuery().aggs {
          ipRangeAggregation("ip_ranges")
            .field("ip")
            .maskRange("10.0.0.0/25")
            .maskRange("10.0.0.127/25")
            .maskRange("10.0.0.128/25")
        }
      }.await.right.get.result

      resp.totalHits shouldBe 5

      val agg = resp.aggs.ipRange("ip_ranges")
      agg.buckets.map(_.copy(data = Map.empty)) shouldBe Seq(
        IpRangeBucket(Some("10.0.0.0/25"), 4, Some("10.0.0.0"), Some("10.0.0.128"), Map.empty),
        IpRangeBucket(Some("10.0.0.127/25"), 4, Some("10.0.0.0"), Some("10.0.0.128"), Map.empty),
        IpRangeBucket(Some("10.0.0.128/25"), 1, Some("10.0.0.128"), Some("10.0.1.0"), Map.empty)
      )
    }

    "should return expected buckets with keyed results" in {
      val resp = http.execute {
        search("iprangeagg").matchAllQuery().aggs {
          ipRangeAggregation("ip_ranges")
            .field("ip")
            .keyed(true)
            .unboundedTo(to = "10.0.0.5")
            .unboundedFrom(from = "10.0.0.5")
        }
      }.await.right.get.result

      resp.totalHits shouldBe 5

      val agg = resp.aggs.ipRange("ip_ranges")
      agg.buckets.map(_.copy(data = Map.empty)) shouldBe Seq(
        IpRangeBucket(Some("*-10.0.0.5"), 2, None, Some("10.0.0.5"), Map.empty),
        IpRangeBucket(Some("10.0.0.5-*"), 3, Some("10.0.0.5"), None, Map.empty)
      )
    }
  }
}
