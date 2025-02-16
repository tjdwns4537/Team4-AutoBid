package com.codesquad.autobid.auction.domain;

import com.codesquad.autobid.car.domain.Car;
import com.codesquad.autobid.user.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@ToString
@Getter
@Table("auction")
@NoArgsConstructor
public class Auction {

    @Id
    @Column(value = "auction_id")
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    @Column(value = "car_id")
    private AggregateReference<Car, Long> carId;
    @Column(value = "user_id")
    private AggregateReference<User, Long> userId;
    @Column(value = "auction_title")
    private String auctionTitle;
    @Column(value = "auction_start_time")
    private LocalDateTime auctionStartTime;
    @Column(value = "auction_end_time")
    private LocalDateTime auctionEndTime;
    @Column(value = "auction_start_price")
    private Long auctionStartPrice;
    @Column(value = "auction_end_price")
    private Long auctionEndPrice;
    @Column(value = "auction_status")
    private AuctionStatus auctionStatus;
    @CreatedDate
    @Column(value = "created_at")
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(value = "updated_at")
    private LocalDateTime updatedAt;

    private Auction(Long carId, Long userId, String auctionTitle, LocalDateTime auctionStartTime,
                    LocalDateTime auctionEndTime,
                    Long auctionStartPrice,
                    Long auctionEndPrice, AuctionStatus auctionStatus) {
        this.carId = AggregateReference.to(carId);
        this.userId = AggregateReference.to(userId);
        this.auctionTitle = auctionTitle;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
        this.auctionStartPrice = auctionStartPrice;
        this.auctionEndPrice = auctionEndPrice;
        this.auctionStatus = auctionStatus;
    }

    public static Auction of(Long carId,
                             Long userId,
                             String auctionTitle,
                             LocalDateTime auctionStartTime,
                             LocalDateTime auctionEndTime,
                             Long auctionStartPrice,
                             Long auctionEndPrice,
                             AuctionStatus auctionStatus) {
        return new Auction(carId, userId, auctionTitle, auctionStartTime, auctionEndTime, auctionStartPrice,
            auctionEndPrice,
            auctionStatus);
    }

    public void open() {
        auctionStatus = AuctionStatus.PROGRESS;
    }

    public void markToFinish(Long endPrice) {
        auctionStatus = AuctionStatus.COMPLETED;
        auctionEndPrice = endPrice;
    }

    public void updateEndPrice(Long auctionEndPrice) {
        this.auctionEndPrice = auctionEndPrice;
    }
}
