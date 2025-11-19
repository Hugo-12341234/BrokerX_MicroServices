package com.microservices.log430.orderservice.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${messaging.exchange.order}")
    private String orderExchange;

    @Value("${messaging.exchange.matching}")
    private String matchingExchange;

    @Value("${messaging.queue.order-placed}")
    private String orderPlacedQueue;

    @Value("${messaging.queue.order-matched}")
    private String orderMatchedQueue;

    @Value("${messaging.queue.order-rejected}")
    private String orderRejectedQueue;

    @Value("${messaging.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    @Value("${messaging.routing-key.order-matched}")
    private String orderMatchedRoutingKey;

    @Value("${messaging.routing-key.order-rejected}")
    private String orderRejectedRoutingKey;

    /**
     * Déclaration des exchanges
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    @Bean
    public TopicExchange matchingExchange() {
        return new TopicExchange(matchingExchange, true, false);
    }

    /**
     * Déclaration des queues
     */
    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(orderPlacedQueue)
                .withArgument("x-dead-letter-exchange", orderExchange + ".dlx")
                .build();
    }

    @Bean
    public Queue orderMatchedQueue() {
        return QueueBuilder.durable(orderMatchedQueue).build();
    }

    @Bean
    public Queue orderRejectedQueue() {
        return QueueBuilder.durable(orderRejectedQueue).build();
    }

    /**
     * Bindings entre les queues et les exchanges
     */
    @Bean
    public Binding orderPlacedBinding() {
        return BindingBuilder
                .bind(orderPlacedQueue())
                .to(orderExchange())
                .with(orderPlacedRoutingKey);
    }

    @Bean
    public Binding orderMatchedBinding() {
        return BindingBuilder
                .bind(orderMatchedQueue())
                .to(matchingExchange())
                .with(orderMatchedRoutingKey);
    }

    @Bean
    public Binding orderRejectedBinding() {
        return BindingBuilder
                .bind(orderRejectedQueue())
                .to(matchingExchange())
                .with(orderRejectedRoutingKey);
    }

    /**
     * Dead Letter Exchange pour les messages échoués
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(orderExchange + ".dlx", true, false);
    }

    /**
     * Dead Letter Queue pour les messages échoués
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(orderPlacedQueue + ".dlq").build();
    }

    /**
     * Binding pour la Dead Letter Queue
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(orderPlacedRoutingKey);
    }

    /**
     * Configuration du RabbitTemplate avec convertisseur JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    /**
     * Convertisseur JSON pour les messages
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
