package com.lal.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lal.demo.model.Ticket;
import com.lal.demo.service.impl.TicketServiceImpl;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class TicketController {

    private TicketServiceImpl ticketService;
    private Retry retry;
    private RestTemplate restTemplate = new RestTemplate();

    public TicketController(TicketServiceImpl ticketService, Retry retry){
        this.ticketService = ticketService;
        this.retry = retry;
    }

    @PostMapping(value="/buyTicket",consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> buyTicket(@RequestBody Ticket ticket,@RequestHeader(value = "Authorization") String token){
        ObjectMapper objectMapper = new ObjectMapper();
        //int nbSoldTickets = ticketService.countTicketByFlightId(ticket.getFlightId());
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String flightserviceUri=null;
        String userserviceUri=null;
        HttpPost zuul = new HttpPost("http://localhost:8762/actuator/routes");
        try {
            response = client.execute(zuul);
            String result = EntityUtils.toString(response.getEntity());
            Map<String, String> map = objectMapper.readValue(result, Map.class);
            String flightservice = "flightservice";
            String userservice = "userservice";
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getValue().equals(flightservice)) {
                    flightserviceUri=entry.getKey();
                }
                if (entry.getValue().equals(userservice)) {
                    userserviceUri=entry.getKey();
                }
            }
            flightserviceUri = flightserviceUri.replace("**","");
            userserviceUri = userserviceUri.replace("**","");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        HttpPost httpPost = new HttpPost("http://localhost:8762"+flightserviceUri+"flight/checkCapacity?flightId="+Long.toString(ticket.getFlightId()));
//        httpPost.setHeader("Accept", "application/json");
//        httpPost.setHeader("Content-type", "application/json");
//        httpPost.setHeader("Authorization","Bearer "+token);
        //httpPost.setHeader("nbSoldTickets",Integer.toString(nbSoldTickets));
        //httpPost.setHeader("flightId",Long.toString(ticket.getFlightId()));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization","Bearer "+token);
        HttpEntity<String> entity = new HttpEntity<String>(null,headers);
        String finalFlightserviceUri = flightserviceUri;
        ResponseEntity<Map<String,Object>> odgovor = Decorators.ofSupplier(()-> {
            System.out.println("Kupuje kartu za let "+ticket.getFlightId());
            return restTemplate.exchange("http://localhost:8762" + finalFlightserviceUri + "flight/checkCapacity?flightId=" + Long.toString(ticket.getFlightId()),
                    HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        }).withRetry(retry).get();
        boolean checkCapacity=(Boolean)odgovor.getBody().get("checkCapacity");
        double price=(int)odgovor.getBody().get("price");
        int miles=(int)odgovor.getBody().get("miles");
//        try {
//            response = client.execute(httpPost);
//            String result = EntityUtils.toString(response.getEntity());
//            Map<String, Object> map = objectMapper.readValue(result, Map.class);
//            checkCapacity = (boolean) map.get("checkCapacity");
//            price = (int) map.get("price");
//            miles = (int) map.get("miles");
//            System.out.println("ima mesta i cena je " + checkCapacity + price);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        if(checkCapacity){
            HttpPost httpPost1 = new HttpPost("http://localhost:8762"+userserviceUri+"userInfo");
            httpPost1.setHeader("Accept", "application/json");
            httpPost1.setHeader("Content-type", "application/json");
            httpPost1.setHeader("Authorization","Bearer "+token);
            httpPost1.setHeader("userId",Long.toString(ticket.getUserId()));
            String rank = null;
            boolean hasCreditCards = false;
            try {
                response = client.execute(httpPost1);
                String result = EntityUtils.toString(response.getEntity());
                Map<String, Object> map = objectMapper.readValue(result, Map.class);
                hasCreditCards = (boolean) map.get("hasCreditCards");
                rank = (String) map.get("rank");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(hasCreditCards){
                switch (rank){
                    case "GOLD":
                        price = price * 0.8;
                        break;
                    case "SILVER":
                        price = price * 0.9;
                        break;
                }
            }
            System.out.println("NOVA CENA JE " + price);
            HttpPost httpPost2 = new HttpPost("http://localhost:8762"+flightserviceUri+"flight/updateCapacity");
            httpPost2.setHeader("Accept", "application/json");
            httpPost2.setHeader("Content-type", "application/json");
            httpPost2.setHeader("Authorization","Bearer "+token);
            httpPost2.setHeader("flightId",Long.toString(ticket.getFlightId()));
            try {
                response = client.execute(httpPost2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            HttpPost httpPost3 = new HttpPost("http://localhost:8762"+userserviceUri+"updatemilesandrank");
            httpPost3.setHeader("Accept", "application/json");
            httpPost3.setHeader("Content-type", "application/json");
            httpPost3.setHeader("Authorization","Bearer "+token);
            httpPost3.setHeader("miles",Integer.toString(miles));
            ticketService.save(ticket);
            try {
                response = client.execute(httpPost3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/getTickets",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Ticket>> getAllTickets(@RequestParam Long id)
    {
        List<Ticket> list = ticketService.findAllByUserIdOrderByDateDesc(id);

        return new ResponseEntity<List<Ticket>>(list,new HttpHeaders(), HttpStatus.OK);
    }

        @GetMapping(value = "/getUsers",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUsersByFlight(@RequestParam Long id)
    {
        List<Ticket> list = ticketService.findAllByFlightId(id);
        List<String> userIds = new ArrayList<>();
        for(Ticket t: list)
        {
            userIds.add(Long.toString(t.getUserId()));
        }
        Map<String,List<String>> map = new HashMap<>();
        map.put("userIds",userIds);

        return new ResponseEntity<Map<String,List<String>>>(map,new HttpHeaders(), HttpStatus.OK);
    }



}
