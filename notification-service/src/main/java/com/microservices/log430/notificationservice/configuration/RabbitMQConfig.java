package com.microservices.log430.notificationservice.configuration;

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

    @Value("${messaging.exchange.notification}")
    private String notificationExchange;

    @Value("${messaging.queue.order-placed}")
    private String orderPlacedQueue;

    @Value("${messaging.queue.order-matched}")
    private String orderMatchedQueue;

    @Value("${messaging.queue.order-rejected}")
    private String orderRejectedQueue;

    @Value("${messaging.queue.notification-send}")
    private String notificationSendQueue;

    @Value("${messaging.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    @Value("${messaging.routing-key.order-matched}")
    private String orderMatchedRoutingKey;

    @Value("${messaging.routing-key.order-rejected}")
    private String orderRejectedRoutingKey;

    @Value("${messaging.routing-key.notification-send}")
    private String notificationSendRoutingKey;

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

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(notificationExchange, true, false);
    }

    /**
     * Déclaration des queues
     */
    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(orderPlacedQueue).build();
    }

    @Bean
    public Queue orderMatchedQueue() {
        return QueueBuilder.durable(orderMatchedQueue).build();
    }

    @Bean
    public Queue orderRejectedQueue() {
        return QueueBuilder.durable(orderRejectedQueue).build();
    }

    @Bean
    public Queue notificationSendQueue() {
        return QueueBuilder.durable(notificationSendQueue).build();
    }

    /**
     * Bindings pour écouter les notifications à envoyer
     */
    @Bean
    public Binding notificationSendBinding() {
        return BindingBuilder
                .bind(notificationSendQueue())
                .to(notificationExchange())
                .with(notificationSendRoutingKey);
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
