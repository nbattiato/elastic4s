package com.sksamuel.elastic4s.mappings

import com.sksamuel.elastic4s.ElasticApi
import com.sksamuel.elastic4s.analyzers.{ArmenianLanguageAnalyzer, EnglishLanguageAnalyzer}
import org.scalatest.{FlatSpec, Matchers}

class BasicFieldDefinitionTest extends FlatSpec with Matchers with ElasticApi {

  "Basic field def" should "support shared properties" in {
    val field = textField("myfield")
      .fielddata(true)
      .stored(true)
      .index(true).norms(true)
      .normalizer("mynorm")
      .analyzer(ArmenianLanguageAnalyzer)
      .copyTo("copy1", "copy2")
      .boost(1.2)
      .searchAnalyzer(EnglishLanguageAnalyzer)
      .includeInAll(false)
      .ignoreMalformed(false)
      .coerce(false)
      .docValues(true)
      .maxInputLength(12)
      .ignoreAbove(30)
      .similarity("classic")
      .nullable(false)
      .nullValue("nully")
    FieldBuilderFn(field).string() shouldBe
      """{"type":"text","analyzer":"armenian","boost":1.2,"coerce":false,"copy_to":["copy1","copy2"],"doc_values":true,"fielddata":true,"ignore_malformed":false,"include_in_all":false,"index":"true","normalizer":"mynorm","norms":true,"null_value":"nully","search_analyzer":"english","similarity":"classic","store":true,"ignore_above":30,"max_input_length":12}"""
  }
}
