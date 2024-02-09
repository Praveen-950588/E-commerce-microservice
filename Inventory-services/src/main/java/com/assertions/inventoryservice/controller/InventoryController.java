package com.assertions.inventoryservice.controller;

import com.assertions.inventoryservice.dto.InventoryResponse;
import com.assertions.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    //http://localhost:8080/api/inventory/iphone-13,iphone13-red

    //http://localhost:8082/api/inventory?sku-code:iphone-13&sku-code=iphone13-red
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> listskucode) throws InterruptedException {

        return inventoryService.isInStock(listskucode);
    }
}
