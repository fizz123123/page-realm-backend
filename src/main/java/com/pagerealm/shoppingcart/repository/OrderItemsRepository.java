package com.pagerealm.shoppingcart.repository;

import com.pagerealm.shoppingcart.entity.OrderItems;
import com.pagerealm.shoppingcart.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

    @Query("select i.nameSnapshot as name, count(i) as qty " +
            "from Orders o join o.items i " +
            "where o.status in :statuses and o.createdAt between :from and :to " +
            "group by i.nameSnapshot " +
            "order by count(i) desc")
    List<TopBookRow> topBooksByCount(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    interface TopBookRow {
        String getName();
        long getQty();
    }
}
