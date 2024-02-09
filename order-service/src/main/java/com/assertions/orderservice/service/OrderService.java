package com.assertions.orderservice.service;

import com.assertions.orderservice.dto.InventoryResponse;
import com.assertions.orderservice.dto.OrderLineItemDto;
import com.assertions.orderservice.dto.OrderRequest;
import com.assertions.orderservice.model.Order;
import com.assertions.orderservice.model.OrderLineItems;
import com.assertions.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import zipkin2.internal.Trace;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;

    private final Tracer tracer;
    public String placeOrder(OrderRequest orderRequest) {

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemDtoList()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .collect(Collectors.toList());
            log.info("calling inventory service");

           Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

           try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
               // Call Inventory service and check if products are in stock
               InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                       .uri("http://inventory-service/api/inventory",
                               uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                       .retrieve()
                       .bodyToMono(InventoryResponse[].class)
                       .block();

               boolean allProductInStock = Arrays.stream(inventoryResponseArray)
                       .allMatch(InventoryResponse::isInStock);

               if (allProductInStock) {
                   orderRepository.save(order);
                   return "Order placed successfully";
               } else {
                   throw new IllegalArgumentException("Product is not in stock");
               }
           }finally {
               inventoryServiceLookup.end();
           }

    }

    private OrderLineItems mapToDto(OrderLineItemDto orderLineItemDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemDto.getPrice());
        orderLineItems.setQuantity(orderLineItemDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemDto.getSkuCode());
        return orderLineItems;
    }
}
