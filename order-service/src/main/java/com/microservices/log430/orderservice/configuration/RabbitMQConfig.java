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

    @Value("${messaging.queue.order-placed}")
    private String orderPlacedQueue;

    @Value("${messaging.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    /**
     * Déclaration de l'exchange pour les événements d'ordre
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    /**
     * Déclaration de la queue pour les ordres placés
     */
    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(orderPlacedQueue)
                .withArgument("x-dead-letter-exchange", orderExchange + ".dlx")
                .build();
    }

    /**
     * Binding entre la queue et l'exchange
     */
    @Bean
    public Binding orderPlacedBinding() {
        return BindingBuilder
                .bind(orderPlacedQueue())
                .to(orderExchange())
                .with(orderPlacedRoutingKey);
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
