/*
 * Copyright 2019 Ververica GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.statefun.examples.greeter;

import com.ververica.statefun.examples.greeter.generated.GreetRequest;
import com.ververica.statefun.examples.greeter.generated.GreetResponse;
import com.ververica.statefun.sdk.io.EgressIdentifier;
import com.ververica.statefun.sdk.io.EgressSpec;
import com.ververica.statefun.sdk.io.IngressIdentifier;
import com.ververica.statefun.sdk.io.IngressSpec;
import com.ververica.statefun.sdk.kafka.KafkaEgressBuilder;
import com.ververica.statefun.sdk.kafka.KafkaEgressSerializer;
import com.ververica.statefun.sdk.kafka.KafkaIngressBuilder;
import com.ververica.statefun.sdk.kafka.KafkaIngressDeserializer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * A collection of all the components necessary to consume from and write to an external system, in
 * this case Apache Kafka.
 *
 * <p>The ingress and egress identifiers provide named references without exposing the underlying
 * system. This way, in a multi-module deployment, functions can interact with IO modules through
 * identifiers without depending on specific implementations.
 */
final class GreetingIO {

  static final IngressIdentifier<GreetRequest> GREETING_INGRESS_ID =
      new IngressIdentifier<>(GreetRequest.class, "ververica", "greet-ingress");

  static final EgressIdentifier<GreetResponse> GREETING_EGRESS_ID =
      new EgressIdentifier<>("ververica", "kafka-greeting-output", GreetResponse.class);

  private final String kafkaAddress;

  GreetingIO(String kafkaAddress) {
    this.kafkaAddress = Objects.requireNonNull(kafkaAddress);
  }

  IngressSpec<GreetRequest> getIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(GREETING_INGRESS_ID)
        .withKafkaAddress(kafkaAddress)
        .withTopic("names")
        .withDeserializer(GreetKafkaDeserializer.class)
        .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "greetings")
        .build();
  }

  EgressSpec<GreetResponse> getEgressSpec() {
    return KafkaEgressBuilder.forIdentifier(GREETING_EGRESS_ID)
        .withKafkaAddress(kafkaAddress)
        .withSerializer(GreetKafkaSerializer.class)
        .build();
  }

  private static final class GreetKafkaDeserializer
      implements KafkaIngressDeserializer<GreetRequest> {

    private static final long serialVersionUID = 1L;

    @Override
    public GreetRequest deserialize(ConsumerRecord<byte[], byte[]> input) {
      String who = new String(input.value(), StandardCharsets.UTF_8);

      return GreetRequest.newBuilder().setWho(who).build();
    }
  }

  private static final class GreetKafkaSerializer implements KafkaEgressSerializer<GreetResponse> {

    private static final long serialVersionUID = 1L;

    @Override
    public ProducerRecord<byte[], byte[]> serialize(GreetResponse response) {
      byte[] key = response.getWho().getBytes(StandardCharsets.UTF_8);
      byte[] value = response.getGreeting().getBytes(StandardCharsets.UTF_8);

      return new ProducerRecord<>("greetings", key, value);
    }
  }
}
