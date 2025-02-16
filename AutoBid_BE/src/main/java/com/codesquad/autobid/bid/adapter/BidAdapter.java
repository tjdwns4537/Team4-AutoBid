package com.codesquad.autobid.bid.adapter;

import com.codesquad.autobid.auction.domain.Auction;
import com.codesquad.autobid.auction.repository.AuctionRedisDTO;
import com.codesquad.autobid.auction.repository.AuctionRepository;
import com.codesquad.autobid.auction.service.AuctionService;
import com.codesquad.autobid.bid.domain.Bid;
import com.codesquad.autobid.bid.repository.BidRedisRepository;
import com.codesquad.autobid.bid.repository.BidRepository;
import com.codesquad.autobid.bid.request.BidRegisterRequest;
import com.codesquad.autobid.websocket.domain.AuctionDtoWebSocket;
import com.codesquad.autobid.websocket.service.WebSocketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BidAdapter {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AuctionService auctionService;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final WebSocketService webSocketService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final BidRedisRepository bidRedisRepository;

    public BidAdapter(KafkaTemplate<String, String> kafkaTemplate,
                      AuctionService auctionService,
                      BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      WebSocketService webSocketService,
                      SimpMessageSendingOperations messagingTemplate,
                      BidRedisRepository bidRedisRepository) {
        this.auctionService = auctionService;
        this.webSocketService = webSocketService;
        this.messagingTemplate = messagingTemplate;
        this.bidRedisRepository = bidRedisRepository;
        this.objectMapper = new ObjectMapper();
        this.kafkaTemplate = kafkaTemplate;
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
    }

    // Bid 데이터베이스에 저장
    // Auction endPrice 갱신
    @KafkaListener(topics = "bid-event", groupId = "bid-mysql")
    public void saveBidAndUpdate(String bidRegisterRequestStr) throws JsonProcessingException {
        BidRegisterRequest bidRegisterRequest = objectMapper.readValue(bidRegisterRequestStr, BidRegisterRequest.class);
        System.out.println(bidRegisterRequest);
        log.info("bid-event bid-mysql {}", bidRegisterRequest);
        // bid 저장
        Bid bid = bidRepository.findBidByAuctionIdAndUserId(bidRegisterRequest.getAuctionId(),
                bidRegisterRequest.getUserId()).orElse(Bid.of(AggregateReference.to(bidRegisterRequest.getAuctionId()),
                AggregateReference.to(bidRegisterRequest.getUserId()), bidRegisterRequest.getSuggestedPrice(), false));

        bid.updatePrice(bidRegisterRequest.getSuggestedPrice());

        bidRepository.save(bid);

        // Auction 저장
        Auction auction = auctionRepository.findById(bidRegisterRequest.getAuctionId())
                .orElseThrow(() -> new RuntimeException("존재하는 경매가 없습니다."));

        auction.updateEndPrice(bidRegisterRequest.getSuggestedPrice());

        auctionRepository.save(auction);
    }

    // TODO
    // 1. redis에 bidders 저장하기
    // 2. bidders 웹소켓에 뿌려주기
    @KafkaListener(topics = "bid-event", groupId = "bidder-redis")
    public void saveBiddersAndBroadcast(@Payload String bidRegisterRequestStr) throws JsonProcessingException {
        BidRegisterRequest bidRegisterRequest = objectMapper.readValue(bidRegisterRequestStr, BidRegisterRequest.class); // auctionID, userID
        Bid bid = Bid.of(AggregateReference.to(bidRegisterRequest.getAuctionId()),
                AggregateReference.to(bidRegisterRequest.getUserId()),
                bidRegisterRequest.getSuggestedPrice(), false);
        bidRedisRepository.save(bid);
        Long auctionId = bidRegisterRequest.getAuctionId();
        AuctionRedisDTO auctionRedis = auctionService.getAuction(auctionId);
        AuctionDtoWebSocket auctionDtoWebSocket = webSocketService.parsingDto(auctionRedis);

        log.error("auctionRedis-id : {}", auctionRedis.getAuctionId());
        log.error("auctionRedis-price : {}", auctionRedis.getPrice());
        log.error("auctionDtoWebSocket-price : {}", auctionDtoWebSocket.getPrice());
        log.error("auctionDtoWebSocket-numberOfUsers : {}", auctionDtoWebSocket.getNumberOfUsers());
        messagingTemplate.convertAndSend("/ws/start/" + auctionId, auctionDtoWebSocket);
        log.info("bid-event bid-redis {}", bidRegisterRequest);
    }

    // TODO
    // 1. 현재가 브로드캐스트하기
    @KafkaListener(topics = "bid-event", groupId = "bidPrice-broadcast")
    public void broadcast(@Payload String bidRegisterRequestStr) throws JsonProcessingException {
        BidRegisterRequest bidRegisterRequest = objectMapper.readValue(bidRegisterRequestStr, BidRegisterRequest.class);
        log.info("bid-event bid-broadcast {}", bidRegisterRequest);
        System.out.println(objectMapper.readValue(bidRegisterRequestStr, BidRegisterRequest.class).getUserId());
    }

    public void produce(BidRegisterRequest bidRegisterRequest) throws JsonProcessingException {
        String bidRegisterRequestStr = objectMapper.writeValueAsString(bidRegisterRequest);
        log.info("produce {}", bidRegisterRequestStr);
        this.kafkaTemplate.send("bid-event", bidRegisterRequestStr);
    }
}
