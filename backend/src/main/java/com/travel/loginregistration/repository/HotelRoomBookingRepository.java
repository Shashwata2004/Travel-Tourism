package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.HotelRoomBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HotelRoomBookingRepository extends JpaRepository<HotelRoomBooking, UUID> {

    @Query("select coalesce(sum(b.roomsBooked),0) " +
           "from HotelRoomBooking b " +
           "where b.roomId = :roomId and b.checkIn < :checkOut and b.checkOut > :checkIn")
    Integer sumBookedBetween(@Param("roomId") UUID roomId,
                              @Param("checkIn") LocalDate checkIn,
                              @Param("checkOut") LocalDate checkOut);

    List<HotelRoomBooking> findByRoomIdOrderByCheckInAsc(UUID roomId);

    List<HotelRoomBooking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<HotelRoomBooking> findByUserIdOrUserEmailOrderByCreatedAtDesc(UUID userId, String userEmail);
}
