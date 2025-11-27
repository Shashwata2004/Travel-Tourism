package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.HotelRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HotelRoomRepository extends JpaRepository<HotelRoom, UUID> {
    List<HotelRoom> findByHotelIdOrderByNameAsc(UUID hotelId);
    void deleteByHotelId(UUID hotelId);

    @Query("select coalesce(sum(r.availableRooms),0) from HotelRoom r where r.hotelId = :hotelId")
    Integer sumAvailableByHotel(@Param("hotelId") UUID hotelId);

    @Query("select coalesce(sum(r.totalRooms),0) from HotelRoom r where r.hotelId = :hotelId")
    Integer sumCapacityByHotel(@Param("hotelId") UUID hotelId);
}
